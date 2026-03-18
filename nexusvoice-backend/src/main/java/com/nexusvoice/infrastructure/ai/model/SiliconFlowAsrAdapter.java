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
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 硅基流动ASR语音识别适配器
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Component
public class SiliconFlowAsrAdapter {
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    private static final String AUDIO_TRANSCRIPTIONS_ENDPOINT = "/audio/transcriptions";
    
    public SiliconFlowAsrAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
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
            byte[] requestBody = buildMultipartBody(request, model);

            String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                    ? apiKey.getBaseUrl() 
                    : model.getDefaultBaseUrl();
            String url = baseUrl + AUDIO_TRANSCRIPTIONS_ENDPOINT;
            
            log.debug("调用硅基流动ASR API: {}", url);

            String boundary = currentBoundary.get();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            long transcriptionTime = System.currentTimeMillis() - startTime;
            
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("硅基流动ASR API调用失败，状态码: {}, 响应: {}", 
                         response.statusCode(), response.body());
                throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                                     "语音识别失败，状态码: " + response.statusCode() + ": " + response.body());
            }
            
            // 4. 解析响应
            AudioTranscriptionResult result = parseResponse(response.body(), transcriptionTime, model);
            
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

    private final ThreadLocal<String> currentBoundary = new ThreadLocal<>();

    private byte[] buildMultipartBody(AudioTranscriptionRequest request, AiModel model) throws Exception {
        String boundary = "----NexusVoiceBoundary" + UUID.randomUUID().toString().replace("-", "");
        currentBoundary.set(boundary);

        String originalFilename = request.getAudioFile().getOriginalFilename();
        String filename = originalFilename != null && !originalFilename.isBlank()
                ? originalFilename
                : "audio-upload.webm";
        String contentType = request.getAudioFile().getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        byte[] fileBytes = request.getAudioFile().getBytes();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeTextPart(output, boundary, "model", model.getModelName());
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            writeTextPart(output, boundary, "language", request.getLanguage());
        }
        writeFilePart(output, boundary, "file", filename, contentType, fileBytes);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return output.toByteArray();
    }

    private void writeTextPart(ByteArrayOutputStream output,
                               String boundary,
                               String name,
                               String value) throws Exception {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream output,
                               String boundary,
                               String name,
                               String filename,
                               String contentType,
                               byte[] fileBytes) throws Exception {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
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
