package com.nexusvoice.annotation;

import com.nexusvoice.domain.user.constant.UserType;

import java.lang.annotation.*;

/**
 * 普通用户权限注解
 * 标记普通用户可以访问的方法或类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequireAuth(UserType.USER)
public @interface RequireUser {

    /**
     * 权限验证失败时的错误消息
     * 
     * @return 错误消息
     */
    String message() default "需要用户权限才能访问此资源";
}
