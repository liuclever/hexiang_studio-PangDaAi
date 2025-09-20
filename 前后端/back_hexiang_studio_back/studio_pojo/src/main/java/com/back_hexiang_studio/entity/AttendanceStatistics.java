package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceStatistics {
    private Long id;              // 主键ID
    private String type;          // 类型：activity-活动考勤,course-课程考勤,duty-值班考勤
    private LocalDate date;       // 统计日期
    private Integer totalCount;   // 总人数
    private Integer presentCount; // 出勤人数
    private Integer lateCount;    // 迟到人数
    private Integer absentCount;  // 缺勤人数
    private Integer leaveCount;   // 请假人数
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
} 