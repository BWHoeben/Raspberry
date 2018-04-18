package UDP;


import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class DownloadThread extends Thread {

    private Map<Integer, byte[]> dataMap;
    private DataOutputStream dataOutputStream;
    private Download download;


    public DownloadThread(Map<Integer, byte[]> dataMap, DataOutputStream dataOutputStream, Download download) {
        this.dataMap = dataMap;
        this.dataOutputStream = dataOutputStream;
        this.download = download;
    }

    @Override
    public void run() {
            int writtenUntil = download.getWrittenUntil();
            while (download.completeUntill() - 1 > writtenUntil) {
                byte[] array = dataMap.get(writtenUntil + 1);
                if (array != null) {
                    try {
                        dataOutputStream.write(array);
                        writtenUntil++;
                    } catch (IOException e) {
                        print(e.getMessage());
                    }
                } else {
                    print("Attempting to write packet " + writtenUntil + " but no data found.");
                    break;
                }
            }
            download.updateWrittenUntil(writtenUntil);
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
