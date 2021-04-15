package com.fadedos.food.orderservicemanager.service;

import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fadedos.food.orderservicemanager.vo.OrderCreateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @Description:处理用户关于订单的业务请求
 */
@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderDetailDao orderDetailDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        // 创建订单
        OrderDetailPO orderDetailPO = new OrderDetailPO();
        orderDetailPO.setStatus(OrderStatus.ORDER_CREATING);
        orderDetailPO.setAddress(orderCreateVO.getAddress());
        orderDetailPO.setAccountId(orderCreateVO.getAccountId());
        orderDetailPO.setProductId(orderCreateVO.getProductId());
        orderDetailPO.setDate(new Date());

        // 订单持久化
        orderDetailDao.insert(orderDetailPO);

        // 消息体
        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderDetailPO.getId());
        orderMessageDTO.setProductId(orderDetailPO.getProductId());
        orderMessageDTO.setAccountId(orderDetailPO.getAccountId());

        // 发送的消息
        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setExpiration("15000"); // 设置消息过期时间 消息属性
        Message message = new Message(messageToSend.getBytes(), messageProperties);

        //发送端发送成功回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) ->
        {
            log.info("correlationData:{},ack:{},cause:{}",
                    correlationData, ack, cause);
        });

        // 路由失败回调
        rabbitTemplate.setReturnsCallback(returned -> {
            log.info("message:{},replyCode:{}",returned.getMessage(),returned.getReplyCode());
        });

        CorrelationData correlationData = new CorrelationData();
        // 设置消息的对应关系 设置订单id
        correlationData.setId(orderDetailPO.getId().toString());

        // String exchange, String routingKey, Message message
        rabbitTemplate.send(
                "exchange.order.restaurant",
                "key.restaurant",
                message,
                correlationData
        );


        log.info("message sent");

    }
}