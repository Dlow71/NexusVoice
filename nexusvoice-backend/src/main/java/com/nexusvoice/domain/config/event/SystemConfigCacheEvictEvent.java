package com.nexusvoice.domain.config.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 系统配置缓存失效事件
 * 用于通知各个实例刷新本地缓存
 *
 * @author NexusVoice
 * @since 2025-10-18
 */
@Getter
public class SystemConfigCacheEvictEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 需要失效的配置键集合
     * 如果为null或空，表示清空所有缓存
     */
    private final Set<String> configKeys;

    /**
     * 是否清空所有缓存
     */
    private final boolean clearAll;

    /**
     * 事件来源实例ID（用于避免重复处理）
     */
    private final String sourceInstanceId;

    /**
     * 构造函数 - 清空所有缓存
     */
    public SystemConfigCacheEvictEvent(Object source, String sourceInstanceId) {
        super(source);
        this.configKeys = null;
        this.clearAll = true;
        this.sourceInstanceId = sourceInstanceId;
    }

    /**
     * 构造函数 - 清空指定配置键
     */
    public SystemConfigCacheEvictEvent(Object source, Set<String> configKeys, String sourceInstanceId) {
        super(source);
        this.configKeys = configKeys;
        this.clearAll = configKeys == null || configKeys.isEmpty();
        this.sourceInstanceId = sourceInstanceId;
    }

    @Override
    public String toString() {
        return "SystemConfigCacheEvictEvent{" +
                "clearAll=" + clearAll +
                ", configKeys=" + configKeys +
                ", sourceInstanceId='" + sourceInstanceId + '\'' +
                '}';
    }
}
