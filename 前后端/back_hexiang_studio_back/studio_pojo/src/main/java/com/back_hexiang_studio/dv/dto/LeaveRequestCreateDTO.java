package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请创建 DTO
 * 对应数据库 leave_request 表
 */
@Data
public class LeaveRequestCreateDTO {
    
    /**
     * 申请人用户ID (由系统自动设置)
     */
    private Long applicantId;
    
    /**
     * 学生ID (由系统根据用户ID查询设置)
     */
    private Long studentId;
    
    /**
     * 关联的考勤计划ID
     */
    private Long attendancePlanId;
    
    /**
     * 请假类型 (如: sick_leave-病假, personal_leave-事假)
     */
    private String type;
    
    /**
     * 请假开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 请假结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 请假原因
     */
    private String reason;
    
    /**
     * 备注信息 (已移除，remark字段用于审批人填写)
     */
    // private String remark;
    
    /**
     * 附件文件URL列表 (对应数据库的JSON字段)
     */
    private List<String> attachments;
    
    /**
     * 紧急联系人
     */
    private String emergencyContact;
    
    /**
     * 紧急联系人电话
     */
    private String emergencyPhone;
    
    /**
     * 是否紧急申请
     */
    private Boolean isUrgent = false;
} 