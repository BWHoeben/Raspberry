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
    private File file;
    private DataOutputStream dataOutputStream;
    private DownloadThread dt;
    private int writtenUntil = 0;
    private long time;
    private int doubles;
    private byte[] generatedHash;
    private byte[] receivedHash;
    private boolean hashReceived = false;
    private boolean hashVerified = false;
    private InputThread it;

    public Download() {
        this.packetLength = Tools.getPacketLength();
        time = System.nanoTime();
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
                print("All packets received. Still writing to file.");
                while (dt.isAlive()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        print(e.getMessage());
                    }
                }
                dt = new DownloadThread(dataMap, dataOutputStream, this);
                dt.start();
                try {
                    dt.join();
                } catch (InterruptedException e) {
                    print(e.getMessage());
                }

                try {
                    dataOutputStream.flush();
                    dataOutputStream.close();
                } catch (IOException e) {
                    print(e.getMessage());
                }
                if (hashReceived) {
                    verifyHash();
                } else {
                    print("Download complete but waiting for hash.");
                }
            }
        } else{
            doubles++;
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
            double elapsedTime = (double) System.nanoTime() - (double) time;
            double timeInSec = elapsedTime / 1000000000.0;
            print("Time elapsed: " + timeInSec + " seconds");
            double speed = ((double) fileSize / 250000.0) / timeInSec;
            print("Average speed: " + speed + " Mbps");
            print("Redundant transmissions: " + doubles);
        }
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

    public void removeProcessedDataFromMemory(int until) {
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
        file = new File(fileName);
        try {
            try {
                dataOutputStream = new DataOutputStream(
                        new BufferedOutputStream(
                                Files.newOutputStream(Paths.get(fileName))));
            } catch (IOException e) {
                print(e.getMessage());
            }
        } catch (Exception e) {
            print(e.getMessage());
        }
        dt = new DownloadThread(dataMap, dataOutputStream, this);
    }

    public void setParameters(java.lang.String fileName, byte identifier, int packetLength, int fileSize) {
        this.fileName = "pic.txt";
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
