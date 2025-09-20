package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserHonorVo {
    private Long id;
    private Long user_id;
    private String honor_name;
    private String honor_level;
    private String issue_org;
    private LocalDate issue_date;
    private String certificate_no;
    private String description;
    private String attachment;
    private LocalDateTime create_time;
    private String create_user;
    private LocalDateTime update_time;
    private String update_user;
} 