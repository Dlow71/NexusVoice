package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置持久化对象
 * 对应数据库表system_config
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfigPO extends BasePO {

    /**
     * 配置键
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

    /**
     * 配置分组
     */
    @TableField("config_group")
    private String configGroup;

    /**
     * 是否启用（0-否 1-是）
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 是否只读（0-否 1-是）
     */
    @TableField("readonly")
    private Integer readonly;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @TableField("version")
    private Integer version;
}
