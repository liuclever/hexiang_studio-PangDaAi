package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * 请假申请查询参数 DTO
 */
@Data
public class LeaveRequestQueryDTO {
    
    // 默认继承分页参数
    private int page = 1;
    private int pageSize = 10;
    
    /**
     * 申请人ID (用于学生查询自己的申请)
     */
    private Long applicantId;
    
    /**
     * 考勤计划创建者ID (用于老师查询自己创建的考勤计划相关的请假申请)
     */
    private Long creatorId;
    
    /**
     * 学生姓名 (用于管理员搜索)
     */
    private String studentName;
    
    /**
     * 申请状态 (pending, approved, rejected)
     */
    private String status;
    
    /**
     * 请假类型
     */
    private String type;
    
    /**
     * 开始日期
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
} 