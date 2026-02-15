package fjnu.edu.common.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.FileInputStream;

public class Md5Helper {
    public static String getMd5(String filePath) throws Exception{
        FileInputStream inputStream = new FileInputStream(filePath);
        String md5Str = DigestUtils.md5Hex(inputStream);
        inputStream.close();
        return md5Str;
    }
}
