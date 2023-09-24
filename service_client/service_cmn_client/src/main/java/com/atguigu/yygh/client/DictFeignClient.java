package com.atguigu.yygh.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-cmn")
public interface DictFeignClient {
    @GetMapping("/admin/cmn/{value}")
    public String getNameByValue(@PathVariable("value") long value);


    @GetMapping("/admin/cmn/{dictCode}/{value}")
    public String getNameByDictCodeAndValue(@PathVariable("dictCode") String dictCode,@PathVariable("value") long value);

}
