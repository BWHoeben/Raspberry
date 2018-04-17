package Test;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Test {

    public Test() {

    }

    public static void main(String[] args) {
        byte[] array = new byte[2];
        new Random().nextBytes(array);

        MessageDigest md5Digest = null;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            print(e.getMessage());
        }
        byte[] hash = md5Digest.digest(array);
        print("Hash length: " + hash.length);
        print("Array length: " + array.length);
    }


    private static void print(String msg) {

        System.out.println(msg);
    }

}