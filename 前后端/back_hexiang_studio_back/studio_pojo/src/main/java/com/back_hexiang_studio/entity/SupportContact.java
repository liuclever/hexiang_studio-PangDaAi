package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SupportContact {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String position;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 