package com.back_hexiang_studio.dv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDto {
    private String userName;
    private String password;
    /**
     * 验证码会话ID
     * 当需要验证码时必填
     */
    private String captchaSessionId;

    /**
     * 用户输入的验证码
     * 当需要验证码时必填
     */
    private String captchaCode;

    /**
     * 是否记住我
     * true: 30天免登录，false: 关闭浏览器需重新登录
     */
    private Boolean rememberMe = false;

}
