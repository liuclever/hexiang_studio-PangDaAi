package com.back_hexiang_studio.dv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserHonorDto {

    private Long honorId;
    private Long userId;
    private String honorName;
    private String honorLevel;
    private String issueOrg;
    private String issueDate; // 将类型从 LocalDate 修改为 String
    private String certificateNo;
    private String description;
    private String attachment;
} 