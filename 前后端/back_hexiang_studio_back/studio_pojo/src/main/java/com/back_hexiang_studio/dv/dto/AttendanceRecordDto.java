package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考勤记录DTO
 */
@Data
public class AttendanceRecordDto {
    /**
     * 记录ID
     */
    private Long recordId;
    
    /**
     * 考勤计划ID
     */
    private Long planId;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 学生姓名（查询用）
     */
    private String studentName;
    
    /**
     * 签到状态：present-已签到,late-迟到,absent-缺勤,leave-请假
     */
    private String status;
    
    /**
     * 签到时间
     */
    private LocalDateTime signInTime;
    
    /**
     * 签到位置
     */
    private String location;
    
    /**
     * 签到纬度
     */
    private Double locationLat;
    
    /**
     * 签到经度
     */
    private Double locationLng;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 最后修改人ID
     */
    private Long updateUser;
} 