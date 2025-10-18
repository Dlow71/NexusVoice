package com.nexusvoice.domain.storage.model;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MinIO存储配置
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MinioStorageConfig extends StorageConfig {
    
    /**
     * MinIO服务端点URL
     */
    private String endpoint;
    
    /**
     * 公网访问URL（用于生成文件访问链接）
     */
    private String publicUrl;
    
    /**
     * 分片上传大小（字节）
     */
    private Long partSize;
    
    /**
     * 是否启用路径样式访问
     */
    private Boolean pathStyleAccess;
    
    /**
     * 预签名URL有效期（秒）
     */
    private Integer presignedUrlExpiry;
    
    /**
     * 是否自动创建Bucket
     */
    private Boolean autoCreateBucket;
    
    /**
     * Bucket访问策略（public-read/private）
     */
    private String bucketPolicy;
    
    public MinioStorageConfig() {
        super(StorageProvider.MINIO);
        this.endpoint = "http://localhost:9000";
        this.partSize = 5L * 1024 * 1024; // 默认5MB
        this.pathStyleAccess = true;
        this.presignedUrlExpiry = 7 * 24 * 3600; // 默认7天
        this.autoCreateBucket = true;
        this.bucketPolicy = "public-read";
        this.region = "us-east-1"; // MinIO默认region
    }
    
    @Override
    public boolean isValid() {
        return enabled != null && enabled
                && endpoint != null && !endpoint.isEmpty()
                && accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()
                && bucket != null && !bucket.isEmpty();
    }
    
    @Override
    public String getDescription() {
        return String.format("MinIO存储配置 - Endpoint: %s, Bucket: %s", 
                endpoint, bucket);
    }
    
    /**
     * 获取实际的公网访问URL
     */
    public String getActualPublicUrl() {
        return (publicUrl != null && !publicUrl.isEmpty()) ? publicUrl : endpoint;
    }
    
    /**
     * 获取完整的文件访问URL
     */
    public String getFileUrl(String fileKey) {
        String baseUrl = getActualPublicUrl();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = (useSSL ? "https://" : "http://") + baseUrl;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + bucket + "/" + fileKey;
    }
}
