package com.nexusvoice.domain.agent.repository;

import com.nexusvoice.domain.agent.enums.AgentType;
import com.nexusvoice.domain.agent.model.Agent;

import java.util.List;
import java.util.Optional;

/**
 * Agent仓储接口（Domain层定义）
 * 
 * 职责：
 * - 定义Agent的持久化操作契约
 * - 由Infrastructure层实现
 * 
 * 设计原则：
 * - 接口在Domain层，实现在Infrastructure层
 * - 依赖倒置原则
 */
public interface AgentRepository {
    
    /**
     * 根据ID查找Agent
     */
    Optional<Agent> findById(Long id);
    
    /**
     * 根据名称查找Agent
     */
    Optional<Agent> findByName(String name);
    
    /**
     * 根据类型查找Agent列表
     */
    List<Agent> findByType(AgentType type);
    
    /**
     * 根据用户ID查找Agent列表
     */
    List<Agent> findByUserId(Long userId);
    
    /**
     * 查找所有启用的系统Agent
     */
    List<Agent> findSystemAgents();
    
    /**
     * 保存Agent
     */
    Agent save(Agent agent);
    
    /**
     * 删除Agent
     */
    void delete(Long id);
    
    /**
     * 检查Agent名称是否存在
     */
    boolean existsByName(String name);
}

