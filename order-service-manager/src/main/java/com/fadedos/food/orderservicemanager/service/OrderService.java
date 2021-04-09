package com.fadedos.food.orderservicemanager.service;

import java.io.IOException;
import java.math.BigDecimal;

import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;

import java.util.Date;
import java.util.concurrent.TimeoutException;


import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fadedos.food.orderservicemanager.vo.OrderCreateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        //消息体
        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderDetailPO.getId());
        orderMessageDTO.setProductId(orderDetailPO.getProductId());
        orderMessageDTO.setAccountId(orderDetailPO.getAccountId());


        // 原生代码 com.rabbitmq.client.ConnectionFactory
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("129.28.198.9");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("wanli");
        connectionFactory.setPassword("123456");

        // Connection ,Channel 都实现 AutoCloseable接口,发生异常可自动关闭
        try (Connection connection = connectionFactory.newConnection();

             Channel channel = connection.createChannel()) {

            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);

            channel.basicPublish(
                    "exchange.order.restaurant",
                    "key.restaurant", // 此路由key是在 餐厅微服务模块中声明
                    null,
                    messageToSend.getBytes()
            );

        }


    }
}
