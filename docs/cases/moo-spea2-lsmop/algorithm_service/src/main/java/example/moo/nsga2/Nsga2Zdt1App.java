package example.moo.nsga2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class Nsga2Zdt1App {
    public static void main(String[] args) {
        SpringApplication.run(Nsga2Zdt1App.class, args);
    }
}
