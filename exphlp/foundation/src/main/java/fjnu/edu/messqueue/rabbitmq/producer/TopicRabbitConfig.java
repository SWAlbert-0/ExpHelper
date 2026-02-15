package fjnu.edu.messqueue.rabbitmq.producer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicRabbitConfig {
    //绑定键
    public final static String saveir = "topic.saveir";
    public final static String popuresult = "topic.popuresult";
    public final static String methodExeRltQueue = "methodExeRltQueue";
    public final static String methodExeRltTopic = "topic.methodExeRlt";
    public final static String BOCAExeRltQueue = "BOCARltQueue";
    public final static String BOCAExeRltTopic = "topic.BOCARlt";
    public final static String COSUHAExeRltQueue = "COSUHARltQueue";
    public final static String COSUHAExeRltTopic = "topic.COSUHARlt";
    public final static String COSUHAClusterRltQueue = "COSUHAClusterRltQueue";
    public final static String COSUHAClusterRltTopic = "topic.COSUHAClusterRlt";
    public final static String ICMCRltQueue ="ICMCRltQueue";
    public final static String ICMCRltTopic = "topic.ICMCRlt";




    @Bean
    public Queue firstQueue() {
        return new Queue(TopicRabbitConfig.saveir);
    }

    @Bean
    public Queue secondQueue() {
        return new Queue(TopicRabbitConfig.popuresult);
    }

    @Bean
    /***
     *@Description创建一个用于保存计划执行结果的消息队列
     *@Date 2022/7/18 15:22
     */
    public Queue thirdQueue(){return new Queue(TopicRabbitConfig.methodExeRltQueue);}

    @Bean
    /***
     *@Description创建一个用于保存BOCA算法执行结果的消息队列
     */
    public Queue fourthQueue(){return new Queue(TopicRabbitConfig.BOCAExeRltQueue);}

    @Bean
    /***
     *@Description创建一个用于保存COSUHA算法执行结果的消息队列
     */
    public Queue fifthQueue(){return new Queue(TopicRabbitConfig.COSUHAExeRltQueue);}

    @Bean
    /***
     *@Description创建一个用于保存COSUHA算法聚簇执行结果的消息队列
     */
    public Queue sixthQueue(){return new Queue(TopicRabbitConfig.COSUHAClusterRltQueue);}

    @Bean
    /***
     *@Description创建一个用于保存ICMC算法聚簇执行结果的消息队列
     */
    public Queue seventhQueue(){return new Queue(TopicRabbitConfig.ICMCRltQueue);}



    @Bean
    TopicExchange exchange() {
        return new TopicExchange("topicExchange");
    }

    //将firstQueue和topicExchange绑定,而且绑定的键值为topic.saveir
    //这样只要是消息携带的路由键是topic.saveir,才会分发到该队列
    @Bean
    Binding bindingExchangeMessage() {
        return BindingBuilder.bind(firstQueue()).to(exchange()).with(saveir);
    }

    //将secondQueue和topicExchange绑定,而且绑定的键值为topic.popuresult
    //这样只要是消息携带的路由键是topic.popuresult,才会分发到该队列
    @Bean
    Binding bindingExchangeMessage1() {
        return BindingBuilder.bind(secondQueue()).to(exchange()).with(popuresult);
    }

    @Bean
    /***
     *@Description 将保存执行结果的消息队列和特定的主题exePlanRunRltTopic捆绑
     *@Date 2022/7/18 15:22
     */
    Binding bindingExchangeMessage2(){return  BindingBuilder.bind(thirdQueue()).to(exchange()).with(methodExeRltTopic);}

    @Bean
    /***
     *@Description 将保存BOCA执行结果的消息队列和特定的主题BOCAExeRltTopic捆绑
     */
    Binding bindingExchangeMessage3(){return  BindingBuilder.bind(fourthQueue()).to(exchange()).with(BOCAExeRltTopic);}

    @Bean
    /***
     *@Description 将保存COSUHA执行结果的消息队列和特定的主题COSUHAExeRltTopic捆绑
     */
    Binding bindingExchangeMessage4(){return  BindingBuilder.bind(fifthQueue()).to(exchange()).with(COSUHAExeRltTopic);}

    @Bean
    /***
     *@Description 将保存COSUHA的聚簇的执行结果的消息队列和特定的主题COSUHAClusterRltTopic捆绑
     */
    Binding bindingExchangeMessage5(){return  BindingBuilder.bind(sixthQueue()).to(exchange()).with(COSUHAClusterRltTopic);}


    @Bean
    /***
     *@Description 将保存ICMC的聚簇的执行结果的消息队列和特定的主题ICMCRltTopic捆绑
     */
    Binding bindingExchangeMessage6(){return  BindingBuilder.bind(seventhQueue()).to(exchange()).with(ICMCRltTopic);}
}
