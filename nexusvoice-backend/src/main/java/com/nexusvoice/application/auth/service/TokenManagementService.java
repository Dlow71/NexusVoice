package com.nexusvoice.application.auth.service;

import com.nexusvoice.domain.auth.model.TokenBlacklist;
import com.nexusvoice.domain.auth.model.UserSession;
import com.nexusvoice.domain.auth.repository.TokenBlacklistRepository;
import com.nexusvoice.domain.auth.repository.UserSessionRepository;
import com.nexusvoice.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Token管理应用服务
 * 负责Token的生命周期管理、黑名单和会话管理
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
@Slf4j
@Service
public class TokenManagementService {
    
    @Autowired
    private UserSessionRepository sessionRepository;
    
    @Autowired
    private TokenBlacklistRepository blacklistRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Value("${jwt.max-sessions-per-user:5}")
    private int maxSessionsPerUser;
    
    @Value("${jwt.single-device-mode:false}")
    private boolean singleDeviceMode;
    
    /**
     * 创建并保存用户会话
     *
     * @param userId 用户ID
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param request HTTP请求
     */
    public void createSession(Long userId, String accessToken, String refreshToken, HttpServletRequest request) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        
        // 提取设备信息
        if (request != null) {
            session.setDeviceId(extractDeviceId(request));
            session.setDeviceType(extractDeviceType(request));
            session.setIpAddress(extractIpAddress(request));
            session.setUserAgent(request.getHeader("User-Agent"));
            // 区分客户端（admin/user），便于并发会话按系统隔离
            session.setClientType(extractClientType(request));
        } else {
            // 默认按用户端处理
            session.setClientType("user");
        }
        
        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setLastActiveAt(now);
        
        // 从JWT中获取过期时间
        Date expirationDate = jwtUtils.getExpirationFromToken(accessToken);
        session.setExpiresAt(convertToLocalDateTime(expirationDate));
        
        // 检查会话数量限制（按客户端类型隔离：管理端与用户端互不影响）
        String clientType = normalizeClientType(session.getClientType());
        List<UserSession> allSessions = sessionRepository.findByUserId(userId);
        List<UserSession> sameClientSessions = allSessions.stream()
                .filter(s -> clientType.equals(normalizeClientType(s.getClientType())))
                .toList();

        if (singleDeviceMode) {
            // 单设备模式：仅清除同一客户端类型的其他会话
            sameClientSessions.forEach(s -> {
                sessionRepository.deleteByAccessToken(s.getAccessToken());
                log.info("单设备模式：清除同客户端旧会话 userId={}, clientType={}, accessToken={}",
                        userId, clientType, maskToken(s.getAccessToken()));
            });
        } else {
            // 多设备模式：同一客户端类型内检查最大会话数
            int sessionCount = sameClientSessions.size();
            if (sessionCount >= maxSessionsPerUser) {
                int toDelete = sessionCount - maxSessionsPerUser + 1;
                sameClientSessions.stream()
                        .sorted((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                        .limit(toDelete)
                        .forEach(s -> {
                            sessionRepository.deleteByAccessToken(s.getAccessToken());
                            log.info("超过同客户端最大会话数限制，删除旧会话 userId={}, clientType={}, accessToken={}",
                                    userId, clientType, maskToken(s.getAccessToken()));
                        });
            }
        }
        
        // 保存新会话
        sessionRepository.save(session);
        log.info("创建用户会话成功: userId={}, deviceType={}, ip={}", 
                userId, session.getDeviceType(), session.getIpAddress());
    }
    
    /**
     * 验证Token是否有效（检查黑名单和会话）
     *
     * @param token 访问令牌
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        // 检查是否在黑名单中
        if (blacklistRepository.isBlacklisted(token)) {
            log.debug("Token在黑名单中: token={}", maskToken(token));
            return false;
        }
        
        // 检查会话是否存在
        return sessionRepository.findByAccessToken(token).isPresent();
    }
    
    /**
     * 刷新Token时的处理
     *
     * @param oldAccessToken 旧的访问令牌
     * @param newAccessToken 新的访问令牌
     * @param newRefreshToken 新的刷新令牌（可选）
     * @param request HTTP请求
     */
    public void refreshSession(String oldAccessToken, String newAccessToken, 
                               String newRefreshToken, HttpServletRequest request) {
        // 查找旧会话
        sessionRepository.findByAccessToken(oldAccessToken).ifPresent(oldSession -> {
            // 将旧Token加入黑名单
            addToBlacklist(oldAccessToken, oldSession.getUserId(), "token_refresh");
            
            // 删除旧会话
            sessionRepository.deleteByAccessToken(oldAccessToken);
            
            // 创建新会话（保留设备信息）
            UserSession newSession = new UserSession();
            newSession.setUserId(oldSession.getUserId());
            newSession.setAccessToken(newAccessToken);
            newSession.setRefreshToken(newRefreshToken != null ? newRefreshToken : oldSession.getRefreshToken());
            newSession.setDeviceId(oldSession.getDeviceId());
            newSession.setDeviceType(oldSession.getDeviceType());
            newSession.setIpAddress(oldSession.getIpAddress());
            newSession.setUserAgent(oldSession.getUserAgent());
            newSession.setClientType(oldSession.getClientType());
            
            LocalDateTime now = LocalDateTime.now();
            newSession.setCreatedAt(now);
            newSession.setLastActiveAt(now);
            
            Date expirationDate = jwtUtils.getExpirationFromToken(newAccessToken);
            newSession.setExpiresAt(convertToLocalDateTime(expirationDate));
            
            sessionRepository.save(newSession);
            
            log.info("刷新会话成功: userId={}, oldToken={}, newToken={}", 
                    oldSession.getUserId(), maskToken(oldAccessToken), maskToken(newAccessToken));
        });
    }
    
    /**
     * 登出（单个会话）
     *
     * @param accessToken 访问令牌
     */
    public void logout(String accessToken) {
        sessionRepository.findByAccessToken(accessToken).ifPresent(session -> {
            // 将Token加入黑名单
            addToBlacklist(accessToken, session.getUserId(), "user_logout");
            
            // 删除会话
            sessionRepository.deleteByAccessToken(accessToken);
            
            log.info("用户登出成功: userId={}, accessToken={}", 
                    session.getUserId(), maskToken(accessToken));
        });
    }
    
    /**
     * 登出所有会话
     *
     * @param userId 用户ID
     */
    public void logoutAll(Long userId) {
        List<UserSession> sessions = sessionRepository.findByUserId(userId);
        
        // 将所有Token加入黑名单
        List<String> tokens = sessions.stream()
                .map(UserSession::getAccessToken)
                .toList();
        
        if (!tokens.isEmpty()) {
            blacklistRepository.addBatch(tokens, userId, "user_logout_all");
        }
        
        // 删除所有会话
        sessionRepository.deleteByUserId(userId);
        
        log.info("用户登出所有会话成功: userId={}, count={}", userId, sessions.size());
    }
    
    /**
     * 踢出用户（强制登出）
     *
     * @param userId 用户ID
     * @param reason 原因
     */
    public void kickoutUser(Long userId, String reason) {
        List<UserSession> sessions = sessionRepository.findByUserId(userId);
        
        // 将所有Token加入黑名单
        List<String> tokens = sessions.stream()
                .map(UserSession::getAccessToken)
                .toList();
        
        if (!tokens.isEmpty()) {
            blacklistRepository.addBatch(tokens, userId, reason);
        }
        
        // 删除所有会话
        sessionRepository.deleteByUserId(userId);
        
        log.warn("强制踢出用户: userId={}, reason={}, sessionCount={}", 
                userId, reason, sessions.size());
    }
    
    /**
     * 获取用户的活跃会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }
    
    /**
     * 更新会话的最后活跃时间
     *
     * @param accessToken 访问令牌
     */
    public void updateLastActive(String accessToken) {
        sessionRepository.updateLastActive(accessToken);
    }
    
    /**
     * 添加Token到黑名单
     *
     * @param token Token
     * @param userId 用户ID
     * @param reason 原因
     */
    private void addToBlacklist(String token, Long userId, String reason) {
        TokenBlacklist blacklist = new TokenBlacklist();
        blacklist.setToken(token);
        blacklist.setUserId(userId);
        blacklist.setTokenType("access");
        blacklist.setReason(reason);
        blacklist.setAddedAt(LocalDateTime.now());
        
        // 从JWT获取过期时间
        Date expirationDate = jwtUtils.getExpirationFromToken(token);
        blacklist.setExpiresAt(convertToLocalDateTime(expirationDate));
        
        blacklistRepository.add(blacklist);
    }
    
    /**
     * 提取设备ID
     */
    private String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        if (!StringUtils.hasText(deviceId)) {
            // 使用User-Agent和IP的哈希作为设备ID
            String userAgent = request.getHeader("User-Agent");
            String ip = extractIpAddress(request);
            deviceId = String.valueOf((userAgent + ip).hashCode());
        }
        return deviceId;
    }
    
    /**
     * 提取设备类型
     */
    private String extractDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "unknown";
        }
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "tablet";
        } else {
            return "desktop";
        }
    }

    /**
     * 提取客户端类型（admin 或 user）。优先从Header，其次从请求路径判断。
     */
    private String extractClientType(HttpServletRequest request) {
        // 1) 优先从自定义Header读取
        String fromHeader = request.getHeader("X-Client-Type");
        if (StringUtils.hasText(fromHeader)) {
            return normalizeClientType(fromHeader);
        }
        // 2) 其次从查询参数读取（用于OAuth回调等无法自定义Header的场景）
        String fromParam = request.getParameter("client");
        if (StringUtils.hasText(fromParam)) {
            return normalizeClientType(fromParam);
        }
        // 3) 最后根据路径前缀简单判断
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/admin/")) {
            return "admin";
        }
        return "user";
    }

    private String normalizeClientType(String type) {
        return (StringUtils.hasText(type) ? type.trim().toLowerCase() : "user");
    }
    
    /**
     * 提取IP地址
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况（取第一个）
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 转换Date到LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }
    
    /**
     * 脱敏Token用于日志输出
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
