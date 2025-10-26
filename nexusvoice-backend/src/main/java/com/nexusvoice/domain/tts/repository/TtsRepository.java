package com.nexusvoice.domain.tts.repository;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.tts.model.TtsRequest;
import com.nexusvoice.domain.tts.model.TtsResult;

/**
 * TTS仓储接口
 * 纯粹的Domain层接口，定义TTS合成的仓储契约
 * 具体实现由Infrastructure层提供
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
public interface TtsRepository {
    
    /**
     * 合成语音
     * 
     * @param request TTS请求
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return TTS合成结果
     */
    TtsResult synthesize(TtsRequest request, AiModel model, AiApiKey apiKey);
    
    /**
     * 检查TTS服务是否可用
     * 
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return 是否可用
     */
    boolean isAvailable(AiModel model, AiApiKey apiKey);
}
