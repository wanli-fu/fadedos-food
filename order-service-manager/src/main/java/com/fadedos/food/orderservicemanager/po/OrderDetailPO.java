package com.fadedos.food.orderservicemanager.po;


import com.fadedos.food.orderservicemanager.enummeration.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/7
 */
@Data
public class OrderDetailPO {
    private Integer id;
    private OrderStatus status;
    private String address;
    private Integer accountId;
    private Integer productId;
    private Integer deliverymanId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal price;
    private Date date;
}
