package com.back_hexiang_studio.dv.dto.task;

import lombok.Data;

@Data
public class TaskStatusUpdateDto {

    private Long taskId;
    private String status;
    private String endTime; // Optional: for extending the deadline

} 