package com.atguigu.yygh.sms.service;

import com.atguigu.yygh.vo.msm.MsmVo;

import java.util.Map;

public interface SmsService {


    boolean sendCode(String phone);

    void sendMessage(MsmVo msmVo);
}


