package com.nexusvoice.application.tts.service;

import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.exception.TTSException;
import com.nexusvoice.utils.TTSToolUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * TTS应用服务
 * 负责TTS业务流程编排和处理
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 */
@Service
public class TTSService {

    @Value("${nexusvoice.tts.token}")
    private String qiniuToken;

    /**
     * 文本转语音
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
            // 创建TTS工具实例
            TTSToolUtils ttsToolUtils = TTSToolUtils.createWithDefaults(
                qiniuToken,
                requestDTO.getVoiceType() != null ? requestDTO.getVoiceType() : "qiniu_zh_female_wwxkjx",
                requestDTO.getEncoding() != null ? requestDTO.getEncoding() : "mp3",
                requestDTO.getSpeedRatio() != null ? requestDTO.getSpeedRatio() : 1.0
            );

            // 调用TTS服务生成音频
            byte[] audioBytes = ttsToolUtils.textToAudioBytes(requestDTO.getText());
            
            if (audioBytes == null || audioBytes.length == 0) {
                throw new TTSException("音频生成失败，返回数据为空");
            }

            // 将音频字节转换为Base64编码
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // 构建响应DTO
            TTSResponseDTO responseDTO = new TTSResponseDTO();
            responseDTO.setAudioData(audioBase64);
            responseDTO.setAudioFormat(requestDTO.getEncoding() != null ? requestDTO.getEncoding() : "mp3");
            responseDTO.setAudioSize(audioBytes.length);
            responseDTO.setText(requestDTO.getText());
            responseDTO.setVoiceType(requestDTO.getVoiceType() != null ? requestDTO.getVoiceType() : "qiniu_zh_female_wwxkjx");
            responseDTO.setSpeedRatio(requestDTO.getSpeedRatio() != null ? requestDTO.getSpeedRatio() : 1.0);

            return responseDTO;

        } catch (TTSException e) {
            throw e;
        } catch (Exception e) {
            throw new TTSException("TTS服务处理失败: " + e.getMessage(), e);
        }
    }

}
