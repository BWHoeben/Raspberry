package Server;

import Tools.Tools;
import UDP.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class ServerThread extends Thread {

    private int packetLength = 2048 * 8;
    private DatagramSocket socket;
    private HashMap<Byte, Download> downloads = new HashMap<>();
    private HashMap<Byte, Upload> uploads = new HashMap<>();
    private int slidingWindow = 50;
    private HashMap<Byte, PacketState[]> packetStates = new HashMap<>();
    private HashMap<Byte, HashMap<Integer, Timer>> timers = new HashMap<>();
    private int timeOut = 1;
    private UploadThread ut;

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
            byte[] buf = new byte[packetLength];
            DatagramPacket packetReceived = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packetReceived);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handlePacketFromClient(packetReceived, socket);
        }
    }

    private void handlePacketFromClient(DatagramPacket packet, DatagramSocket socket) {
        byte[] dataFromClient = packet.getData();
        byte firstByte = dataFromClient[0];
        switch (firstByte) {
            case 0:
                print("Client wants to upload.");
                break;
            case 1:
                print("Client requested list of files.");
                listFiles(packet, socket);
                break;
            case 2:
                startUpload(packet);
                break;
            case 3:
                processAcknowledgement(packet);
                break;
            default:
                print("Received message does not adhere to protocol. Discarding message.");
                break;
        }
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
        byte[] fileContent = getFileContentsFast(fileName);
        int numOfPackets = (int) Math.ceil((double) fileContent.length / (packetLength - 10)) + 1;
        Destination destination = new Destination(packet.getPort(), packet.getAddress());
        byte[] packetContent = createInitialPacketContent(numOfPackets, fileContent.length, fileName, destination, fileContent);
        DatagramPacket initialPacket = new DatagramPacket(packetContent, packetContent.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(initialPacket);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    private byte[] createInitialPacketContent(int numOfPkts, int fileSize, String fileName, Destination destination, byte[] data) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = 1;
        arrays.put(0, first); // indicate that this is the initial packet for requested download
        arrays.put(1, Tools.intToByteArray(numOfPkts)); // total number of packets
        arrays.put(2, Tools.intToByteArray(fileSize)); // file size
        byte[] identifierByte = new byte[1];
        byte identifier = getIdentifierForUpload();
        identifierByte[0] = identifier;
        arrays.put(3, identifierByte); // identifier
        byte[] fileLengthIndicator = new byte[1];
        fileLengthIndicator[0] = (byte) fileName.getBytes().length;
        arrays.put(4, fileLengthIndicator);
        arrays.put(5, fileName.getBytes()); // fileName

        Upload upload = new Upload(destination, data);
        upload.setParameters(fileName, numOfPkts, fileSize, identifier, packetLength);
        uploads.put(identifier, upload);
        initializePacketStates(identifier, numOfPkts);
        return Tools.appendThisMapToAnArray(arrays);
    }

    private void initializePacketStates(byte identifier, int numberOfPkts) {
        PacketState[] states = new PacketState[numberOfPkts];
        for (int i = 0; i < numberOfPkts; i++) {
            states[i] = PacketState.INQUEUE;
        }
        packetStates.put(identifier, states);
    }

    private void processAcknowledgement(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        byte[] pktNum = Arrays.copyOfRange(packet.getData(), 2, 6);
        int pktNumber = ByteBuffer.wrap(pktNum).getInt();
        if (pktNumber != 0) {
            cancelTimerForPacket(identifier, pktNumber);
        }
        print("Acknowledgement received. Packet#: " + pktNumber + " Identifier: " + identifier);
        Upload upload = uploads.get(identifier);
        upload.pktTransfered(pktNumber);
        PacketState[] states = packetStates.get(identifier);
        states[pktNumber] = PacketState.ACKNOWLEDGED;
        if (upload.isComplete()) {
            print("Upload completed!");
            uploads.remove(identifier);
        } else {
            continueUpload(upload);
        }
    }

    public int packetsInTheAir(byte identifier) {
        PacketState[] states = packetStates.get(identifier);
        int counter = 0;
        for (PacketState state : states) {
            if (state.equals(PacketState.SEND)) {
                counter++;
            }
        }
            return counter;
    }

    public int[] getPacketsToTransmit(byte identifier, int numberOfPacketsToTransmit) {
        PacketState[] states = packetStates.get(identifier);
        int totalPkts = states.length;
        int i = 0;
        int pktsSelected = 0;
        int[] packetsToReturn = new int[numberOfPacketsToTransmit];
        while (i < totalPkts && pktsSelected < numberOfPacketsToTransmit) {
            if (states[i].equals(PacketState.INQUEUE) || states[i].equals(PacketState.TIMEDOUT)) {
                packetsToReturn[pktsSelected] = i;
                pktsSelected++;
            }
            i++;
        }
        if (pktsSelected != numberOfPacketsToTransmit) {
            packetsToReturn = Arrays.copyOfRange(packetsToReturn,0,pktsSelected);
        }
        return packetsToReturn;
    }

    private synchronized void continueUpload(Upload upload) {
        int numberOfPktsToTransmit = slidingWindow - packetsInTheAir(upload.getIdentifier());
        int[] packetsToTransmit = getPacketsToTransmit(upload.getIdentifier(), numberOfPktsToTransmit);
        Destination destination = upload.getDestination();
        for (int i = 0; i < packetsToTransmit.length; i++) {
            int pktNumber = packetsToTransmit[i];
            byte[] dataToSend = upload.getPacketData(pktNumber);
            DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, destination.getAddress(), destination.getPort());
            try {
                socket.send(packet);
                setTimerForPacket(upload.getIdentifier(), pktNumber);
                byte identifier = upload.getIdentifier();
                PacketState[] states = packetStates.get(identifier);
                states[pktNumber] = PacketState.SEND;
                print("Packet send: " + pktNumber);
            } catch (IOException e) {
                print(e.getMessage());
            }
        }
    }

    public void setTimerForPacket(byte identifier, int packetNumber) {
        HashMap<Integer, Timer> timeMap;
        if (timers.containsKey(identifier)) {
            timeMap = timers.get(identifier);
        } else {
            timeMap = new HashMap<>();
            timers.put(identifier, timeMap);
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                print("Packet " + packetNumber + " timed out");
                PacketState[] states = packetStates.get(identifier);
                states[packetNumber] = PacketState.TIMEDOUT;
                Upload upload = uploads.get(identifier);
                continueUpload(upload);
            }
        },
        timeOut * 1000
        );
        timeMap.put(packetNumber, timer);
    }

    private void cancelTimerForPacket(byte identifier, int packetNumber) {
        Timer timer = timers.get(identifier).get(packetNumber);
        timer.cancel();
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

    private byte getIdentifierForUpload() {
        boolean loop = true;
        byte b = 0;
        if (uploads.size() > 1) {
            while (loop) {
                if (uploads.containsKey(b)) {
                    b++;
                } else {
                    loop = false;
                }
            }
        }
        return b;
    }


    private byte[] createChecksum() {
        return null;
    }

    private HashSet<String> getFilenames() {
        File folder = new File(System.getProperty("user.dir"));
        File[] listOfFiles = folder.listFiles();
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                set.add(listOfFiles[i].getName());
            }
        }
        return set;
    }

    private void listFiles(DatagramPacket packet, DatagramSocket socket) {
        HashSet<String> fileNames = getFilenames();
        int bytesNeeded = 1;
        for (String fileName : fileNames) {
            bytesNeeded = bytesNeeded + 1 + fileName.getBytes().length;
        }
        byte[] buf = new byte[bytesNeeded];
        buf[0] = 0; // indicate that this is a list of files
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

    private byte[] getFileContentsFast(String fileName) {
        print("Start reading file...");

        try {
            try (FileChannel channel = new FileInputStream(fileName).getChannel()) {
                MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                return data;
            }
        } catch (IOException e) {
            print(e.getMessage());
        }
        return new byte[0];
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
