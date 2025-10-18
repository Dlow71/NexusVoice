package com.nexusvoice.domain.storage.enums;

import lombok.Getter;

/**
 * 存储服务提供商枚举
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Getter
public enum StorageProvider {
    /**
     * 七牛云存储
     */
    QINIU("qiniu", "七牛云存储", "storage.qiniu"),
    
    /**
     * MinIO对象存储
     */
    MINIO("minio", "MinIO对象存储", "storage.minio"),
    
    /**
     * 阿里云OSS（预留）
     */
    ALIYUN_OSS("aliyun_oss", "阿里云OSS", "storage.aliyun"),
    
    /**
     * 腾讯云COS（预留）
     */
    TENCENT_COS("tencent_cos", "腾讯云COS", "storage.tencent"),
    
    /**
     * AWS S3（预留）
     */
    AWS_S3("aws_s3", "AWS S3", "storage.aws");
    
    /**
     * 提供商代码
     */
    private final String code;
    
    /**
     * 提供商名称
     */
    private final String name;
    
    /**
     * 配置前缀
     */
    private final String configPrefix;
    
    StorageProvider(String code, String name, String configPrefix) {
        this.code = code;
        this.name = name;
        this.configPrefix = configPrefix;
    }
    
    /**
     * 根据代码获取存储提供商
     */
    public static StorageProvider fromCode(String code) {
        for (StorageProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("不支持的存储提供商: " + code);
    }
    
    /**
     * 判断是否为支持的提供商
     */
    public static boolean isSupported(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 判断当前提供商是否已实现
     */
    public boolean isImplemented() {
        return this == QINIU || this == MINIO;
    }
}
