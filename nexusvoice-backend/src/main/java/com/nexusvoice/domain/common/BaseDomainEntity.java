package com.nexusvoice.domain.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 领域基础实体
 * 纯净的领域模型，不包含任何技术框架相关的注解
 * 包含通用字段：ID、创建时间、更新时间、逻辑删除标识
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
public abstract class BaseDomainEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实体唯一标识
     */
    protected Long id;

    /**
     * 创建时间
     */
    protected LocalDateTime createdAt;

    /**
     * 更新时间
     */
    protected LocalDateTime updatedAt;

    /**
     * 删除标识（0-未删除，1-已删除）
     */
    protected Integer deleted;

    /**
     * 默认构造函数
     */
    public BaseDomainEntity() {
        this.deleted = 0; // 默认未删除
    }

    /**
     * 带ID的构造函数
     */
    public BaseDomainEntity(Long id) {
        this();
        this.id = id;
    }

    /**
     * 是否为新实体（未持久化）
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return Integer.valueOf(1).equals(this.deleted);
    }

    /**
     * 标记为删除（逻辑删除）
     */
    public void markDeleted() {
        this.deleted = 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为未删除（恢复）
     */
    public void markUndeleted() {
        this.deleted = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 创建实体时调用（设置创建时间和更新时间）
     */
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新实体时调用（更新更新时间）
     */
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 基于ID判断相等性
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseDomainEntity that = (BaseDomainEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    /**
     * 基于ID生成哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * 默认的toString实现
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deleted=" + deleted +
                '}';
    }
}
