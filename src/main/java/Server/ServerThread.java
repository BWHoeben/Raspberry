package Server;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Date;

public class ServerThread extends Thread {

    private DatagramSocket socket;
    private byte[] fileContent;
    public ServerThread() throws IOException {
        this("BWH");
    }

    private ServerThread(String name) throws IOException {
        super(name);

        print("Hello Bart! :)");

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
        int packetLength = 256;
        int numOfPackets = (int) Math.ceil((double) fileContent.length / packetLength);
        try {
        for (int i = 0; i < numOfPackets; i++) {
            byte[] buf = new byte[packetLength];

            //receive request
            DatagramPacket packetRecieved = new DatagramPacket(buf, buf.length);
            socket.receive(packetRecieved);

            // send response
            DatagramPacket packet = createPacket(fileContent, i , packetLength, packetRecieved.getAddress(), packetRecieved.getPort());
            socket.send(packet);
            } } catch (IOException e) {
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
    public DatagramPacket createPacket(byte[] fileContents, int ind, int packetLength, InetAddress address, int port) {
        byte[] packet = new byte[packetLength];
        int filePointer = ind * packetLength;
        System.arraycopy(fileContents, filePointer, packet, 0, packetLength);
        return new DatagramPacket(packet, packetLength, address, port);
    }
}
