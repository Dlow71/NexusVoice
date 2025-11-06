package com.nexusvoice.domain.agent.model;

import com.nexusvoice.domain.agent.enums.AgentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent执行上下文值对象（纯POJO）
 * 
 * 职责：
 * - 维护Agent执行过程中的状态
 * - 存储执行历史和中间结果
 * - 管理变量和记忆
 * 
 * 设计原则：
 * - 值对象，不可变性由使用者保证
 * - 线程安全由上层管理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {
    
    /**
     * 会话ID（关联对话会话）
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 对话ID（可选）
     */
    private Long conversationId;
    
    /**
     * Agent状态
     */
    @Builder.Default
    private AgentState state = AgentState.IDLE;
    
    /**
     * 当前执行步数
     */
    @Builder.Default
    private Integer currentStep = 0;
    
    /**
     * 最大执行步数
     */
    @Builder.Default
    private Integer maxSteps = 10;
    
    /**
     * 上下文变量（用于传递中间结果）
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
    
    /**
     * 对话记忆（消息历史）
     */
    @Builder.Default
    private List<AgentMessage> memory = new ArrayList<>();
    
    /**
     * 执行历史（每一步的操作记录）
     */
    @Builder.Default
    private List<AgentStepRecord> executionHistory = new ArrayList<>();
    
    /**
     * 开始时间戳
     */
    private Long startTimestamp;
    
    /**
     * 结束时间戳
     */
    private Long endTimestamp;
    
    /**
     * 总耗时（毫秒）
     */
    private Long totalTimeMs;
    
    /**
     * 元数据（扩展字段）
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 添加消息到记忆
     */
    public void addMessage(AgentMessage message) {
        if (memory == null) {
            memory = new ArrayList<>();
        }
        memory.add(message);
    }
    
    /**
     * 添加执行记录
     */
    public void addExecutionRecord(AgentStepRecord record) {
        if (executionHistory == null) {
            executionHistory = new ArrayList<>();
        }
        executionHistory.add(record);
    }
    
    /**
     * 获取最后一条用户消息
     */
    public AgentMessage getLastUserMessage() {
        if (memory == null || memory.isEmpty()) {
            return null;
        }
        
        for (int i = memory.size() - 1; i >= 0; i--) {
            AgentMessage msg = memory.get(i);
            if ("user".equals(msg.getRole())) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * 获取最后一条助手消息
     */
    public AgentMessage getLastAssistantMessage() {
        if (memory == null || memory.isEmpty()) {
            return null;
        }
        
        for (int i = memory.size() - 1; i >= 0; i--) {
            AgentMessage msg = memory.get(i);
            if ("assistant".equals(msg.getRole())) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(key, value);
    }
    
    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        if (variables == null) {
            return null;
        }
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }
    
    /**
     * 检查是否达到最大步数
     */
    public boolean hasReachedMaxSteps() {
        return currentStep >= maxSteps;
    }
    
    /**
     * 增加步数
     */
    public void incrementStep() {
        this.currentStep++;
    }
    
    /**
     * 标记开始执行
     */
    public void markStart() {
        this.state = AgentState.RUNNING;
        this.startTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 标记执行完成
     */
    public void markFinished() {
        this.state = AgentState.FINISHED;
        this.endTimestamp = System.currentTimeMillis();
        if (startTimestamp != null) {
            this.totalTimeMs = endTimestamp - startTimestamp;
        }
    }
    
    /**
     * 标记执行失败
     */
    public void markFailed() {
        this.state = AgentState.FAILED;
        this.endTimestamp = System.currentTimeMillis();
        if (startTimestamp != null) {
            this.totalTimeMs = endTimestamp - startTimestamp;
        }
    }
}

