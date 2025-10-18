package com.nexusvoice.domain.storage.model;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.enums.FileTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件上传结果领域模型
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Data
@Builder
public class UploadResult {
    
    /**
     * 文件访问URL（完整路径）
     */
    private String fileUrl;
    
    /**
     * 文件存储路径（相对路径）
     */
    private String filePath;
    
    /**
     * 文件Key（在存储系统中的唯一标识）
     */
    private String fileKey;
    
    /**
     * 原始文件名
     */
    private String originalFileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private FileTypeEnum fileType;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 文件MD5值
     */
    private String md5;
    
    /**
     * 文件ETag
     */
    private String etag;
    
    /**
     * 存储提供商
     */
    private StorageProvider storageProvider;
    
    /**
     * 存储桶/空间名称
     */
    private String bucket;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 上传耗时（毫秒）
     */
    private Long uploadDuration;
    
    /**
     * 是否上传成功
     */
    private Boolean success;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 扩展元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 创建成功的上传结果
     */
    public static UploadResult success(String fileUrl, String fileKey, Long fileSize, StorageProvider provider) {
        return UploadResult.builder()
                .success(true)
                .fileUrl(fileUrl)
                .fileKey(fileKey)
                .fileSize(fileSize)
                .storageProvider(provider)
                .uploadTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败的上传结果
     */
    public static UploadResult failure(String errorMessage, StorageProvider provider) {
        return UploadResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .storageProvider(provider)
                .uploadTime(LocalDateTime.now())
                .build();
    }
}
