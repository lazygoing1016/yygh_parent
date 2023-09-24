package com.atguigu.yygh.sms.service.impl;

import com.atguigu.yygh.sms.service.SmsService;
import com.atguigu.yygh.sms.util.MsgUtils;
import com.atguigu.yygh.sms.util.RandomUtil;
import com.atguigu.yygh.vo.msm.MsmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean sendCode(String phone) {
        String redisCode = (String)redisTemplate.opsForValue().get(phone);
        if(!StringUtils.isEmpty(redisCode)){
            return true;
        }


        try {
            String fourBitRandom = RandomUtil.getFourBitRandom();
            MsgUtils.send(phone, fourBitRandom);
            //保存redis
            redisTemplate.opsForValue().set(phone, fourBitRandom, 50, TimeUnit.DAYS);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return  false;
            }
        }

    @Override
    public void sendMessage(MsmVo msmVo) {
        String phone = msmVo.getPhone();
        String templateCode = msmVo.getTemplateCode();
        System.out.println("发送成功");
        try {
            MsgUtils.send(phone,templateCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}