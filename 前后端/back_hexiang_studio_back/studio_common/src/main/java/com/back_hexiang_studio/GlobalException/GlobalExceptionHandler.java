package com.back_hexiang_studio.GlobalException;


import com.back_hexiang_studio.result.Result;
import lombok.extern.slf4j.Slf4j;


import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;

/**
 * 全局异常类
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //手动抛出
    @ExceptionHandler(BaseException.class)
    public Result<?> handleException(BaseException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    //请求参数异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    // 文件访问权限异常
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleFileAccessDeniedException(AccessDeniedException e) {
        log.warn("文件访问权限异常: {}", e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN.getCode(), "文件访问权限不足，请联系管理员");
    }

    // Spring Security权限访问异常
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<?> handleSecurityAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("Spring Security权限访问异常: {}", e.getMessage());
        
        // 🎯 提供更友好的权限提示信息
        String friendlyMessage = "⚠️ 权限不足\n\n" +
                                "🔒 您当前的权限级别无法访问此功能\n" +
                                "📞 如需使用此功能，请联系管理员提升权限\n\n" +
                                "💡 提示：不同角色具有不同的操作权限：\n" +
                                "• 学员：查看个人信息、课程等\n" +
                                "• 老师：管理课程、查看学生信息\n" +
                                "• 管理员：用户管理、系统配置\n" +
                                "• 超级管理员：全部权限";
        
        return Result.error(ErrorCode.FORBIDDEN.getCode(), friendlyMessage);
    }

    //    //系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e); // 记录完整堆栈
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常");
    }

    // 文件大小超出限制异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超出Spring Boot限制: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), "文件大小超出系统限制，请选择更小的文件");
    }
}
