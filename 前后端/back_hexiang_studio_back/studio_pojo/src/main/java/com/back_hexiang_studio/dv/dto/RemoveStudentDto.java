package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 从课程中移除学生的DTO
 */
@Data
public class RemoveStudentDto {
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 学生ID
     */
    private Long studentId;
} 