package com.back_hexiang_studio.dv.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 已审批记录VO
 */
@Data
@Builder
public class ApprovalRecordVo {
    
    /**
     * 记录ID
     */
    private Long recordId;
    
    /**
     * 审批类型 (task-任务提交, leave-请假申请)
     */
    private String approvalType;
    
    /**
     * 标题/名称
     */
    private String title;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 申请人头像
     */
    private String applicantAvatar;
    
    /**
     * 审批状态 (approved-已通过, rejected-已拒绝)
     */
    private String status;
    
    /**
     * 状态文本
     */
    private String statusText;
    
    /**
     * 审批时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 审批意见
     */
    private String reviewComment;
    
    /**
     * 申请时间
     */
    private LocalDateTime applicationTime;
    
    /**
     * 获取状态文本
     */
    public String getStatusText() {
        if ("approved".equals(status)) {
            return "已通过";
        } else if ("rejected".equals(status)) {
            return "已拒绝";
        }
        return "未知";
    }
} 