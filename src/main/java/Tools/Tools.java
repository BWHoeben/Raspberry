package Tools;

import Client.InputThread;
import UDP.Destination;
import UDP.Download;
import UDP.Upload;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Tools {

    /*
        Recommended values:
            packetLength    =   65000
            slidingWindow   =      25
            timeOut         =    1000
     */

    private static int packetLength = 65000;
    private static int slidingWindow = 25;
    private static int timeOut = 1000;
    private static int port = 12555;

    private Tools() {

    }

    public static int getPort() {
        return port;
    }

    public static int getPacketLength() {
        return packetLength;
    }

    public static byte[] intToByteArray(final int integer) {
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

    public static Set<String> getFilenames() {
        File folder = new File(System.getProperty("user.dir") + "/home/pi/");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            folder = new File(System.getProperty("user.dir"));
            listOfFiles = folder.listFiles();
        }

        HashSet<String> set = new HashSet<>();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    set.add(file.getName());
                }
            }
        }
        return set;
    }

    public static byte[] createInitialPacketContentForUpload(String fileName, Destination destination,
                                                             DatagramSocket socket,
                                                             Map<Byte, Upload> uploads, Map<Byte, HandleHashThread> hashThreads) {
        Upload upload = new Upload(destination);
        byte identifier = getIdentifierForUpload(uploads);

        HandleHashThread hht = new HandleHashThread(fileName, destination, socket, identifier);
        hashThreads.put(identifier, hht);
        hht.start();

        upload.initializeFileRead(fileName);
        upload.setParameters(fileName, identifier, packetLength);

        uploads.put(identifier, upload);
        print("Number of clients: " + uploads.size());
        upload.initialize(socket, slidingWindow, timeOut);


        int fileSize = upload.getFileSize();
        int numOfPkts = upload.getNumberOfPkts();
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.INITUP;
        arrays.put(0, first); // indicate that this is the initial packet for requested download
        arrays.put(1, Tools.intToByteArray(numOfPkts)); // total number of packets
        arrays.put(2, Tools.intToByteArray(fileSize)); // file size
        byte[] identifierByte = new byte[1];
        identifierByte[0] = identifier;
        arrays.put(3, identifierByte); // identifier
        byte[] fileLengthIndicator = new byte[1];
        fileLengthIndicator[0] = (byte) fileName.getBytes().length;
        arrays.put(4, fileLengthIndicator);
        arrays.put(5, fileName.getBytes()); // fileName


        return Tools.appendThisMapToAnArray(arrays);
    }

    private static byte getIdentifierForUpload(Map<Byte, Upload> uploads) {
        boolean loop = true;
        byte b = 0;
        if (uploads.size() > 0) {
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

    public static void handleInitialPacket(DatagramPacket packet, Map<Byte, Download> downloads, DatagramSocket socket, Scanner scanner) {
        byte[] data = packet.getData();
        byte[] numOfPkts = Arrays.copyOfRange(data, 1, 5);
        int totalPkts = ByteBuffer.wrap(numOfPkts).getInt();
        print("Total number of packets: " + totalPkts);
        byte[] fileSize = Arrays.copyOfRange(data, 5, 9);
        int fs = ByteBuffer.wrap(fileSize).getInt();
        print("File size: " + fs + " bytes");
        byte identifier = data[9];
        print("Identifier: " + identifier);
        Download download;
        if (!downloads.containsKey(identifier)) {
            download = new Download();
        } else {
            download = downloads.get(identifier);
        }
        int fileLengthIndicator = (int) data[10];
        byte[] fileName = Arrays.copyOfRange(data, 11, 11 + fileLengthIndicator);
        print("File name: " + new String(fileName));
        download.setNumOfTotalPkts(totalPkts);
        download.setParameters(new String(fileName), identifier, Tools.getPacketLength(), fs);
        download.initializeWrite();
        download.pktTransferred(0);
        downloads.put(identifier, download);
        sendAcknowledgement(packet.getAddress(), packet.getPort(), 0, (byte) identifier, socket);
        print("Downloading...");
        if (scanner != null) {
            Destination destination = new Destination(packet.getPort(), packet.getAddress());
            InputThread<Download> it = new InputThread(scanner, identifier, destination, socket, download, downloads);
            download.setInputThread(it);
            it.start();
        }
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

    private static void sendAcknowledgementForHash(InetAddress address, int port, byte identifier, DatagramSocket socket) {
        byte[] packet = new byte[2];
        packet[0] = Protocol.HASHACK;
        packet[1] = identifier;
        DatagramPacket pkt = new DatagramPacket(packet, packet.length, address, port);
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
        Tools.sendAcknowledgement(packet.getAddress(), packet.getPort(), pktNumber, identifier, socket);

        if (download != null) {
            download.pktTransferred(pktNumber);
            byte[] dataLen = Arrays.copyOfRange(data, 6, 10);
            print("Received packet " + pktNumber +  " for download " + identifier);
            int dataLength = ByteBuffer.wrap(dataLen).getInt();
            byte[] receivedData = Arrays.copyOfRange(data, 10, 10 + dataLength);
            download.addData(pktNumber, receivedData);
            // This method returns true if download is complete
            if (download.isComplete() && download.hasReceivedHash()) {
                downloads.remove(identifier);
                if (download.getInputThread() != null) {
                    download.getInputThread().stopListening();
                }
                    return true;
                } else {
                    return false;
                }

            } else {
                return false;
            }
    }

    public static void processAcknowledgement(DatagramPacket packet, Map<Byte, Upload> uploads) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        byte[] pktNum = Arrays.copyOfRange(packet.getData(), 2, 6);
        int pktNumber = ByteBuffer.wrap(pktNum).getInt();
        if (uploads.containsKey(identifier)) {
            Upload upload = uploads.get(identifier);
            if (upload!= null) {
                if (pktNumber != 0) {
                    upload.cancelTimerForPacket(pktNumber);
                }
                //print("Acknowledgement received. Packet#: " + pktNumber + " Identifier: " + identifier);
                upload.pktTransferred(pktNumber);
                upload.acknowledgePacket(pktNumber);
                if (upload.isComplete()) {
                    upload.cancelAllTimers();
                    double elapsedTime = upload.elapsedTime();
                    print("Upload completed! Elapsed time: " + elapsedTime);
                    print("Time outs: " + upload.getTimeOuts());
                    uploads.remove(identifier);
                } else {
                    upload.continueUpload();
                }
            }
        }
    }

    public static byte[] getHash(String filename) {
        try {
            String dir = System.getProperty("user.dir") + "/home/pi/" + filename;
            File file = new File(dir);

            if (!file.exists()) {
                dir = System.getProperty("user.dir") + "/" + filename;
                file = new File(dir);
            }
            if (!file.exists()) {
                print("Could not generate hash.");
                return new byte[0];
            }


            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount = 0;

                while ((bytesCount = fis.read(byteArray)) != -1) {
                    md5Digest.update(byteArray, 0, bytesCount);
                }

                return md5Digest.digest();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            print(e.getMessage());
            return new byte[0];
        }
    }

    public static void processHash(DatagramPacket packet, Map<Byte, Download> downloads, DatagramSocket socket) {
        byte[] data = packet.getData();
        byte identifier = data[1];
        sendAcknowledgementForHash(packet.getAddress(), packet.getPort(), identifier, socket);
        byte[] hashIndicator = Arrays.copyOfRange(data, 2, 6);
        int hashLength = ByteBuffer.wrap(hashIndicator).getInt();
        byte[] hash = Arrays.copyOfRange(data, 6, 6 + hashLength);
        Download download;
        if (downloads.containsKey(identifier)) {
            download = downloads.get(identifier);
        } else {
            download = new Download();
            downloads.put(identifier, download);
        }
        if (!download.hasReceivedHash()) {
            download.processHash(hash);
        } else {
            print("Hash was already received.");
        }
    }

    public static void handleHashAck(Map<Byte, HandleHashThread> hashThreads, byte[] data) {
        byte identifier = data[1];
        HandleHashThread hht = hashThreads.get(identifier);
        hht.cancelTimerForHashPacket();
    }

    private static void print(String msg) {
        System.out.println(msg);
    }

    public static int getTimeOut() {
        return timeOut;
    }

    public static boolean fileExists(String filename) {
        return new File(System.getProperty("user.dir") + "/home/pi/" + filename).exists() || new File(System.getProperty("user.dir") + "/" + filename).exists();
    }

    public static InetAddress getRaspberryAddress(int lower, int upper) {
        String subnet = "192.168.1.";
        InetAddress myOwnAddress = null;
        try {
            myOwnAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            print(e.getMessage());
        }

        for (int i = lower; i <= upper; i++) {
            String host = subnet + i;
            try {
                InetAddress inetAddress = InetAddress.getByName(host);
                if (inetAddress.isReachable(timeOut) && !inetAddress.equals(myOwnAddress)) {
                    return inetAddress;
                }
            } catch (IOException e) {
                print(e.getMessage());
            }
        }
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            print(e.getMessage());
        }
        return myOwnAddress;
    }
}
