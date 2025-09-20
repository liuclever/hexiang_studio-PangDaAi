package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务提交实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmission {

    /**
     * 提交ID，主键
     */
    private Long submissionId;
    
    /**
     * 子任务ID
     */
    private Long subTaskId;
    
    /**
     * 提交者ID
     */
    private Long userId;
    
    /**
     * 提交说明
     */
    private String submissionNotice;
    
    /**
     * 提交状态(0=进行中,1=已完成,2=待审核,3=已退回)
     */
    private Integer status;
    
    /**
     * 提交时间
     */
    private LocalDateTime submissionTime;
    
    /**
     * 审核备注或退回原因
     */
    private String reviewComment;
    
    /**
     * 审核操作时间
     */
    private LocalDateTime reviewTime;
} 