package com.back_hexiang_studio.controller.wx;



import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.service.TaskService;
import com.back_hexiang_studio.service.TaskSubmissionService;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.github.pagehelper.PageHelper;
import com.back_hexiang_studio.dv.vo.task.TaskProgressVo;
import com.back_hexiang_studio.dv.vo.task.TaskItemVo;
import com.back_hexiang_studio.dv.vo.task.TaskDetailVo;
import com.back_hexiang_studio.dv.vo.task.TaskSubmissionDetailVo;
import com.back_hexiang_studio.dv.dto.task.PageTaskPageDto;
import com.back_hexiang_studio.dv.dto.task.TaskSubmissionDto;
import com.back_hexiang_studio.dv.vo.approval.ApprovalCountVo;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.utils.FileValidationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;


import java.time.LocalDateTime;
import java.util.*;

/**
 * 微信端任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/task")
public class WxTaskController {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskSubmissionService taskSubmissionService;
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private FileValidationManager fileValidationManager;

    /**
     * 获取用户最近任务进度
     * @return 任务进度数据
     */
    @GetMapping("/recent-progress")
    public Result<TaskProgressVo> getRecentTaskProgress() {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取用户最近任务进度");
        try {
            // 获取当前登录用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 使用PageHelper限制返回数量（最多5个任务）
            PageHelper.startPage(1, 5, false);
            
            // 获取用户的紧急任务
            List<com.back_hexiang_studio.dv.vo.task.MyTaskVO> urgentTasks = taskMapper.findUrgentTasksByUserId(currentUserId);
            
            // 构造任务列表
            List<TaskItemVo> tasks = new ArrayList<>();
            for (com.back_hexiang_studio.dv.vo.task.MyTaskVO task : urgentTasks) {
                // 计算任务进度
                int completedSubTasks = taskMapper.getCompletedSubTasks(task.getTaskId());
                int totalSubTasks = taskMapper.getTotalSubTasks(task.getTaskId());
                int progress = 0;
                if (totalSubTasks > 0) {
                    progress = Math.round((completedSubTasks * 100.0f) / totalSubTasks);
                }
                
                // 直接使用数据库中的状态（已通过审批时同步更新）
                
                TaskItemVo taskItem = TaskItemVo.builder()
                        .id(task.getTaskId())
                        .title(task.getTitle())
                        .progress(progress) // 使用计算出的真实进度
                        .status(task.getStatus()) // 直接使用数据库状态
                        .build();
                tasks.add(taskItem);
            }
            
            TaskProgressVo progressData = TaskProgressVo.builder()
                    .tasks(tasks)
                    .hasUncompletedTasks(!urgentTasks.isEmpty())
                    .urgentTasks(urgentTasks.size())
                    .build();
            
            return Result.success(progressData);
        } catch (Exception e) {
            log.error("获取任务进度失败: {}", e.getMessage());
            return Result.error("获取任务进度失败");
        }
    }

    /**
     * 获取任务列表（分页）
     * @param page 页码
     * @param pageSize 每页大小
     * @return 任务列表
     */
    @GetMapping("/list")
    public Result<PageResult> getTaskList(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取微信端任务列表，页码: {}, 每页大小: {}", page, pageSize);
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 构造查询参数
            PageTaskPageDto pageTaskPageDto = new PageTaskPageDto();
            pageTaskPageDto.setPage(page);
            pageTaskPageDto.setSize(pageSize);
            pageTaskPageDto.setCurrentUserId(currentUserId);  // 设置当前用户ID
            
            // 调用TaskService获取任务列表
            PageResult taskList = taskService.getTasks(pageTaskPageDto);
            
            return Result.success(taskList);
        } catch (Exception e) {
            log.error("获取任务列表失败: {}", e.getMessage());
            return Result.error("获取任务列表失败");
        }
    }

    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/detail/{taskId}")
    public Result<TaskDetailVo> getTaskDetail(@PathVariable Long taskId) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取微信端任务详情，任务ID: {}", taskId);
        try {
            if (taskId == null) {
                return Result.error("任务ID不能为空");
            }
            
            // 调用TaskService获取任务详情
            TaskDetailVo taskDetail = taskService.detail(taskId);
            
            return Result.success(taskDetail);
        } catch (Exception e) {
            log.error("获取任务详情失败: {}", e.getMessage());
            return Result.error("获取任务详情失败");
        }
    }

    /**
     * 获取当前用户的任务统计信息
     * @return 任务统计数据
     */
    @GetMapping("/statistics")
    public Result<com.back_hexiang_studio.dv.vo.task.TaskStatisticsVo> getUserTaskStatistics() {
        log.info("获取当前用户的任务统计信息");
        try {
            // 获取当前登录用户ID
            Long userId = UserContextHolder.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            // 调用TaskService获取个人任务统计
            com.back_hexiang_studio.dv.vo.task.TaskStatisticsVo statistics = taskService.getUserTaskStatistics(userId);
            
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户任务统计失败: {}", e.getMessage());
            return Result.error("获取任务统计失败");
        }
    }

    /**
     * 获取子任务提交详情
     * @param subTaskId 子任务ID
     * @return 提交详情
     */
    @GetMapping("/submission/detail/{subTaskId}")
    public Result<TaskSubmissionDetailVo> getTaskSubmissionDetail(@PathVariable Long subTaskId) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取子任务提交详情，子任务ID: {}", subTaskId);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 调用Service获取提交详情
            TaskSubmissionDetailVo submissionDetail = taskSubmissionService.getSubmissionDetail(subTaskId, currentUserId);
            
            return Result.success(submissionDetail);
        } catch (BusinessException e) {
            log.warn("获取子任务提交详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取子任务提交详情失败", e);
            return Result.error("获取子任务提交详情失败");
        }
    }

    /**
     * 提交子任务
     * @param subTaskId 子任务ID
     * @param submissionDto 提交数据(JSON字符串)
     * @param file 提交的文件
     * @return 提交结果
     */
    @PostMapping("/submission/submit/{subTaskId}")
    public Result submitTask(@PathVariable Long subTaskId,
                           @RequestParam("submissionDto") String submissionDto,
                           @RequestParam(value = "files", required = false) MultipartFile file) {
        // 🔧 优化：任务提交操作，降级为DEBUG
        log.debug("提交子任务，子任务ID: {}", subTaskId);
        
        // 🔧 优化：文件信息记录降级为DEBUG
        if (file != null) {
            log.debug("接收到文件: 原始文件名={}, 大小={} bytes", 
                    file.getOriginalFilename(), file.getSize());
        } else {
            log.debug("未接收到文件");
        }
        
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 解析JSON参数
            TaskSubmissionDto taskSubmissionDto;
            try {
                taskSubmissionDto = objectMapper.readValue(submissionDto, TaskSubmissionDto.class);
                // 🔧 优化：解析成功信息降级为DEBUG
                log.debug("解析提交数据成功");
            } catch (Exception e) {
                log.error("解析提交数据失败: {}", submissionDto, e);
                return Result.error("提交数据格式错误");
            }
            
            // 处理文件列表和验证
            List<MultipartFile> fileList = null;
            if (file != null && !file.isEmpty()) {
                // 验证文件安全性和大小
                Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                if (!validationResult.isSuccess()) {
                    log.warn("文件验证失败: {}", validationResult.getMsg());
                    return Result.error("文件验证失败: " + validationResult.getMsg());
                }
                
                fileList = Collections.singletonList(file);
                log.info("文件验证通过，构建文件列表成功，包含{}个文件", fileList.size());
            } else {
                log.info("没有文件需要处理");
            }
            
            // 调用Service提交任务
            taskSubmissionService.submitTask(subTaskId, currentUserId, taskSubmissionDto, fileList);
            
            log.info("子任务提交成功，用户ID: {}, 子任务ID: {}", currentUserId, subTaskId);
            return Result.success("任务提交成功");
        } catch (BusinessException e) {
            log.warn("提交子任务失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交子任务失败", e);
            return Result.error("提交子任务失败");
        }
    }

    /**
     * 审核任务提交
     * @param submissionId 提交ID
     * @param reviewData 审核数据
     * @return 审核结果
     */
    @PostMapping("/submission/review/{submissionId}")
    public Result reviewTaskSubmission(@PathVariable Long submissionId,
                                     @RequestBody Map<String, Object> reviewData) {
        log.info("审核任务提交，提交ID: {}, 审核数据: {}", submissionId, reviewData);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Integer status = (Integer) reviewData.get("status"); // 1: 通过, 3: 退回
            String reviewComment = (String) reviewData.get("reviewComment");
            
            if (status == null) {
                return Result.error("审核状态不能为空");
            }
            
            // 调用Service进行审核
            taskSubmissionService.reviewSubmission(submissionId, status, reviewComment, currentUserId);
            
            log.info("任务审核完成，审核人: {}, 提交ID: {}, 审核结果: {}", currentUserId, submissionId, status);
            return Result.success("审核完成");
        } catch (BusinessException e) {
            log.warn("审核任务提交失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("审核任务提交失败", e);
            return Result.error("审核任务提交失败");
        }
    }

    /**
     * 获取待审核任务提交数量
     * @return 待审核数量
     */
    @GetMapping("/submission/pending/count")
    public Result<ApprovalCountVo> getPendingSubmissionCount() {
        // 🔧 优化：频繁查询的统计接口，降级为DEBUG
        log.debug("获取待审核任务提交数量");
        try {
            // 调用Service获取待审核数量
            int count = taskSubmissionService.getPendingSubmissionCount();
            
            ApprovalCountVo result = ApprovalCountVo.builder()
                    .count(count)
                    .type("pending_task_submission")
                    .build();
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待审核任务数量失败", e);
            return Result.error("获取待审核任务数量失败");
        }
    }

    /**
     * 获取任务提交列表（按角色权限控制）
     * 学生：只能看见自己参与任务的提交
     * 老师：只能看自己创建任务的提交
     * 管理员：可以看见全部任务提交
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @return 提交列表
     */
    @GetMapping("/submission/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER') or hasAuthority('ROLE_TEACHER')")
    public Result getTaskSubmissionList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) Integer status) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取任务提交列表，页码: {}, 每页大小: {}, 状态: {}", page, pageSize, status);
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 调用Service进行按角色权限的分页查询
            PageResult pageResult = taskSubmissionService.getTaskSubmissionListByUserRole(
                page, pageSize, status, currentUserId);  
            
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取任务提交列表失败", e);
            return Result.error("获取任务提交列表失败");
        }
    }

    /**
     * 获取任务提交的附件列表
     * @param submissionId 提交ID
     * @return 附件列表
     */
    @GetMapping("/submission/attachments/{submissionId}")
    public Result getTaskSubmissionAttachments(@PathVariable Long submissionId) {
        log.info("获取任务提交附件列表，提交ID: {}", submissionId);
        try {
            if (submissionId == null) {
                return Result.error("提交ID不能为空");
            }
            
            // 调用Service获取附件列表
            List<com.back_hexiang_studio.entity.TaskSubmissionAttachment> attachments = 
                taskSubmissionService.getSubmissionAttachments(submissionId);
            
            return Result.success(attachments);
        } catch (Exception e) {
            log.error("获取任务提交附件列表失败", e);
            return Result.error("获取附件列表失败");
        }
    }
    
    /**
     * 动态计算任务状态
     * 优先级：被退回 > 待审核 > 已逾期 > 进行中 > 已完成
     */
    private String calculateTaskStatus(Long taskId, String originalStatus, LocalDateTime endTime) {
        try {
            // 获取该任务下所有子任务的提交状态
            List<Integer> submissionStatuses = taskMapper.getSubTaskSubmissionStatuses(taskId);
            if (submissionStatuses == null || submissionStatuses.isEmpty()) {
                return originalStatus;
            }
            
            // 检查是否有被退回的提交 (status = 3)
            boolean hasRejected = submissionStatuses.stream().anyMatch(status -> status != null && status == 3);
            if (hasRejected) {
                return "REJECTED"; // 有提交被退回
            }
            
            // 检查是否有待审核的提交 (status = 2)
            boolean hasPending = submissionStatuses.stream().anyMatch(status -> status != null && status == 2);
            if (hasPending) {
                return "PENDING_REVIEW"; // 有提交待审核
            }
            
            // 检查是否已逾期
            if (endTime != null && LocalDateTime.now().isAfter(endTime)) {
                // 检查是否所有子任务都已通过审核 (status = 1)
                boolean allCompleted = submissionStatuses.stream().allMatch(status -> status != null && status == 1);
                if (!allCompleted) {
                    return "OVERDUE"; // 逾期且未全部完成
                }
            }
            
            // 检查是否全部通过审核 (status = 1)
            boolean allCompleted = submissionStatuses.stream().allMatch(status -> status != null && status == 1);
            if (allCompleted) {
                return "COMPLETED"; // 全部完成
            }
            
            // 检查是否为紧急任务（3天内到期且未完成）
            if (endTime != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime urgentThreshold = now.plusDays(3);
                if (!now.isAfter(endTime) && endTime.isBefore(urgentThreshold)) {
                    return "URGENT"; // 3天内到期的未完成任务
                }
            }
            
            // 默认返回进行中
            return "IN_PROGRESS";
            
        } catch (Exception e) {
            log.error("计算任务状态失败，任务ID: {}", taskId, e);
            return originalStatus; // 出错时返回原状态
        }
    }
} 