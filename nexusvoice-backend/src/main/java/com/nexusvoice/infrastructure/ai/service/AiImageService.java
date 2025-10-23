package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;

/**
 * AI图像生成服务接口
 * 
 * @author NexusVoice Team
 * @since 2025-01-24
 */
public interface AiImageService {
    
    /**
     * 生成图像
     * 
     * @param request 图像生成请求
     * @return 图像生成结果
     */
    ImageGenerationResult generateImage(ImageGenerationRequest request);
    
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
