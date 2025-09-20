package com.back_hexiang_studio.dv.vo.task;

import lombok.Data;
import java.io.Serializable;

@Data
public class TaskStatisticsVo implements Serializable {

    private Integer totalTasks;
    private Integer inProgressTasks;
    private Integer completedTasks;
    private Integer overdueTasks;
    private Integer urgentTasks;        // 新增：紧急任务数量
    private Integer notStartedTasks;
    private Integer needAttentionTasks; // 新增：需要关注的任务数量（紧急+逾期）

} 