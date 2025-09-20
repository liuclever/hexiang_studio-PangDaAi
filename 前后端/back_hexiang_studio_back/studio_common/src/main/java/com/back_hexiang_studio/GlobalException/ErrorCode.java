package com.back_hexiang_studio.GlobalException;

public enum ErrorCode {
    // 通用
    SUCCESS(2000, "成功"),

    // 参数、业务
    PARAM_ERROR(4000, "请求参数错误"),
    BUSINESS_ERROR(4001, "业务处理失败"),

    // 权限相关
    UNAUTHORIZED(4010, "未登录"),
    FORBIDDEN(4030, "没有权限"),

    // 资源/数据
    NOT_FOUND(4040, "资源不存在"),

    // 系统级
    SYSTEM_ERROR(5000, "系统错误"),
    DATABASE_ERROR(5001, "数据库错误");

    private final int code;
    private final String message;


    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
