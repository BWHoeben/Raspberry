package UDP;

import Tools.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class Download extends FileTransfer {

    private Map<Integer, byte[]> dataMap = new HashMap<>();

    public Download() {

    }

    public void addData(int pktNum, byte[] data) {
        dataMap.put(pktNum, data);
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
        byte[] data = getData();
        File fileToWrite = new File(fileName);
        try (FileOutputStream fileStream = new FileOutputStream(fileToWrite)) {
            for (byte fileContent : data) {
                fileStream.write(fileContent);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
