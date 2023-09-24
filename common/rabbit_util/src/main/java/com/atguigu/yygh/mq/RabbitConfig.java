package com.atguigu.yygh.mq;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class RabbitConfig {

    @Bean
    public MessageConverter getMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
