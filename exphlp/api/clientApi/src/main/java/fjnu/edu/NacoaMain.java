package fjnu.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class NacoaMain {
    public static void main(String[] args) {
        SpringApplication.run(NacoaMain.class, args);
    }
    @Bean("algRestTemplate")
    @LoadBalanced
    RestTemplate restTemplate(
            @org.springframework.beans.factory.annotation.Value("${alg.call.connect-timeout-ms:3000}") int connectTimeout,
            @org.springframework.beans.factory.annotation.Value("${alg.call.read-timeout-ms:10000}") int readTimeout
    ){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }
}
