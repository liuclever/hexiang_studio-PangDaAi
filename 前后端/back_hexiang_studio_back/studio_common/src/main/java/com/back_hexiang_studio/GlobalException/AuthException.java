package com.back_hexiang_studio.GlobalException;

/**
 * 参数异常类
 */
public class AuthException extends  BaseException {
    public AuthException(int code, String message) {
        super(code, message);
    }
}
