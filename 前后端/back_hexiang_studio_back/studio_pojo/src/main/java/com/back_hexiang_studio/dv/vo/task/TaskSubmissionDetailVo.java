package com.back_hexiang_studio.dv.vo.task;

import com.back_hexiang_studio.entity.TaskSubmissionAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionDetailVo {
    
    /**
     * 是否存在提交记录
     */
    private Boolean exists;
    
    /**
     * 提交信息
     */
    private SubmissionInfo submission;
    
    /**
     * 附件列表
     */
    private List<TaskSubmissionAttachment> attachments;
    
    /**
     * 提交信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionInfo {
        /**
         * 提交ID
         */
        private Long submissionId;
        
        /**
         * 提交状态
         */
        private Integer status;
        
        /**
         * 提交说明
         */
        private String submissionNotice;
        
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
    }
} 