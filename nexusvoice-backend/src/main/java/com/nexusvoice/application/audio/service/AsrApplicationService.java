package com.nexusvoice.application.audio.service;

import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.audio.model.AudioTranscriptionRequest;
import com.nexusvoice.domain.audio.model.AudioTranscriptionResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.service.AiAsrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ASR语音识别应用服务
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsrApplicationService {
    
    private final DynamicAiModelBeanManager modelManager;
    
    /**
     * 语音转文本
     * 
     * @param modelKey 模型键（格式：provider:model）
     * @param audioFile 音频文件
     * @param userId 用户ID
     * @return 识别结果
     */
    public AudioTranscriptionResult transcribe(String modelKey, MultipartFile audioFile, Long userId) {
        log.info("开始处理语音识别请求，模型：{}，用户ID：{}，文件名：{}", 
                modelKey, userId, audioFile.getOriginalFilename());
        
        // 构建请求
        AudioTranscriptionRequest request = new AudioTranscriptionRequest();
        request.setModelKey(modelKey);
        request.setAudioFile(audioFile);
        request.setUserId(userId);
        
        // 验证请求
        String validateError = request.validate();
        if (validateError != null) {
            log.error("语音识别请求验证失败：{}", validateError);
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, validateError);
        }
        
        // 获取ASR服务并执行识别
        try {
            AiAsrService asrService = modelManager.getAsrServiceByModelKey(modelKey);
            AudioTranscriptionResult result = asrService.transcribe(request);
            
            log.info("语音识别完成，模型：{}，用户ID：{}，文本长度：{}字符", 
                    modelKey, userId, result.getTextLength());
            
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("语音识别失败，模型：{}，用户ID：{}", modelKey, userId, e);
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, "语音识别失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有可用的ASR模型列表
     * 
     * @return 模型键列表
     */
    public List<String> getSupportedModels() {
        List<AiModel> asrModels = modelManager.getAvailableAsrModels();
        return asrModels.stream()
                .map(AiModel::getModelKey)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有可用ASR模型的详细信息
     * 
     * @return ASR模型列表
     */
    public List<AiModel> getAvailableModels() {
        return modelManager.getAvailableAsrModels();
    }
    
    /**
     * 检查指定模型是否可用
     * 
     * @param modelKey 模型键
     * @return 是否可用
     */
    public boolean isModelAvailable(String modelKey) {
        try {
            AiAsrService asrService = modelManager.getAsrServiceByModelKey(modelKey);
            return asrService.isModelAvailable();
        } catch (Exception e) {
            log.warn("检查ASR模型可用性失败，模型：{}", modelKey, e);
            return false;
        }
    }
}
