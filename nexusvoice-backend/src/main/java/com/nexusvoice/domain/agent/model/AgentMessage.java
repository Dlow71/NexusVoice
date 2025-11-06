package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent消息值对象（纯POJO）
 * 
 * 职责：
 * - 表示Agent对话中的单条消息
 * - 支持不同角色（user/assistant/system/tool）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    
    /**
     * 角色：user/assistant/system/tool
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 附加数据（如工具调用结果）
     */
    private Map<String, Object> data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 创建用户消息
     */
    public static AgentMessage user(String content) {
        return AgentMessage.builder()
            .role("user")
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建助手消息
     */
    public static AgentMessage assistant(String content) {
        return AgentMessage.builder()
            .role("assistant")
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建系统消息
     */
    public static AgentMessage system(String content) {
        return AgentMessage.builder()
            .role("system")
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建工具消息
     */
    public static AgentMessage tool(String content) {
        return AgentMessage.builder()
            .role("tool")
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}

