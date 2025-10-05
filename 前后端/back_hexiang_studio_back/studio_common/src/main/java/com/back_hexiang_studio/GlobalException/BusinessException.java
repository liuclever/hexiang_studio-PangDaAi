package com.back_hexiang_studio.GlobalException;

/**
 * 业务异常类
 * 
 * 用于处理业务逻辑相关的异常，如业务规则校验失败、业务状态错误等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class BusinessException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造业务异常
     * 
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造业务异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造业务异常，使用默认业务错误码
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_ERROR, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造业务异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
