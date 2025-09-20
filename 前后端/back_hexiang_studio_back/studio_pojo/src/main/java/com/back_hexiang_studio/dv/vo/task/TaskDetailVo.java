package com.back_hexiang_studio.dv.vo.task;

import com.back_hexiang_studio.entity.SubTask;
import com.back_hexiang_studio.entity.SubTaskMember;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailVo {
    private Long taskId;
    private String title;
    private String description;
    private String creatUserName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    List<SubTaskDetailVo> subTasks;
    List<SubTaskMember> subMembers;
    private List<TaskAttachmentVo> attachments;
}
