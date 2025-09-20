package com.back_hexiang_studio.dv.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageTaskPageDto{
    private Integer page = 1;  // 默认第1页
    private Integer size = 10; // 默认每页10条记录
    private String keyword;
    private String status;
    private String startTime;
    private String endTime;
    private Long currentUserId; // 当前登录用户ID
}
