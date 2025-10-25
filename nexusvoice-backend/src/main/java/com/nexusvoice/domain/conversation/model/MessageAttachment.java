package com.nexusvoice.domain.conversation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息附件领域模型
 * 支持图片、文档、音频、视频等多种类型
 * 
 * @author NexusVoice Team
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {
    
    /**
     * 附件类型
     */
    private AttachmentType type;
    
    /**
     * CDN访问URL（永久链接）
     */
    private String url;
    
    /**
     * 原始文件名
     */
    private String name;
    
    /**
     * 文件大小（字节）
     */
    private Long size;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 图片宽度（仅图片类型有效）
     */
    private Integer width;
    
    /**
     * 图片高度（仅图片类型有效）
     */
    private Integer height;
    
    /**
     * 缩略图URL（可选，用于大图片预览）
     */
    private String thumbnailUrl;
    
    /**
     * 附件类型枚举
     */
    public enum AttachmentType {
        IMAGE("image", "图片"),
        DOCUMENT("document", "文档"),
        AUDIO("audio", "音频"),
        VIDEO("video", "视频");
        
        private final String code;
        private final String description;
        
        AttachmentType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        /**
         * 序列化时使用小写code
         */
        @JsonValue
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * 反序列化时从小写code转换
         * 大小写不敏感
         */
        @JsonCreator
        public static AttachmentType fromCode(String code) {
            if (code == null) {
                return IMAGE; // 默认为图片
            }
            for (AttachmentType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的附件类型: " + code);
        }
        
        /**
         * 根据MIME类型推断附件类型
         */
        public static AttachmentType fromMimeType(String mimeType) {
            if (mimeType == null) {
                return IMAGE; // 默认为图片
            }
            
            if (mimeType.startsWith("image/")) {
                return IMAGE;
            } else if (mimeType.startsWith("audio/")) {
                return AUDIO;
            } else if (mimeType.startsWith("video/")) {
                return VIDEO;
            } else {
                return DOCUMENT;
            }
        }
    }
    
    /**
     * 判断是否为图片
     */
    public boolean isImage() {
        return type == AttachmentType.IMAGE;
    }
    
    /**
     * 判断是否为文档
     */
    public boolean isDocument() {
        return type == AttachmentType.DOCUMENT;
    }
    
    /**
     * 判断是否为音频
     */
    public boolean isAudio() {
        return type == AttachmentType.AUDIO;
    }
    
    /**
     * 判断是否为视频
     */
    public boolean isVideo() {
        return type == AttachmentType.VIDEO;
    }
}
