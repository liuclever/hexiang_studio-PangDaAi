package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.vo.task.TaskAttachmentVo;
import com.back_hexiang_studio.entity.TaskAttachment;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TaskAttachmentMapper {
    /**
     * 插入附件记录
     */
    void insert(TaskAttachment attachment);

    /**
     * 根据主任务ID删除所有附件
     */
    void deleteByTaskId(Long taskId);

    /**
     * 根据主任务ID获取所有附件信息
     */
    List<TaskAttachmentVo> getByTaskId(Long taskId);

    /**
     * 根据附件ID获取附件信息
     */
    TaskAttachment getById(Long attachmentId);

    /**
     * 批量删除附件
     */
    void deleteBatch(List<Long> attachmentIds);
} 