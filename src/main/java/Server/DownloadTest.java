package Server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import Tools.Protocol;
import Tools.Tools;
import UDP.Download;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DownloadTest {

    private ServerThread st;
    private DatagramSocket socket;
    private String fileName = "testLyrics.txt";
    private File file;
    private Download dl;
    private int totalPkts;
    private Map<Byte, Download> downloads = new HashMap<>();

    @Before
    public void setUp() {
        try {
            socket = new DatagramSocket();
            st = new ServerThread();
            st.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestDownload() {
        file = new File(System.getProperty("user.dir") + "/home/pi/" + fileName);
        assertTrue(file.exists());
        sendFileRequest(file.getName());
        byte[] buf = new byte[Tools.getPacketLength()];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        handleInitPacket(packet);
        dl = new Download();
        for (int i = 0; i < totalPkts; i++) {
            try {
                socket.receive(packet);
                byte[] pktdata = packet.getData();
                if (pktdata[0] == Protocol.HASH) {
                    Tools.processHash(packet, downloads, socket);
                } else {
                    if (i != totalPkts - 1) {
                        assertFalse(downloadPacket(packet));
                    } else {
                        assertTrue(downloadPacket(packet));
                    }
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        System.out.println("Done");
    }
}

    // return true if download complete
    private boolean downloadPacket(DatagramPacket packet) {
        return Tools.processDownloadPacket(packet, downloads, socket);
    }

    private void sendFileRequest(String filename) {
        byte[] buf = new byte[Tools.getPacketLength()];
        buf[0] = Protocol.REQDOWN;
        byte[] filenameArray = filename.getBytes();
        buf[1] = (byte) filenameArray.length;
        for (int i = 0; i < filenameArray.length; i++) {
            buf[2 + i] = filenameArray[i];
        }
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), Tools.getPort());
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleInitPacket(DatagramPacket pkt) {
        byte[] data = pkt.getData();
        assertTrue(data[0] == Protocol.INITUP);
        byte[] numOfPkts = Arrays.copyOfRange(data, 1, 5);
        totalPkts = ByteBuffer.wrap(numOfPkts).getInt();
        System.out.println("Packets " + totalPkts);
        byte[] fileSize = Arrays.copyOfRange(data, 5, 9);
        int fs = ByteBuffer.wrap(fileSize).getInt();
        System.out.println("Filesize: " + fs);
        assertTrue(file.length() == fs);
        byte identifier = data[9];
        assertTrue(identifier == 0);
        int fileLengthIndicator = (int) data[10];
        assertTrue(fileLengthIndicator == fileName.getBytes().length);
        byte[] fileNameBytes = Arrays.copyOfRange(data, 11, 11 + fileLengthIndicator);
        assertTrue(new String(fileNameBytes).equals(fileName));
        dl = new Download();
        dl.setNumOfTotalPkts(totalPkts);
        dl.setParameters(fileName, (byte) 0, Tools.getPacketLength(), fs);
        dl.initializeWrite();
        dl.pktTransferred(0);
        downloads.put(identifier, dl);
        sendAck(0);
    }

    private void sendAck(int packetNumber) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.ACKDOWN;
        arrays.put(0, first); // indicate that this is an acknowledgement
        byte[] identifierByte = new byte[1];
        identifierByte[0] = 0;
        arrays.put(1, identifierByte); // identifier
        arrays.put(2, Tools.intToByteArray(packetNumber)); // pktNumber
        byte[] ack = Tools.appendThisMapToAnArray(arrays);
        DatagramPacket pkt = null;
        try {
            pkt = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 12555);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
