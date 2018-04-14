package Client;

import Tools.Tools;
import UDP.Destination;
import UDP.Download;
import UDP.Upload;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private DatagramSocket socket = null;
    private Scanner scanner = new Scanner(System.in);
    private HashMap<Byte, Download> downloads = new HashMap<>();
    private HashMap<Byte, Upload> uploads = new HashMap<>();
    private boolean listen = false;

    public static void main(String[] args) {
        new Client();
    }

    private Client() {
        InetAddress address = askForHost();
        print("Inet address created.");
        upOrDown(address);
    }

    private void upload(Destination destination) {
        while (true) {
            print("Enter the name of the file you want to upload or press the return key to list available files.");
            String answer = scanner.nextLine();
            if (answer.isEmpty()) {
                print("Listing files...");
                listFiles();
            } else {
                try {
                    byte[] fileContent = Tools.getFileContents(answer);
                    startUpload(destination, fileContent, answer);
                    break;
                } catch (IOException e) {
                    print("File could not be read. Try again.");
                }
            }
        }
    }

    private void startUpload(Destination destination, byte[] fileContent, String filename) {
        print("Uploading: " + filename);
        byte[] packetContent = Tools.createInitialPacketContentForUpload(filename, destination, fileContent, socket, uploads);
        DatagramPacket initialPacket = new DatagramPacket(packetContent, packetContent.length, destination.getAddress(), destination.getPort());
        try {
            if (socket == null) {
                socket = new DatagramSocket();
            }
            socket.send(initialPacket);
            listenForPackets(socket);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    private void listFiles() {
        HashSet<String> fileNames = Tools.getFilenames();
        for (String filename : fileNames) {
            print(filename);
        }
    }

    private void listenForPackets(DatagramSocket socket) {
        print("Listening for incoming packets");
        listen = true;
        byte[] buf = new byte[Tools.getPacketLength()];
        while (listen) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                print(e.getMessage());
            }
            handlePacket(packet);
        }
    }

    private void handlePacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte ind = data[0];
        switch (ind) {
            case 0:
                print("Server send list of files:");
                processList(data, packet.getAddress());
                break;
            case 1:
                print("Server send initial packet for requested download");
                Tools.handleInitialPacket(packet, downloads, socket);
                break;
            case 2:
                downloadPacket(packet);
                break;
            case 3:
                print("Server send acknowledgement for upload");
                Tools.processAcknowledgement(packet, uploads);
                break;
            default:
                print("Unknown message received by server");
                break;

        }
    }

    private void downloadPacket(DatagramPacket packet) {
        boolean downloadComplete = Tools.processDownloadPacket(packet, downloads, socket);
        if (downloadComplete) {
            if (downloads.size() + uploads.size() == 0) {
                listen = false;
            }
            print("Download complete");
            askForTerminate(packet.getAddress());
        }
    }

    private void askForTerminate(InetAddress address) {
        print("Terminate connection? (yes/no)");
        String answer = scanner.nextLine();
        while (true) {
            if (answer.equalsIgnoreCase("yes")) {
                print("Disconnecting...");
                break;
            } else if (answer.equalsIgnoreCase("no")) {
                upOrDown(address);
                break;
            } else {
                print("Unknown answer, please provide 'yes' or 'no'.");
                print("Terminate connection? (yes/no)");
            }
        }
    }

    private void processList(byte[] data, InetAddress address) {
        boolean read = true;
        int filePointer = 1;
        int indicator = 0;
        HashSet<String> fileNames = new HashSet<>();
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
                Destination destination = new Destination(Tools.getPort(), address);
                upload(destination);
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
        byte[] buf = new byte[Tools.getPacketLength()];

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
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Tools.getPort());
            socket.send(packet);
        } catch (IOException e) {
            print(e.getMessage());
        }
        listenForPackets(socket);
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


