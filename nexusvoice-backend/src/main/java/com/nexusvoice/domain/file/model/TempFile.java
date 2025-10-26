package com.nexusvoice.domain.file.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 临时文件值对象
 * 表示需要定期清理的临时文件（TTS音频、图片、PDF等）
 * 支持多种存储提供商（七牛云、MinIO等）
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TempFile {
    
    /**
     * 文件URL（完整CDN地址）
     */
    private String fileUrl;
    
    /**
     * 文件Key（存储路径，用于删除）
     */
    private String fileKey;
    
    /**
     * 文件分类（audio/image/pdf/video/document等）
     */
    private FileCategory fileCategory;
    
    /**
     * 文件子类型（tts/tts_segment/upload_image/generated_pdf等）
     */
    private String fileSubType;
    
    /**
     * 存储提供商（qiniu/minio）
     */
    private StorageProvider storageProvider;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 过期时间（超过此时间后将被清理）
     */
    private LocalDateTime expireAt;
    
    /**
     * 关联的对话ID（可选，用于追踪）
     */
    private Long conversationId;
    
    /**
     * 关联的用户ID（可选，用于追踪）
     */
    private Long userId;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return expireAt != null && LocalDateTime.now().isAfter(expireAt);
    }
    
    /**
     * 是否即将过期（1小时内）
     */
    public boolean isExpiringSoon() {
        if (expireAt == null) {
            return false;
        }
        LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);
        return oneHourLater.isAfter(expireAt);
    }
    
    /**
     * 文件分类枚举
     */
    public enum FileCategory {
        AUDIO("音频"),
        IMAGE("图片"),
        PDF("PDF文档"),
        VIDEO("视频"),
        DOCUMENT("文档");
        
        private final String description;
        
        FileCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 存储提供商枚举
     */
    public enum StorageProvider {
        QINIU("qiniu", "七牛云"),
        MINIO("minio", "MinIO");
        
        private final String code;
        private final String name;
        
        StorageProvider(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getName() {
            return name;
        }
        
        /**
         * 根据code获取枚举
         */
        public static StorageProvider fromCode(String code) {
            for (StorageProvider provider : values()) {
                if (provider.code.equalsIgnoreCase(code)) {
                    return provider;
                }
            }
            return QINIU; // 默认七牛云
        }
    }
}
