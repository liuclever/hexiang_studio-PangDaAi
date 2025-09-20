package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DutyScheduleStudent {
    private Long id;              // 主键ID
    private Long scheduleId;      // 值班安排ID
    private Long studentId;       // 学生ID
    private String status;        // 状态：normal-正常,cancelled-已取消
    private LocalDateTime createTime; // 创建时间
    private Long createUser;      // 创建人ID
} 