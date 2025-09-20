package com.back_hexiang_studio.dv.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务项VO（用于前端任务列表显示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskItemVo {
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务标题
     */
    private String title;
    
    /**
     * 任务进度（百分比）
     */
    private Integer progress;
    
    /**
     * 任务状态
     */
    private String status;
} 