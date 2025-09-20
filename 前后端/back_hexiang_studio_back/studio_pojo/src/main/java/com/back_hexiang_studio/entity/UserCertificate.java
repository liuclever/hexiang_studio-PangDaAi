package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserCertificate implements Serializable {

    private Long certificateId;
    private Long userId;
    private String certificateName;
    private String certificateLevel;
    private String certificateNo;
    private String issueOrg;
    private Date issueDate;
    private Date expiryDate;
    private String description;
    private String imageUrl;
    private String originalFileName;
    private String verificationUrl;
    private Integer status;
    private LocalDateTime createTime;
    private String createUser;
    private LocalDateTime updateTime;
    private String updateUser;
} 