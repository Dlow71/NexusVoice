package com.nexusvoice.infrastructure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;
import com.nexusvoice.domain.image.repository.ImageGenerationRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 硅基流动图像生成仓储实现
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Slf4j
@Repository
public class SiliconFlowImageGenerationRepositoryImpl implements ImageGenerationRepository {
    
    @Resource
    @Qualifier("searchRestTemplate")
    private RestTemplate restTemplate;
    
    @Resource
    private ObjectMapper objectMapper;
    
    @Resource 
    private FileUploadService fileUploadService;
    
    @Value("${nexusvoice.image.siliconflow.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;
    
    @Value("${nexusvoice.image.siliconflow.api-key}")
    private String apiKey;
    
    @Value("${nexusvoice.image.enabled:true}")
    private Boolean imageServiceEnabled;
    
    private static final String IMAGES_GENERATIONS_ENDPOINT = "/images/generations";
    
    @Override
    public ImageGenerationResult generateImage(ImageGenerationRequest request) {
        if (!imageServiceEnabled) {
            throw BizException.of(ErrorCodeEnum.IMAGE_SERVICE_ERROR, "图像生成服务未启用");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.IMAGE_API_KEY_INVALID, "硅基流动API密钥未配置");
        }
        
        log.info("开始调用硅基流动API生成图像，模型: {}, 提示词: {}", 
                request.getModel().getModelName(), request.getPrompt());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            
            // 调用API
            String url = baseUrl + IMAGES_GENERATIONS_ENDPOINT;
            log.debug("调用硅基流动API: {} 请求体: {}", url, requestBody);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            long generationTime = System.currentTimeMillis() - startTime;
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("硅基流动API调用失败，状态码: {}, 响应: {}", 
                         response.getStatusCode(), response.getBody());
                throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, 
                                     "图像生成失败，状态码: " + response.getStatusCode());
            }
            
            // 解析响应
            ImageGenerationResult result = parseResponse(response.getBody(), request, generationTime);
            
            // 下载图像并上传到七牛云
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
    
    @Override
    public boolean isServiceAvailable() {
        if (!imageServiceEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 使用简单的验证请求检查服务可用性
            return validateApiKey(apiKey);
        } catch (Exception e) {
            log.warn("检查图像生成服务可用性失败", e);
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedModels() {
        return Arrays.asList(
            "Qwen/Qwen-Image-Edit-2509",
            "Qwen/Qwen-Image-Edit", 
            "Qwen/Qwen-Image",
            "Kwai-Kolors/Kolors"
        );
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        // 这里可以实现一个轻量级的API密钥验证
        // 目前简单检查密钥格式
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.startsWith("sk-");
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ImageGenerationRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // 必需参数
        body.put("model", request.getModel().getModelName());
        body.put("prompt", request.getPrompt());
        
        // 可选参数
        if (request.getNegativePrompt() != null) {
            body.put("negative_prompt", request.getNegativePrompt());
        }
        
        if (request.getImageSize() != null) {
            body.put("image_size", request.getImageSize().getSize());
        }
        
        if (request.getBatchSize() != null && request.getModel().supportsBatchGeneration()) {
            body.put("batch_size", request.getBatchSize());
        }
        
        if (request.getSeed() != null) {
            body.put("seed", request.getSeed());
        }
        
        if (request.getNumInferenceSteps() != null) {
            body.put("num_inference_steps", request.getNumInferenceSteps());
        }
        
        if (request.getGuidanceScale() != null && request.getModel().supportsGuidanceScale()) {
            body.put("guidance_scale", request.getGuidanceScale());
        }
        
        if (request.getCfg() != null && request.getModel().supportsCFG()) {
            body.put("cfg", request.getCfg());
        }
        
        // 输入图像（用于图像编辑）
        if (request.getInputImage() != null) {
            body.put("image", request.getInputImage());
        }
        
        if (request.getInputImage2() != null) {
            body.put("image2", request.getInputImage2());
        }
        
        if (request.getInputImage3() != null) {
            body.put("image3", request.getInputImage3());
        }
        
        return body;
    }
    
    /**
     * 解析API响应
     */
    private ImageGenerationResult parseResponse(String responseBody, ImageGenerationRequest request, long generationTime) {
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            ImageGenerationResult result = new ImageGenerationResult();
            result.setGenerationTime(generationTime);
            result.setModelName(request.getModel().getModelName());
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
            } else {
                fileName = String.format("generated_image_%d_%d.png", 
                                        System.currentTimeMillis(), index);
                contentType = "image/png";
            }
            
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "image";
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
                    return imageData.length == 0;
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
                public void transferTo(java.io.File dest) throws IOException {
                    throw new UnsupportedOperationException("不支持transferTo操作");
                }
            };
        }
    }
}
