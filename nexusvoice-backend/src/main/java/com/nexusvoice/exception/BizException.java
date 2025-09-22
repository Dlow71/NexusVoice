package com.nexusvoice.exception;

import com.nexusvoice.enums.ErrorCodeEnum;

/**
 * 业务异常类
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
public class BizException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
    
    /**
     * 构造函数
     */
    public BizException() {
        super();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     * @param message 自定义错误信息
     */
    public BizException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public BizException(ErrorCodeEnum errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     * @param message 自定义错误信息
     * @param cause 异常原因
     */
    public BizException(ErrorCodeEnum errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 静态工厂方法 - 创建业务异常
     */
    public static BizException of(ErrorCodeEnum errorCode) {
        return new BizException(errorCode);
    }
    
    /**
     * 静态工厂方法 - 创建业务异常（自定义消息）
     */
    public static BizException of(ErrorCodeEnum errorCode, String message) {
        return new BizException(errorCode, message);
    }
    
    /**
     * 静态工厂方法 - 创建业务异常（带原因）
     */
    public static BizException of(ErrorCodeEnum errorCode, Throwable cause) {
        return new BizException(errorCode, cause);
    }
    
    /**
     * 静态工厂方法 - 创建业务异常（自定义消息和原因）
     */
    public static BizException of(ErrorCodeEnum errorCode, String message, Throwable cause) {
        return new BizException(errorCode, message, cause);
    }
}
