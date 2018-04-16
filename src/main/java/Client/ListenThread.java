package Client;

import Tools.Tools;
import com.nedap.university.Computer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ListenThread extends  Thread {

    private DatagramSocket socket;
    private Computer computer;
    private boolean listen = true;

    public ListenThread(DatagramSocket socket, Computer computer) {
        this.socket = socket;
        this.computer = computer;
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
            computer.handlePacket(packet);
        }
    }

    public void stopListening() {
        listen = false;
    }

    private void print(String msg) {
        System.out.println(msg);
    }
}
