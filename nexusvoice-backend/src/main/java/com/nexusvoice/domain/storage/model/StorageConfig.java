package com.nexusvoice.domain.storage.model;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import lombok.Data;

import java.util.Map;
import java.util.HashMap;

/**
 * 存储服务配置领域模型
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Data
public abstract class StorageConfig {
    
    /**
     * 存储提供商
     */
    protected StorageProvider provider;
    
    /**
     * 是否启用
     */
    protected Boolean enabled;
    
    /**
     * 访问密钥
     */
    protected String accessKey;
    
    /**
     * 密钥
     */
    protected String secretKey;
    
    /**
     * 存储桶/空间名称
     */
    protected String bucket;
    
    /**
     * 区域/Region
     */
    protected String region;
    
    /**
     * 文件目录配置
     * key: FileTypeEnum.code
     * value: 目录路径
     */
    protected Map<String, String> directories;
    
    /**
     * 是否使用SSL/HTTPS
     */
    protected Boolean useSSL;
    
    /**
     * 连接超时时间（秒）
     */
    protected Integer connectTimeout;
    
    /**
     * 读取超时时间（秒）
     */
    protected Integer readTimeout;
    
    /**
     * 写入超时时间（秒）
     */
    protected Integer writeTimeout;
    
    /**
     * 最大重试次数
     */
    protected Integer maxRetries;
    
    /**
     * 扩展配置
     */
    protected Map<String, Object> extraConfig;
    
    protected StorageConfig(StorageProvider provider) {
        this.provider = provider;
        this.enabled = false;
        this.useSSL = true;
        this.connectTimeout = 30;
        this.readTimeout = 30;
        this.writeTimeout = 30;
        this.maxRetries = 3;
        this.directories = new HashMap<>();
        this.extraConfig = new HashMap<>();
    }
    
    /**
     * 获取文件类型对应的目录
     */
    public String getDirectory(String fileType) {
        String dir = directories.getOrDefault(fileType, fileType + "/");
        // 确保目录以/结尾
        return dir.endsWith("/") ? dir : dir + "/";
    }
    
    /**
     * 验证配置是否有效
     */
    public abstract boolean isValid();
    
    /**
     * 获取配置描述信息
     */
    public abstract String getDescription();
}
