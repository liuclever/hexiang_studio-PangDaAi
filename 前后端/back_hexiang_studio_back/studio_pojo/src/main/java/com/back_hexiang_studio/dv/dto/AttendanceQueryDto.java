package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤查询参数DTO
 */
@Data
public class AttendanceQueryDto {
    /**
     * 当前页
     */
    private Integer page=1;
    
    /**
     * 每页数量
     */
    private Integer pageSize=10;
    
    /**
     * 考勤类型
     */
    private String type;
    
    /**
     * 关键字 (用于模糊查询)
     */
    private String keyword;
    
    /**
     * 课程名称关键词
     */
    private String courseName;
    
    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    /**
     * 考勤计划ID
     */
    private Long planId;
    
    /**
     * 学生姓名
     */
    private String studentName;
    
    /**
     * 学生ID (用于学生端查询过滤)
     */
    private Long studentId;
    
    /**
     * 签到状态
     */
    private String status;

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getStatus() {
        return status;
    }
} 