package com.back_hexiang_studio.constants;

/**
 * 安全配置常量
 *
 * @author wenhan
 * @since 1.0.0
 */
public class SecurityConstants {

    /**
     * 最大登录失败次数
     */
    public static final int MAX_LOGIN_FAIL_ATTEMPTS = 5;

    /**
     * 出现验证码的失败次数阈值
     */
    public static final int CAPTCHA_THRESHOLD = 3;

    /**
     * 账户锁定时长（秒）- 15分钟
     */
    public static final int ACCOUNT_LOCK_DURATION = 15 * 60;

    /**
     * 记住密码有效期（秒）- 7天
     */
    public static final int REMEMBER_ME_DURATION = 5 * 24 * 60 * 60;

    /**
     * 登录失败重置时间（秒）- 1小时
     */
    public static final int FAIL_COUNT_RESET_TIME = 60 * 60;


}