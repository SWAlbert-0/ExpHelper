package fjnu.edu.common.utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * @ClassName HttpUtil
 * @Author zhh
 * @Date 2021/11/30 19:46
 **/
@Component
public class HttpUtil {
    public CloseableHttpResponse excuteGet(URI uri) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(uri);
        CloseableHttpResponse response = httpClient.execute(httpGet);

        return response;
    }
}
