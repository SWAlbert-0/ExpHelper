package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Md5ValidationRltRabbitConfig {

    public final static String insertQueueName = "method.BOPM.Md5ValidationRlt.insert";

    @Bean
    public Queue Md5ValidationRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }

    //Direct交换机 起名：SolutionInsertExchange
    @Bean
    DirectExchange Md5ValidationRltInsertExchange() {
        return new DirectExchange("Md5ValidationRltInsertExchange",true,false);
    }

    //绑定  将队列和交换机绑定, 并设置用于匹配键：TestDirectRouting
    @Bean
    Binding Md5ValidationRltDirect() {
        return BindingBuilder.bind(Md5ValidationRltInsertQueue()).to(Md5ValidationRltInsertExchange()).with("Md5ValidationRltInsertRouting");
    }
}
