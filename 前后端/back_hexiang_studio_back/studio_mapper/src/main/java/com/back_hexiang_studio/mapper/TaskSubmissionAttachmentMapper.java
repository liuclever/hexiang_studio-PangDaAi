package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.TaskSubmissionAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务提交附件Mapper
 */
@Mapper
public interface TaskSubmissionAttachmentMapper {

    /**
     * 根据提交ID查询附件列表
     * @param submissionId 提交ID
     * @return 附件列表
     */
    List<TaskSubmissionAttachment> findBySubmissionId(@Param("submissionId") Long submissionId);

    /**
     * 插入附件记录
     * @param attachment 附件记录
     */
    void insert(TaskSubmissionAttachment attachment);

    /**
     * 根据提交ID删除附件
     * @param submissionId 提交ID
     */
    void deleteBySubmissionId(@Param("submissionId") Long submissionId);
} 