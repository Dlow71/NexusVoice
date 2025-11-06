package com.nexusvoice.infrastructure.agent.tool;

import com.nexusvoice.domain.agent.model.ToolParameter;

import java.util.List;
import java.util.Map;

/**
 * 工具基础接口（Infrastructure层）
 * 
 * 职责：
 * - 定义工具的执行接口
 * - 提供工具元数据
 * 
 * 实现规范：
 * - 所有工具实现类都要标注@Component
 * - execute方法必须线程安全
 * - 异常统一抛出BizException
 */
public interface BaseTool {
    
    /**
     * 获取工具名称（唯一标识）
     */
    String getName();
    
    /**
     * 获取工具描述（给LLM看的）
     */
    String getDescription();
    
    /**
     * 获取参数定义
     */
    List<ToolParameter> getParameters();
    
    /**
     * 执行工具
     * 
     * @param parameters 工具参数
     * @return 执行结果（JSON格式字符串）
     * @throws com.nexusvoice.exception.BizException 业务异常
     */
    String execute(Map<String, Object> parameters);
    
    /**
     * 是否是原子工具（默认true）
     */
    default boolean isAtomic() {
        return true;
    }
    
    /**
     * 工具分类
     */
    default String getCategory() {
        return "general";
    }
    
    /**
     * 是否启用（默认true）
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 预估执行时间（毫秒）
     */
    default Long getEstimatedDurationMs() {
        return 5000L;
    }
    
    /**
     * 优先级（数值越大优先级越高，默认0）
     */
    default Integer getPriority() {
        return 0;
    }
}

