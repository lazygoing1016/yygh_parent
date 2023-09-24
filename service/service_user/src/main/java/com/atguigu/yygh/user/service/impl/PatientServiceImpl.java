package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.client.DictFeignClient;
import com.atguigu.yygh.common.utils.JwtHelper;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.mapper.PatientMapper;
import com.atguigu.yygh.user.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 就诊人表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-09-21
 */
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Autowired
    private DictFeignClient dictFeignClient;

    @Override
    public List<Patient> findAll(String token) {
        Long userId = JwtHelper.getUserId(token);
        QueryWrapper<Patient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        List<Patient> patients = baseMapper.selectList(queryWrapper);
        patients.stream().forEach(item->{
            this.packgePatient(item);
        });
        return patients;
    }

    @Override
    public Patient detail(Long id) {
        Patient patient = baseMapper.selectById(id);
        this.packgePatient(patient);
        return patient;
    }

    @Override
    public List<Patient> selectList(QueryWrapper<Patient> queryWrapper) {
        List<Patient> patients = baseMapper.selectList(queryWrapper);
        patients.stream().forEach(item -> {
            this.packgePatient(item);
        });
        return patients;
    }

    private void packgePatient(Patient item) {
        item.getParam().put("certificatesTypeString",dictFeignClient.getNameByValue(Long.parseLong(item.getCertificatesType())));
        String provinceString = dictFeignClient.getNameByValue(Long.parseLong(item.getProvinceCode()));
        String cityString = dictFeignClient.getNameByValue(Long.parseLong(item.getCityCode()));
        String districtString = dictFeignClient.getNameByValue(Long.parseLong(item.getDistrictCode()));
        item.getParam().put("provinceString",provinceString);
        item.getParam().put("cityString",cityString);
        item.getParam().put("districtString",districtString);
        item.getParam().put("fullAddress",provinceString+cityString+districtString+item.getAddress());

    }
}
