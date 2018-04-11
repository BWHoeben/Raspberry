package Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        String fileName = "Hello.txt";
        byte[] fileNameByte = new byte[60];
        fileNameByte = fileName.getBytes();
        System.out.println(fileNameByte.length);


        }
}
