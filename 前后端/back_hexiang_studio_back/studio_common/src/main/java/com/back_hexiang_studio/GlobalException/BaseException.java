package com.back_hexiang_studio.GlobalException;


import lombok.Data;

/**
 *基础异常类
 * @Author:Hexiang
 * @Date:2022/5/6-5-06-22:04
 */
@Data
public class BaseException extends RuntimeException {
    private Integer code = 500; // 默认错误码
    private String message;

    public BaseException(String message) {
        super(message);
        this.message = message;
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
