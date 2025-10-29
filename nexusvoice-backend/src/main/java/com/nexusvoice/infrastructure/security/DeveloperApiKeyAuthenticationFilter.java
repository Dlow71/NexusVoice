package com.nexusvoice.infrastructure.security;

import com.nexusvoice.application.developer.service.ApiKeyAuthenticationService;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.infrastructure.context.DeveloperApiContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 统一 API Key 认证过滤器
 * - 读取请求头 X-API-Key
 * - 认证通过后构建 SecurityContext（ROLE_DEVELOPER_API）
 * - 设置 DeveloperApiContext，便于调用链统一记录 API_KEY 日志
 * - 如已有认证（如JWT），不覆盖，仅附加上下文
 */
@Component
public class DeveloperApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyAuthenticationService authenticationService;

    public DeveloperApiKeyAuthenticationFilter(ApiKeyAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);
        boolean contextSet = false;

        try {
            if (StringUtils.hasText(apiKey)) {
                String clientIp = getClientIp(request);
                // 认证与校验（状态/IP）
                DeveloperApiKey key = authenticationService.authenticateAndValidate(apiKey, clientIp);

                // 设置上下文（用于日志与限流维度）
                DeveloperApiContext.setDeveloperApiKeyId(key.getId());
                DeveloperApiContext.setAuthType("API_KEY");
                contextSet = true;

                // 如果当前没有认证信息，则注入一个开发者API认证
                Authentication current = SecurityContextHolder.getContext().getAuthentication();
                if (current == null || !current.isAuthenticated()) {
                    DeveloperApiPrincipal principal = new DeveloperApiPrincipal(key.getId(), key.getUserId(), key.getKeyName());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVELOPER_API"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (contextSet) {
                DeveloperApiContext.clear();
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 开发者API认证主体
     */
    public static class DeveloperApiPrincipal {
        private final Long apiKeyId;
        private final Long ownerUserId;
        private final String keyName;

        public DeveloperApiPrincipal(Long apiKeyId, Long ownerUserId, String keyName) {
            this.apiKeyId = apiKeyId;
            this.ownerUserId = ownerUserId;
            this.keyName = keyName;
        }

        public Long getApiKeyId() { return apiKeyId; }
        public Long getOwnerUserId() { return ownerUserId; }
        public String getKeyName() { return keyName; }
    }
}

