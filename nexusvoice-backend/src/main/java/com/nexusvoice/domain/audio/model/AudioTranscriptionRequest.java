package com.nexusvoice.domain.audio.model;

import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别请求领域模型
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
public class AudioTranscriptionRequest {
    
    /**
     * 模型键（格式：provider:model，如 siliconflow:telespeech-asr）
     */
    private String modelKey;
    
    /**
     * 音频文件
     */
    private MultipartFile audioFile;
    
    /**
     * 用户ID（用于费用统计和调用日志）
     */
    private Long userId;
    
    /**
     * 语言提示（可选，帮助模型识别）
     */
    private String language;
    
    /**
     * 是否启用标点符号
     */
    private Boolean enablePunctuation;
    
    /**
     * 是否启用逆文本规范化（ITN）
     */
    private Boolean enableItn;
    
    // Getter/Setter
    
    public String getModelKey() {
        return modelKey;
    }
    
    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public MultipartFile getAudioFile() {
        return audioFile;
    }
    
    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public Boolean getEnablePunctuation() {
        return enablePunctuation;
    }
    
    public void setEnablePunctuation(Boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }
    
    public Boolean getEnableItn() {
        return enableItn;
    }
    
    public void setEnableItn(Boolean enableItn) {
        this.enableItn = enableItn;
    }
    
    /**
     * 验证请求参数的有效性
     * 
     * @return 验证结果消息，null表示验证通过
     */
    public String validate() {
        if (modelKey == null || modelKey.trim().isEmpty()) {
            return "语音识别模型不能为空";
        }
        
        if (audioFile == null || audioFile.isEmpty()) {
            return "音频文件不能为空";
        }
        
        // 验证文件大小（硅基流动限制通常为100MB）
        if (audioFile.getSize() > 100 * 1024 * 1024) {
            return "音频文件大小不能超过100MB";
        }
        
        // 验证文件格式
        String contentType = audioFile.getContentType();
        String originalFilename = audioFile.getOriginalFilename();
        
        if (!isSupportedAudioFormat(contentType, originalFilename)) {
            return "不支持的音频格式，仅支持：mp3, wav, m4a, flac, opus, webm, ogg";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 检查是否为支持的音频格式
     */
    private boolean isSupportedAudioFormat(String contentType, String filename) {
        // 通过Content-Type检查
        if (contentType != null) {
            if (contentType.contains("audio/mpeg") ||  // mp3
                contentType.contains("audio/wav") ||   // wav
                contentType.contains("audio/x-m4a") || // m4a
                contentType.contains("audio/mp4") ||   // m4a
                contentType.contains("audio/flac") ||  // flac
                contentType.contains("audio/opus") ||  // opus
                contentType.contains("audio/webm") ||  // webm (浏览器录音常用格式)
                contentType.contains("audio/ogg")) {   // ogg
                return true;
            }
        }
        
        // 通过文件扩展名检查
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            return lowerFilename.endsWith(".mp3") ||
                   lowerFilename.endsWith(".wav") ||
                   lowerFilename.endsWith(".m4a") ||
                   lowerFilename.endsWith(".flac") ||
                   lowerFilename.endsWith(".opus") ||
                   lowerFilename.endsWith(".webm") ||
                   lowerFilename.endsWith(".ogg");
        }
        
        return false;
    }
}
