package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 豆包（Doubao）模型适配器
 * 处理字节跳动豆包多模态深度思考模型
 * 支持OCR、图像理解、深度推理等能力
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
@Slf4j
@Component
public class DoubaoModelAdapter extends AbstractOpenAiCompatibleModel {
    
    /**
     * 豆包官方API基础URL（七牛云代理）
     */
    private static final String DEFAULT_BASE_URL = "https://openai.qiniu.com/v1";
    
    @Override
    protected String getBaseUrl(AiApiKey apiKey) {
        // 优先使用API密钥配置的URL，其次使用默认URL
        String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                ? apiKey.getBaseUrl() 
                : DEFAULT_BASE_URL;
        
        log.debug("豆包模型使用API端点：{}", baseUrl);
        return baseUrl;
    }
    
    @Override
    protected String getProviderName() {
        return "Doubao";
    }
    
    @Override
    protected int getDefaultMaxTokens() {
        // 豆包Seed 1.6支持最大16K输出
        // 默认设置8000 tokens以平衡性能和成本
        return 8000;
    }
    
    @Override
    protected int getDefaultTimeout() {
        // OCR和图像理解需要更长的处理时间
        // 深度思考模式（thinking）也需要额外时间
        return 90;
    }
}
