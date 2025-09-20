package com.back_hexiang_studio.dv.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请视图对象 VO
 */
@Data
public class LeaveRequestVO {

    private Long requestId;
    private Long studentId;
    private String studentName; // 通过关联查询获取
    private String studentAvatar; // 学生头像
    private Long attendancePlanId; // 考勤计划ID
    private String attendancePlanName; // 考勤计划名称
    private String type;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String attachments; // 保持JSON字符串形式
    private String status;
    private Long approverId;
    private String approverName; // 通过关联查询获取
    private String remark;
    private LocalDateTime approvedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 