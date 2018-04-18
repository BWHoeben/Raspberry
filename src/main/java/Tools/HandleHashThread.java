package Tools;

import UDP.Destination;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class HandleHashThread extends Thread {

    private String filename;
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private byte identifier;
    private Timer timer;
    private HandleHashThread hht;

    public HandleHashThread(String filename, Destination destination, DatagramSocket socket, byte identifier) {
        this.filename = filename;
        this.address = destination.getAddress();
        this.port = destination.getPort();
        this.socket = socket;
        this.identifier = identifier;
    }

    @Override
    public void run() {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] hash = Tools.getHash(filename);
        byte[] first = new byte[1];
        first[0] = Protocol.HASH;
        arrays.put(0, first); // indicate that this is a hash
        byte[] identifierByte = new byte[1];
        identifierByte[0] = identifier;
        arrays.put(1, identifierByte);
        arrays.put(2, Tools.intToByteArray(hash.length));
        arrays.put(3, hash);
        byte[] packet = Tools.appendThisMapToAnArray(arrays);
        DatagramPacket dp = new DatagramPacket(packet, packet.length, address, port);
        try {
            socket.send(dp);
        } catch (IOException e) {
            print(e.getMessage());
        }
        createTimerForHashPacket();
    }

    private void createTimerForHashPacket() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                print("Hash packet timed out");
                Destination destination = new Destination(port, address);
                hht = new HandleHashThread(filename, destination, socket, identifier);
                hht.start();
            }
        },
        Tools.getTimeOut()
        );
    }

    public void cancelTimerForHashPacket() {
        if (timer != null){
            timer.cancel();
            timer.purge();
            if (hht != null) {
                hht.cancelTimerForHashPacket();
            }
        }
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
