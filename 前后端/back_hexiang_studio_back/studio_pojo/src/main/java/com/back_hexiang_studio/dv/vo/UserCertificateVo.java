package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCertificateVo {
    private Long id;
    private Long userId;
    private String certificateName;
    private String certificateLevel;
    private String issueOrg;
    private LocalDate issueDate;
    private LocalDate expiryDate; // 添加缺失的过期日期字段
    private String certificateNo;
    private String description;
    private String attachment;
    private String verificationUrl; // 添加缺失的验证URL字段
    private LocalDateTime createTime;
    private String createUser;
    private LocalDateTime updateTime;
    private String updateUser;
} 