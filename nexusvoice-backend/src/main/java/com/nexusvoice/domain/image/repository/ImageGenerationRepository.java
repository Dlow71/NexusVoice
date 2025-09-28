package com.nexusvoice.domain.image.repository;

import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;

/**
 * 图像生成仓储接口
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
public interface ImageGenerationRepository {
    
    /**
     * 生成图像
     * 
     * @param request 图像生成请求
     * @return 图像生成结果
     */
    ImageGenerationResult generateImage(ImageGenerationRequest request);
    
    /**
     * 检查图像生成服务是否可用
     * 
     * @return true如果服务可用
     */
    boolean isServiceAvailable();
    
    /**
     * 获取支持的模型列表
     * 
     * @return 支持的模型名称列表
     */
    java.util.List<String> getSupportedModels();
    
    /**
     * 验证API密钥是否有效
     * 
     * @param apiKey API密钥
     * @return true如果密钥有效
     */
    boolean validateApiKey(String apiKey);
}
