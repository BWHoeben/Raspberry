package Client;

import Tools.Tools;
import Tools.Protocol;
import UDP.Destination;
import UDP.Download;
import UDP.PacketState;
import UDP.Upload;

import java.io.IOException;
import java.net.*;
import java.util.*;

import Tools.HandleHashThread;
import com.nedap.university.Computer;

public class Client2 implements Computer {

    private static DatagramSocket socket;
    private HashMap<Byte, Download> downloads = new HashMap<>();
    private static HashMap<Byte, Upload> uploads = new HashMap<>();
    private static HashMap<Byte, HandleHashThread> hashThreads = new HashMap<>();
    private static ListenThread lt;
    private static Timer  timerForReq;
    private static InputThread<Upload> it;

    private static Scanner scanner = new Scanner(System.in);

    private Client2(DatagramSocket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) {
        InetAddress address = Tools.getRaspberryAddress(1, 10);
        print("Connected to " + address.getHostName());
        upOrDown(address);
    }

    private static void print(String msg) {
        System.out.println(msg);
    }

    private static void upOrDown(InetAddress address) {
        while (true) {
            print("Download or upload? (d/u)");
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
                print("Unkown input. Please provide 'd' or 'u'.");
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
        Set<String> fileNames = Tools.getFilenames();
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
            it = new InputThread<>(scanner, identifier, destination, socket, upload, uploads);
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
                cancelTimerForReq();
                print("Server send list of files:");
                processList(data, packet.getAddress());
                break;
            case Protocol.INVALIDREQ:
                cancelTimerForReq();
                invalidReq(data);
                break;
            case Protocol.INITUP:
                cancelTimerForReq();
                print("Server send initial packet for requested download");
                Tools.handleInitialPacket(packet, downloads, socket, scanner);
                break;
            case Protocol.ADDUP:
                downloadPacket(packet);
                break;
            case Protocol.ACKDOWN:
                if (Tools.processAcknowledgement(packet, uploads)) {
                    terminate();
                }
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

    private void cancelTimerForReq() {
        timerForReq.cancel();
        timerForReq.purge();
    }

    private void invalidReq(byte[] data) {
        int filenameLength = data[1];
        byte[] filenameBytes = new byte[filenameLength];
        for (int i = 0; i < filenameLength; i++) {
            filenameBytes[i] = data[2 + i];
        }
        String fileName = new String(filenameBytes);
        print("Server was not able to retrieve file: " + fileName);
        terminate();
    }

    private void downloadPacket(DatagramPacket packet) {
        boolean downloadComplete = Tools.processDownloadPacket(packet, downloads, socket);
        if (downloadComplete) {
            print("Download complete");
            terminate();
        }
    }

    public void terminate() {
        print("Terminating connection...");
        lt.stopListening();
        if (it != null) {
            it.stopListening();
        }
        scanner.close();
    }


    private static void requestFile(String filename, InetAddress address) {
        timerForReq = new Timer();
        timerForReq.schedule(new TimerTask() {
                           @Override
                           public void run() {
                                requestFile(filename, address);
                           }
                       },
                Tools.getTimeOut()
        );
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
