package com.nexusvoice.security;

import com.nexusvoice.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                // 验证是否为访问令牌
                if (!jwtUtils.isAccessToken(token)) {
                    log.warn("令牌类型错误，期望访问令牌");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 从令牌中提取用户信息
                Long userId = jwtUtils.getUserIdFromToken(token);
                String username = jwtUtils.getUsernameFromToken(token);
                String rolesStr = jwtUtils.getRolesFromToken(token);
                
                // 解析角色
                List<SimpleGrantedAuthority> authorities = parseRoles(rolesStr);
                
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, username), 
                        null, 
                        authorities
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT认证成功，用户: {}, ID: {}", username, userId);
            }
        } catch (Exception e) {
            log.error("JWT认证过程中发生错误: {}", e.getMessage(), e);
            // 清除安全上下文
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中提取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 从Authorization头中提取
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        // 从查询参数中提取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 解析角色字符串为权限列表
     * 
     * @param rolesStr 角色字符串，格式：ROLE_USER,ROLE_ADMIN
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> parseRoles(String rolesStr) {
        if (!StringUtils.hasText(rolesStr)) {
            return List.of();
        }
        
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 用户主体类
     */
    public static class UserPrincipal {
        private final Long userId;
        private final String username;
        
        public UserPrincipal(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        @Override
        public String toString() {
            return username;
        }
    }
}
