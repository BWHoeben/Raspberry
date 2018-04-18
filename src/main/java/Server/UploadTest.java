package Server;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import Server.ServerThread;
import Tools.Protocol;
import Tools.Tools;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UploadTest {

    private ServerThread st;
    private int dataLength = 120000;
    private String fileName = "testFileForTesting.testThisFilePlease";
    private byte[] data;
    private DatagramSocket socket;
    private byte identifier;

    @Before
    public void setUp() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        data = new byte[dataLength];
        new Random().nextBytes(data);
        try {
            st = new ServerThread();
            st.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestUpload() {
        try {
            byte[] buf = new byte[Tools.getPacketLength()];
            sendInitUp();
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            handleAck(packet, 0);
            sendHash();
            buf = new byte[Tools.getPacketLength()];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            handleHash(packet);
            sendData(1);
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            handleAck(packet, 1);
            sendData(2);
            buf = new byte[Tools.getPacketLength()];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            handleAck(packet, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleAck(DatagramPacket packet, int pktNum) {
        byte[] pktData = packet.getData();
        assertTrue(pktData[0] == Protocol.ACKDOWN);
        assertTrue(pktData[1] == identifier);
        byte[] pktNumArray = Arrays.copyOfRange(pktData, 2, 6);
        int number = ByteBuffer.wrap(pktNumArray).getInt();
        assertTrue(number == pktNum);
    }

    private void handleHash(DatagramPacket pkt) {
        byte[] pktData = pkt.getData();
        assertTrue(pktData[0] == Protocol.HASHACK);
    }

    private void sendInitUp() {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.INITUP;
        arrays.put(0, first);
        byte[] numOfPkts = Tools.intToByteArray(3);
        arrays.put(1, numOfPkts);
        byte[] fileSize = Tools.intToByteArray(dataLength);
        arrays.put(2, fileSize);
        byte[] ident = new byte[1];
        ident[0] = identifier;
        arrays.put(3, ident);
        byte[] fileNameBytes = fileName.getBytes();
        byte[] fileNameLength = new byte[1];
        fileNameLength[0] = (byte) fileNameBytes.length;
        arrays.put(4, fileNameLength);
        arrays.put(5, fileNameBytes);
        byte[] messageToSend = Tools.appendThisMapToAnArray(arrays);
        DatagramPacket pkt = null;
        try {
            pkt = new DatagramPacket(messageToSend, messageToSend.length, InetAddress.getLocalHost(), 12555);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(int pktnum) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.ADDUP; // indicate that this is a packet for requested download
        arrays.put(0, first);
        byte[] ind = new byte[1];
        ind[0] = this.identifier;
        arrays.put(1, ind);
        arrays.put(2, Tools.intToByteArray(pktnum)); // the packet number
        byte[] header = Tools.appendThisMapToAnArray(arrays);
        int bytesLeft = Tools.getPacketLength() - header.length - 4;
        byte[] dataToSend;
        if (pktnum == 1) {
            dataToSend = Arrays.copyOfRange(data, 0, bytesLeft);
        }  else {
            dataToSend =  Arrays.copyOfRange(data, bytesLeft, dataLength);
        }
        int dataLengthIndicator = dataToSend.length;
        byte[] dataLen = Tools.intToByteArray(dataLengthIndicator);
        header = Tools.appendBytes(header, dataLen);
        byte[] packetData = Tools.appendBytes(header, dataToSend);
        DatagramPacket pkt = null;
        try {
            pkt = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 12555);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHash() {
        try {
            Map<Integer, byte[]> arrays = new HashMap<>();
            byte[] first = new byte[1];
            first[0] = Protocol.HASH;
            arrays.put(0, first); // indicate that this is a hash
            byte[] identifierByte = new byte[1];
            identifierByte[0] = identifier;
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] hash = md5Digest.digest(data);
            arrays.put(1, identifierByte);
            arrays.put(2, Tools.intToByteArray(hash.length));
            arrays.put(3, hash);
            byte[] packet = Tools.appendThisMapToAnArray(arrays);
            DatagramPacket dp = new DatagramPacket(packet, packet.length, InetAddress.getLocalHost(), 12555);
            socket.send(dp);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    private void print(String msg) {
        System.out.println(msg);
    }
}