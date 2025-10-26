package com.nexusvoice.infrastructure.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.audio.model.AudioTranscriptionRequest;
import com.nexusvoice.domain.audio.model.AudioTranscriptionResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 硅基流动ASR语音识别适配器
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Component
public class SiliconFlowAsrAdapter {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String AUDIO_TRANSCRIPTIONS_ENDPOINT = "/audio/transcriptions";
    
    public SiliconFlowAsrAdapter(
            @Qualifier("searchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 语音转文本识别
     * 
     * @param request 语音识别请求
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return 语音识别结果
     */
    public AudioTranscriptionResult transcribe(AudioTranscriptionRequest request, AiModel model, AiApiKey apiKey) {
        log.info("开始调用硅基流动ASR API，模型: {}, 文件名: {}", 
                model.getModelName(), request.getAudioFile().getOriginalFilename());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 构建multipart/form-data请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 添加音频文件
            ByteArrayResource fileResource = new ByteArrayResource(request.getAudioFile().getBytes()) {
                @Override
                public String getFilename() {
                    return request.getAudioFile().getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            
            // 添加模型名称（使用实际模型名称，如 TeleAI/TeleSpeechASR）
            body.add("model", model.getModelName());
            
            // 可选参数
            if (request.getLanguage() != null) {
                body.add("language", request.getLanguage());
            }
            
            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey.getApiKey());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);
            
            // 3. 调用API
            String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                    ? apiKey.getBaseUrl() 
                    : model.getDefaultBaseUrl();
            String url = baseUrl + AUDIO_TRANSCRIPTIONS_ENDPOINT;
            
            log.debug("调用硅基流动ASR API: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            long transcriptionTime = System.currentTimeMillis() - startTime;
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("硅基流动ASR API调用失败，状态码: {}, 响应: {}", 
                         response.getStatusCode(), response.getBody());
                throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                                     "语音识别失败，状态码: " + response.getStatusCode());
            }
            
            // 4. 解析响应
            AudioTranscriptionResult result = parseResponse(response.getBody(), transcriptionTime, model);
            
            log.info("语音识别成功，耗时: {}ms, 文本长度: {}", 
                    result.getTranscriptionTime(), result.getTextLength());
            
            return result;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("语音识别过程中发生异常", e);
            throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                                 "语音识别失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析API响应
     */
    private AudioTranscriptionResult parseResponse(String responseBody, long transcriptionTime, AiModel model) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            AudioTranscriptionResult result = new AudioTranscriptionResult();
            result.setTranscriptionTime(transcriptionTime);
            result.setModelName(model.getModelName());
            result.setRawResponse(responseBody);
            
            // 解析识别文本
            JsonNode textNode = responseJson.get("text");
            if (textNode != null) {
                result.setText(textNode.asText());
            } else {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, "响应中缺少text字段");
            }
            
            // 解析音频时长（如果有）
            JsonNode durationNode = responseJson.get("duration");
            if (durationNode != null) {
                result.setAudioDuration(durationNode.asDouble());
            }
            
            log.debug("解析硅基流动ASR API响应成功，文本长度: {}", result.getTextLength());
            return result;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析硅基流动ASR API响应失败: {}", responseBody, e);
            throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                                 "解析语音识别响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 估算音频时长对应的token数（简单估算）
     * 通常：1秒音频 ≈ 175 tokens
     * 
     * @param audioDurationSeconds 音频时长（秒）
     * @return 估算的token数
     */
    public int estimateTokenCount(double audioDurationSeconds) {
        return (int) Math.ceil(audioDurationSeconds * 175);
    }
}
