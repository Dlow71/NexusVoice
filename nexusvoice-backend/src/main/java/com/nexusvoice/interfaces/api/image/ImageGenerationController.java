package com.nexusvoice.interfaces.api.image;

import com.nexusvoice.application.image.dto.ImageGenerationRequestDTO;
import com.nexusvoice.application.image.dto.ImageGenerationResponseDTO;
import com.nexusvoice.application.image.service.ImageGenerationService;
import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图像生成API控制器
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/image")
@Tag(name = "图像生成", description = "AI图像生成相关接口")
@Validated
public class ImageGenerationController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Operation(summary = "生成图像", description = "根据提示词使用AI模型生成图像")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "图像生成成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "图像生成失败")
    })
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ImageGenerationResponseDTO> generateImage(
            @Parameter(description = "图像生成请求", required = true)
            @Valid @RequestBody ImageGenerationRequestDTO requestDTO) {
        
        log.info("收到图像生成请求，模型: {}, 提示词: {}, 尺寸: {}", 
                requestDTO.getModel(), 
                requestDTO.getPrompt().length() > 50 ? 
                    requestDTO.getPrompt().substring(0, 50) + "..." : requestDTO.getPrompt(),
                requestDTO.getImageSize());

        long startTime = System.currentTimeMillis();
        
        try {
            ImageGenerationResponseDTO responseDTO = imageGenerationService.generateImage(requestDTO);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("图像生成请求处理完成，耗时: {}ms, 生成数量: {}, CDN URL: {}", 
                    processingTime, responseDTO.getImageCount(), responseDTO.getFirstImageUrl());
            
            return Result.success(responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("图像生成请求处理失败，耗时: {}ms", processingTime, e);
            throw e;
        }
    }

    @Operation(summary = "批量生成图像", description = "批量生成多张图像（仅Kolors模型支持）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量图像生成成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "批量图像生成失败")
    })
    @PostMapping(value = "/generate/batch", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ImageGenerationResponseDTO> generateImageBatch(
            @Parameter(description = "批量图像生成请求", required = true)
            @Valid @RequestBody ImageGenerationRequestDTO requestDTO) {
        
        log.info("收到批量图像生成请求，模型: {}, 批量大小: {}, 提示词: {}", 
                requestDTO.getModel(), 
                requestDTO.getBatchSize(),
                requestDTO.getPrompt().length() > 50 ? 
                    requestDTO.getPrompt().substring(0, 50) + "..." : requestDTO.getPrompt());

        long startTime = System.currentTimeMillis();
        
        try {
            ImageGenerationResponseDTO responseDTO = imageGenerationService.generateImageBatch(requestDTO);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("批量图像生成请求处理完成，耗时: {}ms, 生成数量: {}", 
                    processingTime, responseDTO.getImageCount());
            
            return Result.success(responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("批量图像生成请求处理失败，耗时: {}ms", processingTime, e);
            throw e;
        }
    }

    @Operation(summary = "获取支持的模型", description = "获取图像生成服务支持的所有模型列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取模型列表成功"),
        @ApiResponse(responseCode = "500", description = "获取模型列表失败")
    })
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<String>> getSupportedModels() {
        log.debug("收到获取支持模型列表请求");
        
        try {
            List<String> models = imageGenerationService.getSupportedModels();
            log.info("获取支持的模型列表成功，模型数量: {}", models.size());
            return Result.success(models);
            
        } catch (Exception e) {
            log.error("获取支持的模型列表失败", e);
            throw e;
        }
    }

    @Operation(summary = "获取模型推荐参数", description = "获取指定模型的推荐参数配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取推荐参数成功"),
        @ApiResponse(responseCode = "400", description = "模型名称无效"),
        @ApiResponse(responseCode = "500", description = "获取推荐参数失败")
    })
    @GetMapping(value = "/models/{modelName}/recommended-params", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ImageGenerationRequestDTO> getModelRecommendedParams(
            @Parameter(description = "模型名称", required = true, example = "Qwen/Qwen-Image")
            @PathVariable String modelName) {
        
        log.debug("收到获取模型推荐参数请求，模型: {}", modelName);
        
        if (modelName == null || modelName.trim().isEmpty()) {
            return Result.error("模型名称不能为空");
        }
        
        try {
            ImageGenerationRequestDTO recommendedParams = imageGenerationService.getModelRecommendedParams(modelName);
            log.info("获取模型推荐参数成功，模型: {}", modelName);
            return Result.success(recommendedParams);
            
        } catch (Exception e) {
            log.error("获取模型推荐参数失败，模型: {}", modelName, e);
            throw e;
        }
    }

    @Operation(summary = "健康检查", description = "检查图像生成服务的健康状态")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "健康检查完成")
    })
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> healthCheck() {
        log.debug("收到图像生成服务健康检查请求");
        
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("timestamp", System.currentTimeMillis());
        
        try {
            boolean serviceAvailable = imageGenerationService.checkServiceHealth();
            healthInfo.put("status", serviceAvailable ? "UP" : "DOWN");
            healthInfo.put("message", serviceAvailable ? "服务正常" : "服务不可用");
            
            if (serviceAvailable) {
                List<String> supportedModels = imageGenerationService.getSupportedModels();
                healthInfo.put("supportedModels", supportedModels);
                healthInfo.put("modelCount", supportedModels.size());
            }
            
            log.debug("图像生成服务健康检查完成，状态: {}", serviceAvailable ? "正常" : "异常");
            return Result.success(healthInfo);
            
        } catch (Exception e) {
            log.warn("图像生成服务健康检查异常", e);
            healthInfo.put("status", "ERROR");
            healthInfo.put("message", "健康检查异常: " + e.getMessage());
            return Result.success(healthInfo);
        }
    }

    @Operation(summary = "验证API密钥", description = "验证硅基流动API密钥是否有效")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API密钥验证完成")
    })
    @PostMapping(value = "/validate-api-key", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> validateApiKey(
            @Parameter(description = "API密钥", required = true)
            @RequestParam String apiKey) {
        
        log.debug("收到API密钥验证请求");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Result.error("API密钥不能为空");
        }
        
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("timestamp", System.currentTimeMillis());
        
        try {
            boolean valid = imageGenerationService.validateApiKey(apiKey);
            validationResult.put("valid", valid);
            validationResult.put("message", valid ? "API密钥有效" : "API密钥无效");
            
            log.info("API密钥验证完成，结果: {}", valid ? "有效" : "无效");
            return Result.success(validationResult);
            
        } catch (Exception e) {
            log.error("API密钥验证失败", e);
            validationResult.put("valid", false);
            validationResult.put("message", "验证失败: " + e.getMessage());
            return Result.success(validationResult);
        }
    }
}
