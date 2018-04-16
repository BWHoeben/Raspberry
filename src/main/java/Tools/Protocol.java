package Tools;

public class Protocol {

    /*
    Protocol for first byte:

    0 --> show files
    1 --> list of files
    2 --> request to download specific file
    3 --> requested download not available
    4 --> initial packet for upload
    5 --> additional packet for upload
    6 --> acknowledgement for download
    7 --> pause file transfer
    8 --> resume file transfer
    9 --> abort file transfer
    10 --> hash for file
    11 --> acknowledgement for hash
     */

    public static final byte SHOWFILES = 0;
    public static final byte LISTOFFILES = 1;
    public static final byte REQDOWN = 2;
    public static final byte INVALIDREQ = 3;
    public static final byte INITUP = 4;
    public static final byte ADDUP = 5;
    public static final byte ACKDOWN = 6;
    public static final byte PAUSE = 7;
    public static final byte RESUME = 8;
    public static final byte ABORT = 9;
    public static final byte HASH = 10;
    public static final byte HASHACK = 11;
}
