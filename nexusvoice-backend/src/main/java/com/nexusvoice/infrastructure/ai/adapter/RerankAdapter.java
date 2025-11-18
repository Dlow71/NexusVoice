package com.nexusvoice.infrastructure.ai.adapter;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;

/**
 * 重排序适配器接口
 * 
 * <p>纯血DDD架构 - Infrastructure层接口：
 * - 封装不同厂商的Rerank API调用细节
 * - 统一的调用接口，屏蔽底层实现差异
 * - 支持根据查询文本对文档列表进行相关性重排序
 * </p>
 * 
 * <p>设计思想：
 * - 策略模式：每个厂商对应一个适配器实现
 * - 依赖倒置：上层依赖接口而非具体实现
 * - 开闭原则：新增厂商只需添加新实现，无需修改现有代码
 * </p>
 *
 * @author NexusVoice
 * @since 2025-01-12
 */
public interface RerankAdapter {
    
    /**
     * 调用Rerank API进行文档重排序
     * 
     * @param request 重排序请求
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return 重排序响应
     */
    RerankResponse rerank(RerankRequest request, AiModel model, AiApiKey apiKey);
    
    /**
     * 获取适配器支持的提供商代码
     * 
     * @return 提供商代码（如：siliconflow, openai等）
     */
    default String getProviderCode() {
        return "unknown";
    }
    
    /**
     * 估算查询和文档的总token数量
     * 
     * @param query 查询文本
     * @param documents 文档列表
     * @return token数量估算值
     */
    default int estimateTokenCount(String query, java.util.List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return estimateTextTokenCount(query);
        }
        
        int totalTokens = estimateTextTokenCount(query);
        for (String doc : documents) {
            totalTokens += estimateTextTokenCount(doc);
        }
        return totalTokens;
    }
    
    /**
     * 估算单个文本的token数量
     * 
     * @param text 文本内容
     * @return token数量估算值
     */
    default int estimateTextTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 默认估算：中文1.5字符/token，英文4字符/token
        return (int) Math.ceil(text.length() / 2.0);
    }
}
