package com.back_hexiang_studio.controller.admin;


import com.back_hexiang_studio.dv.dto.task.PageTaskPageDto;
import com.back_hexiang_studio.dv.dto.task.TaskAddDto;
import com.back_hexiang_studio.dv.dto.task.TaskStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.task.TaskUpdateDto;
import com.back_hexiang_studio.dv.vo.task.TaskDetailVo;

import com.back_hexiang_studio.dv.vo.task.UserList;
import com.back_hexiang_studio.dv.vo.task.TaskStatisticsVo;

import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.TaskService;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.back_hexiang_studio.utils.FileValidationManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.back_hexiang_studio.context.UserContextHolder;

/**
 * 任务管理控制器
 * 查看功能（任务日历）对所有人开放，管理功能需要权限控制
 */
@Slf4j
@RestController
@RequestMapping("/admin/task")
public class TaskController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FileValidationManager fileValidationManager;
    @Autowired
    private com.back_hexiang_studio.service.TaskSubmissionService taskSubmissionService;

    /**
     * 验证任务时间的合理性
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    private void validateTaskTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 检查开始时间不能早于当前时间（允许5分钟容差）
        if (startTime.isBefore(now.minusMinutes(5))) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }
        
        // 检查结束时间必须晚于开始时间
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        
        // 检查任务持续时间至少为1小时
        if (endTime.isBefore(startTime.plusHours(1))) {
            throw new IllegalArgumentException("任务持续时间至少为1小时");
        }
        
        log.info("任务时间验证通过 - 开始时间: {}, 结束时间: {}", startTime, endTime);
    }


    /**
     * 任务列表
     * 权限控制：只有部长级别及以上用户可以查看任务
     * @param pageTaskPageDto
     * @return
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_MANAGE') or hasAuthority('TASK_CREATE')")
    public Result<PageResult> getTaks( PageTaskPageDto pageTaskPageDto){
        log.info("获取任务列表请求参数：",pageTaskPageDto.toString());
        
        // 获取当前用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        if (currentUserId == null) {
            return Result.error("用户未登录");
        }
        pageTaskPageDto.setCurrentUserId(currentUserId);  // 设置当前用户ID
        
        PageResult tasks = taskService.getTasks(pageTaskPageDto);
        return Result.success(tasks);
    }


    /**
     * 添加任务
     * @param
     * @return
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('TASK_MANAGE')")
    public Result<String> addTask(@RequestPart("taskAddDto") String taskAddDtoString,
                                @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        log.info("新增任务");
        try {
            TaskAddDto taskAddDto = objectMapper.readValue(taskAddDtoString, TaskAddDto.class);
            
            // 验证任务时间
            validateTaskTimes(
                DateTimeUtils.parseDateTime(taskAddDto.getStartTime()),
                DateTimeUtils.parseDateTime(taskAddDto.getEndTime())
            );
            
            // 验证所有文件
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                    if (!validationResult.isSuccess()) {
                        return Result.error(validationResult.getMsg());
                    }
                }
            }
            
            taskService.addTaskWithAttachments(taskAddDto, files);
            return Result.success("新增任务成功");
        } catch (IllegalArgumentException e) {
            log.error("新增任务，时间验证失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("新增任务，JSON解析失败: {}", e.getMessage());
            return Result.error("新增任务失败，数据格式错误");
        }
    }

    /**
     * 根据id获取任务详情
     * 权限控制：只有有任务查看权限的用户可以查看详情
     * @param taskId
     * @return
     */
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_MANAGE') or hasAuthority('TASK_CREATE')")
    public Result<TaskDetailVo> detail(@PathVariable Long taskId){
        if(taskId==null){
            return Result.error("id不能为空");
        }
        TaskDetailVo taskDetail = taskService.detail(taskId);
        return Result.success(taskDetail);
    }

    /**
     * 编辑任务
     * @return
     */
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('TASK_CREATE')")
    public Result<String> updateTask(@RequestPart("taskUpdateDto") String taskUpdateDtoString,
                                     @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        log.info("编辑任务");
        try {
            TaskUpdateDto taskUpdateDto = objectMapper.readValue(taskUpdateDtoString, TaskUpdateDto.class);
            List<Long> keepAttachmentIds = taskUpdateDto.getKeepAttachmentIds();
            
            // 验证任务时间（如果提供了时间信息）
            if (taskUpdateDto.getStartTime() != null && taskUpdateDto.getEndTime() != null) {
                validateTaskTimes(
                    DateTimeUtils.parseDateTime(taskUpdateDto.getStartTime()),
                    DateTimeUtils.parseDateTime(taskUpdateDto.getEndTime())
                );
            }
            
            // 验证所有文件
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                    if (!validationResult.isSuccess()) {
                        return Result.error(validationResult.getMsg());
                    }
                }
            }
            
            taskService.updateTaskWithAttachments(taskUpdateDto, files, keepAttachmentIds);
            return Result.success("编辑任务成功");
        } catch (IllegalArgumentException e) {
            log.error("编辑任务，时间验证失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("编辑任务，JSON解析失败: {}", e.getMessage());
            return Result.error("编辑任务失败，数据格式错误");
        }
    }

    /**
     * 删除任务
     * @param taskId
     * @return
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_CREATE')")
    public Result deleteTask(@PathVariable Long taskId) {
        log.info("删除任务, ID: {}", taskId);
        taskService.deleteTask(taskId);
        return Result.success();
    }

    /**
     * 更新任务状态
     * @param taskStatusUpdateDto
     * @return
     */
    @PutMapping("/updateStatus")
    @PreAuthorize("hasAuthority('TASK_APPROVE')")
    public Result updateTaskStatus(@RequestBody TaskStatusUpdateDto taskStatusUpdateDto) {
        log.info("更新任务状态: {}", taskStatusUpdateDto);
        taskService.updateTaskStatus(taskStatusUpdateDto);
        return Result.success();
    }

    /**
     * 获取任务统计数据
     * 权限控制：只有有任务查看权限的用户可以查看统计数据
     * @return
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_MANAGE') or hasAuthority('TASK_CREATE')")
    public Result<TaskStatisticsVo> getStatistics() {
        log.info("请求任务统计数据");
        TaskStatisticsVo statistics = taskService.getTaskStatistics();
        return Result.success(statistics);
    }


    /**
     * 获取学生老师管理员列表（任务管理）
     * 权限控制：只有有任务管理权限的用户可以查看用户列表
     * @param name 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    @GetMapping("/taskUserList")
    @PreAuthorize("hasAuthority('TASK_MANAGE') or hasAuthority('TASK_CREATE')")
    public Result<List<UserList>> userList(@RequestParam(required = false) String name, 
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "1000") Integer pageSize) {

        List<UserList> userLists = taskService.getUserListPage(name, page, pageSize);
        
        return Result.success(userLists);
    }

    /**
     * 审批任务（通过）
     * @param taskId 任务ID
     * @param approvalComment 审批意见
     * @return 结果
     */
    @PostMapping("/approve/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('TASK_MANAGE')")
    public Result approveTask(@PathVariable Long taskId, 
                             @RequestParam(required = false) String approvalComment) {
        log.info("审批任务通过, 任务ID: {}, 审批意见: {}", taskId, approvalComment);
        
        try {
            taskService.approveTask(taskId, approvalComment);
            return Result.success("任务审批通过");
        } catch (Exception e) {
            log.error("审批任务失败: {}", e.getMessage());
            return Result.error("审批任务失败: " + e.getMessage());
        }
    }

    /**
     * 退回任务
     * @param taskId 任务ID
     * @param rejectionReason 退回原因
     * @return 结果
     */
    @PostMapping("/reject/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('TASK_MANAGE')")
    public Result rejectTask(@PathVariable Long taskId, 
                            @RequestParam String rejectionReason) {
        log.info("退回任务, 任务ID: {}, 退回原因: {}", taskId, rejectionReason);
        
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            return Result.error("退回原因不能为空");
        }
        
        try {
            taskService.rejectTask(taskId, rejectionReason);
            return Result.success("任务已退回");
        } catch (Exception e) {
            log.error("退回任务失败: {}", e.getMessage());
            return Result.error("退回任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取待审批任务列表
     * 权限控制：学生不能访问，老师可以看自己创建的，管理员可以看全部
     * @param pageTaskPageDto 分页查询参数
     * @return 待审批任务列表
     */
    @GetMapping("/pending-approval")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_TEACHER')")
    public Result<PageResult> getPendingApprovalTasks(PageTaskPageDto pageTaskPageDto) {
        log.info("获取待审批任务列表，查询参数: {}", pageTaskPageDto);
        
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 设置当前用户ID到查询参数中，用于角色权限过滤
            pageTaskPageDto.setCurrentUserId(currentUserId);
            
            // 设置状态为待审核
            pageTaskPageDto.setStatus("PENDING_REVIEW");
            PageResult tasks = taskService.getTasks(pageTaskPageDto);
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("获取待审批任务列表失败: {}", e.getMessage());
            return Result.error("获取待审批任务列表失败");
        }
    }

    /**
     * 获取任务提交审核信息
     * @param taskId 任务ID
     * @return 任务提交信息
     */
    @GetMapping("/{taskId}/submissions")
    @PreAuthorize("hasAuthority('TASK_APPROVE')")
    public Result<TaskDetailVo> getTaskSubmissions(@PathVariable Long taskId) {
        log.info("获取任务提交审核信息，任务ID: {}", taskId);
        
        try {
            // 先获取任务基本信息
            TaskDetailVo taskDetail = taskService.detail(taskId);
            
            // 为每个子任务的每个成员获取提交信息
            if (taskDetail.getSubTasks() != null) {
                for (com.back_hexiang_studio.dv.vo.task.SubTaskDetailVo subTask : taskDetail.getSubTasks()) {
                    if (subTask.getMembers() != null) {
                        for (com.back_hexiang_studio.dv.vo.task.SubTaskMemberVo member : subTask.getMembers()) {
                            try {
                                // 获取该成员在该子任务的提交详情
                                com.back_hexiang_studio.dv.vo.task.TaskSubmissionDetailVo submissionDetail = taskSubmissionService.getSubmissionDetail(
                                    subTask.getSubTaskId(), 
                                    member.getUserId()
                                );
                                
                                // 将提交信息添加到成员对象中
                                if (submissionDetail.getExists()) {
                                    member.setSubmission(submissionDetail.getSubmission());
                                    member.setSubmissionAttachments(submissionDetail.getAttachments());
                                }
                            } catch (Exception e) {
                                log.warn("获取成员{}在子任务{}的提交信息失败: {}", 
                                    member.getUserId(), subTask.getSubTaskId(), e.getMessage());
                                // 继续处理其他成员，不中断整个流程
                            }
                        }
                    }
                }
            }
            
            return Result.success(taskDetail);
        } catch (Exception e) {
            log.error("获取任务提交审核信息失败: {}", e.getMessage());
            return Result.error("获取任务提交信息失败");
        }
    }

    /**
     * 审核任务提交（管理端）
     * @param submissionId 提交ID
     * @param reviewData 审核数据
     * @return 审核结果
     */
    @PostMapping("/submission/review/{submissionId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('TASK_MANAGE')")
    public Result reviewTaskSubmission(@PathVariable Long submissionId,
                                     @RequestBody Map<String, Object> reviewData) {
        log.info("管理端审核任务提交，提交ID: {}, 审核数据: {}", submissionId, reviewData);
        
        try {
            Long currentUserId = com.back_hexiang_studio.context.UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Integer status = (Integer) reviewData.get("status"); // 1: 通过, 3: 退回
            String reviewComment = (String) reviewData.get("reviewComment");
            
            if (status == null) {
                return Result.error("审核状态不能为空");
            }
            
            // 调用Service进行审核（复用现有逻辑）
            taskSubmissionService.reviewSubmission(submissionId, status, reviewComment, currentUserId);
            
            log.info("管理端审核完成，审核人: {}, 提交ID: {}, 审核结果: {}", currentUserId, submissionId, status);
            return Result.success("审核完成");
        } catch (Exception e) {
            log.error("管理端审核任务提交失败", e);
            return Result.error("審核任务提交失败: " + e.getMessage());
        }
    }

    /**
     * 批量审批任务
     * @param taskIds 任务ID列表
     * @param approvalComment 审批意见
     * @return 结果
     */
    @PostMapping("/batch-approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('TASK_MANAGE')")
    public Result batchApproveTask(@RequestParam List<Long> taskIds, 
                                  @RequestParam(required = false) String approvalComment) {
        log.info("批量审批任务, 任务ID列表: {}, 审批意见: {}", taskIds, approvalComment);
        
        try {
            int successCount = 0;
            for (Long taskId : taskIds) {
                try {
                    taskService.approveTask(taskId, approvalComment);
                    successCount++;
                } catch (Exception e) {
                    log.error("审批任务失败, 任务ID: {}, 错误: {}", taskId, e.getMessage());
                }
            }
            
            if (successCount == taskIds.size()) {
                return Result.success("批量审批完成，共处理 " + successCount + " 个任务");
            } else {
                return Result.error("批量审批部分失败，成功 " + successCount + "/" + taskIds.size() + " 个任务");
            }
        } catch (Exception e) {
            log.error("批量审批任务失败: {}", e.getMessage());
            return Result.error("批量审批任务失败: " + e.getMessage());
        }
    }
}
