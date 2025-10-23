package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.rag.model.enums.InstallType;
import java.time.LocalDateTime;

/**
 * 用户安装的RAG领域实体（聚合根）
 * 记录用户安装的知识库版本
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class UserRag extends BaseDomainEntity {
    
    /**
     * 安装用户ID
     */
    private Long userId;
    
    /**
     * 关联的版本快照ID
     */
    private Long ragVersionId;
    
    /**
     * 原始知识库ID
     */
    private Long originalKnowledgeBaseId;
    
    /**
     * 安装时的名称
     */
    private String name;
    
    /**
     * 安装时的图标
     */
    private String icon;
    
    /**
     * 安装时的描述
     */
    private String description;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 安装类型：REFERENCE/SNAPSHOT
     */
    private InstallType installType;
    
    /**
     * 安装时间
     */
    private LocalDateTime installedAt;
    
    // 构造函数
    public UserRag() {
        super();
        this.installType = InstallType.SNAPSHOT; // 默认快照类型
    }
    
    // 业务方法
    
    /**
     * 安装知识库
     */
    public void install(Long userId, Long ragVersionId, InstallType installType) {
        this.userId = userId;
        this.ragVersionId = ragVersionId;
        this.installType = installType;
        this.installedAt = LocalDateTime.now();
        this.onCreate();
    }
    
    /**
     * 更新安装信息
     */
    public void updateInfo(String name, String icon, String description) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.onUpdate();
    }
    
    /**
     * 是否为引用类型
     */
    public boolean isReference() {
        return InstallType.REFERENCE.equals(this.installType);
    }
    
    /**
     * 是否为快照类型
     */
    public boolean isSnapshot() {
        return InstallType.SNAPSHOT.equals(this.installType);
    }
    
    /**
     * 卸载
     */
    public void uninstall() {
        this.markDeleted();
    }
    
    // Getters and Setters
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getRagVersionId() {
        return ragVersionId;
    }
    
    public void setRagVersionId(Long ragVersionId) {
        this.ragVersionId = ragVersionId;
    }
    
    public Long getOriginalKnowledgeBaseId() {
        return originalKnowledgeBaseId;
    }
    
    public void setOriginalKnowledgeBaseId(Long originalKnowledgeBaseId) {
        this.originalKnowledgeBaseId = originalKnowledgeBaseId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public InstallType getInstallType() {
        return installType;
    }
    
    public void setInstallType(InstallType installType) {
        this.installType = installType;
    }
    
    public LocalDateTime getInstalledAt() {
        return installedAt;
    }
    
    public void setInstalledAt(LocalDateTime installedAt) {
        this.installedAt = installedAt;
    }
}
