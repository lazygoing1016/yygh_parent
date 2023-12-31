package com.atguigu.yygh.hosp.listener;

import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.mq.MqConst;
import com.atguigu.yygh.mq.RabbitService;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderMqListener {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private RabbitService rabbitService;

    @RabbitListener(bindings = {
            @QueueBinding(
                    value =@Queue(name = MqConst.QUEUE_ORDER,durable = "true"),//创建队列
                    exchange = @Exchange(name = MqConst.EXCHANGE_DIRECT_ORDER), //创建交换机
                    key=MqConst.ROUTING_ORDER
            )
    })
    public void consume(OrderMqVo orderMqVo, Message message, Channel channel){
        String scheduleId = orderMqVo.getScheduleId();
        Integer availableNumber = orderMqVo.getAvailableNumber();
        boolean flag= scheduleService.updateAvailableNumber(scheduleId,availableNumber);

        MsmVo msmVo = orderMqVo.getMsmVo();

        if(flag && msmVo != null){
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SMS,MqConst.ROUTING_SMS_ITEM,msmVo);
        }

    }
}
