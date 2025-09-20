package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import java.util.List;

/**
 * 值班安排与学生关联DTO
 */
@Data
public class DutyScheduleStudentDto {
    /**
     * ID
     */
    private Long id;
    
    /**
     * 值班安排ID
     */
    private Long scheduleId;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 学生姓名
     */
    private String studentName;
    
    /**
     * 学生学号
     */
    private String studentNumber;
    
    /**
     * 专业班级
     */
    private String majorClass;
}

 