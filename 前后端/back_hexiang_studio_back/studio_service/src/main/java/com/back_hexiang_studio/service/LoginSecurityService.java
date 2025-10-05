package com.back_hexiang_studio.service;


import com.back_hexiang_studio.result.Result;

/**
 * 登录安全服务接口
 *
 * 核心功能：
 * 1. 登录失败次数统计和限制
 * 2. 账户锁定和解锁机制
 * 3. 验证码强制验证逻辑
 * 4. 安全事件通知和记录
 */
public interface LoginSecurityService {

    /**
     * 检查用户是否需要验证码
     *
     * 业务逻辑：
     * - 登录失败次数 < 3：不需要验证码
     * - 登录失败次数 ≥ 3：强制验证码
     *
     * @param username 用户名
     * @return 是否需要验证码
     */
    boolean requiresCaptcha(String username);

    /**
     * 检查账户是否被锁定
     *
     * 业务逻辑：
     * - 检查Redis中的锁定状态
     * - 返回锁定剩余时间信息
     *
     * @param username 用户名
     * @return 锁定检查结果
     */
    Result<Boolean> checkAccountLocked(String username);

    /**
     * 记录登录失败
     *
     * 业务逻辑：
     * - 增加失败计数
     * - 检查是否达到锁定阈值
     * - 触发锁定和邮件通知
     *
     * @param username 用户名
     * @param clientIp 客户端IP
     * @return 处理结果，包含是否锁定等信息
     */
    Result<String> recordLoginFailure(String username, String clientIp);

    /**
     * 清除登录失败记录
     *
     * 业务逻辑：
     * - 登录成功后清除失败计数
     * - 解除相关安全限制
     *
     * @param username 用户名
     */
    void clearLoginFailures(String username);

    /**
     * 解锁账户（管理员操作）
     *
     * @param username 用户名
     * @return 解锁结果
     */
    Result<Boolean> unlockAccount(String username);
}