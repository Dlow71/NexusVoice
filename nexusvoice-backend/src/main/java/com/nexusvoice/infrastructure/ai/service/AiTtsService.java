package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.domain.tts.model.TtsRequest;
import com.nexusvoice.domain.tts.model.TtsResult;

/**
 * AI语音合成服务接口
 * 用于将文本转换为语音，支持多语言、多声音类型
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
public interface AiTtsService {
    
    /**
     * 文本转语音
     * 
     * @param request TTS请求
     * @return TTS结果
     */
    TtsResult synthesize(TtsRequest request);
    
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
