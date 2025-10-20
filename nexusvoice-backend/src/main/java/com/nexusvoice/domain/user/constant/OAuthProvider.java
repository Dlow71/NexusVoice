package com.nexusvoice.domain.user.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OAuth提供商枚举
 * 
 * @author NexusVoice
 * @since 2025-01-20
 */
public enum OAuthProvider {
    
    GITHUB("github", "GitHub", "https://github.com"),
    GOOGLE("google", "Google", "https://accounts.google.com"),
    WECHAT("wechat", "微信", "https://open.weixin.qq.com"),
    QQ("qq", "QQ", "https://connect.qq.com"),
    WEIBO("weibo", "微博", "https://open.weibo.com"),
    MICROSOFT("microsoft", "Microsoft", "https://login.microsoftonline.com");
    
    /**
     * 提供商标识（存储在数据库中的值）
     */
    @EnumValue
    @JsonValue
    private final String code;
    
    /**
     * 提供商名称
     */
    private final String name;
    
    /**
     * 提供商官网地址
     */
    private final String website;
    
    OAuthProvider(String code, String name, String website) {
        this.code = code;
        this.name = name;
        this.website = website;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getWebsite() {
        return website;
    }
    
    /**
     * 根据code获取枚举
     */
    public static OAuthProvider fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OAuthProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为有效的提供商代码
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
    
    /**
     * 判断是否支持该提供商
     */
    public boolean isSupported() {
        // 目前只支持GitHub，后续可扩展
        return this == GITHUB;
    }
    
    @Override
    public String toString() {
        return code;
    }
}
