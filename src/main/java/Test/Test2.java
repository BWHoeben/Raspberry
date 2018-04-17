package Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import Tools.Tools;

public class Test2 {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        File file = new File("test.txt");

        System.out.println(file.exists());

        /*File fileTest = new File("test.txt");
        File filePic1 = new File("picture1.bmp");
        File filePic2 = new File("picture1.bmp");

        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        String test = getFileChecksum(md5Digest, fileTest);
        String pic1 = getFileChecksum(md5Digest, filePic1);
        String pic2 = getFileChecksum(md5Digest, filePic2);
        byte[] pic1Byte = pic1.getBytes();
        byte[] pic2Byte = pic2.getBytes();

        System.out.println(new String(pic1Byte).equals(new String(pic2Byte)));

        byte[] test  = Tools.getHash("test.txt");
        byte[] pic1  = Tools.getHash("picture1.bmp");
        byte[] pic2  = Tools.getHash("picture2.bmp");
        System.out.println(Arrays.equals(test, pic1));
        System.out.println(Arrays.equals(pic2, pic1));
        */
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        //Get file input stream for reading the file content
        try (FileInputStream fis = new FileInputStream(file)) {


            //Create byte array to read data in chunks
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            //Read file data and update in message digest
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            //Get the hash's bytes
            byte[] bytes = digest.digest();

            //This bytes[] has bytes in decimal format;
            //Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            //return complete hash
            return sb.toString();
        }
    }

}