package com.back_hexiang_studio.dv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动预约DTO
 */
@Data
public class ActivityReservationDto {
    /**
     * 预约ID
     */
    private Long reservationId;
    
    /**
     * 活动计划ID
     */
    private Long planId;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 学生姓名（查询时使用）
     */
    private String studentName;
    
    /**
     * 学生学号（查询时使用）
     */
    private String studentNumber;
    
    /**
     * 预约状态：reserved-已预约, cancelled-已取消
     */
    private String status;
    
    /**
     * 预约时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reservationTime;
    
    /**
     * 取消时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 批量预约时使用
    /**
     * 学生ID列表（批量预约时使用）
     */
    private List<Long> studentIds;
    
    /**
     * 活动名称（查询时使用）
     */
    private String activityName;
} 