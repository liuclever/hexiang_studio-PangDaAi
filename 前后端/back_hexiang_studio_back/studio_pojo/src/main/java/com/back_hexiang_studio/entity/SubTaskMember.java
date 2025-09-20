package com.back_hexiang_studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubTaskMember {
    private Long id;
    private Long subTaskId;
    private Long userId;
    private String role;
    private String note;
    private LocalDateTime joinTime;
}
