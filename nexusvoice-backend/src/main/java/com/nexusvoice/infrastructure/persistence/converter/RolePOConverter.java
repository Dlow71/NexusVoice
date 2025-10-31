package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.infrastructure.persistence.po.RolePO;
import org.springframework.stereotype.Component;

/**
 * Role实体与RolePO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-21
 * @updated 2025-10-31 V19扩展Agent能力
 */
@Component
public class RolePOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public RolePO toPO(Role role) {
        if (role == null) {
            return null;
        }
        
        RolePO po = new RolePO();
        
        // 基础字段
        po.setId(role.getId());
        po.setCreatedAt(role.getCreatedAt());
        po.setUpdatedAt(role.getUpdatedAt());
        po.setDeleted(role.getDeleted());
        
        // 业务字段
        po.setName(role.getName());
        po.setDescription(role.getDescription());
        po.setPersonaPrompt(role.getPersonaPrompt());
        po.setGreetingMessage(role.getGreetingMessage());
        po.setGreetingAudioUrl(role.getGreetingAudioUrl());
        po.setAvatarUrl(role.getAvatarUrl());
        po.setVoiceType(role.getVoiceType());
        po.setIsPublic(role.getIsPublic());
        po.setUserId(role.getUserId());
        
        // Agent扩展字段（V19）
        po.setToolIds(role.getToolIds());
        po.setToolPresetParams(role.getToolPresetParams());
        po.setKnowledgeBaseIds(role.getKnowledgeBaseIds());
        po.setMultiModal(role.getMultiModal());
        po.setEnabled(role.getEnabled());
        po.setTags(role.getTags());
        po.setConfigParams(role.getConfigParams());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public Role toDomain(RolePO po) {
        if (po == null) {
            return null;
        }
        
        Role role = new Role();
        
        // 基础字段
        role.setId(po.getId());
        role.setCreatedAt(po.getCreatedAt());
        role.setUpdatedAt(po.getUpdatedAt());
        role.setDeleted(po.getDeleted());
        
        // 业务字段
        role.setName(po.getName());
        role.setDescription(po.getDescription());
        role.setPersonaPrompt(po.getPersonaPrompt());
        role.setGreetingMessage(po.getGreetingMessage());
        role.setGreetingAudioUrl(po.getGreetingAudioUrl());
        role.setAvatarUrl(po.getAvatarUrl());
        role.setVoiceType(po.getVoiceType());
        role.setIsPublic(po.getIsPublic());
        role.setUserId(po.getUserId());
        
        // Agent扩展字段（V19）
        role.setToolIds(po.getToolIds());
        role.setToolPresetParams(po.getToolPresetParams());
        role.setKnowledgeBaseIds(po.getKnowledgeBaseIds());
        role.setMultiModal(po.getMultiModal());
        role.setEnabled(po.getEnabled());
        role.setTags(po.getTags());
        role.setConfigParams(po.getConfigParams());
        
        return role;
    }
}
