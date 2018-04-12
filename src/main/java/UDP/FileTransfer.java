package UDP;

import java.io.File;
import java.io.FileOutputStream;

public abstract class FileTransfer {

    protected String fileName;
    protected int numberOfPkts;
    protected int fileSize;
    protected int identifier;
    protected boolean isComplete = false;
    protected boolean[] pktsTransfered;
    protected int numberOfPktsTransfered;


    public FileTransfer() {

    }

    public boolean isComplete() {
        return isComplete;
    }

    public int completeUntill() {
        for (int i = 0; i < numberOfPkts; i++) {
            if (!pktsTransfered[i]) {
                return i;
            }
        }
        return 0;
    }

    public void setParameters(String fileName, int numberOfPkts, int fileSize, int identifier) {
        //this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileName = "derp.txt";
        this.numberOfPkts = numberOfPkts;
        this.identifier = identifier;
        this.pktsTransfered = new boolean[numberOfPkts];
    }

    public void pktTransfered(int index) {
        pktsTransfered[index - 1] = true;
        numberOfPktsTransfered++;
        if (numberOfPkts == numberOfPktsTransfered) {
            isComplete = true;
        }
    }
}
