package com.nexusvoice.interfaces.api.video;

import com.nexusvoice.application.video.assembler.VideoGenerationAssembler;
import com.nexusvoice.application.video.dto.VideoGenerationRequestDTO;
import com.nexusvoice.application.video.dto.VideoGenerationResponseDTO;
import com.nexusvoice.application.video.service.VideoGenerationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.video.model.VideoGenerationRequest;
import com.nexusvoice.domain.video.model.VideoGenerationResult;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频生成API控制器
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/video")
@Tag(name = "视频生成", description = "AI视频生成相关接口（异步模式）")
@Validated
@RequiredArgsConstructor
public class VideoGenerationController {
    
    private final VideoGenerationService videoGenerationService;
    private final VideoGenerationAssembler videoGenerationAssembler;
    
    @Operation(summary = "提交视频生成任务", 
               description = "提交视频生成任务到队列，返回任务ID用于后续查询。支持文生视频、图生视频、首尾帧生成三种模式。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "任务提交成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "任务提交失败")
    })
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<VideoGenerationResponseDTO> generateVideo(
            @Parameter(description = "视频生成请求。modelKey格式为 provider:model，如 zhipu:cogvideox-flash", required = true)
            @Valid @RequestBody VideoGenerationRequestDTO requestDTO) {
        
        log.info("收到视频生成任务提交请求，模型: {}, 提示词长度: {}, 尺寸: {}, 质量: {}", 
                requestDTO.getModelKey(),
                requestDTO.getPrompt() != null ? requestDTO.getPrompt().length() : 0,
                requestDTO.getSize(),
                requestDTO.getQuality());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            
            // DTO转领域模型
            VideoGenerationRequest request = videoGenerationAssembler.toDomain(requestDTO, userId);
            
            // 提交任务
            VideoGenerationResult result = videoGenerationService.submitTask(request);
            
            // 领域模型转DTO
            VideoGenerationResponseDTO responseDTO = videoGenerationAssembler.toDTO(result);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("视频生成任务提交成功，任务ID: {}, 耗时: {}ms", result.getTaskId(), processingTime);
            
            return Result.success(responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("视频生成任务提交失败，耗时: {}ms", processingTime, e);
            throw e;
        }
    }
    
    @Operation(summary = "查询视频生成任务结果", 
               description = "根据任务ID查询视频生成结果。建议每15秒轮询一次，最多查询10分钟。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping(value = "/query/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<VideoGenerationResponseDTO> queryTask(
            @Parameter(description = "任务ID", required = true, example = "202510271234567890")
            @PathVariable String taskId,
            @Parameter(description = "模型标识，格式：provider:model", required = true, example = "zhipu:cogvideox-flash")
            @RequestParam String modelKey) {
        
        log.debug("收到视频生成任务查询请求，任务ID: {}, 模型: {}", taskId, modelKey);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            
            // 构建原始请求（用于费用统计）
            VideoGenerationRequest originalRequest = new VideoGenerationRequest();
            originalRequest.setModelKey(modelKey);
            originalRequest.setUserId(userId);
            
            // 查询任务
            VideoGenerationResult result = videoGenerationService.queryTask(taskId, modelKey, originalRequest);
            
            // 领域模型转DTO
            VideoGenerationResponseDTO responseDTO = videoGenerationAssembler.toDTO(result);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (result.isSuccess()) {
                log.info("视频生成任务查询成功，任务ID: {}, 状态: SUCCESS, 视频URL: {}, 耗时: {}ms", 
                        taskId, result.getVideoUrl(), processingTime);
            } else if (result.isFailed()) {
                log.warn("视频生成任务失败，任务ID: {}, 错误: {}, 耗时: {}ms", 
                        taskId, result.getErrorMessage(), processingTime);
            } else {
                log.debug("视频生成任务处理中，任务ID: {}, 耗时: {}ms", taskId, processingTime);
            }
            
            return Result.success(responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("视频生成任务查询失败，任务ID: {}, 耗时: {}ms", taskId, processingTime, e);
            throw e;
        }
    }
    
    @Operation(summary = "获取支持的视频生成模型", 
               description = "获取所有可用的视频生成模型列表。返回格式为 provider:model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取模型列表成功"),
        @ApiResponse(responseCode = "500", description = "获取模型列表失败")
    })
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<String>> getSupportedModels() {
        log.debug("收到获取视频生成模型列表请求");
        
        try {
            // TODO: 实现获取支持的模型列表
            // 可以从DynamicAiModelBeanManager获取所有VIDEO类型的模型
            List<String> models = List.of("zhipu:cogvideox-flash");
            
            log.info("获取视频生成模型列表成功，模型数量: {}", models.size());
            return Result.success(models);
            
        } catch (Exception e) {
            log.error("获取视频生成模型列表失败", e);
            throw e;
        }
    }
    
    @Operation(summary = "获取模型支持的尺寸列表", 
               description = "获取指定模型支持的视频尺寸列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "模型名称无效")
    })
    @GetMapping(value = "/models/{modelKey}/sizes", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<String>> getSupportedSizes(
            @Parameter(description = "模型标识", required = true, example = "zhipu:cogvideox-flash")
            @PathVariable String modelKey) {
        
        log.debug("收到获取模型支持尺寸请求，模型: {}", modelKey);
        
        try {
            // 智谱CogVideoX-Flash支持的尺寸
            List<String> sizes = List.of(
                "1280x720",   // 横屏720P
                "720x1280",   // 竖屏720P
                "1024x1024",  // 方形1K
                "1920x1080",  // 横屏1080P
                "1080x1920",  // 竖屏1080P
                "2048x1080",  // 超宽屏
                "3840x2160"   // 4K
            );
            
            log.info("获取模型支持尺寸成功，模型: {}, 尺寸数量: {}", modelKey, sizes.size());
            return Result.success(sizes);
            
        } catch (Exception e) {
            log.error("获取模型支持尺寸失败，模型: {}", modelKey, e);
            throw e;
        }
    }
    
    @Operation(summary = "检查模型可用性", 
               description = "检查指定视频生成模型是否可用")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检查完成")
    })
    @GetMapping(value = "/models/{modelKey}/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> checkModelAvailability(
            @Parameter(description = "模型标识", required = true, example = "zhipu:cogvideox-flash")
            @PathVariable String modelKey) {
        
        log.debug("收到模型可用性检查请求，模型: {}", modelKey);
        
        Map<String, Object> availabilityInfo = new HashMap<>();
        availabilityInfo.put("modelKey", modelKey);
        availabilityInfo.put("timestamp", System.currentTimeMillis());
        
        try {
            boolean available = videoGenerationService.isModelAvailable(modelKey);
            String modelName = available ? videoGenerationService.getModelName(modelKey) : "未知";
            
            availabilityInfo.put("available", available);
            availabilityInfo.put("modelName", modelName);
            availabilityInfo.put("message", available ? "模型可用" : "模型不可用");
            
            log.info("模型可用性检查完成，模型: {}, 可用: {}", modelKey, available);
            return Result.success(availabilityInfo);
            
        } catch (Exception e) {
            log.warn("模型可用性检查异常，模型: {}", modelKey, e);
            availabilityInfo.put("available", false);
            availabilityInfo.put("message", "检查失败: " + e.getMessage());
            return Result.success(availabilityInfo);
        }
    }
    
    @Operation(summary = "健康检查", 
               description = "检查视频生成服务的健康状态")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "健康检查完成")
    })
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> healthCheck() {
        log.debug("收到视频生成服务健康检查请求");
        
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("timestamp", System.currentTimeMillis());
        
        try {
            // 检查至少一个模型是否可用
            boolean serviceAvailable = videoGenerationService.isModelAvailable("zhipu:cogvideox-flash");
            
            healthInfo.put("status", serviceAvailable ? "UP" : "DOWN");
            healthInfo.put("message", serviceAvailable ? "服务正常" : "服务不可用");
            healthInfo.put("serviceType", "video-generation");
            
            if (serviceAvailable) {
                healthInfo.put("supportedModels", List.of("zhipu:cogvideox-flash"));
                healthInfo.put("modelCount", 1);
            }
            
            log.debug("视频生成服务健康检查完成，状态: {}", serviceAvailable ? "正常" : "异常");
            return Result.success(healthInfo);
            
        } catch (Exception e) {
            log.warn("视频生成服务健康检查异常", e);
            healthInfo.put("status", "ERROR");
            healthInfo.put("message", "健康检查异常: " + e.getMessage());
            return Result.success(healthInfo);
        }
    }
    
    @Operation(summary = "获取推荐参数", 
               description = "获取指定模型的推荐参数配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping(value = "/models/{modelKey}/recommended-params", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<VideoGenerationRequestDTO> getRecommendedParams(
            @Parameter(description = "模型标识", required = true, example = "zhipu:cogvideox-flash")
            @PathVariable String modelKey) {
        
        log.debug("收到获取模型推荐参数请求，模型: {}", modelKey);
        
        try {
            VideoGenerationRequestDTO recommendedParams = new VideoGenerationRequestDTO();
            recommendedParams.setModelKey(modelKey);
            recommendedParams.setQuality("speed");           // 推荐速度优先
            recommendedParams.setWithAudio(false);           // 默认无音频
            recommendedParams.setWatermarkEnabled(true);     // 默认添加水印
            recommendedParams.setSize("1280x720");           // 推荐720P横屏
            recommendedParams.setFps(30);                    // 推荐30fps
            recommendedParams.setDuration(5);                // 推荐5秒
            recommendedParams.setPrompt("请输入视频描述...");
            
            log.info("获取模型推荐参数成功，模型: {}", modelKey);
            return Result.success(recommendedParams);
            
        } catch (Exception e) {
            log.error("获取模型推荐参数失败，模型: {}", modelKey, e);
            throw e;
        }
    }
}
