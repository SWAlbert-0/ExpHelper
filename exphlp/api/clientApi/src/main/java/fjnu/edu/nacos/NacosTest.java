package fjnu.edu.nacos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@RestController
@CrossOrigin
@RequestMapping("/api/NacosController")
public class NacosTest {
    @Resource
    private RestTemplate restTemplate;
    @Value("${service-url.nacos-user-service}")
    private String serverURL;

    @GetMapping(value = "/consumer/payment/nacos/{id}")
    public void paymentInfo(@PathVariable("id") Long id)
    {
        String s =restTemplate.getForObject(serverURL+"/product/nacos/"+id,String.class);

        System.out.println("远程调用结果"+s);
    }
}
