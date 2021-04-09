# Rabbit MQ实践

# 1. 服务模块

* order-service-manager
  * 订单模块
  * exchange采用direct
* restaurant-service-manager
  * 商家模块
  * exchange采用direct
* deliveryman-service-manager
  * 骑手模块
  * exchange采用direct
* settlement-service-manager
  * 结算模块
  * exchange采用fanout
  * 注意声明了两个交换机
* reward-service-manager
  * 积分模块
  * exchange采用了topic