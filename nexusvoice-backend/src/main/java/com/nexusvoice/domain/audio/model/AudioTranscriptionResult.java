package com.nexusvoice.domain.audio.model;

/**
 * 语音识别结果领域模型
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
public class AudioTranscriptionResult {
    
    /**
     * 识别出的文本
     */
    private String text;
    
    /**
     * 识别耗时（毫秒）
     */
    private Long transcriptionTime;
    
    /**
     * 使用的模型名称
     */
    private String modelName;
    
    /**
     * 音频时长（秒）
     */
    private Double audioDuration;
    
    /**
     * API原始响应（用于调试）
     */
    private String rawResponse;
    
    public AudioTranscriptionResult() {
    }
    
    public AudioTranscriptionResult(String text) {
        this.text = text;
    }
    
    // Getter/Setter
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public Long getTranscriptionTime() {
        return transcriptionTime;
    }
    
    public void setTranscriptionTime(Long transcriptionTime) {
        this.transcriptionTime = transcriptionTime;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public Double getAudioDuration() {
        return audioDuration;
    }
    
    public void setAudioDuration(Double audioDuration) {
        this.audioDuration = audioDuration;
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
    
    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
    
    /**
     * 检查是否识别成功
     * 
     * @return true如果识别成功
     */
    public boolean isSuccess() {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * 获取识别结果摘要
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        return String.format("识别耗时%dms，音频时长%.2fs，使用模型:%s，文本长度:%d字符", 
            transcriptionTime, 
            audioDuration != null ? audioDuration : 0.0, 
            modelName,
            text != null ? text.length() : 0);
    }
    
    /**
     * 获取文本长度
     */
    public int getTextLength() {
        return text != null ? text.length() : 0;
    }
}
