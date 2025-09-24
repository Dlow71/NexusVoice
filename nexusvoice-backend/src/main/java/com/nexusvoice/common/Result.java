package com.nexusvoice.common;

import com.nexusvoice.enums.ErrorCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 统一响应结果类
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Schema(description = "统一响应结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "响应码", example = "200")
    private Integer code;
    
    @Schema(description = "响应消息", example = "操作成功")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;
    
    @Schema(description = "响应时间", example = "2025-09-22T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "请求追踪ID", example = "abc123")
    private String traceId;
    
    /**
     * 私有构造函数
     */
    private Result() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 私有构造函数
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMessage(), null);
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMessage(), data);
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), message, null);
    }
    
    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), message, data);
    }
    
    /**
     * 失败响应（使用错误码枚举）
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
    
    /**
     * 失败响应（使用错误码枚举和自定义消息）
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
    
    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 失败响应（默认服务器错误）
     */
    public static <T> Result<T> error() {
        return error(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 失败响应（自定义消息，默认服务器错误码）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode(), message, null);
    }
    
    /**
     * 根据条件返回成功或失败
     */
    public static <T> Result<T> result(boolean success) {
        return success ? success() : error();
    }
    
    /**
     * 根据条件返回成功或失败（带数据）
     */
    public static <T> Result<T> result(boolean success, T data) {
        return success ? success(data) : error();
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ErrorCodeEnum.SUCCESS.getCode().equals(this.code);
    }
    
    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    // Getter and Setter methods
    public Integer getCode() {
        return code;
    }
    
    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Result<T> setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public T getData() {
        return data;
    }
    
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Result<T> setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public Result<T> setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
