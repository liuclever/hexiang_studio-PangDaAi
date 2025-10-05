package com.back_hexiang_studio.GlobalException;

/**
 * 统一错误码枚举
 * 遵循HTTP状态码规范，便于前后端统一处理
 * 
 * @author wenhan
 * @date 2024/09/27
 */
public enum ErrorCode {
    
    // ==================== 成功状态 ====================
    SUCCESS(200, "操作成功"),
    
    // ==================== 客户端错误 4xx ====================
    // 参数相关 400x
    PARAM_ERROR(4000, "请求参数错误"),
    PARAM_MISSING(4001, "必要参数缺失"),
    PARAM_FORMAT_ERROR(4002, "参数格式错误"),
    PARAM_VALIDATION_FAILED(4003, "参数校验失败"),
    
    // 认证相关 401x
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    TOKEN_INVALID(4011, "令牌无效"),
    TOKEN_EXPIRED(4012, "令牌已过期"),
    TOKEN_MISSING(4013, "缺少访问令牌"),
    LOGIN_FAILED(4014, "用户名或密码错误"),
    ACCOUNT_DISABLED(4015, "账号已被禁用"),
    ACCOUNT_LOCKED(4016, "账号已被锁定"),
    
    // 权限相关 403x
    FORBIDDEN(4030, "权限不足"),
    PERMISSION_DENIED(4031, "操作权限不足"),
    ROLE_ACCESS_DENIED(4032, "角色权限不足"),
    RESOURCE_ACCESS_DENIED(4033, "资源访问权限不足"),
    
    // 资源相关 404x
    NOT_FOUND(4040, "请求的资源不存在"),
    USER_NOT_FOUND(4041, "用户不存在"),
    RESOURCE_NOT_FOUND(4042, "资源不存在"),
    API_NOT_FOUND(4043, "接口不存在"),
    
    // 请求冲突 409x
    CONFLICT(4090, "请求冲突"),
    DATA_CONFLICT(4091, "数据冲突"),
    USERNAME_EXISTS(4092, "用户名已存在"),
    EMAIL_EXISTS(4093, "邮箱已存在"),
    
    // 请求过于频繁 429x
    TOO_MANY_REQUESTS(4290, "请求过于频繁"),
    RATE_LIMIT_EXCEEDED(4291, "访问频率超出限制"),
    
    // ==================== 业务错误 45xx ====================
    BUSINESS_ERROR(4500, "业务处理失败"),
    BUSINESS_VALIDATION_FAILED(4501, "业务规则校验失败"),
    BUSINESS_STATE_ERROR(4502, "业务状态错误"),
    OPERATION_NOT_ALLOWED(4503, "当前状态不允许此操作"),
    
    // 文件相关 46xx
    FILE_ERROR(4600, "文件处理失败"),
    FILE_TOO_LARGE(4601, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(4602, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(4603, "文件上传失败"),
    FILE_NOT_FOUND(4604, "文件不存在"),
    
    // ==================== 服务器错误 5xx ====================
    // 系统级错误 500x
    SYSTEM_ERROR(5000, "系统内部错误"),
    SERVICE_UNAVAILABLE(5001, "服务暂时不可用"),
    TIMEOUT_ERROR(5002, "请求超时"),
    NETWORK_ERROR(5003, "网络连接异常"),
    
    // 数据库相关 501x
    DATABASE_ERROR(5010, "数据库访问异常"),
    DATABASE_CONNECTION_FAILED(5011, "数据库连接失败"),
    DATABASE_OPERATION_FAILED(5012, "数据库操作失败"),
    DATA_INTEGRITY_VIOLATION(5013, "数据完整性约束违反"),
    
    // 外部服务相关 502x
    EXTERNAL_SERVICE_ERROR(5020, "外部服务调用失败"),
    THIRD_PARTY_API_ERROR(5021, "第三方API调用异常"),
    PAYMENT_SERVICE_ERROR(5022, "支付服务异常"),
    
    // 缓存相关 503x
    CACHE_ERROR(5030, "缓存服务异常"),
    REDIS_CONNECTION_FAILED(5031, "Redis连接失败"),
    
    // 消息队列相关 504x
    MQ_ERROR(5040, "消息队列异常"),
    MESSAGE_SEND_FAILED(5041, "消息发送失败");

    /**
     * 错误码
     */
    private final int code;
    
    /**
     * 错误描述
     */
    private final String message;

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误描述
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 获取错误描述
     * 
     * @return 错误描述
     */
    public String getMessage() {
        return message;
    }
}
