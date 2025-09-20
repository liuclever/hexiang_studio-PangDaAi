package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import java.util.List;

/**
 * 值班安排批量分配学生DTO
 */
@Data
public class DutyScheduleStudentBatchDto {
    /**
     * 值班安排ID
     */
    private Long scheduleId;
    
    /**
     * 学生ID列表
     */
    private List<Long> studentIds;
} 