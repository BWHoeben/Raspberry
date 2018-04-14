package UDP;

import Tools.Protocol;
import Tools.Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class Upload extends FileTransfer {

    private Destination destination;
    private byte[] data;
    private PacketState[] states;
    private DatagramSocket socket;
    private int slidingWindow;
    private int timeOut;
    private Timer[] timers;
    private boolean paused = false;
    private boolean aborted = false;
    private int timeOuts = 0;

    public Upload(Destination destination, byte[] data) {
        this.destination = destination;
        this.data = data;
    }

    public void initialize(DatagramSocket socket, int slidingWindow, int timeOut) {
        this.states = new PacketState[numberOfPkts];
        for (int i = 0; i < numberOfPkts; i++) {
            states[i] = PacketState.INQUEUE;
        }
        timers = new Timer[numberOfPkts];
        this.slidingWindow = slidingWindow;
        this.timeOut = timeOut;
        this.socket = socket;
    }

    public void acknowledgePacket(int pktNumber) {
        states[pktNumber] = PacketState.ACKNOWLEDGED;
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
        continueUpload();
    }

    public void abort() {
        this.aborted = true;
    }

    public byte[] getPacketData(int packetNumber) {
        Map<Integer, byte[]> arrays = new HashMap<>();
        byte[] first = new byte[1];
        first[0] = Protocol.ADDUP; // indicate that this is a packet for requested download
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

    public int packetsInTheAir() {
        int counter = 0;
        for (PacketState state : states) {
            if (state.equals(PacketState.SEND)) {
                counter++;
            }
        }
        return counter;
    }

    public int getTimeOuts() {
        return timeOuts;
    }

    public int[] getPacketsToTransmit(int numberOfPacketsToTransmit) {
        int i = 0;
        int pktsSelected = 0;
        int[] packetsToReturn = new int[numberOfPacketsToTransmit];
        while (i < numberOfPkts && pktsSelected < numberOfPacketsToTransmit) {
            if (states[i].equals(PacketState.INQUEUE) || states[i].equals(PacketState.TIMEDOUT)) {
                packetsToReturn[pktsSelected] = i;
                pktsSelected++;
            }
            i++;
        }
        if (pktsSelected != numberOfPacketsToTransmit) {
            packetsToReturn = Arrays.copyOfRange(packetsToReturn,0,pktsSelected);
        }
        return packetsToReturn;
    }

    public synchronized void continueUpload() {
        if (!paused && !aborted) {
            int numberOfPktsToTransmit = slidingWindow - packetsInTheAir();
            int[] packetsToTransmit = getPacketsToTransmit(numberOfPktsToTransmit);
            for (int i = 0; i < packetsToTransmit.length; i++) {
                int pktNumber = packetsToTransmit[i];
                byte[] dataToSend = getPacketData(pktNumber);
                DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, destination.getAddress(), destination.getPort());
                try {
                    socket.send(packet);
                    setTimerForPacket(pktNumber);
                    states[pktNumber] = PacketState.SEND;
                    //print("Packet send: " + pktNumber);
                } catch (IOException e) {
                    print(e.getMessage());
                }
            }
        }
    }

    public void cancelTimerForPacket(int packetNumber) {
        Timer timer = timers[packetNumber];
        timer.cancel();
        timer.purge();
        timers[packetNumber] = null;
    }

    private void setTimerForPacket(int packetNumber) {
        Timer timer = new Timer();
        timers[packetNumber] = timer;
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               print("Packet " + packetNumber + " timed out");
                               timeOuts++;
                               states[packetNumber] = PacketState.TIMEDOUT;
                               continueUpload();
                           }
                       },
                timeOut
        );
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    public void cancelAllTimers() {
        for (int i = 0; i < numberOfPkts; i++) {
            Timer timer = timers[i];
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
        }
    }

}
