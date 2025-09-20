package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AttendancePlan {
    private Long planId;          // 计划ID
    private String type;          // 类型：activity-活动考勤,course-课程考勤,duty-值班考勤
    private String name;          // 考勤名称
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
    private String location;      // 地点
    private Double locationLat;   // 纬度
    private Double locationLng;   // 经度
    private Integer radius;       // 签到有效半径(米)
    private Long courseId;        // 关联课程ID(课程考勤专用)
    private String note;          // 备注
    private Integer status;       // 状态：1-有效，0-已取消
    private Long createUser;      // 创建人ID
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Long updateUserId;    // 更新人ID
    private Long scheduleId;      // 关联值班安排ID
    private Boolean processed;    // 是否已处理
  
} 