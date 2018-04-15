package Test;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Test {

    public static void main(String[] args) {

        try (FileChannel channel = new FileInputStream("picture1.bmp").getChannel()) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        int i = 0;
        int j = 100000000;
            while (i < 10) {
                byte[] data;
                if (buffer.remaining() > j) {
                    data = new byte[j];
                } else {
                    data = new byte[buffer.remaining()];
                }
                buffer.get(data);
                System.out.println(data.length);
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
