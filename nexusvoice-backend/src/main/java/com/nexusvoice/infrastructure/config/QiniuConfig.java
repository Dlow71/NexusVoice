package com.nexusvoice.infrastructure.config;

/**
 * @Author AJ
 * @Date 2025-09-24 15:33
 * @Description TODO
 */

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 七牛云OSS相关配置
 */
@Configuration
@ConfigurationProperties(prefix = "fast-alden.file.oss.qiniu")
@Getter
@Setter
public class QiniuConfig {
    /**
     * AC
     */
    private String accessKey;
    /**
     * SC
     */
    private String secretKey;
    /**
     * 存储空间
     */
    private String bucket;
    /**
     * 上传目录
     */
    private String directory;
    /**
     * 访问域名
     */
    private String domain;
}
