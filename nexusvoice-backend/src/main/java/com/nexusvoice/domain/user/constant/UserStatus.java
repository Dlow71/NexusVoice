package com.nexusvoice.domain.user.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户状态枚举
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "用户状态")
public enum UserStatus {

    /**
     * 正常状态
     */
    NORMAL(1, "正常"),

    /**
     * 封禁状态
     */
    BANNED(2, "封禁"),

    /**
     * 待激活状态
     */
    PENDING_ACTIVATION(3, "待激活");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String description;

    UserStatus(Integer code, String description) {
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
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : UserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态代码: " + code);
    }

    /**
     * 判断是否为正常状态
     *
     * @return true-正常, false-非正常
     */
    public boolean isNormal() {
        return this == NORMAL;
    }

    /**
     * 判断是否被封禁
     *
     * @return true-被封禁, false-未被封禁
     */
    public boolean isBanned() {
        return this == BANNED;
    }

    /**
     * 判断是否待激活
     *
     * @return true-待激活, false-非待激活
     */
    public boolean isPendingActivation() {
        return this == PENDING_ACTIVATION;
    }
}
