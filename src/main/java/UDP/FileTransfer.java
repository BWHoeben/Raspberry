package UDP;

import sun.security.krb5.internal.crypto.Des;

import java.io.File;
import java.io.FileOutputStream;

public abstract class FileTransfer {

    protected String fileName;
    protected int numberOfPkts;
    protected int fileSize;
    protected byte identifier;
    protected boolean isComplete = false;
    protected boolean[] pktsTransfered;
    protected int numberOfPktsTransfered;
    protected int packetLength;

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
        return numberOfPkts;
    }

    public void setParameters(String fileName, int numberOfPkts, int fileSize, byte identifier, int packetLength) {
        //this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileName = "pic.txt";
        this.numberOfPkts = numberOfPkts;
        this.identifier = identifier;
        this.pktsTransfered = new boolean[numberOfPkts];
        this.packetLength = packetLength;
    }

    public void pktTransfered(int index) {
        pktsTransfered[index] = true;
        numberOfPktsTransfered++;
        if (numberOfPkts == numberOfPktsTransfered) {
            isComplete = true;
        }
    }
}
