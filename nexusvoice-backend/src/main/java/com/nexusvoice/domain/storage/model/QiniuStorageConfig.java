package com.nexusvoice.domain.storage.model;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 七牛云存储配置
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QiniuStorageConfig extends StorageConfig {
    
    /**
     * CDN访问域名
     */
    private String domain;
    
    /**
     * 上传凭证有效期（秒）
     */
    private Long uploadTokenExpires;
    
    /**
     * 是否使用CDN加速
     */
    private Boolean useCdn;
    
    /**
     * 存储空间访问控制（public/private）
     */
    private String accessControl;
    
    /**
     * 文件过期时间（天），-1表示永不过期
     */
    private Integer fileExpireDays;
    
    public QiniuStorageConfig() {
        super(StorageProvider.QINIU);
        this.uploadTokenExpires = 3600L; // 默认1小时
        this.useCdn = true;
        this.accessControl = "public";
        this.fileExpireDays = -1;
    }
    
    @Override
    public boolean isValid() {
        return enabled != null && enabled
                && accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()
                && bucket != null && !bucket.isEmpty()
                && domain != null && !domain.isEmpty();
    }
    
    @Override
    public String getDescription() {
        return String.format("七牛云存储配置 - Bucket: %s, Domain: %s, Region: %s", 
                bucket, domain, region != null ? region : "auto");
    }
    
    /**
     * 获取完整的文件访问URL
     */
    public String getFileUrl(String fileKey) {
        if (domain == null || domain.isEmpty()) {
            return fileKey;
        }
        String prefix = useSSL ? "https://" : "http://";
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            prefix = "";
        }
        String domainWithSlash = domain.endsWith("/") ? domain : domain + "/";
        return prefix + domainWithSlash + fileKey;
    }
}
