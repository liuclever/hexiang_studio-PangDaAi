package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 活动预约实体类
 */
@Data
public class ActivityReservation {
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
     * 预约状态：reserved-已预约, cancelled-已取消
     */
    private String status;
    
    /**
     * 预约时间
     */
    private LocalDateTime reservationTime;
    
    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 