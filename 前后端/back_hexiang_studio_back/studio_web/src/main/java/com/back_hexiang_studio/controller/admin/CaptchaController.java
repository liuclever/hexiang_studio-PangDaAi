package com.back_hexiang_studio.controller.admin;


import com.back_hexiang_studio.dv.dto.CaptchaRefreshDto;
import com.back_hexiang_studio.dv.dto.CaptchaValidateDto;
import com.back_hexiang_studio.dv.vo.CaptchaVo;
import com.back_hexiang_studio.result.Result;

import com.back_hexiang_studio.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * 验证码管理接口 - 企业级设计
 *
 * 功能特性：
 * 1. RESTful API设计标准
 * 2. 统一的参数验证机制
 * 3. 完整的日志记录体系
 * 4. 标准化的响应格式
 * 5. 清晰的API文档注释
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/admin/captcha")

public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 生成验证码
     *
     * @return 验证码响应对象，包含图片Base64和会话信息
     */
    @PostMapping("/generate")
    public Result<CaptchaVo> generateCaptcha() {
        log.info("接收生成验证码请求");

        try {
            Result<CaptchaVo> result = captchaService.generateCaptcha();

            if (result.getCode() == 200) {
                log.info("验证码生成成功 - SessionId: {}", result.getData().getSessionId());
            } else {
                log.warn("验证码生成失败 - 原因: {}", result.getMsg());
            }

            return result;

        } catch (Exception e) {
            log.error("验证码生成接口异常", e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 验证验证码
     *
     * @param validateDto 验证请求参数
     * @return 验证结果，true表示验证成功
     */
    @PostMapping("/validate")
    public Result<Boolean> validateCaptcha(@Valid @RequestBody CaptchaValidateDto validateDto) {
        log.info("接收验证码验证请求 - SessionId: {}", validateDto.getSessionId());

        try {
            Result<Boolean> result = captchaService.validateCaptcha(
                    validateDto.getSessionId(),
                    validateDto.getCaptcha()
            );

            if (result.getCode() == 200 && result.getData()) {
                log.info("验证码验证成功 - SessionId: {}", validateDto.getSessionId());
            } else {
                log.warn("验证码验证失败 - SessionId: {}, 原因: {}",
                        validateDto.getSessionId(), result.getMsg());
            }

            return result;

        } catch (Exception e) {
            log.error("验证码验证接口异常 - SessionId: {}", validateDto.getSessionId(), e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 刷新验证码
     *
     *
     * @param refreshDto 刷新请求参数
     * @return 新的验证码响应对象
     */
    @PostMapping("/refresh")
    public Result<CaptchaVo> refreshCaptcha(@Valid @RequestBody CaptchaRefreshDto refreshDto) {
        log.info("接收刷新验证码请求 - SessionId: {}", refreshDto.getSessionId());

        try {
            Result<CaptchaVo> result = captchaService.refreshCaptcha(refreshDto.getSessionId());

            if (result.getCode() == 200) {
                log.info("验证码刷新成功 - SessionId: {}", refreshDto.getSessionId());
            } else {
                log.warn("验证码刷新失败 - SessionId: {}, 原因: {}",
                        refreshDto.getSessionId(), result.getMsg());
            }

            return result;

        } catch (Exception e) {
            log.error("验证码刷新接口异常 - SessionId: {}", refreshDto.getSessionId(), e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 清理过期验证码（管理员接口）
     */
    @DeleteMapping("/clean")  //
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> cleanExpiredCaptcha() {
        log.info("接收清理过期验证码请求");

        try {
            Result<Integer> result = captchaService.cleanExpiredCaptcha();

            if (result.getCode() == 200) {
                log.info("过期验证码清理成功 - 清理数量: {}", result.getData());
            } else {
                log.warn("过期验证码清理失败 - 原因: {}", result.getMsg());
            }

            return result;

        } catch (Exception e) {
            log.error("清理过期验证码接口异常", e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }

}