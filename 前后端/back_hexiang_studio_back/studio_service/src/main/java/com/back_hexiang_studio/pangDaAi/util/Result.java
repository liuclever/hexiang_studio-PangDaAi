package com.back_hexiang_studio.pangDaAi.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一API响应结果封装类
 * 标准化AI助手服务的返回格式
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-13
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /**
     * 响应状态码
     * 200: 成功
     * 400: 客户端错误  
     * 500: 服务器错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 私有构造器
     */
    private Result() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 私有构造器
     */
    private Result(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ===================================================================
    // 成功响应静态方法
    // ===================================================================

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ===================================================================
    // 错误响应静态方法
    // ===================================================================

    /**
     * 客户端错误响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(400, message, null);
    }

    /**
     * 服务器错误响应
     */
    public static <T> Result<T> serverError(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 自定义错误响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 自定义错误响应（带数据）
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ===================================================================
    // 判断方法
    // ===================================================================

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }

    /**
     * 判断是否失败
     */
    public boolean isFailed() {
        return !isSuccess();
    }

    // ===================================================================
    // Getters and Setters
    // ===================================================================



    @Override
    public String toString() {
        return "Result{" +
               "code=" + code +
               ", message='" + message + '\'' +
               ", data=" + data +
               ", timestamp=" + timestamp +
               '}';
    }
} 