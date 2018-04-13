package UDP;

import Tools.Tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Upload extends FileTransfer {

    protected Destination destination;
    protected byte[] data;

    public Upload(Destination destination, byte[] data) {
        this.destination = destination;
        this.data = data;
    }

    public Destination getDestination() {
        return destination;
    }

    public byte[] getPacketData(int packetNumber) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = 2; // indicate that this is a packet for requested download
        arrays.put(0, first);
        byte[] ind = new byte[1];
        ind[0] = this.identifier;
        arrays.put(1, ind);
        arrays.put(2, Tools.intToByteArray(packetNumber)); // the packet number
        byte[] header = Tools.appendThisMapToAnArray(arrays);
        int bytesLeft = packetLength - header.length - 4;
        int from = bytesLeft * (packetNumber - 1);
        int to = Math.min(packetNumber * bytesLeft, data.length);
        from = Math.min(from,to);
        byte[] dataToSend = Arrays.copyOfRange(data, from, to);
        int dataLengthIndicator = dataToSend.length;
        byte[] dataLen = Tools.intToByteArray(dataLengthIndicator);
        header = Tools.appendBytes(header, dataLen);
        return Tools.appendBytes(header, dataToSend);
    }
}
