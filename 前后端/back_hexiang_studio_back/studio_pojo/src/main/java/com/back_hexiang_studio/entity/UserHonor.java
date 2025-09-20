package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserHonor implements Serializable {
    /**
     * 荣誉ID
     */
    private Long honorsId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 荣誉名称
     */
    private String honorName;

    /**
     * 荣誉级别
     */
    private String honorLevel;

    /**
     * 颁发机构
     */
    private String issueOrg;

    /**
     * 颁发日期
     */
    private Date issueDate;

    /**
     * 证书编号
     */
    private String certificateNo;

    /**
     * 描述
     */
    private String description;

    /**
     * 文件附件的存储路径
     */
    private String attachment;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    private String updateUser;
} 