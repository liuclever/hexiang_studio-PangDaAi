package com.back_hexiang_studio.GlobalException;

/**
 * 基础异常类
 * 
 * 所有自定义异常的基础类，提供统一的错误码和消息处理机制
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public class BaseException extends RuntimeException {
    
    /**
     * 错误码，默认为系统错误
     */
    private final Integer code;
    
    /**
     * 错误消息
     */
    private final String errorMessage;

    /**
     * 使用ErrorCode枚举构造异常
     * 
     * @param errorCode 错误码枚举
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    /**
     * 使用ErrorCode枚举和自定义消息构造异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.errorMessage = customMessage;
    }

    /**
     * 使用错误码和消息构造异常
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorMessage = message;
    }

    /**
     * 使用消息构造异常，使用默认错误码
     * 
     * @param message 错误消息
     */
    public BaseException(String message) {
        super(message);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.errorMessage = message;
    }

    /**
     * 使用ErrorCode枚举和异常原因构造异常
     * 
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    /**
     * 使用ErrorCode枚举、自定义消息和异常原因构造异常
     * 
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     * @param cause 异常原因
     */
    public BaseException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.code = errorCode.getCode();
        this.errorMessage = customMessage;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取错误消息
     * 注意：不覆盖getMessage()方法，保持异常链的完整性
     * 
     * @return 错误消息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
