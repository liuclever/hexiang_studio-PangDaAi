package com.back_hexiang_studio.dv.vo.task;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubTaskVo {
    private Long subTaskId;
    private Long taskId; // 添加taskId字段
    private String title;
    private String description;
    private Long status;
    private List<SubTaskMemberVo> members;
    private Long updateUser;
    private LocalDateTime updateTime;
    private Long createUser;
    private LocalDateTime createTime;


}
