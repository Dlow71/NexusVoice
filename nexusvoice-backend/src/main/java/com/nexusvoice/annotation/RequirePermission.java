package com.nexusvoice.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 用于Controller方法级别的权限控制
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 所需权限标识
     * 例如：system:user:add, system:role:edit
     */
    String[] value();
    
    /**
     * 逻辑关系
     * AND：需要拥有所有权限
     * OR：拥有任一权限即可
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        /** 且：必须拥有所有权限 */
        AND,
        /** 或：拥有任一权限即可 */
        OR
    }
}
