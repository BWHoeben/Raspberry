package Client;

import Tools.Tools;
import UDP.Download;
import UDP.Upload;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
    private int packetLength = 2048 * 8;
    private Map<Integer, byte[]> data = new HashMap<>();
    private DatagramSocket socket = null;
    private Scanner scanner = new Scanner(System.in);
    private Map<Byte, Download> downloads = new HashMap<>();
    private Map<Byte, Upload> uploads = new HashMap<>();
    private boolean listen = false;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        InetAddress address = askForHost();
        print("Inet address created.");
        upOrDown(address);
    }

    private void handleInitialPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        Download download = new Download();
        byte[] numOfPkts = Arrays.copyOfRange(data, 1, 5);
        int totalPkts = ByteBuffer.wrap(numOfPkts).getInt();
        print("Total number of packets: " + totalPkts);
        byte[] fileSize = Arrays.copyOfRange(data, 5, 9);
        int fs = ByteBuffer.wrap(fileSize).getInt();
        print("File size: " + fs + " bytes");
        byte identifier = data[9];
        print("Identifier: " + identifier);
        int fileLengthIndicator = (int) data[10];
        byte[] fileName = Arrays.copyOfRange(data, 11, 11 + fileLengthIndicator);
        print("File name: " + new String(fileName));
        download.setParameters(new String(fileName), totalPkts, fs, identifier, packetLength);
        download.initializeWrite();
        download.pktTransfered(0);
        downloads.put(identifier, download);
        sendAcknowledgement(packet.getAddress(), packet.getPort(), 0, (byte) identifier);
    }

    private void sendAcknowledgement(InetAddress address, int port, int pktNumber, byte identifier) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = 3;
        arrays.put(0, first); // indicate that this is an acknowledgement
        byte[] identifierByte = new byte[1];
        identifierByte[0] = identifier;
        arrays.put(1, identifierByte); // identifier
        arrays.put(2, Tools.intToByteArray(pktNumber)); // pktNumber
        byte[] ack = Tools.appendThisMapToAnArray(arrays);
        DatagramPacket pkt = new DatagramPacket(ack, ack.length, address, port);
        // print("Sending ack for pkt: " + pktNumber);
        try {
            socket.send(pkt);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    public byte[] stripHeader(int headerLength, byte[] data) {
        return Arrays.copyOfRange(data, headerLength, data.length);
    }

    private void upload(InetAddress address) {
        byte[] buf = new byte[packetLength];
        //buf[0] = (byte) 1;
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
        try {
            socket.send(packet);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    public void listenForPackets(InetAddress address, int port, DatagramSocket socket) {
        print("Listening for incoming packets");
        listen = true;
        byte[] buf = new byte[packetLength];
        while (listen) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handlePacket(packet);
        }
    }

    public void handlePacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte ind = data[0];
        switch (ind) {
            case 0:
                print("Server send list of files:");
                processList(data, packet.getAddress());
                break;
            case 1:
                print("Server send initial packet for requested download");
                handleInitialPacket(packet);
                break;
            case 2:
                processDownloadPacket(packet);
                break;
            case 3:
                print("Server send acknowledgement for upload");
                //Todo
                break;
            default:
                print("Unknown message received by server");
                break;

        }
    }

    private void processDownloadPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        Download download = downloads.get(identifier);
        byte[] pktNum = Arrays.copyOfRange(data, 2, 6);
        int pktNumber = ByteBuffer.wrap(pktNum).getInt();
        print("Server send packet " + pktNumber +  " for download " + identifier + " with size: " + packet.getData().length);
        download.pktTransfered(pktNumber);
        byte[] dataLen = Arrays.copyOfRange(data, 6, 10);
        int dataLength = ByteBuffer.wrap(dataLen).getInt();
        byte[] receivedData = Arrays.copyOfRange(data, 10, 10 + dataLength);
        download.addData(pktNumber, receivedData);
        sendAcknowledgement(packet.getAddress(), packet.getPort(), pktNumber, identifier);
        if (download.isComplete()) {
            downloads.remove(identifier);
            if (downloads.size() + uploads.size() == 0) {
                listen = false;
            }
            print("Download complete!");
        }
    }

    private void processList(byte[] data, InetAddress address) {
        boolean read = true;
        int filePointer = 1;
        int indicator = 0;
        HashSet<String> fileNames = new HashSet<String>();
        while (read) {
            indicator = (int) data[filePointer];
            if (indicator <= 0) {
                read = false;
            } else {
                filePointer++;
                byte[] buf = new byte[indicator];
                for (int i = 0; i < indicator; i++) {
                    buf[i] = data[filePointer];
                    filePointer++;
                }
                String fileName = new String(buf);
                fileNames.add(fileName);
                print("File : " + fileName);
            }
        }
        print("Enter the name fo the file you want to download or press the return key to cancel.");
        while (true) {
            String answer = scanner.nextLine();
            if (answer.isEmpty()) {
                print("Cancelling download process.");
                break;
            } else if (fileNames.contains(answer)) {
                print("Requesting " + answer);
                requestFile(answer, address);
                break;
            } else {
                print("Unknown file, try again.");
            }
        }
    }

    private void upOrDown(InetAddress address) {
        while (true) {
            print("Download or upload? (u/d)");
            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("u")) {
                upload(address);
                break;
            } else if (answer.equalsIgnoreCase("d")) {
                print("Please provide the name of the file you want to download or press the return key to get a list of available files...");
                answer = scanner.nextLine();
                if (answer.isEmpty()) {
                    print("Requesting available files...");
                    requestFile("", address);
                } else {
                    print("Requesting file: " + answer);
                    requestFile(answer, address);
                }
                break;
            } else {
                print("Unkown input. Please provide 'u' or 'd'.");
            }
        }
    }

    private void requestFile(String filename, InetAddress address) {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            print(e.getMessage());
        }
        byte[] buf = new byte[packetLength];

        if (filename.isEmpty()) {
            buf[0] = 1;
            buf[1] = 0;
        } else {
            buf[0] = 2;
            byte[] filenameArray = filename.getBytes();
            buf[1] = (byte) filenameArray.length;
            for (int i = 0; i < filenameArray.length; i++) {
                buf[2 + i] = filenameArray[i];
            }
        }
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
            socket.send(packet);
        } catch (IOException e) {
            print(e.getMessage());
        }
        listenForPackets(address, 12555, socket);
    }

    private InetAddress askForHost() {
        while (true) {
            try {
                print("Do you want to use local host? (yes/no)");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("yes")) {
                    return InetAddress.getByName("localhost");
                } else if (answer.equalsIgnoreCase("no")) {
                    print("Provide host address: ");
                    String addressString = scanner.nextLine();
                    return InetAddress.getByName(addressString);
                } else {
                    print("Unkown input. Please provide 'yes' or 'no'.");
                }

            } catch (UnknownHostException e) {
                print("Unknown host. Try again");
            }
        }
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}


