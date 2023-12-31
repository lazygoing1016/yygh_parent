package com.atguigu.yygh.user.controller.user;


import com.atguigu.yygh.client.DictFeignClient;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.JwtHelper;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 就诊人表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2023-09-21
 */
@RestController
@RequestMapping("/user/userinfo/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @PostMapping("/save")
    public R save(@RequestBody Patient patient, @RequestHeader String token){
        Long userId = JwtHelper.getUserId(token);
        patient.setUserId(userId);
        patientService.save(patient);
        return R.ok();

    }

    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable Long id){
        patientService.removeById(id);
        return R.ok();
    }

    @GetMapping("/detail/{id}")
    public R detail(@PathVariable Long id){
        Patient patient=patientService.detail(id);
        return R.ok().data("patient",patient);
    }

    @PutMapping("/update")
    public R update(@RequestBody Patient patient){
        patientService.updateById(patient);
        return R.ok();
    }

    @GetMapping("/all")
    public R findAll(@RequestHeader String token){
        List<Patient> list = patientService.findAll(token);
        return R.ok().data("list",list);
    }

    @GetMapping("/{patientId}")
    public Patient getPatientById(@PathVariable Long patientId){
        return patientService.getById(patientId);
    }





}

