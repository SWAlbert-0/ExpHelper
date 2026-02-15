package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SamePointConfig {
    public final static String insertQueueName = "method.BOPM.SamePointConfig.insert";

    @Bean
    public Queue SameDistRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }


    @Bean
    DirectExchange SameDistRltInsertExchange() {
        return new DirectExchange("SameDistRltExchange",true,false);
    }

    @Bean
    Binding SameDistRltBind() {
        return BindingBuilder.bind(SameDistRltInsertQueue()).to(SameDistRltInsertExchange()).with("SameDistRltInsertRouting");
    }
}
