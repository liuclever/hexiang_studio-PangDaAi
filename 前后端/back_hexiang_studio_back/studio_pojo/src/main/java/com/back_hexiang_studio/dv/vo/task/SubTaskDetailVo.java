package com.back_hexiang_studio.dv.vo.task;

import lombok.Data;

import java.util.List;

@Data
public class SubTaskDetailVo {
    private Long subTaskId;
    private Long taskId; // 添加taskId字段
    private String title;
    private String description;
    private Long status;
    private List<SubTaskMemberVo> members;
}
