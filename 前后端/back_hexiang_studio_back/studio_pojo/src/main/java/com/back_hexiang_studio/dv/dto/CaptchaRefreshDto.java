package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 验证码刷新请求DTO - 企业级设计
 *
 * @author wenhan
 * @since 1.0.0
 */
@Data
public class CaptchaRefreshDto {

    /**
     * 验证码会话ID
     * 必填，36位UUID格式
     */
    @NotBlank(message = "验证码会话ID不能为空")
    @Size(min = 32, max = 36, message = "验证码会话ID格式错误")
    private String sessionId;
}