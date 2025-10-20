package com.nexusvoice.infrastructure.security;

import com.nexusvoice.enums.ErrorCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2登录失败处理器
 * 负责处理OAuth2认证失败后的逻辑
 * 
 * @author NexusVoice
 * @since 2025-01-20
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
    
    @Value("${oauth2.failure.redirect.url:http://localhost:5173/login}")
    private String frontendFailureUrl;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2认证失败：{}", exception.getMessage(), exception);
        
        String errorCode = ErrorCodeEnum.OAUTH_CALLBACK_FAILED.toString();
        String errorMessage = "OAuth登录失败";
        
        // 分析具体错误原因
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String error = oauth2Exception.getError().getErrorCode();
            
            switch (error) {
                case "access_denied":
                    errorCode = ErrorCodeEnum.OAUTH_ACCESS_DENIED.toString();
                    errorMessage = "您拒绝了授权请求";
                    break;
                case "invalid_request":
                    errorMessage = "无效的授权请求";
                    break;
                case "unauthorized_client":
                    errorMessage = "未授权的客户端";
                    break;
                case "unsupported_response_type":
                    errorMessage = "不支持的响应类型";
                    break;
                case "invalid_scope":
                    errorMessage = "无效的权限范围";
                    break;
                case "server_error":
                    errorMessage = "OAuth服务器错误";
                    break;
                case "temporarily_unavailable":
                    errorMessage = "OAuth服务暂时不可用";
                    break;
                default:
                    errorMessage = "OAuth认证失败：" + error;
            }
            
            log.error("OAuth2错误代码：{}，错误描述：{}", error, oauth2Exception.getError().getDescription());
        }
        
        // 构建重定向URL
        String redirectUrl = String.format("%s?error=%s&message=%s",
                frontendFailureUrl,
                URLEncoder.encode(errorCode, StandardCharsets.UTF_8),
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
        );
        
        log.info("重定向到前端错误页面：{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
