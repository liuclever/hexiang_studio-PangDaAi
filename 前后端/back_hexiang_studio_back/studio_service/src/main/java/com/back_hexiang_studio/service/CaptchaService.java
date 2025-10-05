package com.back_hexiang_studio.service;


import com.back_hexiang_studio.dv.vo.CaptchaVo;
import com.back_hexiang_studio.result.Result;

/**
 * 验证码服务接口
 *
 * @author wenhan
 * @since 1.0.0
 */
public interface CaptchaService {

    /**
     * 生成图形验证码
     *
     * - 自动生成唯一SessionId
     * - Redis缓存with TTL
     *
     * @return Result<CaptchaVo> 验证码响应对象，包含图片Base64和会话信息
     */
    Result<CaptchaVo> generateCaptcha();

    /**
     * 验证验证码有效性
     *
     * - 验证后自动清理缓存
     * - 防暴力破解计数
     * - 安全事件记录
     * - IP黑名单检查
     *
     * @param sessionId 验证码会话ID，不能为空
     * @param captcha 用户输入的验证码，不能为空且长度为4位
     * @return Result<Boolean> 验证结果，true表示验证成功
     */
    Result<Boolean> validateCaptcha(String sessionId, String captcha);

    /**
     * 刷新验证码
     *
     * - 保持原SessionId不变
     * - 重置验证码内容和有效期
     * - 记录刷新行为
     *
     * @param sessionId 原验证码会话ID，不能为空
     * @return Result<CaptchaVo> 新的验证码响应对象
     */
    Result<CaptchaVo> refreshCaptcha(String sessionId);

    /**
     * 清理过期验证码（定时任务调用）
     *
     *  
     * - 批量清理过期数据
     * - 性能监控统计
     * - 异常容错处理
     *
     * @return Result<Integer> 清理的验证码数量统计
     */
    Result<Integer> cleanExpiredCaptcha();
}