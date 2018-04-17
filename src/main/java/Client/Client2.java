package Client;

import Tools.Tools;
import Tools.Protocol;
import UDP.Destination;
import UDP.Download;
import UDP.Upload;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import Tools.HandleHashThread;
import com.nedap.university.Computer;

public class Client2 extends Thread implements Computer {

    private static DatagramSocket socket;
    private HashMap<Byte, Download> downloads = new HashMap<>();
    private static HashMap<Byte, Upload> uploads = new HashMap<>();
    private static HashMap<Byte, HandleHashThread> hashThreads = new HashMap<>();
    private static ListenThread lt;
    private InputThread it;
    private static Scanner scanner = new Scanner(System.in);

    public Client2(DatagramSocket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) {
        InetAddress address = askForHost();
        print("Inet address created.");
        upOrDown(address);
    }

    private static InetAddress askForHost() {
        while (true) {
            try {
                print("Provide host address or press the return key to use localhost.");
                String answer = scanner.nextLine();
                if (answer.isEmpty()) {
                    return InetAddress.getByName("localhost");
                } else {
                    return InetAddress.getByName(answer);
                }
            } catch (UnknownHostException e) {
                print("Unknown host. Try again");
            }
        }
    }

    private static void print(String msg) {
        System.out.println(msg);
    }

    private static void upOrDown(InetAddress address) {
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

    private static void upload(Destination destination) {
        while (true) {
            print("Enter the name of the file you want to upload or press the return key to list available files.");
            String answer = scanner.nextLine();
            if (answer.isEmpty()) {
                print("Listing files...");
                listFiles();
            } else {
                if (Tools.fileExists(answer)) {
                    startUpload(destination, answer);
                    break;
                } else {
                    print("No such file. Try again.");
                }
            }
        }
    }

    private static void listFiles() {
        HashSet<String> fileNames = Tools.getFilenames();
        for (String filename : fileNames) {
            print(filename);
        }
    }

    private static void startUpload(Destination destination, String filename) {
        print("Uploading: " + filename);
        try {
            if (socket == null) {
                socket = new DatagramSocket();
            }
            byte[] packetContent = Tools.createInitialPacketContentForUpload(filename, destination, socket, uploads, hashThreads);
            DatagramPacket initialPacket = new DatagramPacket(packetContent, packetContent.length, destination.getAddress(), destination.getPort());
            byte identifier = packetContent[9];
            socket.send(initialPacket);
            lt = new ListenThread(socket, new Client2(socket));
            lt.start();
            Upload upload = uploads.get(identifier);
            InputThread<Upload> it = new InputThread<>(scanner, identifier, destination, socket, upload, uploads);
            it.start();
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    public void handlePacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte ind = data[0];
        switch (ind) {
            case Protocol.LISTOFFILES:
                print("Server send list of files:");
                processList(data, packet.getAddress());
                break;
            case Protocol.INVALIDREQ:
                invalidReq(data, packet);
                break;
            case Protocol.INITUP:
                print("Server send initial packet for requested download");
                Tools.handleInitialPacket(packet, downloads, socket, scanner);
                //showOptions(packet);
                break;
            case Protocol.ADDUP:
                downloadPacket(packet);
                break;
            case Protocol.ACKDOWN:
                //print("Server send acknowledgement for upload");
                Tools.processAcknowledgement(packet, uploads);
                break;
            case Protocol.HASH:
                Tools.processHash(packet, downloads, socket);
                break;
            case Protocol.HASHACK:
                Tools.handleHashAck(hashThreads, data);
                break;
            default:
                print("Unknown message received by server. First byte is: " + data[0]);
                break;

        }
    }

    private void invalidReq(byte[] data, DatagramPacket packet) {
        int filenamelength = data[1];
        byte[] filenameBytes = new byte[filenamelength];
        for (int i = 0; i < filenamelength; i++) {
            filenameBytes[i] = data[2 + i];
        }
        String fileName = new String(filenameBytes);
        print("Server was not able to retrieve file: " + fileName);
        terminate();
    }

    private void downloadPacket(DatagramPacket packet) {
        boolean downloadComplete = Tools.processDownloadPacket(packet, downloads, socket);
        if (downloadComplete) {
            //if (downloads.size() + uploads.size() == 0) {
            //  listen = false;
            //}
            print("Download complete");
//            askForTerminate(packet.getAddress());
        }
    }

    private void terminate() {
/*        print("Terminate connection? (yes/no)");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.nextLine();
        while (true) {
            if (answer.equalsIgnoreCase("yes")) {
                print("Disconnecting...");
                break;
            } else if (answer.equalsIgnoreCase("no")) {
                upOrDown(address, scanner);
                break;
            } else {
                print("Unknown answer, please provide 'yes' or 'no'.");
                print("Terminate connection? (yes/no)");
            }
        }
  */
        print("Terminating connection...");
        lt.stopListening();
    }


    private static void requestFile(String filename, InetAddress address) {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            print(e.getMessage());
        }
        byte[] buf = new byte[Tools.getPacketLength()];

        if (filename.isEmpty()) {
            buf[0] = Protocol.SHOWFILES;
            buf[1] = 0;
        } else {
            buf[0] = Protocol.REQDOWN;
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
        lt = new ListenThread(socket, new Client2(socket));
        lt.start();

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

}
