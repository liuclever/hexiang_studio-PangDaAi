package com.back_hexiang_studio.dv.vo.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.back_hexiang_studio.entity.TaskSubmissionAttachment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskMemberVo {
    private Long subTaskId;
    private String name;
    private Long userId;
    private String avatar;    // 头像
    private String role;      // 角色
    private String note;      // 备注
    
    // 提交相关信息 - 仅在审核场景下使用
    private TaskSubmissionDetailVo.SubmissionInfo submission;  // 提交信息
    private List<TaskSubmissionAttachment> submissionAttachments;  // 提交附件
}
