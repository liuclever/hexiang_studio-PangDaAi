package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import java.util.List;

/**
 * 值班安排批量同步DTO
 * 用于前端按格子同步值班安排数据
 */
@Data
public class DutyScheduleSyncDto {
    /**
     * 值班日期，格式：yyyy-MM-dd
     */
    private String dutyDate;
    
    /**
     * 时间段，格式：HH:mm-HH:mm，如：08:30-10:20
     */
    private String timeSlot;
    
    /**
     * 值班地点
     */
    private String location;
    
    /**
     * 值班名称
     */
    private String dutyName;
    
    /**
     * 学生ID列表
     * 包含所有应该在此时间段值班的学生ID
     */
    private List<Long> studentIds;
} 