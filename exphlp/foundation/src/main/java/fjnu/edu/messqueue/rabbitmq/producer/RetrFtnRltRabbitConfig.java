package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetrFtnRltRabbitConfig {

    public final static String insertQueueName = "method.BOPM.RetrFtnRlt.insert";
    public final static String updateQueueName = "method.BOPM.RetrFtnRlt.update";

    @Bean
    public Queue RetrFtnRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }
    @Bean
    public Queue RetrFtnRltUpdateQueue() {
        return new Queue(updateQueueName,true,false,false);
    }

    //Direct交换机 起名：RetrFtnRltInsertExchange
    @Bean
    DirectExchange RetrFtnRltInsertExchange() {
        return new DirectExchange("RetrFtnRltExchange",true,false);
    }
    @Bean
    DirectExchange RetrFtnRltUpdateExchange() {
        return new DirectExchange("RetrFtnRltExchange",true,false);
    }

    //绑定  将队列和交换机绑定, 并设置用于匹配键：TestDirectRouting
    @Bean
    Binding BOPMRetrFtnRltDirect() {
        return BindingBuilder.bind(RetrFtnRltInsertQueue()).to(RetrFtnRltInsertExchange()).with("RetrFtnRltInsertRouting");
    }
    @Bean
    Binding BOPMRetrFtnRltDirect1() {
        return BindingBuilder.bind(RetrFtnRltUpdateQueue()).to(RetrFtnRltUpdateExchange()).with("RetrFtnRltUpdateRouting");
    }

}
