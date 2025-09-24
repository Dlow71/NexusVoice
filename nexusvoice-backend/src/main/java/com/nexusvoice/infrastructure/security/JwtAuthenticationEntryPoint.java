package com.nexusvoice.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT认证入口点
 * 处理未认证的请求
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    // 1. 声明一个 final 的 ObjectMapper 字段，但不要在这里初始化
    private final ObjectMapper objectMapper;

    // 2. 创建一个构造函数，并通过 @Autowired 注解（在只有一个构造函数时可选）
    //    让 Spring 将容器中已经配置好的 ObjectMapper 实例注入进来
    @Autowired
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("未认证访问: {} {}, 错误: {}", 
                request.getMethod(), 
                request.getRequestURI(), 
                authException.getMessage());
        
        // 设置响应头
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 构建响应结果
        Result<Void> result = Result.error(ErrorCodeEnum.UNAUTHORIZED, "访问此资源需要完整的身份验证");
        
        // 写入响应
        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
