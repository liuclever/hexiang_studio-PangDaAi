package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.dto.task.PageTaskPageDto;

import com.back_hexiang_studio.dv.vo.task.SubTaskDetailVo;
import com.back_hexiang_studio.dv.vo.task.SubTaskMemberVo;
import com.back_hexiang_studio.dv.vo.task.TaskDetailVo;
import com.back_hexiang_studio.dv.vo.task.TaskStatisticsVo;
import com.back_hexiang_studio.dv.vo.task.TasksVo;
import com.back_hexiang_studio.dv.vo.task.MyTaskVO;
import com.back_hexiang_studio.entity.SubTask;
import com.back_hexiang_studio.entity.Task;
import com.back_hexiang_studio.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    //分页查询任务（包含创建人信息和子任务统计）
    List<TasksVo> getTasks(Task taskQuery) ;

    //分页查询任务（带角色权限过滤）
    List<TasksVo> getTasksWithRole(@Param("taskQuery") Task taskQuery, 
                                   @Param("currentUserId") Long currentUserId, 
                                   @Param("userRole") String userRole);

    //获取任务完成数
    int getCompletedSubTasks(Long taskId);

    //获取任务总数
    int getTotalSubTasks(Long taskId);

    //添加主任务

    Boolean insert(Task task);

    //获取任务详情
    Task getTaskDetail(Long taskId);

    //获取相关子任务
    List<SubTaskDetailVo> getSubTasks(Long taskId);

    //根据子任务id查询成员
    List<SubTaskMemberVo> getSubMembers(Long subTaskId);

    //获取任务统计
    TaskStatisticsVo getTaskStatistics();

    // 更新主任务
    void update(Task task);

    // 根据ID删除任务
    void deleteById(Long taskId);

    //检擦任务是否存在
    Long isTaskExist(Long taskId);

    // 批量更新逾期任务状态
    void updateOverdueTasksStatus();

    // 根据用户ID查找紧急的待办任务
    List<MyTaskVO> findUrgentTasksByUserId(@Param("userId") Long userId);
    
    // 根据用户ID获取个人任务统计
    TaskStatisticsVo getUserTaskStatistics(@Param("userId") Long userId);
    
    // 根据用户ID获取个人任务列表（包含所有状态）
    List<MyTaskVO> getUserTaskList(@Param("userId") Long userId);

    // 根据用户ID和状态获取个人任务列表
    List<MyTaskVO> getUserTaskListByStatus(@Param("userId") Long userId, @Param("statusList") List<String> statusList);
    
    // 获取任务下所有子任务的提交状态
    List<Integer> getSubTaskSubmissionStatuses(@Param("taskId") Long taskId);
}
