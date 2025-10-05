package com.back_hexiang_studio.GlobalException;


import com.back_hexiang_studio.result.Result;
import lombok.extern.slf4j.Slf4j;


import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理器
 * 
 * 统一处理应用程序中的所有异常，提供一致的错误响应格式
 * 支持自定义异常、系统异常、Spring框架异常等的统一处理
 * 
 * @author wenhan
 * @date 2024/09/27
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义基础异常
     * 
     * @param e 基础异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleException(BaseException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getErrorMessage());
        return Result.error(e.getCode(), e.getErrorMessage());
    }

    /**
     * 处理未授权异常
     * 
     * @param e 未授权异常
     * @return 统一错误响应
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<?> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("未授权异常: code={}, message={}", e.getCode(), e.getErrorMessage());
        return Result.error(e.getCode(), e.getErrorMessage());
    }

    /**
     * 处理禁止访问异常
     * 
     * @param e 禁止访问异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handleForbiddenException(ForbiddenException e) {
        log.warn("禁止访问异常: code={}, message={}", e.getCode(), e.getErrorMessage());
        return Result.error(e.getCode(), e.getErrorMessage());
    }

    /**
     * 处理参数校验异常
     * 
     * @param ex 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("参数校验异常: {}", msg);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 处理文件访问权限异常
     * 
     * @param e 文件访问权限异常
     * @return 统一错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleFileAccessDeniedException(AccessDeniedException e) {
        log.warn("文件访问权限异常: {}", e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN.getCode(), "文件访问权限不足，请联系管理员");
    }

    /**
     * 处理Spring Security权限访问异常
     * 
     * @param e Spring Security权限访问异常
     * @return 统一错误响应
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<?> handleSecurityAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("Spring Security权限访问异常: {}", e.getMessage());
        
        String friendlyMessage = "权限不足，无法访问此功能";
        return Result.error(ErrorCode.FORBIDDEN.getCode(), friendlyMessage);
    }

    /**
     * 处理文件上传大小超出限制异常
     * 
     * @param e 文件上传大小异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超出Spring Boot限制: {}", e.getMessage());
        return Result.error(ErrorCode.FILE_TOO_LARGE.getCode(), "文件大小超出系统限制，请选择更小的文件");
    }

    /**
     * 处理系统未知异常
     * 兜底异常处理，确保所有未捕获的异常都能被妥善处理
     * 
     * @param e 系统异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e); // 记录完整堆栈信息
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }
}
