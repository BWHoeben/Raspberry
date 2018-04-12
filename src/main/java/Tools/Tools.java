package Tools;

import java.io.*;
import java.util.Map;

public class Tools {
    private Tools() {

    }

    public static byte[] intToByteArray (final int integer) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(integer);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static byte[] appendBytes(byte[] originalArray, byte[] toAppend) {
        byte[] returnArray = new byte[originalArray.length + toAppend.length];
        System.arraycopy(originalArray, 0, returnArray, 0, originalArray.length);
        System.arraycopy(toAppend, 0, returnArray, originalArray.length, toAppend.length);
        return returnArray;
    }

    public static byte[] appendThisMapToAnArray(Map<Integer, byte[]> map) {
     byte[] returnArray = new byte[0];
        for (int i = 0; i < map.size(); i++) {
            if (map.containsKey(i)) {
                returnArray = appendBytes(returnArray, map.get(i));
            }
        }
        return returnArray;
    }

}
