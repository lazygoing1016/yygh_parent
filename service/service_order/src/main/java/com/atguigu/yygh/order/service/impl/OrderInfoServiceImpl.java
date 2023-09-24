package com.atguigu.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.client.PatientFeignClient;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.ScheduleFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.mq.MqConst;
import com.atguigu.yygh.mq.RabbitService;
import com.atguigu.yygh.order.mapper.OrderInfoMapper;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.order.utils.HttpRequestHelper;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-09-23
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private ScheduleFeignClient scheduleFeignClient;

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public Long submitOrder(String scheduleId, Long patientId) {
        //1.先根据scheduleId获取医生排班信息
        ScheduleOrderVo scheduleById = scheduleFeignClient.getScheduleById(scheduleId);

        if(new DateTime(scheduleById.getStopTime()).isBeforeNow()){
            throw new YyghException(20001,"超过挂号截至时间");
        }

        //2.先根据patientId获就诊人信息
        Patient patientById = patientFeignClient.getPatientById(patientId);

        //3.从平台请求第三方医院，确认当前用户能否挂号
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode",scheduleById.getHoscode());
        paramMap.put("depcode",scheduleById.getDepcode());
        paramMap.put("hosScheduleId",scheduleById.getHosScheduleId());

        paramMap.put("reserveDate",scheduleById.getReserveDate());
        paramMap.put("reserveTime",scheduleById.getReserveTime());
        paramMap.put("amount",scheduleById.getAmount());


        JSONObject jsonObject = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");
        if(jsonObject != null && jsonObject.getInteger("code") == 200){
            JSONObject data = jsonObject.getJSONObject("data");


            OrderInfo orderInfo=new OrderInfo();
            orderInfo.setUserId(patientById.getUserId());
            String outTradeNo = System.currentTimeMillis() + ""+ new Random().nextInt(100);
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setHoscode(scheduleById.getHoscode());
            orderInfo.setHosname(scheduleById.getHosname());
            orderInfo.setDepcode(scheduleById.getDepcode());
            orderInfo.setDepname(scheduleById.getDepname());
            orderInfo.setTitle(scheduleById.getTitle());
            orderInfo.setReserveDate(scheduleById.getReserveDate());
            orderInfo.setReserveTime(scheduleById.getReserveTime());
            orderInfo.setScheduleId(scheduleById.getHosScheduleId());
            orderInfo.setPatientId(patientById.getId());
            orderInfo.setPatientName(patientById.getName());
            orderInfo.setPatientPhone(patientById.getPhone());


            orderInfo.setHosRecordId(data.getString("hosRecordId"));
            orderInfo.setNumber(data.getInteger("number"));
            orderInfo.setFetchTime(data.getString("fetchTime"));
            orderInfo.setFetchAddress(data.getString("fetchAddress"));

            orderInfo.setAmount(scheduleById.getAmount());
            orderInfo.setQuitTime(scheduleById.getQuitTime());
            orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());

            //3.2 如果返回能挂号，就把取医生排班信息、就诊人信息及第三方医院返回的信息都添加到order_info表中
            baseMapper.insert(orderInfo);

            //3.3 更新平台上对应医生的剩余可预约数
            OrderMqVo orderMqVo=new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            //int reservedNumber = data.getIntValue("reservedNumber");
            int availableNumber = data.getIntValue("availableNumber");
            orderMqVo.setAvailableNumber(availableNumber);

            MsmVo msmVo=new MsmVo();
            msmVo.setPhone(patientById.getPhone());

            msmVo.setTemplateCode("6666");
            Map<String,Object> msmMap=new HashMap<String, Object>();
            msmMap.put("time",scheduleById.getReserveDate()+" "+scheduleById.getReserveTime());
            msmMap.put("name","xxx");
            msmVo.setParam(msmMap);
            orderMqVo.setMsmVo(msmVo);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER,MqConst.ROUTING_ORDER,orderMqVo);
            //3.4 给就诊人发送短信提醒


            //4.返回订单的id
            return orderInfo.getId();

        }else{
            //3.1 如果返回不能挂号，直接抛出异常
            throw new YyghException(20001,"号源已满");
        }
    }

    @Override
    public Page<OrderInfo> getOrderInfoPage(Integer pageNum, Integer pageSize, OrderQueryVo orderQueryVo) {
        Page page=new Page(pageNum,pageSize);
        QueryWrapper<OrderInfo> queryWrapper=new QueryWrapper<OrderInfo>();

        Long userId = orderQueryVo.getUserId(); //用户id
        String outTradeNo = orderQueryVo.getOutTradeNo();//订单号
        String keyword = orderQueryVo.getKeyword();//医院名称
        Long patientId = orderQueryVo.getPatientId(); //就诊人id
        String orderStatus = orderQueryVo.getOrderStatus();//订单状态
        String reserveDate = orderQueryVo.getReserveDate(); //预约日期
        String createTimeBegin = orderQueryVo.getCreateTimeBegin();//下订单时间
        String createTimeEnd = orderQueryVo.getCreateTimeEnd();//下订单时间

        if(!StringUtils.isEmpty(userId)){
            queryWrapper.eq("user_id", userId);
        }
        if(!StringUtils.isEmpty(outTradeNo)){
            queryWrapper.eq("out_trade_no", outTradeNo);
        }
        if(!StringUtils.isEmpty(keyword)){
            queryWrapper.like("hosname", keyword);
        }
        if(!StringUtils.isEmpty(patientId)){
            queryWrapper.eq("patient_id", patientId);
        }
        if(!StringUtils.isEmpty(orderStatus)){
            queryWrapper.eq("order_status", orderStatus);
        }
        if(!StringUtils.isEmpty(reserveDate)){
            queryWrapper.ge("reserve_date", reserveDate);
        }
        if(!StringUtils.isEmpty(createTimeBegin)){
            queryWrapper.ge("create_time", createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)){
            queryWrapper.le("create_time", createTimeEnd);
        }
        Page<OrderInfo> page1 = baseMapper.selectPage(page, queryWrapper);
        page1.getRecords().parallelStream().forEach(item->{
            this.packageOrderInfo(item);
        });

        return page1;
    }

    @Override
    public OrderInfo detail(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        this.packageOrderInfo(orderInfo);
        return orderInfo;
    }

    private void packageOrderInfo(OrderInfo item) {
        item.getParam().put("orderStatusString",OrderStatusEnum.getStatusNameByStatus(item.getOrderStatus()));
    }
}
