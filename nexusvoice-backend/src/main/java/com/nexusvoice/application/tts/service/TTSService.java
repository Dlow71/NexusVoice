package com.nexusvoice.application.tts.service;

import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.TTSException;
import com.nexusvoice.infrastructure.config.QiniuConfig;
import com.nexusvoice.utils.TTSToolUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private QiniuConfig qiniuConfig;

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
            // 获取参数，设置默认值
            String voiceType = requestDTO.getVoiceType() != null ? requestDTO.getVoiceType() : "qiniu_zh_female_wwxkjx";
            String encoding = requestDTO.getEncoding() != null ? requestDTO.getEncoding() : "mp3";
            Double speedRatio = requestDTO.getSpeedRatio() != null ? requestDTO.getSpeedRatio() : 1.0;

            // 创建TTS工具实例
            TTSToolUtils ttsToolUtils = TTSToolUtils.createWithDefaults(
                qiniuToken,
                voiceType,
                encoding,
                speedRatio
            );

            // 调用TTS服务生成音频文件
            MultipartFile audioFile = ttsToolUtils.textToAudioFile(requestDTO.getText(), voiceType, encoding, speedRatio);
            
            if (audioFile == null || audioFile.isEmpty()) {
                throw new TTSException("音频生成失败，返回文件为空");
            }

            // 上传到七牛云并获取URL
            String audioUrl = fileUploadService.upload(audioFile, FileTypeEnum.AUDIO);


            // 构建响应DTO
            TTSResponseDTO responseDTO = new TTSResponseDTO();
            responseDTO.setAudioData(audioUrl);
            responseDTO.setAudioFormat(encoding);
            responseDTO.setAudioSize((int) audioFile.getSize());
            responseDTO.setText(requestDTO.getText());
            responseDTO.setVoiceType(voiceType);
            responseDTO.setSpeedRatio(speedRatio);

            return responseDTO;

        } catch (TTSException e) {
            throw e;
        } catch (IOException e) {
            throw new TTSException("音频文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TTSException("TTS服务处理失败: " + e.getMessage(), e);
        }
    }

}
