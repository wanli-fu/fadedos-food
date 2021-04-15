package com.fadedos.food.orderservicemanager.config;

import com.fadedos.food.orderservicemanager.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/4/7
 */
@Configuration
@Slf4j
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;


//    /*-------restaurant----------*/
//    // 交换机由双方同时声明
//    @Bean
//    public Exchange exchange1() {
//        return new DirectExchange("exchange.order.restaurant");
//    }
//
//    // 队列由接受方 声明并配置绑定关系
//    @Bean
//    public Queue queue1() {
//        return new Queue("queue.order");
//    }
//
//    @Bean
//    public Binding binding1() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.restaurant",
//                "key.order",
//                null);
//    }
//
//
//    /*-------deliveryman----------*/
//    // 交换机由双方同时声明
//    @Bean
//    public Exchange exchange2() {
//        return new DirectExchange("exchange.order.deliveryman");
//    }
//
//    @Bean
//    public Binding binding2() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.deliveryman",
//                "key.order",
//                null);
//    }
//
//
//    /*-------settlement----------*/
//    // 交换机由双方同时声明
//    // 因为Exchange 为fanout routing key没有作用  则声明两个虚拟机 对应不同的微服务
//    // 订单模块 发送消息给 结算微服务
//    @Bean
//    public Exchange exchange3() {
//        return new FanoutExchange("exchange.order.settlement");
//    }
//
//    //订单模块接收 结算微服务消息
//    @Bean
//    public Exchange exchange4() {
//        return new FanoutExchange("exchange.settlement.order");
//    }
//
//    @Bean
//    public Binding binding3() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.settlement.order",
//                "key.order",
//                null);
//    }
//
//    /*-------reward----------*/
//    // 交换机由双方同时声明
//    @Bean
//    public Exchange exchange5() {
//        return new TopicExchange("exchange.order.reward");
//    }
//
//    @Bean
//    public Binding binding4() {
//        return new Binding(
//                "queue.order",
//                Binding.DestinationType.QUEUE,
//                "exchange.order.reward",
//                "key.order",
//                null);
//    }

    /**
     * rabbit mq连接工厂
     *
     * @return
     */
//    @Bean
//    public ConnectionFactory connectionFactory() {
//        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
//        connectionFactory.setHost("129.28.198.9");
//        connectionFactory.setPort(5672);
//        connectionFactory.setUsername("wanli");
//        connectionFactory.setPassword("123456");
//        // 开启消息发送确认
//        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
//        // 开启消息返回确认
//        connectionFactory.setPublisherReturns(true);
//        return connectionFactory;
//    }

//    @Bean
//    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
//        rabbitAdmin.setAutoStartup(true);
//        return rabbitAdmin;
//    }

//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//
//        // 开启消息返回机制(正确路由)
//        rabbitTemplate.setMandatory(true);
//        rabbitTemplate.setReturnsCallback(returned -> {
//            log.info("message:{},replyCode:{}", returned.getMessage(), returned.getReplyCode());
//        });
//
//        // 消息发送确认
//        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
//            log.info("correlationData:{},ack:{},cause:{}",
//                    correlationData, ack, cause);
//        });
//        return rabbitTemplate;
//    }
//
//
//
//    @Bean
//    public RabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
//        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//        factory.setConnectionFactory(connectionFactory);
//        return factory;
//    }
}
