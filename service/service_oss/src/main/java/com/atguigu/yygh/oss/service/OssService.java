package com.atguigu.yygh.oss.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.atguigu.yygh.oss.prop.OssProperties;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.UUID;

@Service
public class OssService {

    @Autowired
    private OssProperties ossProperties;
    public String upload(MultipartFile file) {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = ossProperties.getEndpoint();
        String accessKeyId=ossProperties.getKeyid();
        String accessKeySecret=ossProperties.getKeysecret();

        // 填写Bucket名称，例如examplebucket。
        String bucketName = ossProperties.getBucketname();
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint,accessKeyId,accessKeySecret);
        String filename =new DateTime().toString("yyyy/MM/dd")+ UUID.randomUUID().toString().replaceAll("-","")+file.getOriginalFilename();

        try {
            // 创建PutObject请求。
            PutObjectResult result = ossClient.putObject(bucketName,filename,file.getInputStream());
            return "https://"+ossProperties.getBucketname()+"."+ossProperties.getEndpoint()+"/"+filename;
        } catch (Exception ce) {
            System.out.println(ce.getMessage());
            return "";
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
