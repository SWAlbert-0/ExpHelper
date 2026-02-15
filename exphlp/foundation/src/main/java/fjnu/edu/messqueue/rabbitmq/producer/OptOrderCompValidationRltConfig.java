package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptOrderCompValidationRltConfig {
    public final static String insertQueueName = "method.BOPM.OptOrderCompValidationRlt.insert";

    @Bean
    public Queue OptOrderCompValidationRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }


    @Bean
    DirectExchange OptOrderCompValidationRltInsertExchange() {
        return new DirectExchange("OptOrderCompValidationRltExchange",true,false);
    }

    @Bean
    Binding OptOrderCompValidationRltInsertDirect() {
        return BindingBuilder.bind(OptOrderCompValidationRltInsertQueue()).to(OptOrderCompValidationRltInsertExchange()).with("OptOrderCompValidationRltInsertRouting");
    }
}
