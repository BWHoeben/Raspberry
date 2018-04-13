package UDP;

import Tools.Tools;
import com.sun.org.apache.xpath.internal.operations.String;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Download extends FileTransfer {

    private Map<Integer, byte[]> dataMap = new HashMap<>();
    private File file;
    private DataOutputStream dataOutputStream;
    private DownloadThread dt;
    private int writtenUntil = 0;
    private int written = 0;

    public Download() {

    }

    public void addData(int pktNum, byte[] data) {
        dataMap.put(pktNum, data);
        if (!isComplete) {
           if (!dt.isAlive()) {
                dt = new DownloadThread(dataMap, file, dataOutputStream, this);
                dt.start();
            }
        } else {
            while (dt.isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            dt = new DownloadThread(dataMap, file, dataOutputStream, this);
            dt.start();
            try {
                dt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                dataOutputStream.flush();
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
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

    public void initializeWrite() {
       dataMap.put(0, new byte[0]);
       file = new File(fileName);
        try {
            try {
                dataOutputStream = new DataOutputStream(
                        new BufferedOutputStream(
                                Files.newOutputStream(Paths.get(fileName)))) ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
         dt = new DownloadThread(dataMap, file, dataOutputStream, this);
    }
}
