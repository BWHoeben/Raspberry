package UDP;

import com.sun.org.apache.xpath.internal.operations.String;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DownloadThread extends Thread {

    private Map<Integer, byte[]> dataMap;
    private DataOutputStream dataOutputStream;
    private Download download;


    public DownloadThread(Map<Integer, byte[]> dataMap, File file, DataOutputStream dataOutputStream, Download download) {
        this.dataMap = dataMap;
        this.dataOutputStream = dataOutputStream;
        this.download = download;
    }

    @Override
    public void run() {
            int writtenUntill = download.getWrittenUntil();
            while (download.completeUntill() > writtenUntill) {
                byte[] array = dataMap.get(writtenUntill);
                if (array != null) {
                    for (byte fileContent : array) {
                        try {
                            dataOutputStream.write(fileContent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                writtenUntill++;
            }
            download.updateWrittenUntil(writtenUntill);
    }
}
