package com.back_hexiang_studio.GlobalException;

/**
 * 认证异常类
 * 
 * 用于处理用户认证相关的异常，如登录失败、令牌无效等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class AuthException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造认证异常
     * 
     * @param errorCode 错误码枚举
     */
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造认证异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public AuthException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用错误码和消息构造认证异常
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public AuthException(int code, String message) {
        super(code, message);
    }
    
    /**
     * 使用自定义消息构造认证异常，使用默认未授权错误码
     * 
     * @param message 错误消息
     */
    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造认证异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
