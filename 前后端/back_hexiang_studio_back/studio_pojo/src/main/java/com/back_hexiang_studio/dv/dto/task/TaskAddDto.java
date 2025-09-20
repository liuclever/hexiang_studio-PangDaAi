package com.back_hexiang_studio.dv.dto.task;

import com.back_hexiang_studio.dv.vo.task.SubTaskVo;
import com.back_hexiang_studio.entity.SubTask;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.back_hexiang_studio.dv.vo.task.TaskAttachmentVo;

@Data
public class TaskAddDto {

    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private List<SubTaskVo> subTasks;
    private Long taskId; // For ID returning
    private List<TaskAttachmentVo> attachments;
    private Long  createUser;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
    private Long  updateUser;

}
