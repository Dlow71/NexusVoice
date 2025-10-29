package com.nexusvoice.infrastructure.security;

import com.nexusvoice.application.user.service.OAuthUserService;
import com.nexusvoice.domain.user.constant.OAuthProvider;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.JwtUtils;
import com.nexusvoice.application.auth.service.TokenManagementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2登录成功处理器
 * 负责处理OAuth2认证成功后的逻辑
 * 
 * @author NexusVoice
 * @since 2025-01-20
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private OAuthUserService oAuthUserService;
    
    @Autowired
    private TokenManagementService tokenManagementService;
    
    @Value("${oauth2.success.redirect.url:http://localhost:5173/oauth/callback}")
    private String frontendRedirectUrl;
    
    @Value("${oauth2.failure.redirect.url:http://localhost:5173/login}")
    private String frontendFailureUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        
        log.info("OAuth2认证成功，开始处理用户信息");
        
        try {
            // 1. 获取OAuth2认证信息
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            String providerRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            
            log.info("OAuth2提供商：{}，用户属性：{}", providerRegistrationId, oAuth2User.getAttributes());
            
            // 2. 转换提供商标识为枚举
            OAuthProvider provider = OAuthProvider.fromCode(providerRegistrationId);
            if (provider == null) {
                log.error("不支持的OAuth提供商：{}", providerRegistrationId);
                redirectWithError(response, "unsupported_provider", 
                        "不支持的登录方式：" + providerRegistrationId);
                return;
            }
            
            // 3. 转换OAuth2User为Map（方便处理）
            Map<String, Object> oauthUserInfo = new HashMap<>(oAuth2User.getAttributes());
            
            // 4. 查找或创建系统用户
            User user;
            try {
                user = oAuthUserService.findOrCreateOAuthUser(provider, oauthUserInfo);
            } catch (BizException e) {
                log.error("处理OAuth用户失败：{}", e.getMessage());
                redirectWithError(response, e.getCode().toString(), e.getMessage());
                return;
            }
            
            // 5. 检查用户状态
            if (!user.isAccountNormal()) {
                if (user.isBanned()) {
                    log.warn("OAuth用户{}已被封禁", user.getId());
                    redirectWithError(response, ErrorCodeEnum.USER_BANNED.toString(),
                            "账户已被封禁，请联系管理员");
                } else {
                    log.warn("OAuth用户{}状态异常：{}", user.getId(), user.getStatus());
                    redirectWithError(response, ErrorCodeEnum.USER_STATUS_ABNORMAL.toString(),
                            "账户状态异常，无法登录");
                }
                return;
            }
            
            // 6. 生成JWT令牌
            String accessToken = jwtUtils.generateAccessToken(
                    user.getId(), 
                    user.getEmail(), 
                    user.getUserType()
            );
            String refreshToken = jwtUtils.generateRefreshToken(
                    user.getId(), 
                    user.getEmail()
            );
            
            // 7. 获取令牌过期时间
            Date expirationDate = jwtUtils.getExpirationFromToken(accessToken);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            
            log.info("OAuth用户{}登录成功，生成JWT令牌", user.getId());
            
            // 8. 创建会话（区分客户端类型），以便后续JWT校验通过
            tokenManagementService.createSession(user.getId(), accessToken, refreshToken, request);

            // 9. 构建重定向URL，携带令牌和用户信息
            String redirectUrl = buildSuccessRedirectUrl(
                    accessToken, 
                    refreshToken, 
                    expiresAt,
                    user
            );
            
            // 10. 重定向到前端
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            log.error("OAuth2登录处理异常：{}", e.getMessage(), e);
            redirectWithError(response, ErrorCodeEnum.OAUTH_CALLBACK_FAILED.toString(),
                    "登录处理失败，请重试");
        }
    }
    
    /**
     * 构建成功重定向URL
     */
    private String buildSuccessRedirectUrl(String accessToken, 
                                          String refreshToken, 
                                          LocalDateTime expiresAt,
                                          User user) {
        StringBuilder urlBuilder = new StringBuilder(frontendRedirectUrl);
        urlBuilder.append("?access_token=").append(URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        urlBuilder.append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
        urlBuilder.append("&expires_at=").append(URLEncoder.encode(expiresAt.toString(), StandardCharsets.UTF_8));
        
        // 添加用户基本信息（可选）
        urlBuilder.append("&user_id=").append(user.getId());
        urlBuilder.append("&email=").append(URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8));
        urlBuilder.append("&nickname=").append(URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8));
        if (user.getAvatarUrl() != null) {
            urlBuilder.append("&avatar_url=").append(URLEncoder.encode(user.getAvatarUrl(), StandardCharsets.UTF_8));
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * 重定向并携带错误信息
     */
    private void redirectWithError(HttpServletResponse response, 
                                  String errorCode, 
                                  String errorMessage) throws IOException {
        String redirectUrl = String.format("%s?error=%s&message=%s",
                frontendFailureUrl,
                URLEncoder.encode(errorCode, StandardCharsets.UTF_8),
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
        );
        response.sendRedirect(redirectUrl);
    }
}
