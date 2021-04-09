package com.fadedos.food.orderservicemanager.controller;

import com.fadedos.food.orderservicemanager.service.OrderService;
import com.fadedos.food.orderservicemanager.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/4/7
 */
@RestController
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    public void createOrder(@RequestBody OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        log.info("createOrder:orderCreateVO{}",orderCreateVO);
        orderService.createOrder(orderCreateVO);

    }
}
