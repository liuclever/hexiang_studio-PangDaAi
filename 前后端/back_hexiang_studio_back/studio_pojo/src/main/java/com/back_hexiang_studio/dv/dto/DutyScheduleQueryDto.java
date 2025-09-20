package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 值班安排查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DutyScheduleQueryDto extends PageDto {
    /**
     * 值班名称关键词
     */
    private String keyword;
    
    /**
     * 值班地点
     */
    private String location;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 开始时间 (ISO格式字符串: yyyy-MM-dd'T'HH:mm:ss)
     */
    private String startTime;
    
    /**
     * 结束时间 (ISO格式字符串: yyyy-MM-dd'T'HH:mm:ss)
     */
    private String endTime;
    
    /**
     * 开始日期 (用于日期筛选: yyyy-MM-dd)
     */
    private String startDate;
    
    /**
     * 结束日期 (用于日期筛选: yyyy-MM-dd)
     */
    private String endDate;
    
    /**
     * 重复类型
     */
    private String repeatType;
    
    /**
     * 值班状态：1-有效，0-已取消
     */
    private Integer dutyStatus;

} 