package Client;

import javax.jnlp.FileContents;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    Map<Integer, byte[]> data = new HashMap<Integer, byte[]>();
    DatagramSocket socket = null;
    PrintWriter out = null;

    public static void main(String[] args) throws IOException {
        new Client();
    }

    public Client() throws IOException {
        int i = 0;
        try {
            socket = new DatagramSocket();
            out = new PrintWriter("output.txt");
            InetAddress address = askForHost();
            print("Inet address created.");
            while (true) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12555);
                socket.send(packet);
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                print("Packet recieved: " + i);
                data.put(i, buf);
                if (i == 12) {
                    byte[] dataComplete = getFileContents(data);
                    writeFile(dataComplete, "output.txt");
                    break;
                }
                i++;
            }
        } finally {
            if (socket != null) {
                socket.close();

            }
            if (out != null) {
                out.close();
            }
        }
    }

    private InetAddress askForHost() {
        Scanner scanner = new Scanner(System.in);
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


