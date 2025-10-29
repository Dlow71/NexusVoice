package com.nexusvoice.domain.rbac.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单类型枚举
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Schema(description = "菜单类型")
public enum MenuType {

    /**
     * 目录（有子菜单的父节点）
     */
    DIRECTORY(1, "目录"),

    /**
     * 菜单（具体页面）
     */
    MENU(2, "菜单"),

    /**
     * 按钮（页面内操作权限）
     */
    BUTTON(3, "按钮");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String description;

    MenuType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 代码
     * @return 枚举值
     */
    public static MenuType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MenuType type : MenuType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的菜单类型代码: " + code);
    }

    /**
     * 判断是否为目录
     *
     * @return true-目录, false-其他
     */
    public boolean isDirectory() {
        return this == DIRECTORY;
    }

    /**
     * 判断是否为菜单
     *
     * @return true-菜单, false-其他
     */
    public boolean isMenu() {
        return this == MENU;
    }

    /**
     * 判断是否为按钮
     *
     * @return true-按钮, false-其他
     */
    public boolean isButton() {
        return this == BUTTON;
    }
}
