package com.nexusvoice.domain.video.repository;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.video.model.VideoGenerationRequest;
import com.nexusvoice.domain.video.model.VideoGenerationResult;

/**
 * 视频生成仓储接口（零基础设施依赖）
 * 定义视频生成的领域操作，具体技术实现由Infrastructure层提供
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
public interface VideoRepository {
    
    /**
     * 提交视频生成任务（异步）
     * 
     * @param request 视频生成请求
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return 视频生成结果（包含taskId，状态为PROCESSING）
     */
    VideoGenerationResult submitTask(VideoGenerationRequest request, AiModel model, AiApiKey apiKey);
    
    /**
     * 查询视频生成任务结果
     * 
     * @param taskId 任务ID
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return 视频生成结果（包含最新状态）
     */
    VideoGenerationResult queryTask(String taskId, AiModel model, AiApiKey apiKey);
    
    /**
     * 检查服务是否可用
     * 
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable(AiModel model, AiApiKey apiKey);
}
