package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProbModelRltRabbitConfig {

    public final static String insertQueueName = "method.BOPM.ProbModelRlt.insert";

    @Bean
    public Queue ProbModelRltInsertQueue() {
        return new Queue(insertQueueName,true,false,false);
    }

    //Direct交换机 起名：SolutionInsertExchange
    @Bean
    DirectExchange ProbModelRltInsertExchange() {
        return new DirectExchange("ProbModelRltExchange",true,false);
    }

//    绑定  将队列和交换机绑定, 并设置用于匹配键：TestDirectRouting
    @Bean
    Binding BOPMProbModelRltDirect() {
        return BindingBuilder.bind(ProbModelRltInsertQueue()).to(ProbModelRltInsertExchange()).with("ProbModelRltInsertRouting");
    }

}
