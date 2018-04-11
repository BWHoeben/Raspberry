package UDP;

public class Download {

    private String fileName;
    private int numberOfPkts;
    private int fileSize;
    private byte identifier;
    private Integer[] pktsReceived;

    public Download(String fileName, int numberOfPkts, int fileSize, byte identifier) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.numberOfPkts = numberOfPkts;
        this.identifier = identifier;
        pktsReceived = new Integer[numberOfPkts];
    }



}
