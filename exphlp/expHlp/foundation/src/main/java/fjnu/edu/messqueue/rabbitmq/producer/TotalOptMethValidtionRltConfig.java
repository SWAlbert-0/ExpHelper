package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotalOptMethValidtionRltConfig {

    public final static String insertQueueName = "method.BOPM.TotalOptMethValidtionRlt.insert";

    @Bean
    public Queue TotalOptMethValidtionRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }


    @Bean
    DirectExchange TotalOptMethValidtionRltInsertExchange() {
        return new DirectExchange("TotalOptMethValidtionRltExchange",true,false);
    }

    @Bean
    Binding TotalOptMethValidtionRltBind() {
        return BindingBuilder.bind(TotalOptMethValidtionRltInsertQueue()).to(TotalOptMethValidtionRltInsertExchange()).with("TotalOptMethValidtionRltInsertRouting");
    }
}
