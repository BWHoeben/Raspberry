package Client;

import javax.jnlp.FileContents;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
    private int packetLength = 512;
    private Map<Integer, byte[]> data = new HashMap<Integer, byte[]>();
    private DatagramSocket socket = null;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        int i = 0;
        try {
            InetAddress address = askForHost();
            print("Inet address created.");
            byte upOrDown = upOrDown(address);
            try {
                socket = new DatagramSocket();
                while (true) {
                    byte[] buf = new byte[packetLength];
                    buf[0] = upOrDown;
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
                    socket.send(packet);
                    print("Packet send");
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    print("Packet recieved: " + i);

                    byte[] firstFour = Arrays.copyOfRange(packet.getData(),0,4);
                    int packetNum = ByteBuffer.wrap(firstFour).getInt();
                    if (packetNum == 0) {
                        handleInitialPacket(packet.getData());
                    }
                    data.put(i, buf);
                    if (i == 12) {
                        byte[] dataComplete = getFileContents(data);
                        writeFile(dataComplete, "output.txt");
                        break;
                    }
                    i++;
                }
            } catch (IOException e) {
                print(e.getMessage());
            }

        } finally {
            if (socket != null) {
                socket.close();

            }
        }
    }

    private void handleInitialPacket(byte[] packet) {
        byte[] numOfPkts = Arrays.copyOfRange(packet,4,8);
        int totalPkts = ByteBuffer.wrap(numOfPkts).getInt();
        print("Total number of packets: " + totalPkts);
        byte[] fileSize = Arrays.copyOfRange(packet,8,12);
        int fs = ByteBuffer.wrap(fileSize).getInt();
        print("File size: " + fs + " bytes");
        int identifier = (int) packet[12];
        print("Identifier: " + identifier);
        int fileLengthIndicator = (int) packet[13];
        byte[] fileName = Arrays.copyOfRange(packet,14, 14 + fileLengthIndicator);
        print("File name: " + new String(fileName));
    }

    private void upload(InetAddress address) {
        byte[] buf = new byte[packetLength];
        //buf[0] = (byte) 1;
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
        try {
            socket.send(packet);
        } catch (IOException e) {
            print(e.getMessage());
        }
    }

    private byte upOrDown(InetAddress address) {
        while (true) {
                print("Download or upload? (u/d)");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("u")) {
                    return (byte) 0;
                } else if (answer.equalsIgnoreCase("d")) {
                    return (byte) 1;
                } else {
                    print("Unkown input. Please provide 'u' or 'd'.");
                }


        }

    }

    private InetAddress askForHost() {
    while (true) {
            try {
                print("Do you want to use local host? (yes/no)");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("yes")) {
                    return InetAddress.getByName("localhost");
                } else if (answer.equalsIgnoreCase("no")) {
                    print("Provide host address: ");
                    String addressString = scanner.nextLine();
                    return InetAddress.getByName(addressString);
                } else {
                    print("Unkown input. Please provide 'yes' or 'no'.");
                }

            } catch (UnknownHostException e) {
                print("Unknown host. Try again");
            }
        }
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    private byte[] getFileContents(Map<Integer, byte[]> data) {
        int dataLength = 0;
        int index = 0;
        for (int i = 0; i < data.size(); i++) {
            dataLength = dataLength + data.get(i).length;
        }
        print("Datalength: " + dataLength);
        byte[] fileContents = new byte[dataLength];
        for (int i = 0; i < data.size(); i++) {
            byte[] packetToAdd = data.get(i);
            System.arraycopy(packetToAdd, 0, fileContents, index, packetToAdd.length);
            index = index + packetToAdd.length;
            print("Index: " + index);
            print("Packet length: " + packetToAdd.length);
        }
        return fileContents;
    }

    public void writeFile(byte[] data, String fileName) {
        File fileToWrite = new File(fileName);
        try (FileOutputStream fileStream = new FileOutputStream(fileToWrite)) {
            for (byte fileContent : data) {
                fileStream.write(fileContent);
            }
        } catch (Exception e) {
            print(e.getMessage());
        }
    }
}


