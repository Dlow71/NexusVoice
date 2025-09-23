package com.nexusvoice.aspect;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.annotation.RequireUser;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * 权限认证切面
 * 处理@RequireAuth、@RequireAdmin、@RequireUser等权限注解
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Aspect
@Component
public class AuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAspect.class);

    /**
     * 处理@RequireAuth注解
     */
    @Around("@annotation(com.nexusvoice.annotation.RequireAuth) || @within(com.nexusvoice.annotation.RequireAuth)")
    public Object handleRequireAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireAuth requireAuth = getRequireAuthAnnotation(joinPoint);
        if (requireAuth != null) {
            validateAuth(requireAuth, joinPoint);
        }
        return joinPoint.proceed();
    }

    /**
     * 处理@RequireAdmin注解
     */
    @Around("@annotation(com.nexusvoice.annotation.RequireAdmin) || @within(com.nexusvoice.annotation.RequireAdmin)")
    public Object handleRequireAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireAdmin requireAdmin = getRequireAdminAnnotation(joinPoint);
        if (requireAdmin != null) {
            validateAdminAuth(requireAdmin, joinPoint);
        }
        return joinPoint.proceed();
    }

    /**
     * 处理@RequireUser注解
     */
    @Around("@annotation(com.nexusvoice.annotation.RequireUser) || @within(com.nexusvoice.annotation.RequireUser)")
    public Object handleRequireUser(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireUser requireUser = getRequireUserAnnotation(joinPoint);
        if (requireUser != null) {
            validateUserAuth(requireUser, joinPoint);
        }
        return joinPoint.proceed();
    }

    /**
     * 验证基础权限
     */
    private void validateAuth(RequireAuth requireAuth, ProceedingJoinPoint joinPoint) {
        String methodName = getMethodName(joinPoint);
        
        // 检查是否需要登录
        if (requireAuth.requireLogin()) {
            if (!SecurityUtils.isAuthenticated()) {
                log.warn("用户未登录，无法访问方法: {}", methodName);
                throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录");
            }
        }

        // 检查用户类型权限
        UserType[] requiredTypes = requireAuth.value();
        if (requiredTypes.length > 0) {
            Optional<UserType> currentUserType = SecurityUtils.getCurrentUserType();
            
            if (currentUserType.isEmpty()) {
                log.warn("无法获取当前用户类型，拒绝访问方法: {}", methodName);
                throw BizException.of(ErrorCodeEnum.FORBIDDEN, requireAuth.message());
            }

            boolean hasPermission = Arrays.asList(requiredTypes).contains(currentUserType.get());
            if (!hasPermission) {
                log.warn("用户类型 {} 无权限访问方法: {}, 需要权限: {}", 
                        currentUserType.get(), methodName, Arrays.toString(requiredTypes));
                throw BizException.of(ErrorCodeEnum.FORBIDDEN, requireAuth.message());
            }
        }

        log.debug("权限验证通过，允许访问方法: {}", methodName);
    }

    /**
     * 验证管理员权限
     */
    private void validateAdminAuth(RequireAdmin requireAdmin, ProceedingJoinPoint joinPoint) {
        String methodName = getMethodName(joinPoint);
        
        if (!SecurityUtils.isAuthenticated()) {
            log.warn("用户未登录，无法访问管理员方法: {}", methodName);
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录");
        }

        if (!SecurityUtils.isAdmin()) {
            log.warn("非管理员用户尝试访问管理员方法: {}", methodName);
            throw BizException.of(ErrorCodeEnum.FORBIDDEN, requireAdmin.message());
        }

        log.debug("管理员权限验证通过，允许访问方法: {}", methodName);
    }

    /**
     * 验证普通用户权限
     */
    private void validateUserAuth(RequireUser requireUser, ProceedingJoinPoint joinPoint) {
        String methodName = getMethodName(joinPoint);
        
        if (!SecurityUtils.isAuthenticated()) {
            log.warn("用户未登录，无法访问用户方法: {}", methodName);
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录");
        }

        // 普通用户和管理员都可以访问标记为@RequireUser的方法
        if (!SecurityUtils.isUser() && !SecurityUtils.isAdmin()) {
            log.warn("用户权限不足，无法访问用户方法: {}", methodName);
            throw BizException.of(ErrorCodeEnum.FORBIDDEN, requireUser.message());
        }

        log.debug("用户权限验证通过，允许访问方法: {}", methodName);
    }

    /**
     * 获取@RequireAuth注解
     */
    private RequireAuth getRequireAuthAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 先检查方法上的注解
        RequireAuth annotation = AnnotationUtils.findAnnotation(method, RequireAuth.class);
        if (annotation != null) {
            return annotation;
        }
        
        // 再检查类上的注解
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequireAuth.class);
    }

    /**
     * 获取@RequireAdmin注解
     */
    private RequireAdmin getRequireAdminAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 先检查方法上的注解
        RequireAdmin annotation = AnnotationUtils.findAnnotation(method, RequireAdmin.class);
        if (annotation != null) {
            return annotation;
        }
        
        // 再检查类上的注解
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequireAdmin.class);
    }

    /**
     * 获取@RequireUser注解
     */
    private RequireUser getRequireUserAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 先检查方法上的注解
        RequireUser annotation = AnnotationUtils.findAnnotation(method, RequireUser.class);
        if (annotation != null) {
            return annotation;
        }
        
        // 再检查类上的注解
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequireUser.class);
    }

    /**
     * 获取方法名称（用于日志）
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
