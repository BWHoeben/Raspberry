package Client;

import UDP.Download;
import UDP.Upload;

import javax.jnlp.FileContents;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
    private int packetLength = 512;
    private Map<Integer, byte[]> data = new HashMap<>();
    private DatagramSocket socket = null;
    private Scanner scanner = new Scanner(System.in);
    private Map<Integer, Download> downloads = new HashMap<>();
    private Map<Integer, Upload> uploads = new HashMap<>();
    private Map<String, Integer> fileNames = new HashMap<>();
    private boolean isDownloading = false;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        InetAddress address = askForHost();
        print("Inet address created.");
        upOrDown(address);
    }

    private void handleInitialPacket(byte[] packet, Download download) {
        byte[] numOfPkts = Arrays.copyOfRange(packet, 4, 8);
        int totalPkts = ByteBuffer.wrap(numOfPkts).getInt();
        print("Total number of packets: " + totalPkts);
        byte[] fileSize = Arrays.copyOfRange(packet, 8, 12);
        int fs = ByteBuffer.wrap(fileSize).getInt();
        print("File size: " + fs + " bytes");
        int identifier = (int) packet[12];
        print("Identifier: " + identifier);
        int fileLengthIndicator = (int) packet[13];
        byte[] fileName = Arrays.copyOfRange(packet, 14, 14 + fileLengthIndicator);
        print("File name: " + new String(fileName));
        download.setParameters(new String(fileName), totalPkts, fs, identifier);
    }

    private void handlePacket(int packetNumber, byte[] data) {
        int identifier = (int) data[4];
        Download download = downloads.get(identifier);
        byte[] dataToAdd = stripHeader(5, data);
        download.pktTransfered(packetNumber);
        download.addData(packetNumber, dataToAdd);
        if (download.isComplete()) {
            downloads.remove(identifier);
            download.writeArrayToFile();
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

    public void startDownloadProcess(InetAddress address) {
        isDownloading = true;
        Download download = new Download();
        downloads.put(0, download);
        try {
            try {
                socket = new DatagramSocket();
                while (downloads.size() > 0) {
                    byte[] buf = new byte[packetLength];
                    buf[0] = 1;
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
                    socket.send(packet);
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    print("Packet recieved");

                    byte[] firstFour = Arrays.copyOfRange(packet.getData(), 0, 4);
                    int packetNum = ByteBuffer.wrap(firstFour).getInt();
                    if (packetNum == 0) {
                        handleInitialPacket(packet.getData(), download);
                    } else {
                        handlePacket(packetNum, packet.getData());
                    }
                }
            } catch (IOException e) {
                print(e.getMessage());
            }

        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        isDownloading = false;
    }

    private void upOrDown(InetAddress address) {
        while (true) {
            print("Download or upload? (u/d)");
            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("u")) {
                upload(address);
                break;
            } else if (answer.equalsIgnoreCase("d")) {
                if (!isDownloading) {
                    startDownloadProcess(address);
                } else {
                    addDownload(address);
                }
                break;
            } else {
                print("Unkown input. Please provide 'u' or 'd'.");
            }
        }
    }

    public void addDownload(InetAddress address) {
        //Todo
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


