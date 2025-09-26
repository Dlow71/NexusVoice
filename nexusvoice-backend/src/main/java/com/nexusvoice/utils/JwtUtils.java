package com.nexusvoice.utils;

import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Component
public class JwtUtils {
    
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    
    /**
     * JWT密钥
     */
    @Value("${jwt.secret:nexusvoice-jwt-secret-key-for-authentication-and-authorization-system}")
    private String secret;
    
    /**
     * JWT过期时间（毫秒）
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    
    /**
     * 刷新令牌过期时间（毫秒）
     */
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;
    
    /**
     * JWT签发者
     */
    @Value("${jwt.issuer:nexusvoice}")
    private String issuer;
    
    /**
     * 用户ID声明键
     */
    private static final String CLAIM_USER_ID = "userId";
    
    /**
     * 用户名声明键
     */
    private static final String CLAIM_USERNAME = "username";
    
    /**
     * 角色声明键
     */
    private static final String CLAIM_ROLES = "roles";
    
    /**
     * 令牌类型声明键
     */
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    
    /**
     * 访问令牌类型
     */
    private static final String TOKEN_TYPE_ACCESS = "access";
    
    /**
     * 刷新令牌类型
     */
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    
    /**
     * 获取密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成访问令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param userType 用户类型
     * @return JWT令牌
     */
    public String generateAccessToken(Long userId, String username, UserType userType) {
        String roles = "ROLE_" + userType.getCode();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        
        return createToken(claims, username, expiration);
    }
    
    /**
     * 生成刷新令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        
        return createToken(claims, username, refreshExpiration);
    }
    
    /**
     * 创建令牌
     * 
     * @param claims 声明
     * @param subject 主题
     * @param expiration 过期时间
     * @return JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }
    
    /**
     * 解析令牌
     * 
     * @param token JWT令牌
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            throw BizException.of(ErrorCodeEnum.TOKEN_EXPIRED, "令牌已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT令牌: {}", e.getMessage());
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "不支持的令牌格式");
        } catch (MalformedJwtException e) {
            log.warn("JWT令牌格式错误: {}", e.getMessage());
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌格式错误");
        } catch (SecurityException e) {
            log.warn("JWT令牌签名验证失败: {}", e.getMessage());
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌签名验证失败");
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌参数非法: {}", e.getMessage());
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌参数非法");
        } catch (Exception e) {
            log.error("解析JWT令牌时发生未知错误: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌解析失败");
        }
    }
    
    /**
     * 验证令牌
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (BizException e) {
            return false;
        }
    }
    
    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userIdObj = claims.get(CLAIM_USER_ID);
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof String) {
            // 兼容旧版本的String类型ID
            return Long.valueOf((String) userIdObj);
        } else if (userIdObj instanceof Integer) {
            // 兼容Integer类型
            return ((Integer) userIdObj).longValue();
        }
        throw new IllegalArgumentException("无效的用户ID类型: " + userIdObj.getClass());
    }
    
    /**
     * 从令牌中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从令牌中获取角色
     * 
     * @param token JWT令牌
     * @return 角色字符串
     */
    public String getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get(CLAIM_ROLES);
    }
    
    /**
     * 检查令牌类型
     * 
     * @param token JWT令牌
     * @param expectedType 期望的令牌类型
     * @return 是否匹配
     */
    public boolean isTokenType(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            String tokenType = (String) claims.get(CLAIM_TOKEN_TYPE);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查是否为访问令牌
     * 
     * @param token JWT令牌
     * @return 是否为访问令牌
     */
    public boolean isAccessToken(String token) {
        return isTokenType(token, TOKEN_TYPE_ACCESS);
    }
    
    /**
     * 检查是否为刷新令牌
     * 
     * @param token JWT令牌
     * @return 是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        return isTokenType(token, TOKEN_TYPE_REFRESH);
    }
    
    /**
     * 获取令牌过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }
    
    /**
     * 检查令牌是否即将过期（30分钟内）
     * 
     * @param token JWT令牌
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            Date now = new Date();
            long timeUntilExpiration = expiration.getTime() - now.getTime();
            // 30分钟 = 30 * 60 * 1000 毫秒
            return timeUntilExpiration < 1800000;
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 从令牌中获取用户类型
     * 
     * @param token JWT令牌
     * @return 用户类型
     */
    public UserType getUserTypeFromToken(String token) {
        String roles = getRolesFromToken(token);
        if (roles != null && roles.startsWith("ROLE_")) {
            String userTypeCode = roles.substring(5); // 移除 "ROLE_" 前缀
            return UserType.fromCode(userTypeCode);
        }
        return UserType.USER; // 默认为普通用户
    }
    
    /**
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    public String refreshAccessToken(String refreshToken) {
        if (!isRefreshToken(refreshToken)) {
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "无效的刷新令牌");
        }
        
        Claims claims = parseToken(refreshToken);
        Long userId = getUserIdFromToken(refreshToken);
        String username = claims.getSubject();
        UserType userType = getUserTypeFromToken(refreshToken);
        
        return generateAccessToken(userId, username, userType);
    }
}
