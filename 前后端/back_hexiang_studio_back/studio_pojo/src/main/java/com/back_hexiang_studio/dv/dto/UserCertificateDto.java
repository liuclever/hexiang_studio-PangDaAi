package com.back_hexiang_studio.dv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCertificateDto {
    private Long id;
    private Long userId;
    private String certificateName;
    private String certificateLevel;
    private String issueOrg;
    private String issueDate;
    private String expiryDate;
    private String certificateNo;
    private String description;
    private String verificationUrl;
    private Integer status;
    private String attachment;
} 