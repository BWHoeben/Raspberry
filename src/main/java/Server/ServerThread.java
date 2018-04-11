package Server;

import Tools.Tools;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ServerThread extends Thread {

    private int packetLength = 512;
    private DatagramSocket socket;
    private byte[] fileContent;
    private HashSet<Integer> fileIdentifiers;

    public ServerThread() throws IOException {
        this("BWH");
    }

    private ServerThread(String name) throws IOException {
        super(name);

        int port = 12555;
        try {
            socket = new DatagramSocket(port);
            print("Created socket on port: " + port);
        } catch (SocketException e) {
            System.err.println("Could not create socket on port: " + port);
            print(e.getMessage());
        }
        try {
            // use this on one pi
            //in = new BufferedReader(new FileReader("/home/pi/test.txt"));

            this.fileContent = getFileContents("test.txt");
            print("file found!");
        } catch (FileNotFoundException e) {
            System.err.println("Could not open file.");
            File folder = new File(System.getProperty("user.dir"));
            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    print("File " + listOfFiles[i].getName());
                } else if (listOfFiles[i].isDirectory()) {
                    print("Directory " + listOfFiles[i].getName());
                }
            }

        }
    }

    @Override
    public void run() {

        String host = null;
        try {
            host = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("Localhost = " + host);
        int numOfPackets = (int) Math.ceil((double) fileContent.length / packetLength);
        try {
            for (int i = 0; i <= numOfPackets; i++) {
                byte[] buf = new byte[packetLength];
                DatagramPacket packetRecieved = new DatagramPacket(buf, buf.length);
                socket.receive(packetRecieved);
                byte[] dataFromClient = packetRecieved.getData();
                if (dataFromClient[0] == 0) {
                    print("Client wants to upload");
                } else if (dataFromClient[0] == 1) {
                    print("Client wants to download");
                } else {
                    print("Corrupt message");
                }

                byte[] packet;
                if (i == 0) {
                    packet = createInitialPacketContent(numOfPackets, fileContent.length, (byte) 0, "test.txt");
                } else {
                    packet = createPacketContent(i, (byte) 0, fileContent);
                }

                // send response
                DatagramPacket dp = new DatagramPacket(packet, packet.length, packetRecieved.getAddress(), packetRecieved.getPort());
                socket.send(dp);
            }
        } catch (IOException e) {
            print(e.getMessage());
        }
        socket.close();
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    private byte[] getFileContents(String fileName) throws FileNotFoundException {
        File fileToTransmit = new File(fileName);
        try (FileInputStream fileInputStream = new FileInputStream(fileToTransmit)) {
            byte[] fileContents = new byte[(int) fileToTransmit.length()];
            for (int i = 0; i < fileContents.length; i++) {
                int nextByte = fileInputStream.read();
                fileContents[i] = (byte) nextByte;
            }
            return fileContents;
        } catch (Exception e) {
            print(e.getMessage());
            return null;
        }

    }

    private DatagramPacket createPacket(byte[] fileContents, int ind, int packetLength, InetAddress address, int port) {
        byte[] packet = new byte[packetLength];
        int filePointer = ind * packetLength;
        System.arraycopy(fileContents, filePointer, packet, 0, packetLength);
        return new DatagramPacket(packet, packetLength, address, port);
    }

    private byte[] createInitialPacketContent(int numOfPkts, int fileSize, byte identifier, String fileName) {
        Map<Integer, byte[]> arrays = new HashMap<Integer, byte[]>();
        arrays.put(0, Tools.intToByteArray(0)); // the packet number
        arrays.put(1, Tools.intToByteArray(numOfPkts)); // total number of packets
        arrays.put(2, Tools.intToByteArray(fileSize)); // file size
        byte[] identifierByte = new byte[1];
        identifierByte[0] = (byte) identifier;
        arrays.put(3, identifierByte); // identifier
        byte[] fileLengthIndicator = new byte[1];
        fileLengthIndicator[0] = (byte) fileName.getBytes().length;
        arrays.put(4, fileLengthIndicator);
        arrays.put(5, fileName.getBytes()); // fileName
        return Tools.appendThisMapToAnArray(arrays);
    }

    private byte[] createPacketContent(int pktNum, int identifier, byte[] data) {
        Map<Integer, byte[]> arrays = new HashMap<Integer, byte[]>();
        arrays.put(0, Tools.intToByteArray(pktNum)); // the packet number
        byte[] identifierByte = new byte[1];
        identifierByte[0] = (byte) identifier;
        arrays.put(1, identifierByte); // identifier
        byte[] header = Tools.appendThisMapToAnArray(arrays);
        int bytesLeft = packetLength - header.length;
        byte[] dataToSend = Arrays.copyOfRange(data, packetLength * (pktNum - 1), (packetLength * pktNum) - 1);
        return Tools.appendBytes(header, dataToSend);
    }

    private byte[] createChecksum() {
        return null;
    }

    private byte[] writeHeader() {
        return null;
    }


}
