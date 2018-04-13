package UDP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DownloadThread extends Thread {

    private Map<Integer, byte[]> dataMap = new HashMap<>();
    private File file;
    private FileOutputStream fileStream;
    private Download download;


    public DownloadThread(Map<Integer, byte[]> dataMap, File file, FileOutputStream fileOutputStream, Download download) {
        this.dataMap = dataMap;
        this.file = file;
        this.fileStream = fileOutputStream;
        this.download = download;
    }

    @Override
    public void run() {
        int writtenUntill = download.getWrittenUntil();
        System.out.println("Written until 1: " + writtenUntill);
        System.out.println("Complete untill: " + download.completeUntill());
        while (download.completeUntill() > writtenUntill) {
            byte[] array = dataMap.get(writtenUntill);
            for (byte fileContent : array) {
                try {
                    fileStream.write(fileContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writtenUntill++;
            System.out.println("Written until 2: " + writtenUntill);
        }
        download.updateWrittenUntil(writtenUntill);
    }
}
