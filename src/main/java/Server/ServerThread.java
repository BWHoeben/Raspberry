package Server;

import Tools.HandleHashThread;
import Tools.Protocol;
import Tools.Tools;
import UDP.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ServerThread extends Thread {

    private DatagramSocket socket;
    private HashMap<Byte, Download> downloads = new HashMap<>();
    private HashMap<Byte, Upload> uploads = new HashMap<>();
    private HashMap<Byte, HandleHashThread> hashThreads = new HashMap<>();

    public ServerThread() throws IOException {
        this("BWH");
    }

    private ServerThread(String name) throws IOException {
        super(name);

        int port = Tools.getPort();
        try {
            socket = new DatagramSocket(port);
            print("Created socket on port: " + port);
        } catch (SocketException e) {
            print("Could not create socket on port: " + port);
            print(e.getMessage());
        }
    }

    @Override
    public void run() {

        String host = null;
        try {
            host = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            print(e.getMessage());
        }
        print("Localhost = " + host);
        while (true) {
            byte[] buf = new byte[Tools.getPacketLength()];
            DatagramPacket packetReceived = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packetReceived);
            } catch (IOException e) {
                print(e.getMessage());
            }
            handlePacketFromClient(packetReceived, socket);
        }
    }

    private void handlePacketFromClient(DatagramPacket packet, DatagramSocket socket) {
        byte[] dataFromClient = packet.getData();
        byte firstByte = dataFromClient[0];
        switch (firstByte) {
            case Protocol.SHOWFILES:
                print("Client requested list of files.");
                listFiles(packet, socket);
                break;
            case Protocol.INITUP:
                print("Client wants to upload.");
                Tools.handleInitialPacket(packet, downloads, socket);
                break;
            case Protocol.REQDOWN:
                startUpload(packet);
                break;
            case Protocol.ADDUP:
                downloadPacket(packet);
                break;
            case Protocol.ACKDOWN:
                Tools.processAcknowledgement(packet, uploads);
                break;
            case Protocol.PAUSE:
                print("Client wants to pauze file transfer.");
                pauseTransfer(packet);
                break;
            case Protocol.RESUME:
                print("Client wants to resume file transfer.");
                resumeTransfer(packet);
                break;
            case Protocol.ABORT:
                print("Client wants to abort file transfer.");
                abortTransfer(packet);
                break;
            case Protocol.HASHACK:
                Tools.handleHashAck(hashThreads, dataFromClient);
                break;
            case Protocol.HASH:
                Tools.processHash(packet, downloads, socket);
                break;
            default:
                print("Received message does not adhere to protocol. Discarding message.");
                break;
        }
    }

    private void downloadPacket(DatagramPacket packet) {
        boolean downloadComplete = Tools.processDownloadPacket(packet, downloads, socket);
        if (downloadComplete) {
            print("Download complete");
        }
    }

    private void abortTransfer(DatagramPacket packet) {
        byte[] msg = packet.getData();
        byte identifier = msg[1];
        Upload upload = uploads.get(identifier);
        upload.abort();
        uploads.remove(identifier);
    }

    private void resumeTransfer(DatagramPacket packet) {
        byte[] msg = packet.getData();
        byte identifier = msg[1];
        Upload upload = uploads.get(identifier);
        upload.resume();
    }

    private void pauseTransfer(DatagramPacket packet) {
        byte[] msg = packet.getData();
        byte identifier = msg[1];
        Upload upload = uploads.get(identifier);
        upload.pause();
    }

    private void startUpload(DatagramPacket packet) {
        byte[] message = packet.getData();
        int filenameLengthIndicator = (int) message[1];
        byte[] filenameBytes = new byte[filenameLengthIndicator];
        for (int i = 0; i < filenameLengthIndicator; i++) {
            filenameBytes[i] = message[i + 2];
        }
        String fileName = new String(filenameBytes);
        print("Client requested: " + fileName);
        Destination destination = new Destination(packet.getPort(), packet.getAddress());
        byte[] packetContent = Tools.createInitialPacketContentForUpload(fileName, destination, socket, uploads, hashThreads);
        DatagramPacket initialPacket = new DatagramPacket(packetContent, packetContent.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(initialPacket);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    private void sendInvalidReq(DatagramPacket packet, byte[] filename, int fileLengthIndicator) {
        byte[] packetToSend = new byte[Tools.getPacketLength()];
        packetToSend[0] = Protocol.INVALIDREQ;
        packetToSend[1] = (byte) fileLengthIndicator;
        for (int i = 0; i < fileLengthIndicator; i++) {
            packetToSend[2 + i] = filename[i];
        }
    }

    private byte getIdentifierForDownload() {
        boolean loop = true;
        byte b = 0;
        if (downloads.size() > 1) {
            while (loop) {
                if (downloads.containsKey(b)) {
                    b++;
                } else {
                    loop = false;
                }
            }
        }
        return b;
    }

    private void listFiles(DatagramPacket packet, DatagramSocket socket) {
        HashSet<String> fileNames = Tools.getFilenames();
        int bytesNeeded = 1;
        for (String fileName : fileNames) {
            bytesNeeded = bytesNeeded + 1 + fileName.getBytes().length;
        }
        byte[] buf = new byte[bytesNeeded];
        buf[0] = Protocol.LISTOFFILES; // indicate that this is a list of files
        int filePointer = 1;
        for (String fileName : fileNames) {
            byte[] fileNameBytes = fileName.getBytes();
            buf[filePointer] = (byte) fileNameBytes.length;
            filePointer++;
            for (int i = 0; i < fileNameBytes.length; i++) {
                buf[filePointer] = fileNameBytes[i];
                filePointer++;
            }
        }
        DatagramPacket dp = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(dp);
        } catch (IOException e) {
            print(e.getMessage());
        }

    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
