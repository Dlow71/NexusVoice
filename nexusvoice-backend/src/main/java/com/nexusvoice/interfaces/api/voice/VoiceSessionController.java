package com.nexusvoice.interfaces.api.voice;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeConfigDto;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeUpdateRequest;
import com.nexusvoice.application.voice.dto.VoiceSessionStartRequest;
import com.nexusvoice.application.voice.dto.VoiceSessionStartResponse;
import com.nexusvoice.application.voice.service.VoiceSessionApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 语音会话接口。
 */
@RestController
@RequestMapping("/api/v1/voice/sessions")
@RequireAuth
@RequiredArgsConstructor
@Tag(name = "语音会话", description = "站内语音通话相关接口")
public class VoiceSessionController {

    private final VoiceSessionApplicationService voiceSessionApplicationService;

    @PostMapping
    @Operation(summary = "创建语音会话")
    public Result<VoiceSessionStartResponse> startSession(@Valid @RequestBody VoiceSessionStartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(voiceSessionApplicationService.startSession(request, userId));
    }

    @GetMapping("/{voiceSessionId}/runtime-config")
    @Operation(summary = "获取语音会话运行配置")
    public Result<VoiceSessionRuntimeConfigDto> getRuntimeConfig(
            @Parameter(description = "语音会话ID") @PathVariable String voiceSessionId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(voiceSessionApplicationService.getRuntimeConfig(voiceSessionId, userId));
    }

    @PutMapping("/{voiceSessionId}/runtime-config")
    @Operation(summary = "更新语音会话运行配置")
    public Result<VoiceSessionRuntimeConfigDto> updateRuntimeConfig(
            @Parameter(description = "语音会话ID") @PathVariable String voiceSessionId,
            @Valid @RequestBody VoiceSessionRuntimeUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(voiceSessionApplicationService.updateRuntimeConfig(voiceSessionId, request, userId));
    }

    @PostMapping("/{voiceSessionId}/interrupt")
    @Operation(summary = "打断语音会话")
    public Result<Void> interrupt(@PathVariable String voiceSessionId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        voiceSessionApplicationService.interrupt(voiceSessionId, userId);
        return Result.success();
    }

    @DeleteMapping("/{voiceSessionId}")
    @Operation(summary = "结束语音会话")
    public Result<Void> endSession(@PathVariable String voiceSessionId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        voiceSessionApplicationService.endSession(voiceSessionId, userId);
        return Result.success();
    }
}
