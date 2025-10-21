package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;

/**
 * AI重排序服务接口
 * 用于对搜索结果进行相关性重排序，提升搜索质量
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
public interface AiRerankService {
    
    /**
     * 重排序文档列表
     * 
     * @param request 重排序请求
     * @return 重排序响应
     */
    RerankResponse rerank(RerankRequest request);
    
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
