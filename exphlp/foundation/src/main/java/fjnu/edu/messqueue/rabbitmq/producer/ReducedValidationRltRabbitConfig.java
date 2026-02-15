package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReducedValidationRltRabbitConfig {

    public final static String insertQueueName = "method.BOPM.ReducedValidationRlt.insert";

    @Bean
    public Queue  ReducedValidationRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }

    //Direct交换机 起名：SolutionInsertExchange
    @Bean
    DirectExchange  ReducedValidationRltInsertExchange() {
        return new DirectExchange(" ReducedValidationRltInsertExchange",true,false);
    }

    //绑定  将队列和交换机绑定, 并设置用于匹配键：TestDirectRouting
    @Bean
    Binding  ReducedValidationRltDirect() {
        return BindingBuilder.bind( ReducedValidationRltInsertQueue()).to(ReducedValidationRltInsertExchange()).with(" ReducedValidationRltInsertRouting");
    }
}
