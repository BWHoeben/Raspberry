package UDP;

import Client.InputThread;
import Tools.Tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Download extends FileTransfer {

    private Map<Integer, byte[]> dataMap = new HashMap<>();
    private DataOutputStream dataOutputStream;
    private DownloadThread dt;
    private int writtenUntil = 0;
    private int doubles;
    private byte[] generatedHash;
    private byte[] receivedHash;
    private boolean hashReceived = false;
    private boolean hashVerified = false;
    private InputThread it;

    public Download() {
        this.packetLength = Tools.getPacketLength();
        this.time = System.nanoTime();
    }

    public boolean hasReceivedHash() {
        return hashReceived;
    }

    public void addData(int pktNum, byte[] data) {
        if (!dataMap.containsKey(pktNum)) {
            dataMap.put(pktNum, data);
            if (!isComplete) {
                if (!dt.isAlive()) {
                    dt = new DownloadThread(dataMap, dataOutputStream, this);
                    dt.start();
                }
            } else {
                finishWriting();
            }
        } else {
            doubles++;
        }
    }

    private void finishWriting() {
        try {
            print("All packets received. Still writing to file.");
            if (dt.isAlive()) {
                dt.join();
            }
            dt = new DownloadThread(dataMap, dataOutputStream, this);
            dt.start();
            dt.join();
            dataOutputStream.flush();
            dataOutputStream.close();
            if (hashReceived) {
                verifyHash();
            } else {
                print("Download complete but waiting for hash.");
            }
        } catch (IOException | InterruptedException e) {
            print(e.getMessage());
        }
    }

    private void generateHash() {
        generatedHash = Tools.getHash(fileName);
    }

    private void verifyHash() {
        if (!hashVerified) {
            generateHash();
            if (Arrays.equals(receivedHash, generatedHash)) {
                hashVerified = true;
                print("Hashes match!");
            } else {
                print("Hashes don't match");
                print("Received hash: " + new java.lang.String(receivedHash));
                print("Generated hash: " + new java.lang.String(generatedHash));
            }
            printStats();
        }
    }

    private void printStats() {
        double elapsedTime = (double) System.nanoTime() - (double) time;
        double timeInSec = elapsedTime / 1000000000.0;
        print("Time elapsed: " + timeInSec + " seconds");
        double speed = ((double) fileSize / 250000.0) / timeInSec;
        print("Average speed: " + speed + " Mbps");
        print("Redundant transmissions: " + doubles);
    }

    public void processHash(byte[] hash) {
        if (!hashReceived) {
            receivedHash = hash;
            if (!isComplete) {
                hashReceived = true;
            } else {
                verifyHash();
            }
        }
    }

    public int getWrittenUntil() {
        return writtenUntil;
    }

    public void updateWrittenUntil(int writtenUntil) {
        this.writtenUntil = writtenUntil;
        removeProcessedDataFromMemory(writtenUntil);
    }

    private void removeProcessedDataFromMemory(int until) {
        int i = 0;
        while (i <= until) {
            dataMap.remove(i);
            i++;
        }
    }

    public void setNumOfTotalPkts(int numOfTotalPkts) {
        this.numberOfPkts = numOfTotalPkts;
    }

    public void initializeWrite() {
        dataMap.put(0, new byte[0]);
        new File(fileName);
        try {
            dataOutputStream = new DataOutputStream(
                    new BufferedOutputStream(
                            Files.newOutputStream(Paths.get(fileName))));
        } catch (Exception e) {
            print(e.getMessage());
        }
        dt = new DownloadThread(dataMap, dataOutputStream, this);
    }

    public void setParameters(java.lang.String fileName, byte identifier, int packetLength, int fileSize) {
        this.fileName = fileName;
        this.identifier = identifier;
        this.pktsTransfered = new boolean[numberOfPkts];
        this.packetLength = packetLength;
        this.fileSize = fileSize;
    }

    private void print(java.lang.String msg) {
        System.out.println(msg);
    }

    public void setInputThread(InputThread it) {
        this.it = it;
    }

    public InputThread getInputThread() {
        return it;
    }
}
