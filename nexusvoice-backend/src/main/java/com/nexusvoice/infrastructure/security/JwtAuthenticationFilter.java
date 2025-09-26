package com.nexusvoice.infrastructure.security;

import com.nexusvoice.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        
        String requestURI = request.getRequestURI();
        log.debug("JWT过滤器处理请求: {} {}", request.getMethod(), requestURI);
        
        try {
            String token = extractTokenFromRequest(request);
            log.debug("提取到的令牌: {}", token != null ? "存在(长度:" + token.length() + ")" : "不存在");
            
            if (StringUtils.hasText(token)) {
                log.debug("检测到JWT令牌，开始验证...");
                
                // 只有当token存在时才进行验证
                boolean isValid = jwtUtils.validateToken(token);
                log.debug("JWT令牌验证结果: {}", isValid);
                
                if (isValid) {
                    log.debug("JWT令牌验证通过");
                    
                    // 验证是否为访问令牌
                    boolean isAccessToken = jwtUtils.isAccessToken(token);
                    log.debug("是否为访问令牌: {}", isAccessToken);
                    
                    if (!isAccessToken) {
                        log.warn("令牌类型错误，期望访问令牌，请求路径: {}", requestURI);
                        // 不要return，继续处理请求，让Spring Security决定是否需要认证
                    } else {
                        // 从令牌中提取用户信息
                        Long userId = jwtUtils.getUserIdFromToken(token);
                        String username = jwtUtils.getUsernameFromToken(token);
                        String rolesStr = jwtUtils.getRolesFromToken(token);
                        
                        log.debug("提取用户信息 - ID: {}, 用户名: {}, 角色: {}", userId, username, rolesStr);
                        
                        // 解析角色
                        List<SimpleGrantedAuthority> authorities = parseRoles(rolesStr);
                        log.debug("解析的权限: {}", authorities);
                        
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
                        
                        // 验证设置是否成功
                        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                        log.debug("JWT认证成功，用户: {}, ID: {}, 权限: {}", username, userId, authorities);
                        log.debug("SecurityContext中的认证信息: {}", currentAuth != null ? currentAuth.getName() : "null");
                    }
                } else {
                    log.warn("JWT令牌验证失败，请求路径: {}", requestURI);
                    // 令牌无效，但不清除SecurityContext，让Spring Security处理
                }
            } else {
                log.debug("未检测到JWT令牌，请求路径: {}", requestURI);
            }
            // 如果没有token，也不做任何处理，让Spring Security决定是否需要认证
        } catch (Exception e) {
            log.error("JWT认证过程中发生错误，请求路径: {}, 错误: {}", requestURI, e.getMessage(), e);
            // 发生异常时，不清除SecurityContext，让Spring Security处理
            // SecurityContextHolder.clearContext(); // 注释掉这行
        }
        
        log.debug("JWT过滤器处理完成，继续过滤器链");
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中提取JWT令牌
     * 支持以下格式：
     * 1. Authorization: Bearer <token>
     * 2. Authorization: <token> (自动添加Bearer前缀)
     * 3. ?token=<token> (查询参数)
     * 
     * @param request HTTP请求
     * @return JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 从Authorization头中提取
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        log.debug("Authorization头内容: {}", authHeader != null ? authHeader : "不存在");
        
        if (StringUtils.hasText(authHeader)) {
            String token;
            
            // 检查是否已经包含Bearer前缀
            if (authHeader.startsWith(BEARER_PREFIX)) {
                token = authHeader.substring(BEARER_PREFIX.length());
                log.debug("从Authorization头提取到Bearer令牌，长度: {}", token.length());
            } else {
                // 没有Bearer前缀，直接使用整个header值作为token
                token = authHeader.trim();
                log.debug("从Authorization头提取到无前缀令牌，长度: {}", token.length());
            }
            
            // 验证token格式（JWT通常以ey开头）
            if (token.length() > 0 && (token.startsWith("ey") || token.contains("."))) {
                return token;
            } else {
                log.warn("Authorization头中的令牌格式不正确: {}", authHeader);
            }
        }
        
        // 从查询参数中提取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.debug("从查询参数提取到令牌，长度: {}", tokenParam.length());
            return tokenParam;
        }
        
        log.debug("未找到JWT令牌");
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
