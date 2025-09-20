package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 值班安排DTO
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DutyScheduleDto {
    /**
     * 值班安排ID
     */
    private Long scheduleId;
    
    /**
     * 值班学生ID
     */
    private Long studentId;
    
    /**
     * 值班名称
     */
    private String dutyName;
    
    /**
     * 值班地点
     */
    private String location;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 重复类型：once-单次, daily-每天, weekly-每周
     */
    private String repeatType;
    
    /**
     * 状态：1-有效，0-已取消
     */
    private Integer status;
} 