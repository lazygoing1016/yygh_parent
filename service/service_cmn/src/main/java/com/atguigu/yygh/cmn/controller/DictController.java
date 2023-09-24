package com.atguigu.yygh.cmn.controller;


import com.alibaba.excel.EasyExcel;
import com.atguigu.yygh.cmn.listener.DictListener;
import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 组织架构表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2023-09-11
 */
@RestController
@RequestMapping("/admin/cmn")
public class DictController {
    @Autowired
    private DictService dictService;


    @PostMapping("/upload")
    public R upload(MultipartFile file) throws IOException {
        dictService.upload(file);
        return R.ok();
    }

    @GetMapping("/dowmload")
    public void dowmload(HttpServletResponse response) throws IOException {
        dictService.download(response);
    }

    @GetMapping("/childList/{pid}")
    public R getChildListByPid(@PathVariable Long pid){
        List<Dict> list=dictService.GetChildListBypid(pid);
        return R.ok().data("items",list);

    }

    @GetMapping("/{value}")
    public String getNameByValue(@PathVariable("value") long value){
        return dictService.getNameByValue(value);

    }

    @GetMapping("/{dictCode}/{value}")
    public String getNameByDictCodeAndValue(@PathVariable("dictCode") String dictCode,@PathVariable("value") long value){
        return dictService.getNameByDictCodeAndValue(dictCode,value);

    }

}

