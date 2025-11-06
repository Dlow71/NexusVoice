package com.nexusvoice.infrastructure.agent.registry;

import com.nexusvoice.domain.agent.model.Tool;
import com.nexusvoice.domain.agent.repository.ToolRegistry;
import com.nexusvoice.infrastructure.agent.tool.BaseTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心实现（Infrastructure层）
 * 
 * 职责：
 * - 实现Domain层定义的ToolRegistry接口
 * - 管理所有工具实例
 * - 提供工具查询和注册功能
 * 
 * 设计：
 * - 使用ConcurrentHashMap保证线程安全
 * - 启动时自动注册所有BaseTool实现类
 */
@Slf4j
@Component
public class ToolRegistryImpl implements ToolRegistry {
    
    /**
     * 工具存储：name -> BaseTool
     */
    private final Map<String, BaseTool> toolMap = new ConcurrentHashMap<>();
    
    /**
     * 构造函数：自动注册所有BaseTool实现
     */
    public ToolRegistryImpl(List<BaseTool> tools) {
        log.info("初始化工具注册中心，发现 {} 个工具", tools.size());
        
        for (BaseTool tool : tools) {
            try {
                String name = tool.getName();
                toolMap.put(name, tool);
                log.info("注册工具：{} - {}", name, tool.getDescription());
            } catch (Exception e) {
                log.error("注册工具失败：{}", tool.getClass().getName(), e);
            }
        }
        
        log.info("工具注册中心初始化完成，共注册 {} 个工具", toolMap.size());
    }
    
    @Override
    public void registerTool(Tool tool) {
        if (tool == null || !tool.isValid()) {
            log.warn("尝试注册无效工具：{}", tool);
            return;
        }
        
        // Domain Tool 转换为 Infrastructure BaseTool
        // 这里简化处理，实际应该创建一个适配器
        log.info("注册领域工具：{}", tool.getName());
        // TODO: 实现Domain Tool到BaseTool的转换
    }
    
    @Override
    public void registerTools(List<Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }
        tools.forEach(this::registerTool);
    }
    
    @Override
    public Optional<Tool> getTool(String name) {
        BaseTool baseTool = toolMap.get(name);
        if (baseTool == null) {
            return Optional.empty();
        }
        
        // 将Infrastructure的BaseTool转换为Domain的Tool
        Tool domainTool = Tool.builder()
            .name(baseTool.getName())
            .description(baseTool.getDescription())
            .parameters(baseTool.getParameters())
            .atomic(baseTool.isAtomic())
            .category(baseTool.getCategory())
            .enabled(baseTool.isEnabled())
            .estimatedDurationMs(baseTool.getEstimatedDurationMs())
            .priority(baseTool.getPriority())
            .build();
        
        return Optional.of(domainTool);
    }
    
    /**
     * 获取Infrastructure层的BaseTool（内部使用）
     */
    public BaseTool getBaseTool(String name) {
        return toolMap.get(name);
    }
    
    @Override
    public List<Tool> getAllTools() {
        List<Tool> tools = new ArrayList<>();
        
        for (BaseTool baseTool : toolMap.values()) {
            Tool domainTool = Tool.builder()
                .name(baseTool.getName())
                .description(baseTool.getDescription())
                .parameters(baseTool.getParameters())
                .atomic(baseTool.isAtomic())
                .category(baseTool.getCategory())
                .enabled(baseTool.isEnabled())
                .estimatedDurationMs(baseTool.getEstimatedDurationMs())
                .priority(baseTool.getPriority())
                .build();
            tools.add(domainTool);
        }
        
        return tools;
    }
    
    @Override
    public List<Tool> getEnabledTools() {
        return toolMap.values().stream()
            .filter(BaseTool::isEnabled)
            .map(baseTool -> Tool.builder()
                .name(baseTool.getName())
                .description(baseTool.getDescription())
                .parameters(baseTool.getParameters())
                .atomic(baseTool.isAtomic())
                .category(baseTool.getCategory())
                .enabled(baseTool.isEnabled())
                .estimatedDurationMs(baseTool.getEstimatedDurationMs())
                .priority(baseTool.getPriority())
                .build())
            .toList();
    }
    
    @Override
    public List<Tool> getToolsByCategory(String category) {
        return toolMap.values().stream()
            .filter(tool -> category.equals(tool.getCategory()))
            .map(baseTool -> Tool.builder()
                .name(baseTool.getName())
                .description(baseTool.getDescription())
                .parameters(baseTool.getParameters())
                .atomic(baseTool.isAtomic())
                .category(baseTool.getCategory())
                .enabled(baseTool.isEnabled())
                .estimatedDurationMs(baseTool.getEstimatedDurationMs())
                .priority(baseTool.getPriority())
                .build())
            .toList();
    }
    
    @Override
    public boolean hasToolRegistered(String name) {
        return toolMap.containsKey(name);
    }
    
    @Override
    public void unregisterTool(String name) {
        BaseTool removed = toolMap.remove(name);
        if (removed != null) {
            log.info("注销工具：{}", name);
        }
    }
    
    @Override
    public void clear() {
        log.warn("清空所有工具");
        toolMap.clear();
    }
    
    @Override
    public int getToolCount() {
        return toolMap.size();
    }
}

