package UDP;

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

    public void pktTransferred(int index) {
        if (pktsTransfered != null) {
            pktsTransfered[index] = true;
            numberOfPktsTransfered++;
            if (numberOfPkts == numberOfPktsTransfered) {
                isComplete = true;
            }
        }
    }

    public int getNumberOfPkts() {
        return numberOfPkts;
    }

    public int getFileSize() {
        return fileSize;
    }
}
