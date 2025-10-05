package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 *
 *
 * @author wenhan
 * @since 1.0.0
 */
@Data
public class CaptchaValidateDto {

    /**
     * 验证码会话ID
     * 必填，36位UUID格式
     */
    @NotBlank(message = "验证码会话ID不能为空")
    @Size(min = 32, max = 36, message = "验证码会话ID格式错误")
    private String sessionId;

    /**
     * 用户输入的验证码
     * 必填，4位数字
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 4, message = "验证码必须为4位")
    private String captcha;
}