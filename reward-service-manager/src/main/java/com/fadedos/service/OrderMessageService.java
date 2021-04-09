package com.fadedos.service;


import com.fadedos.dao.RewardDao;
import com.fadedos.dto.OrderMessageDTO;
import com.fadedos.enummeration.RewardStatus;
import com.fadedos.po.RewardPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/12
 */
@Service
@Slf4j
public class OrderMessageService {

    @Autowired
    RewardDao rewardDao;

    @Autowired
    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start listening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(
                    "exchange.order.reward",
                    BuiltinExchangeType.TOPIC,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.reward",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.reward",
                    "exchange.order.reward",
                    "key.reward",
                    null);

            channel.basicConsume(
                    "queue.reward",
                    true,
                    deliverCallback,
                    consumerTag -> {
                    });


            while (true) {
                Thread.sleep(100000);
            }
        }

    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        String messageBody = new String(message.getBody());

        log.info("deliveryCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            log.info("OrderMessageService.orderMessageDTO:{}" + orderMessageDTO);

            //积分存库
            RewardPO rewardPO = new RewardPO();
            rewardPO.setOrderId(orderMessageDTO.getOrderId());
            rewardPO.setAmount(orderMessageDTO.getPrice());
            rewardPO.setStatus(RewardStatus.SUCCESS);
            rewardPO.setDate(new Date());
            rewardDao.insert(rewardPO);

            //orderMessageDTO获取积分id
            orderMessageDTO.setRewardId(rewardPO.getId());

            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish(
                        "exchange.order.reward",
                        "key.order",
                        null,
                        messageToSend.getBytes());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    });
}
