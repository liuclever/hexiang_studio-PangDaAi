package com.back_hexiang_studio.GlobalException;

/**
 * 系统异常类
 * 
 * 用于处理系统级别的异常，如服务不可用、网络超时、系统内部错误等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class SystemException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造系统异常
     * 
     * @param errorCode 错误码枚举
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造系统异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public SystemException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造系统异常，使用默认系统错误码
     * 
     * @param message 错误消息
     */
    public SystemException(String message) {
        super(ErrorCode.SYSTEM_ERROR, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造系统异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
