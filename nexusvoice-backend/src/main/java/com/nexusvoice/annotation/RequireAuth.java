package com.nexusvoice.annotation;

import com.nexusvoice.domain.user.constant.UserType;

import java.lang.annotation.*;

/**
 * 权限认证注解
 * 用于标记需要特定权限才能访问的方法或类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {

    /**
     * 需要的用户类型
     * 默认为空数组，表示只需要登录即可
     * 
     * @return 用户类型数组
     */
    UserType[] value() default {};

    /**
     * 是否需要登录
     * 默认为true，表示需要登录
     * 
     * @return 是否需要登录
     */
    boolean requireLogin() default true;

    /**
     * 权限验证失败时的错误消息
     * 
     * @return 错误消息
     */
    String message() default "权限不足，无法访问此资源";
}
