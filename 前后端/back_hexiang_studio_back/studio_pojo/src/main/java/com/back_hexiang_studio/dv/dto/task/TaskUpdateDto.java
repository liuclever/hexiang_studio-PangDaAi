package com.back_hexiang_studio.dv.dto.task;

import com.back_hexiang_studio.dv.vo.task.SubTaskVo;
import lombok.Data;
import java.util.List;
import com.back_hexiang_studio.dv.vo.task.TaskAttachmentVo;

@Data
public class TaskUpdateDto {

    private Long taskId;
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private List<SubTaskVo> subTasks;
    private List<Long> keepAttachmentIds;
} 