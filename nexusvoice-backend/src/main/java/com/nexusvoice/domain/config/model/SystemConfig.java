package com.nexusvoice.domain.config.model;

import com.nexusvoice.domain.common.BaseDomainEntity;

/**
 * 系统配置领域实体
 * 纯净的领域模型，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
public class SystemConfig extends BaseDomainEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 配置分组
     */
    private String configGroup;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否只读
     */
    private Boolean readonly;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;
    
    /**
     * 版本号（用于乐观锁）
     */
    private Integer version;

    // 构造函数
    public SystemConfig() {
        super();
    }

    public SystemConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.enabled = true;
        this.readonly = false;
        this.sortOrder = 0;
    }

    // Getter and Setter methods
    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 验证配置键是否有效
     */
    public boolean isValidKey() {
        return configKey != null && !configKey.trim().isEmpty();
    }

    /**
     * 检查配置是否可以修改
     */
    public boolean isModifiable() {
        return !Boolean.TRUE.equals(readonly);
    }

    /**
     * 检查配置是否启用
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public String toString() {
        return "SystemConfig{" +
                "id=" + getId() +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", description='" + description + '\'' +
                ", configGroup='" + configGroup + '\'' +
                ", enabled=" + enabled +
                ", readonly=" + readonly +
                ", sortOrder=" + sortOrder +
                ", remark='" + remark + '\'' +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}
