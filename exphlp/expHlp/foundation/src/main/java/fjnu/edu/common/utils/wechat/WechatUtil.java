package fjnu.edu.common.utils.wechat;

import com.alibaba.fastjson.JSONObject;
import fjnu.edu.common.config.weconfig.WxMpProperties;
import fjnu.edu.common.utils.HttpUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName WechatUtil
 * @Author zhh
 * @Date 2021/11/30 19:31
 **/
@Component
public class WechatUtil {
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    HttpUtil httpUtil;

    public String getAccessToken() throws URISyntaxException, IOException {
        String token = (String) redisTemplate.opsForValue().get("access_token");
        if (token == null){
            return token;
        }else {
            URI uri = new URIBuilder().setScheme("https")
                    .setHost("api.weixin.qq.com")
                    .setPath("/cgi-bin/token")
                    .setParameter("grant_type","client_credential")
                    .setParameter("appid", WxMpProperties.APP_ID)
                    .setParameter("secret",WxMpProperties.SECRET)
                    .build();
            CloseableHttpResponse response = httpUtil.excuteGet(uri);
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            byte[] data = readInputStream(inputStream);
            inputStream.read(data);
            String str = new String(data);
            JSONObject json = JSONObject.parseObject(str);
            token = (String) json.get("access_token");
            redisTemplate.opsForValue().set("access_token", token,55*2, TimeUnit.MINUTES);
            System.out.println(str);
            return token;
        }

    }

    public static  byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}
