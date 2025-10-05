package com.back_hexiang_studio.service;

/**
 * 邮件服务接口 - 企业级设计
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
public interface EmailService {

    /**
     * 发送账户锁定安全警告邮件
     *
     * @param username 被锁定的用户名
     * @param clientIp 客户端IP
     * @param lockTime 锁定时间（分钟）
     * @return 发送结果
     */
    boolean sendAccountLockAlert(String username, String clientIp, int lockTime);

    /**
     * 发送登录异常警告邮件
     *
     * @param username 用户名
     * @param clientIp 客户端IP
     * @param failCount 失败次数
     * @return 发送结果
     */
    boolean sendLoginFailureAlert(String username, String clientIp, int failCount);
}