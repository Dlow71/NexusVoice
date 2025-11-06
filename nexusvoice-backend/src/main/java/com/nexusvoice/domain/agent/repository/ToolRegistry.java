package com.nexusvoice.domain.agent.repository;

import com.nexusvoice.domain.agent.model.Tool;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册中心接口（Domain层定义）
 * 
 * 职责：
 * - 定义工具注册和查询的契约
 * - 管理可用工具的生命周期
 * 
 * 设计原则：
 * - 接口在Domain层，实现在Infrastructure层
 * - 工具动态注册，即插即用
 */
public interface ToolRegistry {
    
    /**
     * 注册工具
     */
    void registerTool(Tool tool);
    
    /**
     * 批量注册工具
     */
    void registerTools(List<Tool> tools);
    
    /**
     * 根据名称获取工具
     */
    Optional<Tool> getTool(String name);
    
    /**
     * 获取所有工具
     */
    List<Tool> getAllTools();
    
    /**
     * 获取所有启用的工具
     */
    List<Tool> getEnabledTools();
    
    /**
     * 根据分类获取工具
     */
    List<Tool> getToolsByCategory(String category);
    
    /**
     * 检查工具是否存在
     */
    boolean hasToolRegistered(String name);
    
    /**
     * 注销工具
     */
    void unregisterTool(String name);
    
    /**
     * 清空所有工具
     */
    void clear();
    
    /**
     * 获取工具数量
     */
    int getToolCount();
}

