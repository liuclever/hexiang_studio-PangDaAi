package com.back_hexiang_studio.constants;

/**
 * 缓存常量类
 *
 * 设计原则：
 * 1. 统一管理所有Redis Key前缀
 * 2. 避免Key冲突和命名混乱
 * 3. 支持分环境配置
 * 4. 便于监控和调试
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
public class CacheConstants {

    /**
     *  验证码
     * 格式：captcha:sessionId
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /**
     *  登录失败计数
     * 格式：login_fail:username
     */
    public static final String LOGIN_FAIL_PREFIX = "login_fail:";

    /**
     *  账户锁定
     * 格式：account_lock:username
     */
    public static final String ACCOUNT_LOCK_PREFIX = "account_lock:";

    /**
     * 生成验证码缓存Key
     * @param sessionId 会话ID
     * @return 完整的缓存Key
     */
    public static String captchaKey(String sessionId) {
        return CAPTCHA_PREFIX + sessionId;
    }

    /**
     * 生成登录失败计数Key
     * @param username 用户名
     * @return 完整的缓存Key
     */
    public static String loginFailKey(String username) {
        return LOGIN_FAIL_PREFIX + username;
    }



    /**
     * 生成账户锁定缓存Key
     * @param username
     * @return
     */
    public static String accountLockKey(String username) {
        return ACCOUNT_LOCK_PREFIX + username;
    }


    /**
     * 验证码失败计数Key匹配模式
     * 格式：captcha_fail:*
     */
    public static final String CAPTCHA_FAIL_PATTERN = "captcha_fail:*";

}