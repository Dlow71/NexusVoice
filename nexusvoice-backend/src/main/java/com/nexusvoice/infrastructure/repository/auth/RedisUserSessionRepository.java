package com.nexusvoice.infrastructure.repository.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusvoice.domain.auth.model.UserSession;
import com.nexusvoice.domain.auth.repository.UserSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话Redis仓储实现
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
@Slf4j
@Repository
public class RedisUserSessionRepository implements UserSessionRepository {
    
    private static final String SESSION_PREFIX = "session:access:";
    private static final String REFRESH_SESSION_PREFIX = "session:refresh:";
    private static final String USER_SESSIONS_PREFIX = "session:user:";
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisUserSessionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void save(UserSession session) {
        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            
            // 计算过期时间（秒）
            long ttl = Duration.between(LocalDateTime.now(), session.getExpiresAt()).getSeconds();
            if (ttl <= 0) {
                log.warn("会话已过期，不保存: userId={}", session.getUserId());
                return;
            }
            
            // 保存访问令牌 -> 会话映射
            String accessKey = SESSION_PREFIX + session.getAccessToken();
            redisTemplate.opsForValue().set(accessKey, sessionJson, ttl, TimeUnit.SECONDS);
            
            // 保存刷新令牌 -> 会话映射
            String refreshKey = REFRESH_SESSION_PREFIX + session.getRefreshToken();
            redisTemplate.opsForValue().set(refreshKey, sessionJson, ttl, TimeUnit.SECONDS);
            
            // 保存用户 -> 会话列表映射
            String userKey = USER_SESSIONS_PREFIX + session.getUserId();
            redisTemplate.opsForSet().add(userKey, session.getAccessToken());
            redisTemplate.expire(userKey, ttl, TimeUnit.SECONDS);
            
            log.debug("保存用户会话成功: userId={}, accessToken={}", 
                    session.getUserId(), maskToken(session.getAccessToken()));
            
        } catch (JsonProcessingException e) {
            log.error("序列化会话失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存会话失败", e);
        }
    }
    
    @Override
    public Optional<UserSession> findByAccessToken(String accessToken) {
        try {
            String key = SESSION_PREFIX + accessToken;
            String sessionJson = redisTemplate.opsForValue().get(key);
            
            if (sessionJson == null) {
                return Optional.empty();
            }
            
            UserSession session = objectMapper.readValue(sessionJson, UserSession.class);
            return Optional.of(session);
            
        } catch (JsonProcessingException e) {
            log.error("反序列化会话失败: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        try {
            String key = REFRESH_SESSION_PREFIX + refreshToken;
            String sessionJson = redisTemplate.opsForValue().get(key);
            
            if (sessionJson == null) {
                return Optional.empty();
            }
            
            UserSession session = objectMapper.readValue(sessionJson, UserSession.class);
            return Optional.of(session);
            
        } catch (JsonProcessingException e) {
            log.error("反序列化会话失败: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<UserSession> findByUserId(Long userId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        Set<String> accessTokens = redisTemplate.opsForSet().members(userKey);
        
        if (accessTokens == null || accessTokens.isEmpty()) {
            return List.of();
        }
        
        List<UserSession> sessions = new ArrayList<>();
        for (String accessToken : accessTokens) {
            findByAccessToken(accessToken).ifPresent(sessions::add);
        }
        
        return sessions;
    }
    
    @Override
    public void deleteByAccessToken(String accessToken) {
        Optional<UserSession> sessionOpt = findByAccessToken(accessToken);
        
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            
            // 删除访问令牌映射
            String accessKey = SESSION_PREFIX + accessToken;
            redisTemplate.delete(accessKey);
            
            // 删除刷新令牌映射
            String refreshKey = REFRESH_SESSION_PREFIX + session.getRefreshToken();
            redisTemplate.delete(refreshKey);
            
            // 从用户会话集合中移除
            String userKey = USER_SESSIONS_PREFIX + session.getUserId();
            redisTemplate.opsForSet().remove(userKey, accessToken);
            
            log.debug("删除用户会话成功: userId={}, accessToken={}", 
                    session.getUserId(), maskToken(accessToken));
        }
    }
    
    @Override
    public void deleteByUserId(Long userId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        Set<String> accessTokens = redisTemplate.opsForSet().members(userKey);
        
        if (accessTokens != null && !accessTokens.isEmpty()) {
            for (String accessToken : accessTokens) {
                deleteByAccessToken(accessToken);
            }
        }
        
        // 删除用户会话集合
        redisTemplate.delete(userKey);
        
        log.info("删除用户所有会话成功: userId={}, count={}", userId, 
                accessTokens != null ? accessTokens.size() : 0);
    }
    
    @Override
    public void deleteOtherSessions(Long userId, String currentAccessToken) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        Set<String> accessTokens = redisTemplate.opsForSet().members(userKey);
        
        if (accessTokens != null && !accessTokens.isEmpty()) {
            int deletedCount = 0;
            for (String accessToken : accessTokens) {
                if (!accessToken.equals(currentAccessToken)) {
                    deleteByAccessToken(accessToken);
                    deletedCount++;
                }
            }
            log.info("删除用户其他会话成功: userId={}, count={}", userId, deletedCount);
        }
    }
    
    @Override
    public void updateLastActive(String accessToken) {
        Optional<UserSession> sessionOpt = findByAccessToken(accessToken);
        
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.updateLastActive();
            save(session); // 重新保存
            
            log.debug("更新会话活跃时间: userId={}, accessToken={}", 
                    session.getUserId(), maskToken(accessToken));
        }
    }
    
    @Override
    public long countByUserId(Long userId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        Long count = redisTemplate.opsForSet().size(userKey);
        return count != null ? count : 0L;
    }
    
    @Override
    public long cleanExpiredSessions() {
        // Redis的TTL机制会自动清理过期数据
        // 这里只需要清理用户会话集合中的过期引用
        long cleanedCount = 0;
        
        Set<String> userKeys = redisTemplate.keys(USER_SESSIONS_PREFIX + "*");
        if (userKeys != null) {
            for (String userKey : userKeys) {
                Set<String> accessTokens = redisTemplate.opsForSet().members(userKey);
                if (accessTokens != null) {
                    for (String accessToken : accessTokens) {
                        String sessionKey = SESSION_PREFIX + accessToken;
                        if (!redisTemplate.hasKey(sessionKey)) {
                            // 会话已过期，从集合中移除
                            redisTemplate.opsForSet().remove(userKey, accessToken);
                            cleanedCount++;
                        }
                    }
                }
            }
        }
        
        log.info("清理过期会话完成: count={}", cleanedCount);
        return cleanedCount;
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
