package Client;

import Tools.Protocol;
import UDP.Destination;
import UDP.Download;
import UDP.FileTransfer;
import UDP.Upload;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.Scanner;

public class InputThread<U extends FileTransfer> extends Thread {

    private boolean listen = true;
    private Scanner scanner;
    private byte identifier;
    private Destination destination;
    private DatagramSocket socket;
    private boolean paused;
    private String transfer;
    private FileTransfer ft;
    private Map<Byte, U> transfers;

    public InputThread(Scanner scanner, byte identifier, Destination destination, DatagramSocket socket,
                       FileTransfer ft, Map<Byte, U> transfers) {
        this.scanner = scanner;
        this.identifier = identifier;
        this.destination = destination;
        this.socket = socket;
        this.ft = ft;
        if (ft instanceof Download) {
            transfer = "Download";
        } else {
            transfer = "Upload";
        }
        this.transfers = transfers;
    }

    @Override
    public void run() {
        print("Enter 'p' to pause " + transfer + " or 'a' to abort");
        while (listen) {
            String answer = scanner.nextLine();
            if (listen) {
                if (answer.equalsIgnoreCase("p") && !paused) {
                    print(transfer + " paused. Press 'r' to resume transfer.");
                    pauseTransfer();
                    paused = true;
                } else if (answer.equalsIgnoreCase("a") && !paused) {
                    print(transfer + " aborted");
                    abortTransfer();
                    listen = false;
                } else if (answer.equalsIgnoreCase("r") && paused) {
                    print("Resuming " + transfer + ". Enter 'p' to pause " + transfer + " or 'a' to abort.");
                    paused = false;
                    resumeTransfer();
                } else if (!paused) {
                    print("Incorrect input, valid options are 'p' and 'a'. You typed: " + answer);
                } else {
                    print("Incorrect input. Press 'r' to resume transfer.");
                }
            }
        }
    }

    public void stopListening() {
        listen = false;
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    private void pauseTransfer() {
        if (ft instanceof Upload) {
            ((Upload) ft).pause();
        } else {
            byte[] data = new byte[2];
            data[0] = Protocol.PAUSE;
            data[1] = identifier;
            send(data);
        }
    }

    private void abortTransfer() {
        byte[] data = new byte[2];
        if (ft instanceof Upload) {
            ((Upload) ft).abort();
            data[0] = Protocol.ABORTUP;
        } else {
            data[0] = Protocol.ACKDOWN;
        }
        data[1] = identifier;
        transfers.remove(identifier);
        send(data);
        scanner.close();
    }

    private void resumeTransfer() {
        if (ft instanceof Upload) {
            ((Upload) ft).resume();
        } else {
            byte[] data = new byte[2];
            data[0] = Protocol.RESUME;
            data[1] = identifier;
            send(data);
        }
    }

    private void send(byte[] data) {
        DatagramPacket pkt = new DatagramPacket(data, data.length, destination.getAddress(), destination.getPort());
        try {
            socket.send(pkt);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }
}
