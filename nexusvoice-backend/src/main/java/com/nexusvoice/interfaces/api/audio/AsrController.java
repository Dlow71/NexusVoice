package com.nexusvoice.interfaces.api.audio;

import com.nexusvoice.application.audio.service.AsrApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.audio.model.AudioTranscriptionResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ASR语音识别控制器
 * 提供语音转文本识别功能
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/asr")
@RequiredArgsConstructor
@Tag(name = "ASR语音识别", description = "语音转文本识别相关接口")
public class AsrController {
    
    private final AsrApplicationService asrApplicationService;
    
    /**
     * 语音转文本
     */
    @PostMapping("/transcribe")
    @Operation(summary = "语音转文本", description = "上传音频文件进行语音识别，支持mp3、wav、m4a、flac、opus格式")
    public Result<AudioTranscriptionResult> transcribe(
            @Parameter(description = "模型键（格式：provider:model，如 siliconflow:telespeech-asr）", required = true)
            @RequestParam String modelKey,
            
            @Parameter(description = "音频文件，最大100MB", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到语音识别请求，模型：{}，文件名：{}，文件大小：{}MB", 
                modelKey, file.getOriginalFilename(), file.getSize() / 1024.0 / 1024.0);
        
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        AudioTranscriptionResult result = asrApplicationService.transcribe(modelKey, file, userId);
        
        return Result.success(result);
    }
    
    /**
     * 获取所有可用的ASR模型
     */
    @GetMapping("/models")
    @Operation(summary = "获取可用ASR模型列表", description = "返回所有启用的ASR语音识别模型")
    public Result<List<String>> getSupportedModels() {
        List<String> models = asrApplicationService.getSupportedModels();
        return Result.success(models);
    }
    
    /**
     * 检查模型健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "检查ASR服务健康状态", description = "检查指定ASR模型是否可用")
    public Result<Boolean> checkHealth(
            @Parameter(description = "模型键", required = true)
            @RequestParam String modelKey) {
        
        boolean available = asrApplicationService.isModelAvailable(modelKey);
        return Result.success(available);
    }
}
