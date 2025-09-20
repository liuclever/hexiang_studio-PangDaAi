package com.back_hexiang_studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 请假申请实体类
 */
@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(value = "request_id", type = IdType.AUTO)
    private Long requestId;

    private Long studentId;
    private Long attendancePlanId; // 关联的考勤计划ID
    private String type;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String attachments; // JSON格式的附件URL列表
    private String status;
    private Long approverId;
    private String remark;
    private LocalDateTime approvedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 