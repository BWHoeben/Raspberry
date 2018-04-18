package Test;
import Tools.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Test {
    private static MappedByteBuffer buffer;
    public Test() {

    }

    public static void main(String[] args) {
        String fileName = "berg.bmp";
        String dir = System.getProperty("user.dir") + "/home/pi/" + fileName;
        print(System.getProperty("user.dir"));
        int fileSize = (int) new File(dir).length();
        int numberOfPkts = (int) Math.ceil((double) fileSize / (Tools.getPacketLength() - 10)) + 1;
        try (FileChannel channel = new FileInputStream(dir).getChannel()) {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (IOException e1) {
            dir = fileName;
            fileSize = (int) new File(dir).length();
            numberOfPkts = (int) Math.ceil((double) fileSize / (Tools.getPacketLength() - 10)) + 1;
            try (FileChannel channel = new FileInputStream(dir).getChannel()) {
                buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            } catch (IOException e2) {
                print(e2.getMessage());
            }
        }
    }


    private static void print(String msg) {

        System.out.println(msg);
    }

}