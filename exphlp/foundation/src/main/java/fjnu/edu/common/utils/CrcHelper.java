package fjnu.edu.common.utils;

import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public class CrcHelper {
    public static long getCRC32Value(String filePath) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public static String getCRC32Str(String filePath) throws Exception {
        return Long.toHexString(getCRC32Value(filePath)).toUpperCase();
    }

    //将CRC码转换成32位的二进制串
    public static String getCRC32StrOfBinary(String filePath) throws Exception {
        long str = getCRC32Value(filePath);
        String hex = Long.toHexString(str).toUpperCase();
        if(StringUtils.isEmpty(hex)||hex.length()%2!=0)
        {
            return "";
        }
        StringBuilder binaryString = new StringBuilder();
        String tmp;
        for(int i=0;i<hex.length();i++)
        {
            tmp = "0000"+Integer.toBinaryString(Integer.parseInt(hex.substring(i,i+1),16));
            binaryString.append(tmp.substring(tmp.length()-4));
        }
        System.out.println(binaryString.toString());
        return binaryString.toString();
    }
}
