package com.back_hexiang_studio.dv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 值班记录数据传输对象
 */
@Data
public class DutyRecordDto {
    
    private Long recordId;
    private Long scheduleId;
    private Long studentId;
    private String studentName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date checkInTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date checkOutTime;
    
    private String status;
    private String location;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String remark;
    
    // 关联的值班安排信息
    private String dutyName;
    private String dutyLocation;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    
    private String date; // 日期字符串，格式为 yyyy-MM-dd，方便前端使用
    private String timeSlot; // 时间段，用于前端展示
} 