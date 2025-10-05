package com.back_hexiang_studio.GlobalException;

/**
 * 权限异常类
 * 
 * 用于处理权限控制相关的异常，如操作权限不足、角色权限不足等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class PermissionException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造权限异常
     * 
     * @param errorCode 错误码枚举
     */
    public PermissionException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造权限异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public PermissionException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造权限异常，使用默认权限不足错误码
     * 
     * @param message 错误消息
     */
    public PermissionException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造权限异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public PermissionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
