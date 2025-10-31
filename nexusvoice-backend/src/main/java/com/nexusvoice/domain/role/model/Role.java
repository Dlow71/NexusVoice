package com.nexusvoice.domain.role.model;

import com.nexusvoice.domain.common.BaseDomainEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI角色/Agent领域实体
 * 对应数据库表 roles
 *
 * 字段说明：
 * - 公共角色：is_public = 1，user_id 为空
 * - 私人角色：is_public = 0，user_id 为创建者ID
 *
 * Agent能力扩展（V19）：
 * - 工具调用：tool_ids, tool_preset_params
 * - 知识库集成：knowledge_base_ids
 * - 多模态支持：multi_modal
 * - 启用控制：enabled
 * - 标签分类：tags
 *
 * @author NexusVoice
 * @since 2025-09-25
 * @updated 2025-10-31 V19扩展为Agent能力
 */
public class Role extends BaseDomainEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色人设提示词
     */
    private String personaPrompt;

    /**
     * 开场白文本
     */
    private String greetingMessage;

    /**
     * 开场白音频URL
     */
    private String greetingAudioUrl;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * TTS声音类型
     */
    private String voiceType;

    /**
     * 是否公共角色：0-私有 1-公共
     */
    private Integer isPublic;

    /**
     * 创建者用户ID（私人角色）
     */
    private Long userId;

    // ============ Agent扩展字段（V19） ============

    /**
     * 可用工具ID列表
     */
    private List<Long> toolIds;

    /**
     * 工具预设参数
     * 格式：{"search": {"max_results": 10}, "calculator": {"precision": 2}}
     */
    private Map<String, Object> toolPresetParams;

    /**
     * 关联的知识库ID列表（用于RAG）
     */
    private List<Long> knowledgeBaseIds;

    /**
     * 是否支持多模态（图像、语音输入）
     */
    private Boolean multiModal;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 角色标签（用于分类和搜索）
     */
    private List<String> tags;

    /**
     * 扩展配置参数
     * 用于存储其他自定义配置
     */
    private Map<String, Object> configParams;

    // ============ 业务方法 ============

    /**
     * 设为公共角色
     */
    public void makePublic() {
        this.isPublic = 1;
        this.userId = null;
    }

    /**
     * 设为私人角色
     * @param ownerUserId 创建者用户ID
     */
    public void makePrivate(Long ownerUserId) {
        this.isPublic = 0;
        this.userId = ownerUserId;
    }

    /**
     * 判断是否属于指定用户
     */
    public boolean ownedBy(Long uid) {
        return uid != null && uid.equals(this.userId);
    }

    // ============ Agent能力业务方法 ============

    /**
     * 是否启用了特定工具
     */
    public boolean hasToolEnabled(Long toolId) {
        return toolIds != null && toolIds.contains(toolId);
    }

    /**
     * 是否关联了知识库（支持RAG）
     */
    public boolean hasKnowledgeBase() {
        return knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty();
    }

    /**
     * 获取特定知识库数量
     */
    public int getKnowledgeBaseCount() {
        return knowledgeBaseIds != null ? knowledgeBaseIds.size() : 0;
    }

    /**
     * 添加工具
     */
    public void addTool(Long toolId) {
        if (this.toolIds == null) {
            this.toolIds = new ArrayList<>();
        }
        if (!this.toolIds.contains(toolId)) {
            this.toolIds.add(toolId);
        }
    }

    /**
     * 移除工具
     */
    public void removeTool(Long toolId) {
        if (this.toolIds != null) {
            this.toolIds.remove(toolId);
        }
    }

    /**
     * 添加知识库
     */
    public void addKnowledgeBase(Long knowledgeBaseId) {
        if (this.knowledgeBaseIds == null) {
            this.knowledgeBaseIds = new ArrayList<>();
        }
        if (!this.knowledgeBaseIds.contains(knowledgeBaseId)) {
            this.knowledgeBaseIds.add(knowledgeBaseId);
        }
    }

    /**
     * 移除知识库
     */
    public void removeKnowledgeBase(Long knowledgeBaseId) {
        if (this.knowledgeBaseIds != null) {
            this.knowledgeBaseIds.remove(knowledgeBaseId);
        }
    }

    /**
     * 获取工具预设参数
     */
    public Object getToolParam(String toolName, String paramKey) {
        if (toolPresetParams == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> toolParams = (Map<String, Object>) toolPresetParams.get(toolName);
        return toolParams != null ? toolParams.get(paramKey) : null;
    }

    /**
     * 设置工具参数
     */
    public void setToolParam(String toolName, String paramKey, Object value) {
        if (this.toolPresetParams == null) {
            this.toolPresetParams = new HashMap<>();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> toolParams = (Map<String, Object>) this.toolPresetParams.get(toolName);
        if (toolParams == null) {
            toolParams = new HashMap<>();
            this.toolPresetParams.put(toolName, toolParams);
        }
        toolParams.put(paramKey, value);
    }

    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    /**
     * 是否包含标签
     */
    public boolean hasTag(String tag) {
        return this.tags != null && this.tags.contains(tag);
    }

    /**
     * 启用角色
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 禁用角色
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    /**
     * 是否支持多模态
     */
    public boolean isMultiModal() {
        return Boolean.TRUE.equals(this.multiModal);
    }

    // ============ Getter / Setter ============
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPersonaPrompt() {
        return personaPrompt;
    }

    public void setPersonaPrompt(String personaPrompt) {
        this.personaPrompt = personaPrompt;
    }

    public String getGreetingMessage() {
        return greetingMessage;
    }

    public void setGreetingMessage(String greetingMessage) {
        this.greetingMessage = greetingMessage;
    }

    public String getGreetingAudioUrl() {
        return greetingAudioUrl;
    }

    public void setGreetingAudioUrl(String greetingAudioUrl) {
        this.greetingAudioUrl = greetingAudioUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getVoiceType() {
        return voiceType;
    }

    public void setVoiceType(String voiceType) {
        this.voiceType = voiceType;
    }

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer aPublic) {
        isPublic = aPublic;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // ============ Agent扩展字段 Getter / Setter ============

    public List<Long> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<Long> toolIds) {
        this.toolIds = toolIds;
    }

    public Map<String, Object> getToolPresetParams() {
        return toolPresetParams;
    }

    public void setToolPresetParams(Map<String, Object> toolPresetParams) {
        this.toolPresetParams = toolPresetParams;
    }

    public List<Long> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public Boolean getMultiModal() {
        return multiModal;
    }

    public void setMultiModal(Boolean multiModal) {
        this.multiModal = multiModal;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getConfigParams() {
        return configParams;
    }

    public void setConfigParams(Map<String, Object> configParams) {
        this.configParams = configParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", isPublic=" + isPublic +
                ", userId=" + userId +
                ", enabled=" + enabled +
                ", multiModal=" + multiModal +
                ", toolCount=" + (toolIds != null ? toolIds.size() : 0) +
                ", kbCount=" + (knowledgeBaseIds != null ? knowledgeBaseIds.size() : 0) +
                '}';
    }
}
