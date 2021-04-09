package com.fadedos.food.orderservicemanager.dto;


import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/7
 */
@Data
public class OrderMessageDTO {
    /**
     * 订单id
     */
    private Integer orderId;
    /**
     * 订单状态
     */
    private OrderStatus orderStatus;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 骑手id
     */
    private Integer deliverymanId;

    /**
     * 产品id
     */
    private Integer productId;

    /**
     * 用户id
     */
    private Integer accountId;

    /**
     * 结算id
     */

    private Integer settlementId;

    /**
     * 积分结算id
     */
    private Integer rewardId;

    /**
     * 积分奖励数量
     */
    private Integer rewardAmount;

    /**
     * 确认
     */
    private Boolean confirmed;
}
