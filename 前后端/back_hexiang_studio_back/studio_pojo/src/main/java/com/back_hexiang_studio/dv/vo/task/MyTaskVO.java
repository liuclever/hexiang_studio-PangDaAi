package com.back_hexiang_studio.dv.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTaskVO {
    private Long taskId;
    private String title;
    private LocalDateTime endTime;
    private String status;
} 