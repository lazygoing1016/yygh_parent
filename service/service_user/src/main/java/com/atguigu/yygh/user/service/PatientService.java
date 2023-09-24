package com.atguigu.yygh.user.service;

import com.atguigu.yygh.model.user.Patient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 就诊人表 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-09-21
 */
public interface PatientService extends IService<Patient> {

    List<Patient> findAll(String token);

    Patient detail(Long id);

    List<Patient> selectList(QueryWrapper<Patient> queryWrapper);
}
