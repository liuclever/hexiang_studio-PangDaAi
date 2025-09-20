package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubTask {

    private Long subTaskId;
    private Long taskId;
    private String title;
    private String description;
    private String status;  // 子任务状态 (not_started, in_progress, completed, delayed)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

} 