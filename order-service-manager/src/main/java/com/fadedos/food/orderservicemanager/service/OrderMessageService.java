package com.fadedos.food.orderservicemanager.service;


import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 消息处理相关业务逻辑
 *
 * @author wlzfw
 */
@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderDetailDao orderDetailDao;

    /**
     * 声明消息队列,交换机,绑定,消息处理
     */
    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {

        // 原生代码 com.rabbitmq.client.ConnectionFactory
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        // Connection ,Channel 都实现 AutoCloseable接口,发生异常可自动关闭
        try (Connection connection = connectionFactory.newConnection();

             Channel channel = connection.createChannel()) {

            /*-------restaurant----------*/
            // 交换机由双方同时声明
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            // 队列由接受方 声明并配置绑定关系
            channel.queueDeclare(
                    "queue.order",
                    true,
                    false,
                    false,
                    null);

            // 绑定 交换机和队列 设置路由键
            channel.queueBind(
                    "queue.order",
                    "exchange.order.restaurant",
                    "key.order");

            /*-------deliveryman----------*/
            // 交换机由双方同时声明
            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.deliveryman",
                    "key.order"
            );


            /*-------settlement----------*/
            // 交换机由双方同时声明
            // 因为Exchange 为fanout routing key没有作用  则声明两个虚拟机 对应不同的微服务
            channel.exchangeDeclare(
                    "exchange.order.settlement", // 订单模块 发送消息给 结算微服务
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.exchangeDeclare(
                    "exchange.settlement.order", //订单模块接收 结算微服务消息
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            // 绑定
            channel.queueBind(
                    "queue.order",
                    "exchange.settlement.order",
                    "key.order"
            );

            /*-------reward----------*/
            // 交换机由双方同时声明
            channel.exchangeDeclare(
                    "exchange.order.reward",
                    BuiltinExchangeType.TOPIC,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.reward",
                    "key.order"
            );


            // 消息消费
            channel.basicConsume(
                    "queue.order",
                    true,
                    deliverCallback,
                    consumerTag -> {  //消费者标签 暂时为空
                    });
            log.info("收到其他微服务的消息");

            // 该线程不会退出,随时监听队列消息
            while (true) {
                Thread.sleep(10000000);
            }
        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        String messageBody = new String(message.getBody());


        // 原生代码 com.rabbitmq.client.ConnectionFactory
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        try {
            // 将消息体反序列化成DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);

            // 数据库中读取订单
            OrderDetailPO orderDetailPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());

            // 根据订单状态  确认消息是来自哪个微服务 商家微服务 骑手微服务
            switch (orderDetailPO.getStatus()) {

                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderDetailPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderDetailPO.setPrice(orderMessageDTO.getPrice());
                        // 更新数据库 商家已确认
                        orderDetailDao.update(orderDetailPO);

                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {

                            // 商家返回的消息 通过订单模块发送给骑士微服务
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.deliveryman",
                                    "key.deliveryman",
                                    null,
                                    messageToSend.getBytes()
                            );
                        }
                    } else {
                        // 商家确认失败 ,订单失败
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        // 骑手确认,回复订单模块消息
                        orderDetailPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderDetailPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());

                        // 更新订单详情
                        orderDetailDao.update(orderDetailPO);

                        // 骑手确认OK后, 订单微服务向结算模块发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.settlement",
                                    "key.settlement",
                                    null,
                                    messageToSend.getBytes()
                            );
                        }
                    } else {
                        // 骑手微服务 处理失败,更新订单详情
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    if (null != orderMessageDTO.getSettlementId()) {
                        // 结算完成, 更新订单详情
                        orderDetailPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderDetailDao.update(orderDetailPO);

                        // 订单向 积分微服务发送
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                         channel.basicPublish(
                                 "exchange.order.reward",
                                 "key.reward",
                                 null,
                                 messageToSend.getBytes()
                         );
                        }
                    } else {
                        // 结算失败 导致订单失败
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()){
                        // 积分微服务返回消息  积分成功
                        // 订单结束  更新数据库
                        orderDetailPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderDetailPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderDetailPO);
                    } else{
                        // 积分失败 导致订单失败
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case ORDER_CREATED:
                    break;
                case FAILED:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + orderDetailPO.getStatus());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}