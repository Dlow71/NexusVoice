package com.nexusvoice.application.tts.service;

import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.tts.model.TtsRequest;
import com.nexusvoice.domain.tts.model.TtsResult;
import com.nexusvoice.domain.tts.model.TtsSegment;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.exception.TTSException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.service.AiTtsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * TTS应用服务
 * 负责TTS业务流程编排和处理，使用动态AI模型管理架构
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 * @updated 2025-10-27 重构到动态AI模型管理架构
 */
@Slf4j
@Service
public class TTSService {

    @Resource
    private DynamicAiModelBeanManager modelBeanManager;

    @Resource
    private SystemConfigService systemConfigService;


    /**
     * 文本转语音（新架构）
     * 使用动态AI模型管理器，完全复用密钥池、费用统计、调用日志等基础设施
     * 
     * @param requestDTO TTS请求DTO
     * @return TTS响应DTO
     * @throws TTSException TTS处理异常
     */
    public TTSResponseDTO textToSpeech(TTSRequestDTO requestDTO) throws TTSException {
        // 参数校验
        if (requestDTO == null || requestDTO.getText() == null || requestDTO.getText().trim().isEmpty()) {
            throw new TTSException("文本内容不能为空");
        }

        try {
            // 1. 获取默认TTS模型键（从system_config读取）
            String modelKey = systemConfigService.getString("tts.default.model", "qiniu:qiniu-tts");
            log.info("使用TTS模型：{}", modelKey);
            
            // 2. 获取TTS服务
            AiTtsService ttsService = modelBeanManager.getTtsServiceByModelKey(modelKey);
            
            // 3. 从system_config读取分段配置
            boolean chunkEnabled = systemConfigService.getBoolean("tts.chunk.enabled", true);
            int maxChunkChars = Math.max(50, Math.min(2000, systemConfigService.getInt("tts.chunk.max_chars", 300)));
            int maxConcurrency = Math.max(1, Math.min(16, systemConfigService.getInt("tts.chunk.max_concurrency", 4)));
            
            // 4. 构建Domain层请求
            TtsRequest domainRequest = TtsRequest.builder()
                    .userId(null) // TODO: 从SecurityContext获取当前用户ID
                    .text(requestDTO.getText().trim())
                    .voiceType(requestDTO.getVoiceType()) // 可以为null，适配器会从模型配置读取
                    .encoding(requestDTO.getEncoding() != null ? requestDTO.getEncoding() : "mp3")
                    .speedRatio(requestDTO.getSpeedRatio() != null ? requestDTO.getSpeedRatio() : 1.0)
                    .chunked(chunkEnabled && requestDTO.getText().trim().length() > maxChunkChars)
                    .maxChunkChars(maxChunkChars)
                    .maxConcurrency(maxConcurrency)
                    .conversationId(null) // 如果需要关联会话，从请求中获取
                    .expireMinutes(null) // 永久文件
                    .build();
            
            // 5. 调用TTS服务（自动处理密钥、费用、日志）
            TtsResult result = ttsService.synthesize(domainRequest);
            
            // 6. 转换为DTO返回
            return convertToResponseDTO(result);
            
        } catch (BizException e) {
            log.error("TTS服务调用失败：{}", e.getMessage(), e);
            throw new TTSException("TTS服务调用失败：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("TTS服务处理异常：{}", e.getMessage(), e);
            throw new TTSException("TTS服务处理异常：" + e.getMessage(), e);
        }
    }
    
    /**
     * 将TtsResult转换为TTSResponseDTO
     */
    private TTSResponseDTO convertToResponseDTO(TtsResult result) {
        TTSResponseDTO responseDTO = new TTSResponseDTO();
        responseDTO.setAudioData(result.getAudioUrl());
        responseDTO.setAudioFormat(result.getAudioFormat());
        responseDTO.setAudioSize(result.getAudioSize());
        responseDTO.setText(result.getText());
        responseDTO.setVoiceType(result.getVoiceType());
        responseDTO.setSpeedRatio(result.getSpeedRatio());
        responseDTO.setDuration(result.getAudioDuration());
        responseDTO.setChunked(result.getChunked());
        responseDTO.setGroupId(result.getGroupId());
        
        // 转换分段列表
        if (result.getSegments() != null && !result.getSegments().isEmpty()) {
            List<TTSResponseDTO.Segment> segmentDTOs = new ArrayList<>(result.getSegments().size());
            for (TtsSegment seg : result.getSegments()) {
                TTSResponseDTO.Segment segDTO = new TTSResponseDTO.Segment();
                segDTO.setIndex(seg.getIndex());
                segDTO.setText(seg.getText());
                segDTO.setUrl(seg.getUrl());
                segDTO.setSize(seg.getSize());
                segDTO.setDuration(seg.getDuration());
                segmentDTOs.add(segDTO);
            }
            responseDTO.setSegments(segmentDTOs);
        }
        
        return responseDTO;
    }
}
