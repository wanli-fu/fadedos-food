package com.fadedos.food.orderservicemanager.vo;

import lombok.Data;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/7
 */
@Data
public class OrderCreateVO {
    /**
     * 用户id
     */
    private Integer accountId;
    /**
     * 地址
     */
    private String address;
    /**
     * 产品id
     */
    private Integer productId;
}
