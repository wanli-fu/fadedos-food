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
import java.util.HashMap;
import java.util.Map;
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

    @Autowired
    private Channel channel;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {

        // 声明接收死信的exchange
        channel.exchangeDeclare(
                "exchange.dlx",
                BuiltinExchangeType.TOPIC,
                true,
                false,
                null
        );

        // 声明接收死信消息的队列
        channel.queueDeclare(
                "queue.dlx",
                true,
                false,
                false,
                null
        );

        // 绑定
        channel.queueBind(
                "queue.dlx",
                "exchange.dlx",
                "#"
        );



        // 声明 exchange
        channel.exchangeDeclare(
                "exchange.order.restaurant",
                BuiltinExchangeType.DIRECT,
                true,
                false,
                null
        );

        Map<String, Object> args = new HashMap<>(16);
        args.put("x-message-ttl", 15000); // 设置队列中所有消息的过期时间15s
        args.put("x-dead-letter-exchange", "exchange.dlx"); //死信转发的交换机
        args.put("x-max-length", 10); //队列最大长度是10

        //声明队列
        channel.queueDeclare(
                "queue.restaurant",
                true,
                false,
                false,
                args //加入额外参数 此处队列中所有消息的过期时间
        );

        //绑定
        channel.queueBind(
                "queue.restaurant",
                "exchange.order.restaurant",
                "key.restaurant"
        );

        // 一个消费端最多推送2条未确认的消息,其他消息ready状态,便于横向扩展
        channel.basicQos(2);

        // 监听消息队列 随时接收消息
        channel.basicConsume(
                "queue.restaurant",
                false,
                deliverCallback,
                consumerTag -> {
                });

        // 该线程不会退出,随时监听队列消息
        while (true) {
            Thread.sleep(10000000);
        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        String messageBody = new String(message.getBody());

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

            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    log.info("Message Return: returnMessage:{}", returnMessage);

                    //除了打印log,可以别的业务操作 业务告警 短信等等处理这个消息
                }
            });

            Thread.sleep(3000);
            //  收到消息 手动ACK 单条
            // 该channel必须是 接收消息消费时channel

            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);

            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            // 给订单微服务 发商家确认订单消息
            channel.basicPublish(
                    "exchange.order.restaurant",
                    "key.order",
                    true, // rabbit mq收到消息,若无法路由,则调用发送端的runListener
                    null,
                    messageToSend.getBytes()
            );
            Thread.sleep(1000);


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}

