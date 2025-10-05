package com.back_hexiang_studio.GlobalException;

/**
 * 资源不存在异常类
 * 
 * 用于处理资源查找失败的异常，如用户不存在、文件不存在、数据记录不存在等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class NotFoundException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造资源不存在异常
     * 
     * @param errorCode 错误码枚举
     */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造资源不存在异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public NotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造资源不存在异常，使用默认资源不存在错误码
     * 
     * @param message 错误消息
     */
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造资源不存在异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public NotFoundException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
