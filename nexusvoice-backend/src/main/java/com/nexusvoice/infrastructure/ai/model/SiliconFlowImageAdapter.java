package com.nexusvoice.infrastructure.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;
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
import java.util.*;

/**
 * 硅基流动图像生成适配器
 * 
 * @author NexusVoice Team
 * @since 2025-01-24
 */
@Slf4j
@Component
public class SiliconFlowImageAdapter {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FileUploadService fileUploadService;
    
    private static final String IMAGES_GENERATIONS_ENDPOINT = "/images/generations";
    
    public SiliconFlowImageAdapter(
            @Qualifier("searchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            FileUploadService fileUploadService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.fileUploadService = fileUploadService;
    }
    
    /**
     * 生成图像
     * 
     * @param request 图像生成请求
     * @param model AI模型配置
     * @param apiKey API密钥
     * @return 图像生成结果
     */
    public ImageGenerationResult generateImage(ImageGenerationRequest request, AiModel model, AiApiKey apiKey) {
        log.info("开始调用硅基流动API生成图像，模型: {}, 提示词: {}", 
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
            String url = baseUrl + IMAGES_GENERATIONS_ENDPOINT;
            
            log.debug("调用硅基流动API: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            long generationTime = System.currentTimeMillis() - startTime;
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("硅基流动API调用失败，状态码: {}, 响应: {}", 
                         response.getStatusCode(), response.getBody());
                throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, 
                                     "图像生成失败，状态码: " + response.getStatusCode());
            }
            
            // 4. 解析响应
            ImageGenerationResult result = parseResponse(response.getBody(), request, generationTime, model);
            
            // 5. 下载图像并上传到七牛云CDN
            List<String> cdnUrls = uploadImagesToCdn(result.getImageUrls());
            result.setImageUrls(cdnUrls);
            
            log.info("图像生成成功，耗时: {}ms, 生成数量: {}, 种子: {}", 
                    result.getGenerationTime(), result.getImageCount(), result.getUsedSeed());
            
            return result;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("图像生成过程中发生异常", e);
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, 
                                 "图像生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ImageGenerationRequest request, AiModel model) {
        Map<String, Object> body = new HashMap<>();
        
        // 必需参数：使用实际的模型名称（如 Kwai-Kolors/Kolors）
        body.put("model", model.getModelName());
        body.put("prompt", request.getPrompt());
        
        // 可选参数
        if (request.getNegativePrompt() != null) {
            body.put("negative_prompt", request.getNegativePrompt());
        }
        
        if (request.getImageSize() != null) {
            body.put("image_size", request.getImageSize().getSize());
        }
        
        // 批量生成（Kolors模型支持，根据modelKey判断）
        if (request.getBatchSize() != null && request.getModelKey().contains("kolors")) {
            body.put("batch_size", request.getBatchSize());
        }
        
        if (request.getSeed() != null) {
            body.put("seed", request.getSeed());
        }
        
        if (request.getNumInferenceSteps() != null) {
            body.put("num_inference_steps", request.getNumInferenceSteps());
        }
        
        // Kolors模型支持guidanceScale
        if (request.getGuidanceScale() != null && request.getModelKey().contains("kolors")) {
            body.put("guidance_scale", request.getGuidanceScale());
        }
        
        // Qwen模型支持CFG
        if (request.getCfg() != null && request.getModelKey().contains("qwen")) {
            body.put("cfg", request.getCfg());
        }
        
        return body;
    }
    
    /**
     * 解析API响应
     */
    private ImageGenerationResult parseResponse(String responseBody, ImageGenerationRequest request, 
                                                long generationTime, AiModel model) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            ImageGenerationResult result = new ImageGenerationResult();
            result.setGenerationTime(generationTime);
            result.setModelName(model.getModelName());
            result.setImageSize(request.getImageSize().getSize());
            result.setRawResponse(responseBody);
            
            // 解析图像URL列表
            List<String> imageUrls = new ArrayList<>();
            JsonNode imagesNode = responseJson.get("images");
            if (imagesNode != null && imagesNode.isArray()) {
                for (JsonNode imageNode : imagesNode) {
                    JsonNode urlNode = imageNode.get("url");
                    if (urlNode != null) {
                        imageUrls.add(urlNode.asText());
                    }
                }
            }
            result.setImageUrls(imageUrls);
            
            // 解析使用的种子
            JsonNode seedNode = responseJson.get("seed");
            if (seedNode != null) {
                result.setUsedSeed(seedNode.asLong());
            }
            
            log.debug("解析硅基流动API响应成功，图像数量: {}", imageUrls.size());
            return result;
            
        } catch (Exception e) {
            log.error("解析硅基流动API响应失败: {}", responseBody, e);
            throw BizException.of(ErrorCodeEnum.IMAGE_SERVICE_ERROR, 
                                 "解析图像生成响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传图像到七牛云CDN
     */
    private List<String> uploadImagesToCdn(List<String> originalUrls) {
        if (originalUrls == null || originalUrls.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> cdnUrls = new ArrayList<>();
        
        for (int i = 0; i < originalUrls.size(); i++) {
            String originalUrl = originalUrls.get(i);
            try {
                log.debug("下载并上传第{}张图像到CDN: {}", i + 1, originalUrl);
                
                // 下载图像
                MultipartFile imageFile = downloadImageAsMultipartFile(originalUrl, i);
                
                // 上传到七牛云
                String cdnUrl = fileUploadService.upload(imageFile, FileTypeEnum.IMAGE);
                cdnUrls.add(cdnUrl);
                
                log.info("第{}张图像上传CDN成功: {}", i + 1, cdnUrl);
                
            } catch (Exception e) {
                log.error("上传第{}张图像到CDN失败: {}", i + 1, originalUrl, e);
                throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                                     "图像上传CDN失败: " + e.getMessage());
            }
        }
        
        return cdnUrls;
    }
    
    /**
     * 下载图像并转换为MultipartFile
     */
    private MultipartFile downloadImageAsMultipartFile(String imageUrl, int index) throws IOException {
        java.net.URI uri = java.net.URI.create(imageUrl);
        try (InputStream inputStream = uri.toURL().openStream()) {
            byte[] imageData = inputStream.readAllBytes();
            
            // 从URL推断文件格式
            final String fileName;
            final String contentType;
            
            if (imageUrl.toLowerCase().contains(".jpg") || imageUrl.toLowerCase().contains(".jpeg")) {
                fileName = String.format("generated_image_%d_%d.jpg", 
                                        System.currentTimeMillis(), index);
                contentType = "image/jpeg";
            } else if (imageUrl.toLowerCase().contains(".png")) {
                fileName = String.format("generated_image_%d_%d.png", 
                                        System.currentTimeMillis(), index);
                contentType = "image/png";
            } else {
                fileName = String.format("generated_image_%d_%d.jpg", 
                                        System.currentTimeMillis(), index);
                contentType = "image/jpeg";
            }
            
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
                    return imageData == null || imageData.length == 0;
                }
                
                @Override
                public long getSize() {
                    return imageData.length;
                }
                
                @Override
                public byte[] getBytes() {
                    return imageData;
                }
                
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(imageData);
                }
                
                @Override
                public void transferTo(File dest) throws IOException {
                    java.nio.file.Files.write(dest.toPath(), imageData);
                }
            };
        }
    }
}
