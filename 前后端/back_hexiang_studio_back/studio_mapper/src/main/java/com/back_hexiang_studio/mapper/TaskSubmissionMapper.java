package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.TaskSubmission;
import com.back_hexiang_studio.dv.vo.task.TaskSubmissionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 任务提交Mapper
 */
@Mapper
public interface TaskSubmissionMapper {

    /**
     * 根据子任务ID和用户ID查询提交记录
     * @param subTaskId 子任务ID
     * @param userId 用户ID
     * @return 提交记录
     */
    TaskSubmission findBySubTaskIdAndUserId(@Param("subTaskId") Long subTaskId, @Param("userId") Long userId);

    /**
     * 插入提交记录
     * @param submission 提交记录
     */
    void insert(TaskSubmission submission);

    /**
     * 更新提交记录
     * @param submission 提交记录
     */
    void update(TaskSubmission submission);

    /**
     * 根据提交ID查询提交记录
     * @param submissionId 提交ID
     * @return 提交记录
     */
    TaskSubmission findById(@Param("submissionId") Long submissionId);

    /**
     * 获取待审核提交数量（带角色过滤）
     * @param currentUserId 当前用户ID
     * @param userRole 用户角色
     * @return 待审核数量
     */
    int countPendingSubmissions(@Param("currentUserId") Long currentUserId, @Param("userRole") String userRole);

    /**
     * 获取今日已处理的任务提交数量（带角色过滤）
     * @param currentUserId 当前用户ID
     * @param userRole 用户角色
     * @return 今日已处理数量
     */
    int countTodayProcessedSubmissions(@Param("currentUserId") Long currentUserId, @Param("userRole") String userRole);

    /**
     * 查询已审批的任务提交记录（带角色过滤）
     * @param days 查询天数
     * @param currentUserId 当前用户ID
     * @param userRole 用户角色
     * @return 已审批记录列表
     */
    List<TaskSubmissionVo> findProcessedSubmissions(@Param("days") Integer days, @Param("currentUserId") Long currentUserId, @Param("userRole") String userRole);



    /**
     * 分页查询任务提交列表
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @return 提交列表
     */
    List<TaskSubmissionVo> findSubmissionsByPage(@Param("offset") Integer offset, 
                                                @Param("pageSize") Integer pageSize, 
                                                @Param("status") Integer status);

    /**
     * 统计任务提交数量
     * @param status 状态筛选
     * @return 数量
     */
    int countSubmissions(@Param("status") Integer status);
    
    /**
     * 按任务创建人分页查询任务提交列表（老师看自己创建的任务提交）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @param creatorId 任务创建人ID
     * @return 提交列表
     */
    List<TaskSubmissionVo> findSubmissionsByCreator(@Param("offset") Integer offset, 
                                                   @Param("pageSize") Integer pageSize, 
                                                   @Param("status") Integer status,
                                                   @Param("creatorId") Long creatorId);

    /**
     * 按任务创建人统计任务提交数量
     * @param status 状态筛选
     * @param creatorId 任务创建人ID
     * @return 数量
     */
    int countSubmissionsByCreator(@Param("status") Integer status, @Param("creatorId") Long creatorId);
    
    /**
     * 按学生分页查询任务提交列表（学生看自己参与的任务提交）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @param studentId 学生ID
     * @return 提交列表
     */
    List<TaskSubmissionVo> findSubmissionsByStudent(@Param("offset") Integer offset, 
                                                   @Param("pageSize") Integer pageSize, 
                                                   @Param("status") Integer status,
                                                   @Param("studentId") Long studentId);

    /**
     * 按学生统计任务提交数量
     * @param status 状态筛选
     * @param studentId 学生ID
     * @return 数量
     */
    int countSubmissionsByStudent(@Param("status") Integer status, @Param("studentId") Long studentId);
    
    /**
     * 根据子任务ID获取所有提交状态
     * @param subTaskId 子任务ID
     * @return 提交状态列表
     */
    List<Integer> getSubmissionStatusesBySubTaskId(@Param("subTaskId") Long subTaskId);
    
    /**
     * 根据日期统计任务提交数量（用于活跃度计算）
     * @param date 日期
     * @return 提交数量
     */
    int countSubmissionsByDate(@Param("date") java.time.LocalDate date);
    
    /**
     * 根据日期统计完成的任务提交数量（用于活跃度计算）
     * @param date 日期
     * @return 完成的提交数量
     */
    int countCompletedSubmissionsByDate(@Param("date") java.time.LocalDate date);
} 