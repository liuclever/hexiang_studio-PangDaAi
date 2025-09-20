package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 修改密码的数据传输对象
 */
@Data
public class ChangePasswordDto {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 旧密码
     */
    private String oldPassword;
    
    /**
     * 新密码
     */
    private String newPassword;
} 