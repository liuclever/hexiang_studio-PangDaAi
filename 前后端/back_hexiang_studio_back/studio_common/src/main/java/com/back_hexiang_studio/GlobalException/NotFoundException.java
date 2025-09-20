package com.back_hexiang_studio.GlobalException;

//资源不存在
public class NotFoundException extends  BaseException{
    public NotFoundException(String message) {
        super(message);
    }
}
