package com.nexusvoice.domain.user.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户类型枚举
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "用户类型")
public enum UserType {

    /**
     * 普通用户
     */
    USER("USER", "普通用户"),

    /**
     * 管理员用户
     */
    ADMIN("ADMIN", "管理员用户");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    UserType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
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
    public static UserType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserType userType : UserType.values()) {
            if (userType.code.equals(code)) {
                return userType;
            }
        }
        throw new IllegalArgumentException("未知的用户类型代码: " + code);
    }

    /**
     * 判断是否为管理员
     *
     * @return true-管理员, false-普通用户
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
}
