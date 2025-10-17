package com.nexusvoice.infrastructure.websocket;

import com.nexusvoice.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket JWT认证拦截器
 * 在WebSocket握手阶段验证JWT令牌并提取用户信息
 *
 * @author NexusVoice
 * @since 2025-09-28
 */
@Slf4j
@Component
public class WebSocketJwtInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    public WebSocketJwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler, 
                                 Map<String, Object> attributes) throws Exception {
        
        String token = extractToken(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket握手失败：未提供JWT令牌，URI：{}", request.getURI());
            return false;
        }

        try {
            // 验证令牌
            if (!jwtUtils.validateToken(token)) {
                log.warn("WebSocket握手失败：JWT令牌验证失败，URI：{}", request.getURI());
                return false;
            }

            // 验证是否为访问令牌
            if (!jwtUtils.isAccessToken(token)) {
                log.warn("WebSocket握手失败：令牌类型错误，期望访问令牌，URI：{}", request.getURI());
                return false;
            }

            // 提取用户信息并存储到WebSocket会话属性中
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            String roles = jwtUtils.getRolesFromToken(token);

            // 将用户信息存储到WebSocket会话属性中
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("roles", roles);
            attributes.put("token", token);

            // 如果前端使用子协议传递token，需要在响应中确认子协议
            String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
            if (StringUtils.hasText(protocol) && protocol.startsWith("Bearer.")) {
                response.getHeaders().set("Sec-WebSocket-Protocol", protocol);
                log.debug("设置响应子协议：{}", protocol);
            }

            log.info("WebSocket握手成功，用户ID：{}，用户名：{}，URI：{}", userId, username, request.getURI());
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手时JWT处理失败，URI：{}，错误：{}", request.getURI(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, 
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler, 
                             Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手后处理异常，URI：{}，错误：{}", request.getURI(), exception.getMessage(), exception);
        } else {
            log.debug("WebSocket握手完成，URI：{}", request.getURI());
        }
    }

    /**
     * 从HTTP请求中提取JWT令牌
     * 支持以下方式：
     * 1. Sec-WebSocket-Protocol子协议：Bearer.{token}（推荐，更安全）
     * 2. Authorization请求头：Authorization: Bearer <token>
     * 3. 查询参数：?token=<token>
     * 4. 查询参数：?access_token=<token>
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. 从Sec-WebSocket-Protocol子协议中提取（推荐方式）
        String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (StringUtils.hasText(protocol)) {
            // 支持格式：Bearer.{token}
            if (protocol.startsWith("Bearer.")) {
                String token = protocol.substring(7);
                if (StringUtils.hasText(token)) {
                    log.debug("从Sec-WebSocket-Protocol子协议中提取到令牌");
                    return token;
                }
            }
        }

        // 2. 从Authorization头中提取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            log.debug("从Authorization头中提取到令牌");
            return authHeader.substring(7);
        }

        // 3. 从查询参数中提取（向后兼容）
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (StringUtils.hasText(query)) {
            // 解析token参数
            String token = extractQueryParam(query, "token");
            if (StringUtils.hasText(token)) {
                log.debug("从查询参数token中提取到令牌");
                return token;
            }
            
            // 解析access_token参数
            token = extractQueryParam(query, "access_token");
            if (StringUtils.hasText(token)) {
                log.debug("从查询参数access_token中提取到令牌");
                return token;
            }
        }

        return null;
    }

    /**
     * 从查询字符串中提取指定参数的值
     */
    private String extractQueryParam(String query, String paramName) {
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && paramName.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
