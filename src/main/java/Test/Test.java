package Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        File folder = new File(System.getProperty("user.dir"));
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
            //} else if (listOfFiles[i].isDirectory()) {
            //    System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        }
}
