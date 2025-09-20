package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.task.TaskSubmissionDto;
import com.back_hexiang_studio.dv.vo.task.TaskSubmissionDetailVo;
import com.back_hexiang_studio.entity.TaskSubmission;
import com.back_hexiang_studio.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 任务提交Service接口
 */
public interface TaskSubmissionService {

    /**
     * 获取子任务提交详情
     * @param subTaskId 子任务ID
     * @param userId 用户ID
     * @return 提交详情
     */
    TaskSubmissionDetailVo getSubmissionDetail(Long subTaskId, Long userId);

    /**
     * 提交子任务
     * @param subTaskId 子任务ID
     * @param userId 用户ID
     * @param submissionDto 提交数据
     * @param files 文件列表
     * @return 操作结果
     */
    void submitTask(Long subTaskId, Long userId, TaskSubmissionDto submissionDto, List<MultipartFile> files);

    /**
     * 审核任务提交
     * @param submissionId 提交ID
     * @param status 审核状态
     * @param reviewComment 审核意见
     * @param reviewerId 审核人ID
     */
    void reviewSubmission(Long submissionId, Integer status, String reviewComment, Long reviewerId);

    /**
     * 获取待审核提交数量
     * @return 待审核数量
     */
    int getPendingSubmissionCount();

    /**
     * 获取今日已处理的任务提交数量
     * @return 今日已处理数量
     */
    int getTodayProcessedCount();

    /**
     * 获取已审批的任务提交记录
     * @param days 查询天数 (1-今天, 3-三天内, 30-一个月内)
     * @return 已审批记录列表
     */
    List<com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo> getProcessedTaskSubmissions(Integer days);



    /**
     * 分页查询任务提交列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 状态筛选 (null表示查询所有状态, 2表示待审核)
     * @return 分页结果
     */
    PageResult getTaskSubmissionList(Integer page, Integer pageSize, Integer status);
    
    /**
     * 根据用户角色获取任务提交列表（分页）
     * 学生：只能看见自己参与任务的提交
     * 老师：只能看自己创建任务的提交
     * 管理员：可以看见全部任务提交
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @param currentUserId 当前用户ID
     * @return 提交列表
     */
    PageResult getTaskSubmissionListByUserRole(Integer page, Integer pageSize, Integer status, Long currentUserId);

    /**
     * 获取任务提交的附件列表
     * @param submissionId 提交ID
     * @return 附件列表
     */
    List<com.back_hexiang_studio.entity.TaskSubmissionAttachment> getSubmissionAttachments(Long submissionId);
} 