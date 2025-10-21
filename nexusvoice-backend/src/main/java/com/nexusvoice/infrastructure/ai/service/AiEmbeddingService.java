package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;

/**
 * AI向量化服务接口
 * 用于将文本转换为向量表示，支持语义搜索、相似度计算等场景
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
public interface AiEmbeddingService {
    
    /**
     * 向量化文本（单个或批量）
     * 
     * @param request 向量化请求
     * @return 向量化响应
     */
    EmbeddingResponse embed(EmbeddingRequest request);
    
    /**
     * 获取模型名称
     */
    String getModelName();
    
    /**
     * 检查模型是否可用
     */
    boolean isModelAvailable();
    
    /**
     * 估算token数量
     * 
     * @param text 文本内容
     * @return 预估的token数
     */
    int estimateTokenCount(String text);
}
