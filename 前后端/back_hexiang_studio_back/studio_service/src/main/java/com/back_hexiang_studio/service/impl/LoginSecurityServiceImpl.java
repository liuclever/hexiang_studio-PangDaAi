package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.cache.RedisCache;
import com.back_hexiang_studio.constants.CacheConstants;
import com.back_hexiang_studio.constants.SecurityConstants;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.EmailService;
import com.back_hexiang_studio.service. LoginSecurityService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录安全服务实现 - 企业级设计
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
@Slf4j
@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {


    @Autowired
    private RedisCache redisCache;

    @Autowired
    private EmailService emailService;

    //
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 检查用户是否需要验证码
     * @param username 用户名
     * @return
     */
    @Override
    public boolean requiresCaptcha(String username) {
        // 检查登录失败次数，失败3次或以上需要验证码
        String failCountKey = CacheConstants.loginFailKey(username);
        Integer failCount = redisCache.getCacheObject(failCountKey);
        
        boolean needsCaptcha = failCount != null && failCount >= SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS;
        log.info("验证码需求检查 - Username: {}, FailCount: {}, NeedsCaptcha: {}", 
                username, failCount, needsCaptcha);
        
        return needsCaptcha;
    }

    /**
     * 检查账户是否被锁定
     * @param username 用户名
     * @return
     */
    @Override
    public Result<Boolean> checkAccountLocked(String username) {
        log.info("检查账户锁定状态 - Username: {}", username);

        try {
            String lockKey = CacheConstants.accountLockKey(username);
            Boolean isLocked = redisCache.getCacheObject(lockKey);

            if (isLocked != null && isLocked) {
                // 获取锁定剩余时间
                Long ttl = redisTemplate.getExpire(lockKey);
                int remainingMinutes = ttl != null ? (int)(ttl / 60) : 0;

                log.warn("账户已被锁定 - Username: {}, 剩余时间: {}分钟", username, remainingMinutes);
                return Result.error(String.format("账户已被锁定，请%d分钟后重试", remainingMinutes));
            }

            log.info("账户状态正常 - Username: {}", username);
            return Result.success(false, "账户状态正常");

        } catch (Exception e) {
            log.error("检查账户锁定状态异常 - Username: {}", username, e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    @Override
    public Result<String> recordLoginFailure(String username, String clientIp) {
        log.info("记录登录失败 - Username: {}, IP: {}", username, clientIp);

        try {
            String failCountKey = CacheConstants.loginFailKey(username);
            Integer failCount = redisCache.getCacheObject(failCountKey);
            failCount = (failCount == null) ? 1 : failCount + 1;

            // 更新失败计数（1小时过期）
            redisCache.setCacheObject(failCountKey, failCount, SecurityConstants.FAIL_COUNT_RESET_TIME);

            log.warn("登录失败记录更新 - Username: {}, 当前失败次数: {}", username, failCount);

            // 检查是否达到锁定阈值
            if (failCount >= SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS) {
                // 锁定账户
                String lockKey = CacheConstants.accountLockKey(username);
                redisCache.setCacheObject(lockKey, true, SecurityConstants.ACCOUNT_LOCK_DURATION);

                log.warn("账户已锁定 - Username: {}, 锁定时长: {}分钟",
                        username, SecurityConstants.ACCOUNT_LOCK_DURATION / 60);

                // 发送邮件通知
                try {
                    emailService.sendAccountLockAlert(username, clientIp, SecurityConstants.ACCOUNT_LOCK_DURATION / 60);
                } catch (Exception e) {
                    log.error("发送锁定通知邮件失败", e);
                }

                return Result.error(String.format("登录失败次数过多，账户已锁定%d分钟",
                        SecurityConstants.ACCOUNT_LOCK_DURATION / 60));

            } else if (failCount >= SecurityConstants.CAPTCHA_THRESHOLD) {
                // 达到验证码阈值，但未锁定
                return Result.error(String.format("登录失败%d次，请输入验证码", failCount));

            } else {
                // 普通登录失败
                return Result.error(String.format("用户名或密码错误，剩余尝试次数：%d次",
                        SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS - failCount));
            }

        } catch (Exception e) {
            log.error("记录登录失败异常 - Username: {}", username, e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    @Override
    public void clearLoginFailures(String username) {
        log.info("清除登录失败记录 - Username: {}", username);

        try {
            String failCountKey = CacheConstants.loginFailKey(username);
            String lockKey = CacheConstants.accountLockKey(username);

            // 清除失败计数和锁定状态
            redisCache.deleteObject(failCountKey);
            redisCache.deleteObject(lockKey);

            log.info("登录失败记录已清除 - Username: {}", username);

        } catch (Exception e) {
            log.error("清除登录失败记录异常 - Username: {}", username, e);
        }
    }

    @Override
    public Result<Boolean> unlockAccount(String username) {
        log.info("手动解锁账户 - Username: {}", username);

        try {
            String lockKey = CacheConstants.accountLockKey(username);
            String failCountKey = CacheConstants.loginFailKey(username);

            // 删除锁定状态和失败计数
            redisCache.deleteObject(lockKey);
            redisCache.deleteObject(failCountKey);

            log.info("账户解锁成功 - Username: {}", username);
            return Result.success(true, "账户解锁成功");

        } catch (Exception e) {
            log.error("解锁账户异常 - Username: {}", username, e);
            return Result.error("解锁失败，请稍后重试");
        }
    }
}






