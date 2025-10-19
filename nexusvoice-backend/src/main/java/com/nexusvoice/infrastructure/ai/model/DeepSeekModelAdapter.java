package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 模型适配器
 * 处理DeepSeek API的模型创建，使用OpenAI兼容的API格式
 * 支持显式推理（Think）、动态搜索（Search）、工具调用（Tool）等功能
 * 
 * DeepSeek V3.1 特性：
 * - 128K tokens 上下文窗口
 * - 深度思考模式（Think）
 * - 动态搜索能力（Search）
 * - 高效工具调用（Tool）
 * - 多模态支持
 * 
 * @author NexusVoice
 * @since 2025-10-19
 */
@Slf4j
@Component
public class DeepSeekModelAdapter extends AbstractOpenAiCompatibleModel {
    
    // 七牛云API特征标识
    private static final String QINIU_API_INDICATOR_1 = "qiniu.com";
    private static final String QINIU_API_INDICATOR_2 = "qnaigc.com";
    
    // DeepSeek官方API
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    
    @Override
    protected String getBaseUrl(AiApiKey apiKey) {
        // 优先使用API密钥配置的URL，其次使用默认的DeepSeek API端点
        String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                ? apiKey.getBaseUrl() 
                : DEFAULT_BASE_URL;
        
        log.debug("DeepSeek模型使用API端点：{}", baseUrl);
        return baseUrl;
    }
    
    @Override
    protected String getProviderName() {
        return "DeepSeek";
    }
    
    /**
     * 转换模型名称以适配不同的API提供商
     * 七牛云代理的DeepSeek不需要添加前缀，直接使用原始名称
     * 
     * @param originalModelCode 原始模型代码（如：deepseek-v3.1）
     * @param baseUrl API基础URL
     * @return 转换后的模型名称
     */
    @Override
    protected String transformModelName(String originalModelCode, String baseUrl) {
        // DeepSeek通过七牛云代理时，直接使用原始模型名称，不需要转换
        log.debug("DeepSeek模型名称：{}，API端点：{}", originalModelCode, baseUrl);
        
        // 如果是七牛云API，记录日志
        if (baseUrl != null && (baseUrl.contains(QINIU_API_INDICATOR_1) || baseUrl.contains(QINIU_API_INDICATOR_2))) {
            log.info("检测到七牛云代理的DeepSeek API（{}），使用原始模型名称：{}", baseUrl, originalModelCode);
        }
        
        return originalModelCode;
    }
    
    @Override
    protected void beforeModelCreation(AiModel model, AiApiKey apiKey) {
        // DeepSeek特有的前置处理
        log.info("准备创建DeepSeek模型：{}，支持深度思考和工具调用", model.getModelCode());
        
        // 记录上下文窗口大小
        if (model.getContextWindow() != null) {
            log.info("DeepSeek模型上下文窗口：{} tokens", model.getContextWindow());
        }
    }
    
    @Override
    protected void afterModelCreation(AiModel model, AiApiKey apiKey) {
        // DeepSeek特有的后置处理
        log.debug("DeepSeek模型创建完成，模型：{}", model.getModelCode());
        
        // 提示可用的高级特性
        log.info("DeepSeek V3.1 支持：显式推理（Think）、动态搜索（Search）、工具调用（Tool）");
    }
    
    @Override
    protected int getDefaultMaxTokens() {
        // DeepSeek默认使用8000 tokens（支持更长的输出）
        return 8000;
    }
    
    @Override
    protected int getDefaultTimeout() {
        // DeepSeek默认120秒超时（深度思考模式可能需要更长时间）
        return 120;
    }
}
