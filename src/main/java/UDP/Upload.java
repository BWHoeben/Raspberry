package UDP;

import Tools.Protocol;
import Tools.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class Upload extends FileTransfer {

    private Destination destination;
    private PacketState[] states;
    private DatagramSocket socket;
    private int slidingWindow;
    private int timeOut;
    private Timer[] timers;
    private boolean paused = false;
    private boolean aborted = false;
    private int timeOuts = 0;
    private MappedByteBuffer buffer;
    private Map<Integer, byte[]> dataMap = new HashMap<>();
    private int readUntill = 0;

    public Upload(Destination destination) {
        this.destination = destination;
        this.packetLength = Tools.getPacketLength();
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
        removeAcknowledgedData(pktNumber);
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

    public void initializeFileRead(String fileName) {
        //String dir = "/home/pi/" + fileName;
        String dir = fileName;
        this.fileSize = (int) new File(dir).length();
        this.numberOfPkts = (int) Math.ceil((double) fileSize / (packetLength - 10)) + 1;
        try (FileChannel channel = new FileInputStream(dir).getChannel()) {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (IOException e1) {
            dir = fileName;
            this.fileSize = (int) new File(dir).length();
            this.numberOfPkts = (int) Math.ceil((double) fileSize / (packetLength - 10)) + 1;
            try (FileChannel channel = new FileInputStream(dir).getChannel()) {
                buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            } catch (IOException e2) {
                print(e2.getMessage());
            }
        }
    }

    public void getPacket(int length) {
        if (buffer.hasRemaining()) {
            readUntill++;
            byte[] data;
            if (buffer.remaining() > length) {
                data = new byte[length];
                buffer.get(data);
            } else {
                data = new byte[buffer.remaining()];
                buffer.get(data);
            }
            dataMap.put(readUntill, data);
        }
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
        byte[] dataToSend;
        while (!dataMap.containsKey(packetNumber) && readUntill <= packetNumber) {
            //for (int i = 0; i < slidingWindow; i++) {
            getPacket(bytesLeft);
            //}
        }
        dataToSend = dataMap.get(packetNumber);
        if (dataToSend != null) {
            int dataLengthIndicator = dataToSend.length;
            byte[] dataLen = Tools.intToByteArray(dataLengthIndicator);
            header = Tools.appendBytes(header, dataLen);
            return Tools.appendBytes(header, dataToSend);
        } else {
            return new byte[0];
        }
    }

    public void removeAcknowledgedData(int pktNumber) {
        if (dataMap.containsKey(pktNumber)) {
            dataMap.remove(pktNumber);
        }
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
            packetsToReturn = Arrays.copyOfRange(packetsToReturn, 0, pktsSelected);
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
                if (dataToSend.length > 0) {
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
    }

    public void cancelTimerForPacket(int packetNumber) {
        Timer timer = timers[packetNumber];
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timers[packetNumber] = null;
        }
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

    public void setParameters(String fileName, byte identifier, int packetLength) {
        //this.fileName = fileName;
        this.fileName = "pic.txt";
        this.identifier = identifier;
        this.pktsTransfered = new boolean[numberOfPkts];
        this.packetLength = packetLength;
    }
}
