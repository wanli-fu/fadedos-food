package com.fadedos.food.orderservicemanager.config;

import com.fadedos.food.orderservicemanager.service.OrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/4/7
 */
@Configuration
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;

    @Autowired // 此处注入到spring 此方法会在应用启动自动执行
    public  void startListenMessage() throws InterruptedException, TimeoutException, IOException {
        orderMessageService.handleMessage();
    }
}
