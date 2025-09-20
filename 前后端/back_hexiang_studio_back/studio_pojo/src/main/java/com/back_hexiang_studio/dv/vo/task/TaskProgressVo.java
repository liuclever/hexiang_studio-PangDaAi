package com.back_hexiang_studio.dv.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务进度VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressVo {
    /**
     * 任务列表
     */
    private List<TaskItemVo> tasks;
    
    /**
     * 是否有未完成任务
     */
    private Boolean hasUncompletedTasks;
    
    /**
     * 紧急任务数量
     */
    private Integer urgentTasks;
} 