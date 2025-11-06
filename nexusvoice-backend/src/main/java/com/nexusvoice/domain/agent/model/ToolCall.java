package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具调用值对象（纯POJO）
 * 
 * 职责：
 * - 表示一次工具调用请求
 * - 包含工具名称和参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {
    
    /**
     * 调用ID（用于追踪）
     */
    private String callId;
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具参数
     */
    private Map<String, Object> params;
    
    /**
     * 调用时间戳
     */
    private Long timestamp;
    
    /**
     * 创建工具调用
     */
    public static ToolCall create(String name, Map<String, Object> params) {
        return ToolCall.builder()
            .callId(java.util.UUID.randomUUID().toString())
            .name(name)
            .params(params)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}

