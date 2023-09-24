package com.atguigu.yygh.oss.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "oss.file")
@Component
@Data
public class OssProperties {

    private String endpoint;
    private String keyid;
    private String keysecret;
    private String bucketname;
}
