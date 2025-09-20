package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DutySchedule {
    private Long scheduleId;       // 安排ID
    private String title;          // 值班标题
    private LocalDateTime startTime;   // 开始时间
    private LocalDateTime endTime;     // 结束时间
    private String timeSlot;       // 时间段，例如 "08:00-10:00"
    private String location;       // 值班地点
    private Integer status;        // 状态：1-有效，0-已取消
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
} 