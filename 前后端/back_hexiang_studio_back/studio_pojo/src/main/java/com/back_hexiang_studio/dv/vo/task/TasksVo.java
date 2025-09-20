package com.back_hexiang_studio.dv.vo.task;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TasksVo {

    private Long taskId;
    private String title;
    private String description;
    private String status;
    private String startTime;
    private String endTime;
    private Integer completedSubTasks;
    private Integer totalSubTasks;
    private String creatUserName; // 创建人姓名
}
