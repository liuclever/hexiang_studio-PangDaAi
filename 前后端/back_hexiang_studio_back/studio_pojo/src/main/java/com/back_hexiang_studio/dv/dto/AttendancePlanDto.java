package com.back_hexiang_studio.dv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考勤计划DTO
 */
@Data
public class AttendancePlanDto {
    /**
     * 计划ID
     */
    private Long planId;
    
    /**
     * 类型：activity-活动考勤,course-课程考勤,duty-值班考勤
     */
    private String type;
    
    /**
     * 考勤名称
     */
    private String name;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 地点
     */
    private String location;
    
    /**
     * 纬度
     */
    private Double locationLat;
    
    /**
     * 经度
     */
    private Double locationLng;
    
    /**
     * 签到有效半径(米)
     */
    private Integer radius;
    
    /**
     * 关联课程ID(课程考勤专用)
     */
    private Long courseId;
    
    /**
     * 备注
     */
    private String note;
    
    /**
     * 状态：1-有效，0-已取消
     */
    private Integer status;
    
    /**
     * 创建人ID
     */
    private Long createUser;
    /**
     * 值班安排id
     */
    private Long scheduleId;
    
    /**
     * 常用地点ID，如果使用常用地点则填写此字段
     */
    private Integer commonLocationId;
} 