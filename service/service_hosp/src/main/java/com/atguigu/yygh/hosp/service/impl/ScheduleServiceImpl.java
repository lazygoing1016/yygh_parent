package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Override
    public void saveSchedule(Map<String, Object> stringObjectMap) {
        Schedule schedule = JSONObject.parseObject(JSONObject.toJSONString(stringObjectMap), Schedule.class);
        String hoscode = schedule.getHoscode();
        String depcode = schedule.getDepcode();
        String hosScheduleId = schedule.getHosScheduleId();
        Schedule platformSchedule = scheduleRepository.findByHoscodeAndDepcodeAndHosScheduleId(hoscode, depcode, hosScheduleId);
        if (platformSchedule == null) {
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);

        } else {
            schedule.setCreateTime((platformSchedule.getCreateTime()));
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(platformSchedule.getIsDeleted());
            schedule.setId(platformSchedule.getId());
            scheduleRepository.save(schedule);
        }


    }

    @Override
    public Page<Schedule> getSchedulePage(Map<String, Object> stringObjectMap) {
        Schedule schedule = new Schedule();
        String hoscode = (String) stringObjectMap.get("hoscode");
        schedule.setHoscode(hoscode);
        Example<Schedule> scheduleExample = Example.of(schedule);
        int page = Integer.parseInt(stringObjectMap.get("page").toString());
        int limit = Integer.parseInt(stringObjectMap.get("limit").toString());
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("createTime").ascending());
        Page<Schedule> result = scheduleRepository.findAll(scheduleExample, pageRequest);
        return result;
    }

    @Override
    public void remove(Map<String, Object> stringObjectMap) {
        String hoscode = (String)stringObjectMap.get("hoscode");
        String hosScheduleId = (String)stringObjectMap.get("hosScheduleId");
        Schedule schedule=scheduleRepository.findByHoscodeAndHosScheduleId(hoscode,hosScheduleId);
        if(schedule!=null){
            scheduleRepository.deleteById(schedule.getId());
        }

    }

    @Override
    public Map<String, Object> page(Integer pageNum, Integer pageSize, String hoscode, String depcode) {

        Criteria criteria=Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);

        Aggregation aggregation=Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
                        .first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),
                Aggregation.sort(Sort.Direction.ASC,"workDate"),
                Aggregation.skip((pageNum-1)*pageSize),
                Aggregation.limit(pageSize)
        );
        AggregationResults<BookingScheduleRuleVo> aggregate = mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> mappedResults = aggregate.getMappedResults();
        for (BookingScheduleRuleVo bookingScheduleRuleVo : mappedResults) {
            Date workDate=bookingScheduleRuleVo.getWorkDate();
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

        }

        Aggregation aggregation2=Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
        );
        AggregationResults<BookingScheduleRuleVo> aggregate2 = mongoTemplate.aggregate(aggregation2, Schedule.class, BookingScheduleRuleVo.class);

        HashMap<String, Object> map = new HashMap<>();
        map.put("list",mappedResults);
        map.put("total",aggregate2.getMappedResults().size());

        //获取医院名称
        Hospital hospital = hospitalService.getHospitalByHoscode(hoscode);
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname",hospital.getHosname());
        map.put("baseMap",baseMap);
        return map;
    }

    @Override
    public List<Schedule> detail(String hoscode, String depcode, String workdate) {
        Date date = new DateTime(workdate).toDate();
        List<Schedule> scheduleList=scheduleRepository.findByHoscodeAndDepcodeAndWorkDate(hoscode,depcode,date);
        scheduleList.stream().forEach(item->{
            this.packgeSchedule(item);
        });
        return scheduleList;
    }

    @Override
    public Map<String, Object> getSchedulePageByCondition(String hoscode, String depcode, Integer pageNum, Integer pageSize) {
        Hospital hospital = hospitalService.getHospitalByHoscode(hoscode);
        if(hoscode==null){
            throw new YyghException(20001,"该医院信息不存在");
        }
        BookingRule bookingRule = hospital.getBookingRule();

        IPage page = this.getListDate(pageNum, pageSize, bookingRule);
        List<Date> records = page.getRecords();
        Criteria criteria=Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode).and("workDate").in(records);


        Aggregation aggregation=Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate").first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),
                Aggregation.sort(Sort.Direction.ASC,"workDate")
        );


        AggregationResults<BookingScheduleRuleVo> aggregate = mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);

        List<BookingScheduleRuleVo> mappedResults = aggregate.getMappedResults();

        Map<Date, BookingScheduleRuleVo> collect = mappedResults.stream().collect(Collectors.toMap(BookingScheduleRuleVo::getWorkDate, BookingScheduleRuleVo -> BookingScheduleRuleVo));
        int size = records.size();

        List<BookingScheduleRuleVo> bookingScheduleRuleVoList=new ArrayList<BookingScheduleRuleVo>();

        for(int i=0;i<size;i++){
            Date date = records.get(i);
            BookingScheduleRuleVo bookingScheduleRuleVo = collect.get(date);
            if(bookingScheduleRuleVo == null){
                bookingScheduleRuleVo=new BookingScheduleRuleVo();
                bookingScheduleRuleVo.setWorkDate(date);
                //bookingScheduleRuleVo.setWorkDateMd(date);
                bookingScheduleRuleVo.setDocCount(0);
                bookingScheduleRuleVo.setReservedNumber(0);
                bookingScheduleRuleVo.setAvailableNumber(0);//当天所有医生的总的剩余可预约数
                //bookingScheduleRuleVo.setStatus(0);
            }
            bookingScheduleRuleVo.setWorkDateMd(date);
            bookingScheduleRuleVo.setDayOfWeek(this.getDayOfWeek(new DateTime(date)));

            bookingScheduleRuleVo.setStatus(0); //

            //第一页第一条做特殊判断处理
            if(i==0 && pageNum == 1){
                DateTime dateTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                //如果医院规定的当前的挂号截止时间在此时此刻之前，说明：此时此刻已经过了当天的挂号截止时间了
                if(dateTime.isBeforeNow()){
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            //最后一页的最后一条做特殊判断处理
            if(pageNum==page.getPages() && i== (size-1) ){
                bookingScheduleRuleVo.setStatus(1);
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("total",page.getTotal());
        map.put("list",bookingScheduleRuleVoList);

        Map<String,Object> baseMap = new HashMap<String,Object>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospitalByHoscode(hoscode).getHosname());
        //科室
        Department department=departmentService.getDepartment(hoscode,depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());

        map.put("baseMap",baseMap);

        return map;
    }

    @Override
    public Schedule getScheduleInfo(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        this.packgeSchedule(schedule);
        return schedule;
    }

    @Override
    public ScheduleOrderVo getScheduleById(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        ScheduleOrderVo scheduleOrderVo=new ScheduleOrderVo();
        BeanUtils.copyProperties(schedule,scheduleOrderVo);
        Hospital hospital = hospitalService.getHospitalByHoscode(schedule.getHoscode());
        scheduleOrderVo.setHosname(hospital.getHosname());

        Department department = departmentService.getDepartment(schedule.getHoscode(), schedule.getDepcode());
        scheduleOrderVo.setDepname(department.getDepname());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());




        DateTime dateTime = this.getDateTime(new DateTime(schedule.getWorkDate()).plusDays(hospital.getBookingRule().getQuitDay()).toDate(), hospital.getBookingRule().getQuitTime());
        scheduleOrderVo.setQuitTime(dateTime.toDate()); //预约的退号截止时间

        Date workDate = schedule.getWorkDate();
        String stopTime = hospital.getBookingRule().getStopTime();

        scheduleOrderVo.setStartTime(this.getDateTime(workDate,stopTime).toDate());

        return scheduleOrderVo;
    }

    @Override
    public boolean updateAvailableNumber(String scheduleId, Integer availableNumber) {
        Schedule schedule = scheduleRepository.findById(scheduleId).get();

        schedule.setAvailableNumber(availableNumber);
        schedule.setUpdateTime(new Date());

        scheduleRepository.save(schedule);
        return true;
    }


    private IPage getListDate(Integer pageNum, Integer pageSize, BookingRule bookingRule) {
        Integer cycle = bookingRule.getCycle();
        String releaseTime = bookingRule.getReleaseTime();
        DateTime dateTime = this.getDateTime(new Date(), releaseTime);
        if(dateTime.isBeforeNow()){
            cycle=cycle+1;

        }


        List<Date> list=new ArrayList<Date>();
        for (int i = 0; i < cycle; i++) {
            list.add(new DateTime(new DateTime().plusDays(i).toString("yyyy-MM-dd")).toDate());
        }

        int start=(pageNum-1)*pageSize;
        int end=start+pageSize;
        if(end>list.size()){
            end=list.size();
        }

        List<Date> currentPageDateList=new ArrayList<Date>();

        for (int j=start;j<end;j++){
            Date date = list.get(j);
            currentPageDateList.add(date);
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Date> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize, list.size());
        page.setRecords(currentPageDateList);
        return page;

    }

    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " "+ timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }


    private void packgeSchedule(Schedule schedule) {
        //设置医院名称
        schedule.getParam().put("hosname",hospitalService.getHospitalByHoscode(schedule.getHoscode()).getHosname());
        //设置科室名称
        schedule.getParam().put("depname",
                departmentService.getDepName(schedule.getHoscode(),schedule.getDepcode()));
        //设置日期对应星期
        schedule.getParam().put("dayOfWeek",this.getDayOfWeek(new DateTime(schedule.getWorkDate())));

    }

    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }


}
