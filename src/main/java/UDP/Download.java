package UDP;

import Tools.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Download extends FileTransfer {

    private Map<Integer, byte[]> dataMap = new HashMap<>();
    private File file;
    private FileOutputStream fileStream;
    private DownloadThread dt;
    private int writtenUntil = 0;

    public Download() {

    }

    public void addData(int pktNum, byte[] data) {
        dataMap.put(pktNum, data);
        if (!isComplete) {
           if (!dt.isAlive()) {
                dt = new DownloadThread(dataMap, file, fileStream, this);
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
            dt = new DownloadThread(dataMap, file, fileStream, this);
            dt.start();
            try {
                dt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getWrittenUntil() {
        return writtenUntil;
    }

    public void updateWrittenUntil(int writtenUntil) {
        this.writtenUntil = writtenUntil;
    }

    public void initializeWrite() {
       dataMap.put(0, new byte[0]);
       file = new File(fileName);
        try {
            fileStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
         dt = new DownloadThread(dataMap, file, fileStream, this);
    }

    /*
    public void updateWrite() {
        while (completeUntill() > writtenUntil) {
            byte[] dataToAdd = dataMap.get(writtenUntil);
            System.out.println("Written untill: " + writtenUntil);
            System.out.println("Complete untill: " + completeUntill());
            for (byte fileContent : dataToAdd) {
                try {
                    fileStream.write(fileContent);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            writtenUntil++;
        }
    }

    public byte[] getData() {
        if (isComplete) {
            return Tools.appendThisMapToAnArray(dataMap);
        } else {
            System.out.println("Download is not yet complete. Not writing to file!");
            return null;
        }
    }

    public void writeArrayToFile() {
        System.out.println("Assembling data...");
        byte[] data = getData();

        System.out.println("File size: " + data.length + " bytes.");

        File fileToWrite = new File(fileName);
        try (FileOutputStream fileStream = new FileOutputStream(fileToWrite)) {
            for (byte fileContent : data) {
                fileStream.write(fileContent);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }*/

}
