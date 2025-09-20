package com.back_hexiang_studio.service;


import com.back_hexiang_studio.dv.dto.task.PageTaskPageDto;
import com.back_hexiang_studio.dv.dto.task.TaskAddDto;
import com.back_hexiang_studio.dv.vo.task.TaskDetailVo;
import com.back_hexiang_studio.dv.vo.task.TasksVo;
import com.back_hexiang_studio.dv.vo.task.UserList;
import com.back_hexiang_studio.result.PageResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.back_hexiang_studio.dv.vo.task.SubTaskMemberVo;
import com.back_hexiang_studio.dv.vo.task.TaskStatisticsVo;
import com.back_hexiang_studio.dv.dto.task.TaskUpdateDto;
import com.back_hexiang_studio.dv.dto.task.TaskStatusUpdateDto;
import org.springframework.web.multipart.MultipartFile;
import com.back_hexiang_studio.dv.vo.task.MyTaskVO;

public interface TaskService {

    //获取任务列表
    PageResult getTasks(PageTaskPageDto pageTaskPageDto);

    //添加任务
      Boolean addTask(TaskAddDto taskAddDto);

    void addTaskWithAttachments(TaskAddDto taskAddDto, List<MultipartFile> files);

  
    
    //获取学生老师管理员列表（任务管理）- 带分页
    List<UserList> getUserListPage(String name, Integer page, Integer pageSize);

    //获取任务详情
    TaskDetailVo detail( Long taskId);

    //根据子任务id查询成员
    List<SubTaskMemberVo> getSubMembers(Long subTaskId);

    //获取任务统计
    TaskStatisticsVo getTaskStatistics();

    // 根据用户ID获取个人任务统计
    TaskStatisticsVo getUserTaskStatistics(Long userId);

    // 根据用户ID获取个人任务列表（包含所有状态）
    List<MyTaskVO> getUserTaskList(Long userId);

    // 根据用户ID和状态获取个人任务列表
    List<MyTaskVO> getUserTaskListByStatus(Long userId, List<String> statusList);

    // 更新任务
    void updateTask(TaskUpdateDto taskUpdateDto);

    void updateTaskWithAttachments(TaskUpdateDto taskUpdateDto, List<MultipartFile> newFiles, List<Long> keepAttachmentIds);

    // 删除任务
    void deleteTask(Long taskId);

    // 更新任务状态
    void updateTaskStatus(TaskStatusUpdateDto taskStatusUpdateDto);

    // 审批任务（通过）
    void approveTask(Long taskId, String approvalComment);

    // 退回任务
    void rejectTask(Long taskId, String rejectionReason);
}
