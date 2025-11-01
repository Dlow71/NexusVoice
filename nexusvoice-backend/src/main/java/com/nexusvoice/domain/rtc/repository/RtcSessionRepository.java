package com.nexusvoice.domain.rtc.repository;

import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import com.nexusvoice.domain.rtc.model.RtcSession;

import java.util.List;
import java.util.Optional;

/**
 * RTC会话仓储接口
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
public interface RtcSessionRepository {
    
    /**
     * 保存会话
     */
    RtcSession save(RtcSession session);
    
    /**
     * 根据ID查找
     */
    Optional<RtcSession> findById(Long id);
    
    /**
     * 根据会话ID查找
     */
    Optional<RtcSession> findBySessionId(String sessionId);
    
    /**
     * 查找用户的活跃会话
     */
    List<RtcSession> findActiveSessionsByUserId(Long userId);
    
    /**
     * 统计用户活跃会话数
     */
    int countActiveSessionsByUserId(Long userId);
    
    /**
     * 根据状态查找会话
     */
    List<RtcSession> findByState(RtcSessionState state);
    
    /**
     * 删除会话（软删除）
     */
    void deleteById(Long id);
}

