package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParaValidationRltConfig {
    public final static String insertQueueName = "method.BOPM.ParaValidationRlt.insert";

    @Bean
    public Queue ParaValidtionRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }


    @Bean
    DirectExchange ParaValidtionRltInsertExchange() {
        return new DirectExchange("ParaValidationRltExchange",true,false);
    }

    @Bean
    Binding ParaValidtionRltInsertDirect() {
        return BindingBuilder.bind(ParaValidtionRltInsertQueue()).to(ParaValidtionRltInsertExchange()).with("ParaValidationRltInsertRouting");
    }
}
