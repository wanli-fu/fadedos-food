package com.fadedos.food.orderservicemanager.service;


import com.fadedos.food.orderservicemanager.dao.OrderDetailDao;
import com.fadedos.food.orderservicemanager.dto.OrderMessageDTO;
import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import com.fadedos.food.orderservicemanager.po.OrderDetailPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;


//    public void handlerMessage2(@Payload Message message, Channel channel) {
//        log.info("还没有ack");
//        // 手动ack
//        try {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        log.info("自定义方法处理业务逻辑 messageBody:{}", new String(message.getBody()));
//    }


    @RabbitListener(
            bindings = {
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order",
                                    arguments = {
//                                            @Argument(
//                                                    name = "x-message-ttl",
//                                                    value = "1000",
//                                                    type = "java.lang.Integer"
//                                            ),
//                                            @Argument(
//                                                    name = "x-dead-letter-exchange",
//                                                    value = "exchange.dlx"
//                                            )
                                    }
                            ),
                            exchange = @Exchange(name = "exchange.order.restaurant"),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order"
                            ),
                            exchange = @Exchange(name = "exchange.order.deliverymen"),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order"
                            ),
                            exchange = @Exchange(name = "exchange.settlement.order", type = ExchangeTypes.FANOUT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(
                                    name = "queue.order"
                            ),
                            exchange = @Exchange(name = "exchange.order.reward", type = ExchangeTypes.TOPIC),
                            key = "key.order"
                    )
            } )

    public void handleMessage(@Payload Message message, Channel Channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("handleMessage.message:{}", new String(message.getBody()));

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);

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


                        // 商家返回的消息 通过订单模块发送给骑士微服务
                        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                        long deliveryTag1 = message.getMessageProperties().getDeliveryTag();
                        Channel.basicAck(tag,false);

                        rabbitTemplate.convertAndSend(
                                "exchange.order.deliveryman",
                                "key.deliveryman",
                                messageToSend
                        );

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
                        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                        rabbitTemplate.convertAndSend(
                                "exchange.order.settlement",
                                "key.settlement",
                                messageToSend
                        );
                        long deliveryTag1 = message.getMessageProperties().getDeliveryTag();

                        Channel.basicAck(tag,false);
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
                        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                        rabbitTemplate.convertAndSend(
                                "exchange.order.reward",
                                "key.reward",
                                messageToSend
                        );
                        long deliveryTag1 = message.getMessageProperties().getDeliveryTag();

                        Channel.basicAck(tag,false);
                    } else {
                        // 结算失败 导致订单失败
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderDetailPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()) {
                        // 积分微服务返回消息  积分成功
                        // 订单结束  更新数据库
                        orderDetailPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderDetailPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderDetailPO);
                    } else {
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
        } catch (
                Exception e) {
            log.error(e.getMessage(), e);
        }

    }


}