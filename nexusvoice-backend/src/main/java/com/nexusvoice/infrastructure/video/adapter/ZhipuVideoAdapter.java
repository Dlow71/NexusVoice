package com.nexusvoice.infrastructure.video.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.file.service.UnifiedFileUploadService;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.video.model.VideoGenerationRequest;
import com.nexusvoice.domain.video.model.VideoGenerationResult;
import com.nexusvoice.domain.video.repository.VideoRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 智谱AI视频生成适配器
 * 实现CogVideoX-Flash异步视频生成
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@Component
public class ZhipuVideoAdapter implements VideoRepository {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UnifiedFileUploadService fileUploadService;
    
    private static final String VIDEO_GENERATIONS_ENDPOINT = "/paas/v4/videos/generations";
    private static final String ASYNC_RESULT_ENDPOINT = "/paas/v4/async-result/";
    
    public ZhipuVideoAdapter(
            @Qualifier("searchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            UnifiedFileUploadService fileUploadService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.fileUploadService = fileUploadService;
    }
    
    @Override
    public VideoGenerationResult submitTask(VideoGenerationRequest request, AiModel model, AiApiKey apiKey) {
        log.info("提交智谱视频生成任务，模型: {}, 提示词: {}", 
                model.getModelName(), request.getPrompt());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request, model);
            
            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey.getApiKey());
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            
            // 3. 调用API
            String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                    ? apiKey.getBaseUrl() 
                    : model.getDefaultBaseUrl();
            String url = baseUrl + VIDEO_GENERATIONS_ENDPOINT;
            
            log.debug("调用智谱视频生成API: {}", url);
            log.debug("请求参数: {}", objectMapper.writeValueAsString(requestBody));
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("智谱视频生成API调用失败，状态码: {}, 响应: {}", 
                         response.getStatusCode(), response.getBody());
                throw BizException.of(ErrorCodeEnum.VIDEO_GENERATION_FAILED, 
                                     "视频生成任务提交失败，状态码: " + response.getStatusCode());
            }
            
            // 4. 解析响应
            VideoGenerationResult result = parseSubmitResponse(response.getBody(), model);
            result.setGenerationTime(System.currentTimeMillis() - startTime);
            
            log.info("视频生成任务提交成功，任务ID: {}, 模型: {}", result.getTaskId(), model.getModelName());
            
            return result;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频生成任务提交过程中发生异常", e);
            throw BizException.of(ErrorCodeEnum.VIDEO_GENERATION_FAILED, 
                                 "视频生成任务提交失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public VideoGenerationResult queryTask(String taskId, AiModel model, AiApiKey apiKey) {
        log.debug("查询智谱视频生成任务，任务ID: {}", taskId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey.getApiKey());
            
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            
            // 2. 调用API
            String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                    ? apiKey.getBaseUrl() 
                    : model.getDefaultBaseUrl();
            String url = baseUrl + ASYNC_RESULT_ENDPOINT + taskId;
            
            log.debug("查询智谱视频生成结果: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("智谱视频结果查询失败，状态码: {}, 响应: {}", 
                         response.getStatusCode(), response.getBody());
                throw BizException.of(ErrorCodeEnum.VIDEO_QUERY_FAILED, 
                                     "视频生成结果查询失败，状态码: " + response.getStatusCode());
            }
            
            // 3. 解析响应
            VideoGenerationResult result = parseQueryResponse(response.getBody(), taskId, model);
            
            // 4. 如果生成成功，下载视频并上传到CDN
            if (result.isSuccess() && result.getVideoUrl() != null) {
                String cdnUrl = uploadVideoToCdn(result.getVideoUrl());
                result.setVideoUrl(cdnUrl);
                
                // 如果有封面图也上传到CDN
                if (result.getCoverImageUrl() != null) {
                    String coverCdnUrl = uploadImageToCdn(result.getCoverImageUrl());
                    result.setCoverImageUrl(coverCdnUrl);
                }
            }
            
            long queryTime = System.currentTimeMillis() - startTime;
            log.debug("视频生成结果查询完成，任务ID: {}, 状态: {}, 耗时: {}ms", 
                     taskId, result.getTaskStatus(), queryTime);
            
            return result;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频生成结果查询过程中发生异常，任务ID: {}", taskId, e);
            throw BizException.of(ErrorCodeEnum.VIDEO_QUERY_FAILED, 
                                 "视频生成结果查询失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable(AiModel model, AiApiKey apiKey) {
        return model != null && model.isEnabled() && 
               apiKey != null && apiKey.isAvailable();
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(VideoGenerationRequest request, AiModel model) {
        Map<String, Object> body = new HashMap<>();
        
        // 必需参数：模型代码
        body.put("model", model.getModelCode());
        
        // 文本描述
        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            body.put("prompt", request.getPrompt());
        }
        
        // 质量模式
        if (request.getQuality() != null) {
            body.put("quality", request.getQuality().getCode());
        }
        
        // 音效
        if (request.getWithAudio() != null) {
            body.put("with_audio", request.getWithAudio());
        }
        
        // 水印
        if (request.getWatermarkEnabled() != null) {
            body.put("watermark_enabled", request.getWatermarkEnabled());
        }
        
        // 图像URL（图生视频或首尾帧模式）
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            body.put("image_url", request.getImageUrl());
        } else if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
            body.put("image_url", request.getImageBase64());
        } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            body.put("image_url", request.getImageUrls());
        }
        
        // 视频尺寸（智谱API接受字符串格式，如 "1280x720"）
        if (request.getSize() != null) {
            body.put("size", request.getSize().getSize());
        }
        
        // 帧率
        if (request.getFps() != null) {
            body.put("fps", request.getFps());
        }
        
        // 持续时长
        if (request.getDuration() != null) {
            body.put("duration", request.getDuration());
        }
        
        // 请求ID
        if (request.getRequestId() != null) {
            body.put("request_id", request.getRequestId());
        }
        
        // 用户ID（转为字符串）
        if (request.getUserId() != null) {
            body.put("user_id", String.valueOf(request.getUserId()));
        }
        
        return body;
    }
    
    /**
     * 解析提交响应
     */
    private VideoGenerationResult parseSubmitResponse(String responseBody, AiModel model) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            VideoGenerationResult result = new VideoGenerationResult();
            result.setRawResponse(responseBody);
            result.setModelName(model.getModelName());
            
            // 解析任务ID
            JsonNode idNode = responseJson.get("id");
            if (idNode != null) {
                result.setTaskId(idNode.asText());
            }
            
            // 解析请求ID
            JsonNode requestIdNode = responseJson.get("request_id");
            if (requestIdNode != null) {
                result.setRequestId(requestIdNode.asText());
            }
            
            // 解析任务状态
            JsonNode statusNode = responseJson.get("task_status");
            if (statusNode != null) {
                result.setTaskStatus(statusNode.asText());
            } else {
                result.setTaskStatus("PROCESSING"); // 默认处理中
            }
            
            log.debug("解析智谱视频提交响应成功，任务ID: {}", result.getTaskId());
            return result;
            
        } catch (Exception e) {
            log.error("解析智谱视频提交响应失败: {}", responseBody, e);
            throw BizException.of(ErrorCodeEnum.VIDEO_SERVICE_ERROR, 
                                 "解析视频生成提交响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析查询响应
     */
    private VideoGenerationResult parseQueryResponse(String responseBody, String taskId, AiModel model) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            VideoGenerationResult result = new VideoGenerationResult();
            result.setTaskId(taskId);
            result.setRawResponse(responseBody);
            result.setModelName(model.getModelName());
            
            // 解析请求ID
            JsonNode requestIdNode = responseJson.get("request_id");
            if (requestIdNode != null) {
                result.setRequestId(requestIdNode.asText());
            }
            
            // 解析创建时间
            JsonNode createdNode = responseJson.get("created");
            if (createdNode != null) {
                result.setCreatedAt(createdNode.asLong());
            }
            
            // 解析video_result
            JsonNode videoResultNode = responseJson.get("video_result");
            if (videoResultNode != null && videoResultNode.isArray() && videoResultNode.size() > 0) {
                JsonNode firstVideo = videoResultNode.get(0);
                
                // 视频URL
                JsonNode urlNode = firstVideo.get("url");
                if (urlNode != null) {
                    result.setVideoUrl(urlNode.asText());
                    result.setTaskStatus("SUCCESS");
                }
                
                // 封面图URL
                JsonNode coverNode = firstVideo.get("cover_image_url");
                if (coverNode != null) {
                    result.setCoverImageUrl(coverNode.asText());
                }
            } else {
                // 检查是否有错误信息
                JsonNode choicesNode = responseJson.get("choices");
                if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode firstChoice = choicesNode.get(0);
                    JsonNode finishReasonNode = firstChoice.get("finish_reason");
                    
                    if (finishReasonNode != null && "network_error".equals(finishReasonNode.asText())) {
                        result.setTaskStatus("FAIL");
                        result.setErrorMessage("模型推理异常");
                    } else {
                        result.setTaskStatus("PROCESSING");
                    }
                } else {
                    result.setTaskStatus("PROCESSING");
                }
            }
            
            // 解析Token使用情况
            JsonNode usageNode = responseJson.get("usage");
            if (usageNode != null) {
                VideoGenerationResult.TokenUsage tokenUsage = new VideoGenerationResult.TokenUsage();
                
                JsonNode promptTokensNode = usageNode.get("prompt_tokens");
                if (promptTokensNode != null) {
                    tokenUsage.setPromptTokens(promptTokensNode.asInt());
                }
                
                JsonNode completionTokensNode = usageNode.get("completion_tokens");
                if (completionTokensNode != null) {
                    tokenUsage.setCompletionTokens(completionTokensNode.asInt());
                }
                
                JsonNode totalTokensNode = usageNode.get("total_tokens");
                if (totalTokensNode != null) {
                    tokenUsage.setTotalTokens(totalTokensNode.asInt());
                }
                
                result.setTokenUsage(tokenUsage);
            }
            
            log.debug("解析智谱视频查询响应成功，任务ID: {}, 状态: {}", taskId, result.getTaskStatus());
            return result;
            
        } catch (Exception e) {
            log.error("解析智谱视频查询响应失败: {}", responseBody, e);
            throw BizException.of(ErrorCodeEnum.VIDEO_SERVICE_ERROR, 
                                 "解析视频生成查询响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传视频到CDN
     */
    private String uploadVideoToCdn(String videoUrl) {
        try {
            log.debug("下载并上传视频到CDN: {}", videoUrl);
            
            // 下载视频
            MultipartFile videoFile = downloadFileAsMultipartFile(videoUrl, "video", ".mp4", "video/mp4");
            
            // 上传到CDN
            String cdnUrl = fileUploadService.upload(videoFile, FileTypeEnum.VIDEO);
            
            log.info("视频上传CDN成功: {}", cdnUrl);
            return cdnUrl;
            
        } catch (Exception e) {
            log.error("上传视频到CDN失败: {}", videoUrl, e);
            // 不抛出异常，返回原始URL
            return videoUrl;
        }
    }
    
    /**
     * 上传图像到CDN
     */
    private String uploadImageToCdn(String imageUrl) {
        try {
            log.debug("下载并上传封面图到CDN: {}", imageUrl);
            
            // 下载图像
            MultipartFile imageFile = downloadFileAsMultipartFile(imageUrl, "cover", ".jpg", "image/jpeg");
            
            // 上传到CDN
            String cdnUrl = fileUploadService.upload(imageFile, FileTypeEnum.IMAGE);
            
            log.info("封面图上传CDN成功: {}", cdnUrl);
            return cdnUrl;
            
        } catch (Exception e) {
            log.error("上传封面图到CDN失败: {}", imageUrl, e);
            // 不抛出异常，返回原始URL
            return imageUrl;
        }
    }
    
    /**
     * 下载文件并转换为MultipartFile
     */
    private MultipartFile downloadFileAsMultipartFile(String fileUrl, String prefix, 
                                                     String extension, String contentType) throws IOException {
        java.net.URI uri = java.net.URI.create(fileUrl);
        try (InputStream inputStream = uri.toURL().openStream()) {
            byte[] fileData = inputStream.readAllBytes();
            
            final String fileName = String.format("%s_%d%s", prefix, System.currentTimeMillis(), extension);
            
            // 创建自定义的MultipartFile实现
            return new MultipartFile() {
                @Override
                public String getName() {
                    return fileName;
                }
                
                @Override
                public String getOriginalFilename() {
                    return fileName;
                }
                
                @Override
                public String getContentType() {
                    return contentType;
                }
                
                @Override
                public boolean isEmpty() {
                    return fileData == null || fileData.length == 0;
                }
                
                @Override
                public long getSize() {
                    return fileData.length;
                }
                
                @Override
                public byte[] getBytes() {
                    return fileData;
                }
                
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(fileData);
                }
                
                @Override
                public void transferTo(File dest) throws IOException {
                    java.nio.file.Files.write(dest.toPath(), fileData);
                }
            };
        }
    }
}
