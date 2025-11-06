package com.nexusvoice.interfaces.api.rtc;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.rtc.dto.InterruptRequestDto;
import com.nexusvoice.application.rtc.dto.RtcSessionCreateRequest;
import com.nexusvoice.application.rtc.dto.RtcSessionCreateResponse;
import com.nexusvoice.application.rtc.service.RtcSessionApplicationService;
import com.nexusvoice.domain.rtc.model.RtcSession;
import com.nexusvoice.domain.rtc.repository.RtcSessionRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.common.Result;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * RTC会话控制器
 * 
 * 接口路径：/api/v1/rtc
 * 功能：
 * - POST /session - 创建RTC会话
 * - GET /session/{sessionId} - 获取会话信息
 * - POST /session/{sessionId}/interrupt - 打断
 * - DELETE /session/{sessionId} - 结束会话
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rtc")
@RequiredArgsConstructor
@RequireAuth
@Tag(name = "RTC会话管理", description = "WebRTC实时语音对话会话管理接口")
@ConditionalOnProperty(name = "rtc.enabled", havingValue = "true", matchIfMissing = false)
public class RtcSessionController {
    
    private final RtcSessionApplicationService rtcSessionApplicationService;
    private final RtcSessionRepository rtcSessionRepository;
    
    /**
     * 创建RTC会话
     * 
     * @param request 创建请求
     * @return 创建响应
     */
    @Operation(summary = "创建RTC会话", description = "创建WebRTC实时语音对话会话，返回会话ID和信令WebSocket地址")
    @PostMapping("/session")
    public Result<RtcSessionCreateResponse> createSession(
            @Valid @RequestBody RtcSessionCreateRequest request) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "用户未登录"));
        log.info("创建RTC会话: userId={}, roleId={}", userId, request.getRoleId());
        
        RtcSessionCreateResponse response = rtcSessionApplicationService.createSession(request, userId);
        
        return Result.success(response);
    }
    
    /**
     * 获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @Operation(summary = "获取会话信息", description = "根据会话ID获取RTC会话详细信息")
    @GetMapping("/session/{sessionId}")
    public Result<RtcSession> getSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "用户未登录"));
        
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        // 验证权限
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }
        
        return Result.success(session);
    }
    
    /**
     * 打断当前播放
     * 
     * @param sessionId 会话ID
     * @param request 打断请求
     * @return 成功响应
     */
    @Operation(summary = "打断当前播放", description = "打断AI正在播放的语音（支持SOFT软中断和HARD硬中断）")
    @PostMapping("/session/{sessionId}/interrupt")
    public Result<Void> interrupt(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @Valid @RequestBody InterruptRequestDto request) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "用户未登录"));
        log.info("打断RTC会话: sessionId={}, mode={}, userId={}", 
                sessionId, request.getMode(), userId);
        
        rtcSessionApplicationService.interrupt(sessionId, request, userId);
        
        return Result.success();
    }
    
    /**
     * 结束会话
     * 
     * @param sessionId 会话ID
     * @return 成功响应
     */
    @Operation(summary = "结束会话", description = "结束RTC会话并释放所有资源")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> endSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "用户未登录"));
        log.info("结束RTC会话: sessionId={}, userId={}", sessionId, userId);
        
        rtcSessionApplicationService.endSession(sessionId, userId);
        
        return Result.success();
    }
    
    /**
     * 开始连接（创建Kurento媒体管线）
     * 
     * @param sessionId 会话ID
     * @return 成功响应
     */
    @Operation(summary = "开始连接", description = "开始WebRTC连接，创建Kurento媒体管线")
    @PostMapping("/session/{sessionId}/connect")
    public Result<Void> startConnection(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED, "用户未登录"));
        log.info("开始WebRTC连接: sessionId={}, userId={}", sessionId, userId);
        
        rtcSessionApplicationService.startConnection(sessionId, userId);
        
        return Result.success();
    }
}

