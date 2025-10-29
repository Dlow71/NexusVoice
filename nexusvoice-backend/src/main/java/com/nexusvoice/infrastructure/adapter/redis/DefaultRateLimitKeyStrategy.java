package com.nexusvoice.infrastructure.adapter.redis;

import com.nexusvoice.domain.developer.port.RateLimitKeyStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 默认限流键策略：
 * - 可在后续按需开启 route 维度（当前保持关闭，避免破坏现有统计）
 */
@Component
public class DefaultRateLimitKeyStrategy implements RateLimitKeyStrategy {

    private static final String PREFIX = "developer:api:";
    // 预留开关：是否拼接路由维度（如 :route:chat 或 :route:image）
    private static final boolean INCLUDE_ROUTE_DIMENSION = false;

    @Override
    public String minuteKey(Long keyId) {
        return PREFIX + "rate_limit:minute:" + keyId + routeSuffix();
    }

    @Override
    public String dayKey(Long keyId) {
        return PREFIX + "rate_limit:day:" + keyId + routeSuffix();
    }

    @Override
    public String dailyCostKey(Long keyId) {
        return PREFIX + "daily_cost:" + keyId + routeSuffix();
    }

    private String routeSuffix() {
        if (!INCLUDE_ROUTE_DIMENSION) return "";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        HttpServletRequest req = attrs.getRequest();
        if (req == null) return "";
        String uri = req.getRequestURI();
        if (uri == null || uri.isEmpty()) return "";
        // 取 /api/v1/{first-segment} 作为维度（示例）
        String[] parts = uri.split("/");
        if (parts.length >= 4 && "api".equals(parts[1])) {
            String first = parts[3];
            if (first != null && !first.isEmpty()) {
                return ":route:" + first;
            }
        }
        return "";
    }
}

