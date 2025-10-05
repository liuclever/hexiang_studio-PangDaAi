package com.back_hexiang_studio.GlobalException;

/**
 * 数据库异常类
 * 
 * 用于处理数据库访问相关的异常，如连接失败、操作失败、数据完整性违反等
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class DatabaseException extends BaseException {
    
    /**
     * 使用ErrorCode枚举构造数据库异常
     * 
     * @param errorCode 错误码枚举
     */
    public DatabaseException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 使用ErrorCode枚举和自定义消息构造数据库异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public DatabaseException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    /**
     * 使用自定义消息构造数据库异常，使用默认数据库错误码
     * 
     * @param message 错误消息
     */
    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR, message);
    }
    
    /**
     * 使用ErrorCode枚举和异常原因构造数据库异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public DatabaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
