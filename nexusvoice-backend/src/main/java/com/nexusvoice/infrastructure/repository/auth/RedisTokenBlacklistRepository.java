package com.nexusvoice.infrastructure.repository.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusvoice.domain.auth.model.TokenBlacklist;
import com.nexusvoice.domain.auth.repository.TokenBlacklistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单Redis仓储实现
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
@Slf4j
@Repository
public class RedisTokenBlacklistRepository implements TokenBlacklistRepository {
    
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String USER_BLACKLIST_PREFIX = "blacklist:user:";
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisTokenBlacklistRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void add(TokenBlacklist tokenBlacklist) {
        try {
            String token = tokenBlacklist.getToken();
            String tokenJson = objectMapper.writeValueAsString(tokenBlacklist);
            
            // 计算过期时间（秒）
            long ttl = Duration.between(LocalDateTime.now(), tokenBlacklist.getExpiresAt()).getSeconds();
            if (ttl <= 0) {
                log.warn("Token已过期，不添加到黑名单: token={}", maskToken(token));
                return;
            }
            
            // 保存到黑名单
            String blacklistKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, tokenJson, ttl, TimeUnit.SECONDS);
            
            // 添加到用户黑名单集合
            if (tokenBlacklist.getUserId() != null) {
                String userKey = USER_BLACKLIST_PREFIX + tokenBlacklist.getUserId();
                redisTemplate.opsForSet().add(userKey, token);
                redisTemplate.expire(userKey, ttl, TimeUnit.SECONDS);
            }
            
            log.info("添加Token到黑名单成功: token={}, userId={}, reason={}", 
                    maskToken(token), tokenBlacklist.getUserId(), tokenBlacklist.getReason());
            
        } catch (JsonProcessingException e) {
            log.error("序列化Token黑名单失败: {}", e.getMessage(), e);
            throw new RuntimeException("添加Token到黑名单失败", e);
        }
    }
    
    @Override
    public void addBatch(List<String> tokens, Long userId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (String token : tokens) {
            TokenBlacklist blacklist = new TokenBlacklist();
            blacklist.setToken(token);
            blacklist.setUserId(userId);
            blacklist.setTokenType("access");
            blacklist.setReason(reason);
            blacklist.setAddedAt(now);
            // 设置24小时后过期（根据JWT配置调整）
            blacklist.setExpiresAt(now.plusHours(24));
            
            add(blacklist);
        }
        
        log.info("批量添加Token到黑名单成功: userId={}, count={}", userId, tokens.size());
    }
    
    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        
        boolean blacklisted = Boolean.TRUE.equals(exists);
        if (blacklisted) {
            log.debug("Token在黑名单中: token={}", maskToken(token));
        }
        
        return blacklisted;
    }
    
    @Override
    public void remove(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        
        log.debug("从黑名单移除Token: token={}", maskToken(token));
    }
    
    @Override
    public List<TokenBlacklist> findByUserId(Long userId) {
        String userKey = USER_BLACKLIST_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        
        List<TokenBlacklist> blacklists = new ArrayList<>();
        for (String token : tokens) {
            String key = BLACKLIST_PREFIX + token;
            String blacklistJson = redisTemplate.opsForValue().get(key);
            
            if (blacklistJson != null) {
                try {
                    TokenBlacklist blacklist = objectMapper.readValue(blacklistJson, TokenBlacklist.class);
                    blacklists.add(blacklist);
                } catch (JsonProcessingException e) {
                    log.error("反序列化Token黑名单失败: {}", e.getMessage(), e);
                }
            }
        }
        
        return blacklists;
    }
    
    @Override
    public long cleanExpired() {
        // Redis的TTL机制会自动清理过期数据
        // 这里只需要清理用户黑名单集合中的过期引用
        long cleanedCount = 0;
        
        Set<String> userKeys = redisTemplate.keys(USER_BLACKLIST_PREFIX + "*");
        if (userKeys != null) {
            for (String userKey : userKeys) {
                Set<String> tokens = redisTemplate.opsForSet().members(userKey);
                if (tokens != null) {
                    for (String token : tokens) {
                        String blacklistKey = BLACKLIST_PREFIX + token;
                        if (!redisTemplate.hasKey(blacklistKey)) {
                            // Token已过期，从集合中移除
                            redisTemplate.opsForSet().remove(userKey, token);
                            cleanedCount++;
                        }
                    }
                }
            }
        }
        
        log.info("清理过期黑名单记录完成: count={}", cleanedCount);
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
