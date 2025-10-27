package com.nexusvoice.domain.ai.model;

import lombok.Getter;

/**
 * AI模型类型枚举
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Getter
public enum AiModelType {
    
    /**
     * 对话模型：用于文本生成、对话交互
     */
    CHAT("chat", "对话模型"),
    
    /**
     * 向量模型：用于文本向量化、语义搜索
     */
    EMBEDDING("embedding", "向量模型"),
    
    /**
     * 重排序模型：用于搜索结果重排序、相关性排序
     */
    RERANK("rerank", "重排序模型"),
    
    /**
     * 图像生成模型：用于AI图像生成、图像编辑
     */
    IMAGE("image", "图像生成模型"),
    
    /**
     * 语音识别模型：用于语音转文本、ASR识别
     */
    ASR("asr", "语音识别模型"),
    
    /**
     * 语音合成模型：用于文本转语音、TTS合成
     */
    TTS("tts", "语音合成模型"),
    
    /**
     * 视频生成模型：用于AI视频生成、视频编辑
     */
    VIDEO("video", "视频生成模型");
    
    /**
     * 类型代码
     */
    private final String code;
    
    /**
     * 类型名称
     */
    private final String name;
    
    AiModelType(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static AiModelType fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (AiModelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * 是否为对话模型
     */
    public boolean isChat() {
        return this == CHAT;
    }
    
    /**
     * 是否为向量模型
     */
    public boolean isEmbedding() {
        return this == EMBEDDING;
    }
    
    /**
     * 是否为重排序模型
     */
    public boolean isRerank() {
        return this == RERANK;
    }
    
    /**
     * 是否为图像生成模型
     */
    public boolean isImage() {
        return this == IMAGE;
    }
    
    /**
     * 是否为语音识别模型
     */
    public boolean isAsr() {
        return this == ASR;
    }
    
    /**
     * 是否为语音合成模型
     */
    public boolean isTts() {
        return this == TTS;
    }
    
    /**
     * 是否为视频生成模型
     */
    public boolean isVideo() {
        return this == VIDEO;
    }
}
