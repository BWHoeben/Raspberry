package UDP;

import Server.ServerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;

public class UploadThread extends Thread {

    private int slidingWindow;
    private Upload upload;
    private ServerThread st;
    private DatagramSocket socket;
    private HashMap<Byte, PacketState[]> packetStates;

    public UploadThread(int slidingWindow, Upload upload, ServerThread st, DatagramSocket socket, HashMap<Byte, PacketState[]> packetStates) {
        this.slidingWindow = slidingWindow;
        this.upload = upload;
        this.socket = socket;
        this.st = st;
        this.packetStates = packetStates;
    }

    @Override
    public void run() {
        int numberOfPktsToTransmit = slidingWindow - st.packetsInTheAir(upload.getIdentifier());
        int[] packetsToTransmit = st.getPacketsToTransmit(upload.getIdentifier(), numberOfPktsToTransmit);
        Destination destination = upload.getDestination();
        for (int i = 0; i < packetsToTransmit.length; i++) {
            int pktNumber = packetsToTransmit[i];
            byte[] dataToSend = upload.getPacketData(pktNumber);
            DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, destination.getAddress(), destination.getPort());
            try {
                socket.send(packet);
                st.setTimerForPacket(upload.getIdentifier(), pktNumber);
                byte identifier = upload.getIdentifier();
                PacketState[] states = packetStates.get(identifier);
                states[pktNumber] = PacketState.SEND;
                System.out.println("Packet send: " + pktNumber);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
