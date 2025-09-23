package com.nexusvoice.annotation;

import com.nexusvoice.domain.user.constant.UserType;

import java.lang.annotation.*;

/**
 * 管理员权限注解
 * 标记只有管理员才能访问的方法或类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequireAuth(UserType.ADMIN)
public @interface RequireAdmin {

    /**
     * 权限验证失败时的错误消息
     * 
     * @return 错误消息
     */
    String message() default "需要管理员权限才能访问此资源";
}
