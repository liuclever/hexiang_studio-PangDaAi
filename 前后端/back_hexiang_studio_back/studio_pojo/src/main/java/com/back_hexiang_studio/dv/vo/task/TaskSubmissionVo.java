package com.back_hexiang_studio.dv.vo.task;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务提交审批列表VO
 */
@Data
public class TaskSubmissionVo {
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
    /**
     * 子任务ID
     */
    private Long subTaskId;
    
    /**
     * 子任务标题
     */
    private String subTaskTitle;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户姓名
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String userAvatar;
    
    /**
     * 提交说明
     */
    private String submissionNotice;
    
    /**
     * 状态：0-待提交，1-已通过，2-待审核，3-已退回
     */
    private Integer status;
    
    /**
     * 状态文本
     */
    private String statusText;
    
    /**
     * 提交时间
     */
    private LocalDateTime submissionTime;
    
    /**
     * 审核评论
     */
    private String reviewComment;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 获取状态文本
     */
    public String getStatusText() {
        if (status == null) return "未知";
        
        switch (status) {
            case 0: return "待提交";
            case 1: return "已通过";
            case 2: return "待审核";
            case 3: return "已退回";
            default: return "未知";
        }
    }
} 