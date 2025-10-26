package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.domain.audio.model.AudioTranscriptionRequest;
import com.nexusvoice.domain.audio.model.AudioTranscriptionResult;

/**
 * AI语音识别服务接口
 * 用于将语音转换为文本，支持多语言、多方言识别
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
public interface AiAsrService {
    
    /**
     * 语音转文本
     * 
     * @param request 语音识别请求
     * @return 识别结果
     */
    AudioTranscriptionResult transcribe(AudioTranscriptionRequest request);
    
    /**
     * 检查模型是否可用
     * 
     * @return 是否可用
     */
    boolean isModelAvailable();
    
    /**
     * 获取模型名称
     * 
     * @return 模型名称
     */
    String getModelName();
}
