package com.fadedos.service;

import com.fadedos.dao.ProductDao;
import com.fadedos.dao.RestaurantDao;
import com.fadedos.dto.OrderMessageDTO;
import com.fadedos.enummeration.ProductStatus;
import com.fadedos.enummeration.RestaurantStatus;
import com.fadedos.po.ProductPO;
import com.fadedos.po.RestaurantPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private RestaurantDao restaurantDao;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        // 原生代码 com.rabbitmq.client.ConnectionFactory
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {


            // 声明 exchange
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null
            );

            //声明队列
            channel.queueDeclare(
                    "queue.restaurant",
                    true,
                    false,
                    false,
                    null
            );

            //绑定
            channel.queueBind(
                    "queue.restaurant",
                    "exchange.order.restaurant",
                    "key.restaurant"
            );


            // 监听消息队列 随时接收消息
            channel.basicConsume(
                    "queue.restaurant",
                    true,
                    deliverCallback,
                    consumerTag -> {
                    });

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
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);

            // 校验商品 商户 是否正常状态
            ProductPO productPO = productDao.selsctProduct(orderMessageDTO.getProductId());
            RestaurantPO restaurantPO = restaurantDao.selsctRestaurant(productPO.getRestaurantId());

            if (productPO.getStatus() == ProductStatus.AVAILABLE &&
                    restaurantPO.getStatus() == RestaurantStatus.OPEN) {

                // 正常 则消息体加上 商家确认  商品价格
                orderMessageDTO.setConfirmed(true);
                orderMessageDTO.setPrice(productPO.getPrice());

            } else {
                orderMessageDTO.setConfirmed(false);
            }
            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                // 给订单微服务 发商家确认订单消息
                channel.basicPublish(
                        "exchange.order.restaurant",
                        "key.order",
                        null,
                        messageToSend.getBytes()
                );
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}

