package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket测试控制器
 * 提供WebSocket连接信息和测试指南
 *
 * @author NexusVoice
 * @since 2025-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/websocket")
@Tag(name = "WebSocket测试", description = "WebSocket流式对话测试相关接口")
public class WebSocketTestController {

    @Value("${server.port:8080}")
    private String serverPort;

    @GetMapping("/info")
    @Operation(summary = "获取WebSocket连接信息", description = "获取WebSocket流式对话的连接地址和使用说明")
    public Result<Map<String, Object>> getWebSocketInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // WebSocket连接信息
        info.put("websocket_url", "ws://localhost:" + serverPort + "/ws/chat/stream");
        info.put("sockjs_url", "http://localhost:" + serverPort + "/ws/chat/stream");
        
        // 认证信息
        Map<String, String> auth = new HashMap<>();
        auth.put("method1", "查询参数: ?token=your_jwt_token");
        auth.put("method2", "查询参数: ?access_token=your_jwt_token");
        auth.put("method3", "WebSocket子协议: Authorization: Bearer your_jwt_token");
        info.put("authentication", auth);
        
        // 消息格式示例
        Map<String, Object> messageFormat = new HashMap<>();
        messageFormat.put("message", "你好，AI助手！");
        messageFormat.put("conversationId", null); // null表示创建新对话
        messageFormat.put("modelName", "gpt-4o-mini");
        messageFormat.put("temperature", 0.7);
        messageFormat.put("maxTokens", 2000);
        messageFormat.put("title", "新对话");
        messageFormat.put("systemPrompt", "你是一个有用的AI助手");
        messageFormat.put("roleId", null);
        messageFormat.put("enableWebSearch", false);
        messageFormat.put("enableAudio", false);
        info.put("message_format", messageFormat);
        
        // 响应格式示例
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "CONTENT | START | END | ERROR | HEARTBEAT");
        responseFormat.put("delta", "增量文本内容");
        responseFormat.put("isEnd", false);
        responseFormat.put("errorMessage", "错误信息（仅在ERROR类型时存在）");
        info.put("response_format", responseFormat);
        
        // 使用说明
        Map<String, String> usage = new HashMap<>();
        usage.put("step1", "首先通过 /api/v1/auth/login 获取JWT令牌");
        usage.put("step2", "使用JWT令牌连接WebSocket: ws://localhost:" + serverPort + "/ws/chat/stream?token=your_jwt_token");
        usage.put("step3", "连接成功后发送JSON格式的聊天请求");
        usage.put("step4", "接收流式响应，type为CONTENT的消息包含AI回复内容");
        usage.put("step5", "收到type为END的消息表示对话结束");
        info.put("usage_guide", usage);
        
        // 测试工具推荐
        Map<String, String> tools = new HashMap<>();
        tools.put("postman", "支持WebSocket测试");
        tools.put("wscat", "命令行工具: wscat -c 'ws://localhost:" + serverPort + "/ws/chat/stream?token=your_jwt_token'");
        tools.put("browser", "浏览器开发者工具 WebSocket 面板");
        info.put("test_tools", tools);
        
        log.info("返回WebSocket连接信息");
        return Result.success(info);
    }
    

}
