package Client;

import Tools.Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ListenThread extends  Thread {

    private DatagramSocket socket;
    private Client2 client2;
    private boolean listen = true;

    public ListenThread(DatagramSocket socket, Client2 client2) {
        this.socket = socket;
        this.client2 = client2;
    }

    @Override
    public void run() {
        while (listen) {
            byte[] buf = new byte[Tools.getPacketLength()];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                print(e.getMessage());
            }
            client2.handlePacket(packet);
        }
    }

    public void stopListening() {
        listen = false;
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
