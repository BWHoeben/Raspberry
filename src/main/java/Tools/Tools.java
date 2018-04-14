package Tools;

import UDP.Destination;
import UDP.Download;
import UDP.Upload;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Tools {

    private static int packetLength = 2048 * 8;
    private static int slidingWindow = 50;
    private static int timeOut = 400;
    private static int port = 12555;

    private Tools() {

    }

    public static int getPort() {
        return port;
    }

    public static int getPacketLength() {
        return packetLength;
    }

    private static int getNumberOfPackets(int fileSize) {
        return (int) Math.ceil((double) fileSize / (packetLength - 10)) + 1;
    }

    public static byte[] intToByteArray (final int integer) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(integer);
            dos.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return bos.toByteArray();
    }

    public static byte[] appendBytes(byte[] originalArray, byte[] toAppend) {
        byte[] returnArray = new byte[originalArray.length + toAppend.length];
        System.arraycopy(originalArray, 0, returnArray, 0, originalArray.length);
        System.arraycopy(toAppend, 0, returnArray, originalArray.length, toAppend.length);
        return returnArray;
    }

    public static byte[] appendThisMapToAnArray(Map<Integer, byte[]> map) {
     byte[] returnArray = new byte[0];
     int entriesInMap = map.size();
     int entriesObtained = 0;
     int i = 0;
     while (entriesObtained < entriesInMap) {
         if (map.containsKey(i)) {
             returnArray = appendBytes(returnArray, map.get(i));
             entriesObtained++;
         }
     i++;
     }
        return returnArray;
    }

    public static HashSet<String> getFilenames() {
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

    public static byte[] getFileContents(String fileName) throws IOException {
        print("Start reading file...");
            try (FileChannel channel = new FileInputStream(fileName).getChannel()) {
                MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                return data;
            }
    }

    public static byte[] createInitialPacketContentForUpload(String fileName, Destination destination,
                                                             byte[] data, DatagramSocket socket,
                                                             Map<Byte, Upload> uploads) {
        int fileSize = data.length;
        int numOfPkts = getNumberOfPackets(fileSize);
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.INITUP;
        arrays.put(0, first); // indicate that this is the initial packet for requested download
        arrays.put(1, Tools.intToByteArray(numOfPkts)); // total number of packets
        arrays.put(2, Tools.intToByteArray(fileSize)); // file size
        byte[] identifierByte = new byte[1];
        byte identifier = getIdentifierForUpload(uploads);
        identifierByte[0] = identifier;
        arrays.put(3, identifierByte); // identifier
        byte[] fileLengthIndicator = new byte[1];
        fileLengthIndicator[0] = (byte) fileName.getBytes().length;
        arrays.put(4, fileLengthIndicator);
        arrays.put(5, fileName.getBytes()); // fileName

        Upload upload = new Upload(destination, data);
        upload.setParameters(fileName, numOfPkts, fileSize, identifier, packetLength);
        uploads.put(identifier, upload);
        upload.initialize(socket, slidingWindow, timeOut);
        return Tools.appendThisMapToAnArray(arrays);
    }

    private static byte getIdentifierForUpload(Map<Byte, Upload> uploads) {
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

    public static void handleInitialPacket(DatagramPacket packet, Map<Byte, Download> downloads, DatagramSocket socket) {
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
        download.setParameters(new String(fileName), totalPkts, fs, identifier, Tools.getPacketLength());
        download.initializeWrite();
        download.pktTransfered(0);
        downloads.put(identifier, download);
        sendAcknowledgement(packet.getAddress(), packet.getPort(), 0, (byte) identifier, socket);
        print("Downloading...");
    }

    private static void sendAcknowledgement(InetAddress address, int port, int pktNumber, byte identifier, DatagramSocket socket) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.ACKDOWN;
        arrays.put(0, first); // indicate that this is an acknowledgement
        byte[] identifierByte = new byte[1];
        identifierByte[0] = identifier;
        arrays.put(1, identifierByte); // identifier
        arrays.put(2, Tools.intToByteArray(pktNumber)); // pktNumber
        byte[] ack = Tools.appendThisMapToAnArray(arrays);
        DatagramPacket pkt = new DatagramPacket(ack, ack.length, address, port);
        try {
            socket.send(pkt);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    public static boolean processDownloadPacket(DatagramPacket packet, Map<Byte, Download> downloads, DatagramSocket socket) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        Download download = downloads.get(identifier);
        byte[] pktNum = Arrays.copyOfRange(data, 2, 6);
        int pktNumber = ByteBuffer.wrap(pktNum).getInt();
        //print("Server send packet " + pktNumber +  " for download " + identifier + " with size: " + packet.getData().length);
        Tools.sendAcknowledgement(packet.getAddress(), packet.getPort(), pktNumber, identifier, socket);
        download.pktTransfered(pktNumber);
        byte[] dataLen = Arrays.copyOfRange(data, 6, 10);
        int dataLength = ByteBuffer.wrap(dataLen).getInt();
        byte[] receivedData = Arrays.copyOfRange(data, 10, 10 + dataLength);
        download.addData(pktNumber, receivedData);

        // This method returns true if download is complete
        if (download.isComplete()) {
            downloads.remove(identifier);
            return true;
        } else {
            return false;
        }
    }

    public static void processAcknowledgement(DatagramPacket packet, Map<Byte, Upload> uploads) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        byte[] pktNum = Arrays.copyOfRange(packet.getData(), 2, 6);
        int pktNumber = ByteBuffer.wrap(pktNum).getInt();
        Upload upload = uploads.get(identifier);
        if (pktNumber != 0) {
            upload.cancelTimerForPacket(pktNumber);
        }
        //print("Acknowledgement received. Packet#: " + pktNumber + " Identifier: " + identifier);
        upload.pktTransfered(pktNumber);
        upload.acknowledgePacket(pktNumber);
        if (upload.isComplete()) {
            upload.cancelAllTimers();
            print("Upload completed!");
            print("Time outs: " + upload.getTimeOuts());
            uploads.remove(identifier);
        } else {
            upload.continueUpload();
        }
    }


    private static void print(String msg) {
        System.out.println(msg);
    }

}
