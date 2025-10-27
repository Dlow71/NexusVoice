package com.nexusvoice.application.video.service;

import com.nexusvoice.domain.video.model.VideoGenerationRequest;
import com.nexusvoice.domain.video.model.VideoGenerationResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 视频生成应用服务
 * 职责：协调视频生成的业务流程
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationService {
    
    private final DynamicAiModelBeanManager modelBeanManager;
    
    /**
     * 提交视频生成任务（异步）
     * 
     * @param request 视频生成请求
     * @return 视频生成结果（包含任务ID）
     */
    public VideoGenerationResult submitTask(VideoGenerationRequest request) {
        log.info("提交视频生成任务，模型：{}，提示词：{}", request.getModelKey(), request.getPrompt());
        
        // 1. 验证请求参数
        String validationError = request.validate();
        if (validationError != null) {
            log.warn("视频生成请求参数验证失败：{}", validationError);
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, validationError);
        }
        
        // 2. 获取视频生成服务
        DynamicAiModelBeanManager.DynamicAiVideoService videoService = 
                modelBeanManager.getVideoServiceByModelKey(request.getModelKey());
        
        if (videoService == null) {
            log.error("视频生成模型不存在：{}", request.getModelKey());
            throw BizException.of(ErrorCodeEnum.AI_MODEL_NOT_AVAILABLE, 
                    "视频生成模型不存在：" + request.getModelKey());
        }
        
        // 3. 检查服务可用性
        if (!videoService.isModelAvailable()) {
            log.warn("视频生成模型不可用：{}", request.getModelKey());
            throw BizException.of(ErrorCodeEnum.AI_MODEL_NOT_AVAILABLE, 
                    "视频生成模型暂不可用：" + request.getModelKey());
        }
        
        // 4. 提交任务
        return videoService.submitTask(request);
    }
    
    /**
     * 查询视频生成任务结果
     * 
     * @param taskId 任务ID
     * @param modelKey 模型键
     * @param originalRequest 原始请求（用于费用统计）
     * @return 视频生成结果
     */
    public VideoGenerationResult queryTask(String taskId, String modelKey, VideoGenerationRequest originalRequest) {
        log.debug("查询视频生成任务，任务ID：{}，模型：{}", taskId, modelKey);
        
        // 1. 参数验证
        if (taskId == null || taskId.trim().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "任务ID不能为空");
        }
        
        if (modelKey == null || modelKey.trim().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "模型键不能为空");
        }
        
        // 2. 获取视频生成服务
        DynamicAiModelBeanManager.DynamicAiVideoService videoService = 
                modelBeanManager.getVideoServiceByModelKey(modelKey);
        
        if (videoService == null) {
            log.error("视频生成模型不存在：{}", modelKey);
            throw BizException.of(ErrorCodeEnum.AI_MODEL_NOT_AVAILABLE, 
                    "视频生成模型不存在：" + modelKey);
        }
        
        // 3. 查询任务结果
        return videoService.queryTask(taskId, originalRequest);
    }
    
    /**
     * 检查模型是否可用
     * 
     * @param modelKey 模型键
     * @return true表示可用，false表示不可用
     */
    public boolean isModelAvailable(String modelKey) {
        try {
            DynamicAiModelBeanManager.DynamicAiVideoService videoService = 
                    modelBeanManager.getVideoServiceByModelKey(modelKey);
            return videoService != null && videoService.isModelAvailable();
        } catch (Exception e) {
            log.warn("检查视频生成模型可用性失败，模型：{}，错误：{}", modelKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取模型名称
     * 
     * @param modelKey 模型键
     * @return 模型名称
     */
    public String getModelName(String modelKey) {
        DynamicAiModelBeanManager.DynamicAiVideoService videoService = 
                modelBeanManager.getVideoServiceByModelKey(modelKey);
        
        if (videoService == null) {
            throw BizException.of(ErrorCodeEnum.AI_MODEL_NOT_AVAILABLE, 
                    "视频生成模型不存在：" + modelKey);
        }
        
        return videoService.getModelName();
    }
}
