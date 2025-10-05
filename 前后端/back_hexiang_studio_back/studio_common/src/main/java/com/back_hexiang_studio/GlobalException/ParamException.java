package com.back_hexiang_studio.GlobalException;

/**
 * 参数异常类
 * 
 * 用于处理请求参数相关的异常，如参数缺失、格式错误、校验失败等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class ParamException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造参数异常
     * 
     * @param errorCode 错误码枚举
     */
    public ParamException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造参数异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public ParamException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造参数异常，使用默认参数错误码
     * 
     * @param message 错误消息
     */
    public ParamException(String message) {
        super(ErrorCode.PARAM_ERROR, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造参数异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public ParamException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
