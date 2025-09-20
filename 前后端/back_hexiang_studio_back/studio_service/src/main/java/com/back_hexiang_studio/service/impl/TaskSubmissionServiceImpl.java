package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.task.TaskSubmissionDto;
import com.back_hexiang_studio.dv.vo.task.TaskSubmissionDetailVo;
import com.back_hexiang_studio.dv.vo.task.TaskSubmissionVo;
import com.back_hexiang_studio.entity.Task;
import com.back_hexiang_studio.entity.SubTask;
import com.back_hexiang_studio.entity.TaskSubmission;
import com.back_hexiang_studio.entity.TaskSubmissionAttachment;
import com.back_hexiang_studio.enumeration.FileType;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.back_hexiang_studio.mapper.TaskSubmissionAttachmentMapper;
import com.back_hexiang_studio.mapper.TaskSubmissionMapper;
import com.back_hexiang_studio.mapper.SubTaskMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.service.TaskSubmissionService;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.FileValidationManager;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import com.back_hexiang_studio.result.PageResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务提交Service实现类
 */
@Service
@Slf4j
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    @Autowired
    private TaskSubmissionMapper taskSubmissionMapper;

    @Autowired
    private TaskSubmissionAttachmentMapper attachmentMapper;

    @Autowired
    private SubTaskMapper subTaskMapper;
    
    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private FileValidationManager fileValidationManager;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private StudentMapper studentMapper;

    @Override
    public TaskSubmissionDetailVo getSubmissionDetail(Long subTaskId, Long userId) {
        log.info("获取任务提交详情，子任务ID: {}, 用户ID: {}", subTaskId, userId);
        
        try {
            // 查询提交记录
            TaskSubmission submission = taskSubmissionMapper.findBySubTaskIdAndUserId(subTaskId, userId);
            
            if (submission == null) {
                // 没有提交记录
                return TaskSubmissionDetailVo.builder()
                        .exists(false)
                        .submission(null)
                        .attachments(new ArrayList<>())
                        .build();
            }
            
            // 查询附件
            List<TaskSubmissionAttachment> attachments = attachmentMapper.findBySubmissionId(submission.getSubmissionId());
            
            // 构建提交信息
            TaskSubmissionDetailVo.SubmissionInfo submissionInfo = TaskSubmissionDetailVo.SubmissionInfo.builder()
                    .submissionId(submission.getSubmissionId())
                    .status(submission.getStatus())
                    .submissionNotice(submission.getSubmissionNotice())
                    .submissionTime(submission.getSubmissionTime())
                    .reviewComment(submission.getReviewComment())
                    .reviewTime(submission.getReviewTime())
                    .build();
            
            return TaskSubmissionDetailVo.builder()
                    .exists(true)
                    .submission(submissionInfo)
                    .attachments(attachments)
                    .build();
            
        } catch (Exception e) {
            log.error("获取任务提交详情失败", e);
            throw new BusinessException("获取提交详情失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(Long subTaskId, Long userId, TaskSubmissionDto submissionDto, List<MultipartFile> files) {
        // 🔧 优化：任务提交操作，降级为DEBUG
        log.debug("提交任务，子任务ID: {}, 文件数量: {}", 
                subTaskId, files != null ? files.size() : 0);
        
        try {
            // 1. 查询或创建提交记录
            TaskSubmission submission = taskSubmissionMapper.findBySubTaskIdAndUserId(subTaskId, userId);
            boolean isNewSubmission = false;
            
            if (submission == null) {
                // 创建新的提交记录
                // 🔧 优化：首次提交降级为DEBUG，减少用户信息泄露
                log.debug("首次提交任务，子任务ID: {}", subTaskId);
                
                submission = TaskSubmission.builder()
                        .subTaskId(subTaskId)
                        .userId(userId)
                        .submissionNotice(submissionDto.getSubmissionNotice())
                        .status(2) // 待审核状态
                        .submissionTime(LocalDateTime.now())
                        .build();
                
                // 🔧 优化：准备插入的对象信息降级为DEBUG
                log.debug("准备插入的提交对象: 子任务ID={}", submission.getSubTaskId());
                
                taskSubmissionMapper.insert(submission);
                isNewSubmission = true;
                // 🔧 优化：创建记录降级为DEBUG
                log.debug("创建新的提交记录，提交ID: {}", submission.getSubmissionId());
            } else {
                // 更新现有提交记录（重新提交）
                log.info("重新提交任务，原提交ID: {}, 新的提交说明: '{}'", 
                        submission.getSubmissionId(), submissionDto.getSubmissionNotice());
                
                TaskSubmission submissionToUpdate = TaskSubmission.builder()
                        .submissionId(submission.getSubmissionId()) // 必须有ID
                        .submissionNotice(submissionDto.getSubmissionNotice())
                        .submissionTime(LocalDateTime.now())
                        .status(2) // 重新设为待审核状态
                        .build();
                
                log.info("准备更新的提交对象: ID={}, 提交说明='{}'", 
                        submissionToUpdate.getSubmissionId(), submissionToUpdate.getSubmissionNotice());
                
                taskSubmissionMapper.update(submissionToUpdate);
                
                // 【修复】只有在有新文件时才删除旧附件
                if (files != null && !files.isEmpty()) {
                    log.info("重新提交任务，有新文件上传，删除旧附件记录，提交ID: {}", submission.getSubmissionId());
                    attachmentMapper.deleteBySubmissionId(submission.getSubmissionId());
                } else {
                    log.info("重新提交任务，无新文件上传，保留原有附件，提交ID: {}", submission.getSubmissionId());
                }
                
                // 同时更新子任务状态为待审核
                updateSubTaskStatus(subTaskId, 2);
                
                log.info("重新提交任务，提交ID: {}, 子任务ID: {}", submission.getSubmissionId(), subTaskId);
            }
            
            // 2. 处理文件上传
            if (files != null && !files.isEmpty()) {
                log.info("开始处理文件上传，文件数量: {}", files.size());
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    if (file.isEmpty()) {
                        log.warn("文件{}为空，跳过处理", i);
                        continue;
                    }
                    
                    // 验证文件安全性和大小
                    Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                    if (!validationResult.isSuccess()) {
                        log.error("文件验证失败: {}", validationResult.getMsg());
                        throw new BusinessException("文件验证失败: " + validationResult.getMsg());
                    }
                    
                    log.info("处理文件{}: 原始文件名={}, 大小={} bytes, 类型={}, 验证通过", 
                            i, file.getOriginalFilename(), file.getSize(), file.getContentType());
                    
                    // 获取原始文件名
                    String originalFileName = null;
                    if (submissionDto.getOriginalFileNames() != null && i < submissionDto.getOriginalFileNames().size()) {
                        originalFileName = submissionDto.getOriginalFileNames().get(i);
                    }
                    if (originalFileName == null || originalFileName.trim().isEmpty()) {
                        originalFileName = file.getOriginalFilename();
                    }
                    
                    log.info("确定文件名: {}", originalFileName);
                    
                    // 保存文件
                    try {
                        String relativePath = FileUtils.saveFile(file, FileType.TASK_SUBMISSION);
                        log.info("文件保存成功，原始文件名: {}, 相对路径: {}", originalFileName, relativePath);
                        
                        // 创建附件记录
                        TaskSubmissionAttachment attachment = TaskSubmissionAttachment.builder()
                                .submissionId(submission.getSubmissionId())
                                .fileName(originalFileName)
                                .filePath(relativePath)
                                .fileSize(file.getSize())
                                .fileType(file.getContentType())
                                .createTime(LocalDateTime.now())
                                .build();
                        
                        attachmentMapper.insert(attachment);
                        log.info("附件记录创建成功，文件名: {}", originalFileName);
                    } catch (Exception fileEx) {
                        log.error("保存文件失败，文件名: {}", originalFileName, fileEx);
                        throw new BusinessException("保存文件失败: " + originalFileName);
                    }
                }
            }
            
            // 3. 更新子任务状态为待审核
            updateSubTaskStatus(subTaskId, 2); // 2表示待审核
            
            // 4. 更新主任务状态
            updateMainTaskStatusSimple(subTaskId);
            
            // 5. 清除相关缓存
            clearTaskRelatedCache(subTaskId, userId);
            
            log.info("任务提交完成，子任务ID: {}, 用户ID: {}", subTaskId, userId);
            
        } catch (Exception e) {
            log.error("提交任务失败", e);
            throw new BusinessException("提交任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewSubmission(Long submissionId, Integer status, String reviewComment, Long reviewerId) {
        log.info("审核任务提交，提交ID: {}, 状态: {}, 审核人: {}", submissionId, status, reviewerId);
        
        try {
            // 查询提交记录
            TaskSubmission submission = taskSubmissionMapper.findById(submissionId);
            if (submission == null) {
                throw new BusinessException("提交记录不存在");
            }
            
            // 更新提交记录
            submission.setStatus(status);
            submission.setReviewComment(reviewComment);
            submission.setReviewTime(LocalDateTime.now());
            taskSubmissionMapper.update(submission);
            
            // 智能更新子任务状态（基于所有成员的提交状态）
            updateSubTaskStatusIntelligently(submission.getSubTaskId());
            
            // 更新主任务状态
            updateMainTaskStatusSimple(submission.getSubTaskId());
            
            // 清除相关缓存
            clearTaskRelatedCache(submission.getSubTaskId(), submission.getUserId());
            
            log.info("审核完成，提交ID: {}", submissionId);
            
        } catch (Exception e) {
            log.error("审核任务提交失败", e);
            throw new BusinessException("审核失败: " + e.getMessage());
        }
    }

    /**
     * 获取待审核提交数量（根据当前用户角色过滤）
     * @return 待审核数量
     */
    @Override
    public int getPendingSubmissionCount() {
        // 获取当前用户信息
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取待审核提交数量，角色: {}", userRole);
        
        // 调用修改后的方法，传递角色参数
        return taskSubmissionMapper.countPendingSubmissions(currentUserId, userRole);
    }

    /**
     * 获取今日已处理的任务提交数量（根据当前用户角色过滤）
     * @return 今日已处理数量
     */
    @Override
    public int getTodayProcessedCount() {
        // 获取当前用户信息
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取今日已处理任务提交数量，角色: {}", userRole);
        
        // 调用修改后的方法，传递角色参数
        return taskSubmissionMapper.countTodayProcessedSubmissions(currentUserId, userRole);
    }

    /**
     * 获取已审批的任务提交记录（根据当前用户角色过滤）
     * @param days 查询天数
     * @return 已审批记录列表
     */
    @Override
    public List<com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo> getProcessedTaskSubmissions(Integer days) {
        // 获取当前用户信息
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取已审批的任务提交记录，天数: {}, 角色: {}", days, userRole);
        
        // 调用修改后的方法，传递角色参数
        List<TaskSubmissionVo> submissions = taskSubmissionMapper.findProcessedSubmissions(days, currentUserId, userRole);
        
        return submissions.stream().map(submission -> {
            String status = submission.getStatus() == 1 ? "approved" : "rejected";
            
            return com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo.builder()
                    .recordId(submission.getSubmissionId())
                    .approvalType("task")
                    .title(submission.getTaskName() + " - " + submission.getSubTaskTitle())
                    .applicantName(submission.getUserName())
                    .applicantAvatar(submission.getUserAvatar())
                    .status(status)
                    .reviewTime(submission.getReviewTime())
                    .reviewComment(submission.getReviewComment())
                    .applicationTime(submission.getSubmissionTime())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 分页查询任务提交列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @return 分页结果
     */
    @Override
    public PageResult getTaskSubmissionList(Integer page, Integer pageSize, Integer status) {
        log.info("分页查询任务提交列表，页码: {}, 每页大小: {}, 状态: {}", page, pageSize, status);
        
        // 计算偏移量
        Integer offset = (page - 1) * pageSize;
        
        // 查询总数
        int total = taskSubmissionMapper.countSubmissions(status);
        log.info("查询到任务提交总数: {}", total);
        
        // 查询列表数据（直接返回VO）
        List<TaskSubmissionVo> submissionVos = taskSubmissionMapper.findSubmissionsByPage(offset, pageSize, status);
        log.info("查询到任务提交数量: {}", submissionVos.size());
        
        // 构建分页结果
        PageResult pageResult = new PageResult();
        pageResult.setRecords(submissionVos);
        pageResult.setTotal((long) total);
        pageResult.setPage(page);
        pageResult.setPageSize(pageSize);
        
        return pageResult;
    }
    
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
    @Override
    public PageResult getTaskSubmissionListByUserRole(Integer page, Integer pageSize, Integer status, Long currentUserId) {
        log.info("根据用户角色分页查询任务提交列表，页码: {}, 每页大小: {}, 状态: {}, 用户ID: {}", page, pageSize, status, currentUserId);
        
        try {
            // 获取用户信息，判断角色
            User currentUser = userMapper.getUserById(currentUserId);
            if (currentUser == null) {
                throw new BusinessException("用户不存在");
            }
            
            Long positionId = currentUser.getPositionId();
            if (positionId == null) {
                throw new BusinessException("用户职位信息不完整");
            }
            
            // 计算偏移量
            Integer offset = (page - 1) * pageSize;
            
            int total = 0;
            List<TaskSubmissionVo> submissionVos = new ArrayList<>();
            
            // 根据职位ID判断角色权限
            if (positionId == 8L || positionId == 6L || positionId == 7L) {
                // 超级管理员、主任、副主任：查看所有任务提交
                log.info("管理员权限，获取所有任务提交");
                total = taskSubmissionMapper.countSubmissions(status);
                submissionVos = taskSubmissionMapper.findSubmissionsByPage(offset, pageSize, status);
                
            } else if (positionId == 5L) {
                // 老师：只能看自己创建任务的提交
                log.info("老师权限，获取自己创建任务的提交");
                total = taskSubmissionMapper.countSubmissionsByCreator(status, currentUserId);
                submissionVos = taskSubmissionMapper.findSubmissionsByCreator(offset, pageSize, status, currentUserId);
                
            } else {
                // 学生：只能看自己参与任务的提交
                log.info("学生权限，获取自己参与任务的提交");
                // 先通过user_id获取student_id
                Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
                if (studentId != null) {
                    total = taskSubmissionMapper.countSubmissionsByStudent(status, studentId);
                    submissionVos = taskSubmissionMapper.findSubmissionsByStudent(offset, pageSize, status, studentId);
                } else {
                    log.warn("用户ID {} 未找到对应的学生记录", currentUserId);
                }
            }
            
            log.info("用户ID {} (职位ID: {}) 查询到任务提交数量: {}, 总数: {}", currentUserId, positionId, submissionVos.size(), total);
            
            // 构建分页结果
            PageResult pageResult = new PageResult();
            pageResult.setRecords(submissionVos);
            pageResult.setTotal((long) total);
            pageResult.setPage(page);
            pageResult.setPageSize(pageSize);
            
            return pageResult;
            
        } catch (Exception e) {
            log.error("根据用户角色获取任务提交列表失败: {}", e.getMessage());
            throw new BusinessException("获取任务提交列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务提交的附件列表
     * @param submissionId 提交ID
     * @return 附件列表
     */
    @Override
    public List<TaskSubmissionAttachment> getSubmissionAttachments(Long submissionId) {
        log.info("获取任务提交附件列表，提交ID: {}", submissionId);
        try {
            List<TaskSubmissionAttachment> attachments = attachmentMapper.findBySubmissionId(submissionId);
            log.info("获取到附件数量: {}", attachments.size());
            return attachments;
        } catch (Exception e) {
            log.error("获取任务提交附件列表失败", e);
            throw new BusinessException("获取附件列表失败");
        }
    }

    /**
     * 转换TaskSubmission为TaskSubmissionVo
     * @param submission 任务提交实体
     * @return TaskSubmissionVo
     */
    private TaskSubmissionVo convertToVo(TaskSubmission submission) {
        TaskSubmissionVo vo = new TaskSubmissionVo();
        vo.setSubmissionId(submission.getSubmissionId());
        vo.setSubTaskId(submission.getSubTaskId());
        vo.setUserId(submission.getUserId());
        vo.setSubmissionNotice(submission.getSubmissionNotice());
        // 设置文件列表（如果字段存在的话）
        // vo.setSubmissionFileList(submission.getSubmissionFileList());
        vo.setStatus(submission.getStatus());
        vo.setSubmissionTime(submission.getSubmissionTime());
        vo.setReviewComment(submission.getReviewComment());
        vo.setReviewTime(submission.getReviewTime());
        
        return vo;
    }

    /**
     * 更新子任务状态
     * @param subTaskId 子任务ID
     * @param status 状态
     */
    private void updateSubTaskStatus(Long subTaskId, int status) {
        try {
            subTaskMapper.updateStatus(subTaskId, status);
            log.info("子任务状态更新成功，子任务ID: {}, 状态: {}", subTaskId, status);
        } catch (Exception e) {
            log.error("更新子任务状态失败，子任务ID: {}, 状态: {}", subTaskId, status, e);
            throw new BusinessException("更新子任务状态失败");
        }
    }

    /**
     * 清除任务相关缓存
     * @param subTaskId 子任务ID
     * @param userId 用户ID
     */
    private void clearTaskRelatedCache(Long subTaskId, Long userId) {
        try {
            log.info("开始清除任务相关缓存，子任务ID: {}, 用户ID: {}", subTaskId, userId);
            
            // 清除任务列表缓存
            Set<String> taskKeys = redisTemplate.keys("task:*");
            if (taskKeys != null && !taskKeys.isEmpty()) {
                redisTemplate.delete(taskKeys);
                log.info("清除任务列表缓存，数量: {}", taskKeys.size());
            }
            
            // 清除用户任务进度缓存
            Set<String> userTaskKeys = redisTemplate.keys("user:task:*");
            if (userTaskKeys != null && !userTaskKeys.isEmpty()) {
                redisTemplate.delete(userTaskKeys);
                log.info("清除用户任务缓存，数量: {}", userTaskKeys.size());
            }
            
            // 清除任务提交相关缓存
            Set<String> submissionKeys = redisTemplate.keys("task:submission:*");
            if (submissionKeys != null && !submissionKeys.isEmpty()) {
                redisTemplate.delete(submissionKeys);
                log.info("清除任务提交缓存，数量: {}", submissionKeys.size());
            }
            
            // 清除仪表盘统计缓存
            Set<String> dashboardKeys = redisTemplate.keys("dashboard:*");
            if (dashboardKeys != null && !dashboardKeys.isEmpty()) {
                redisTemplate.delete(dashboardKeys);
                log.info("清除仪表盘缓存，数量: {}", dashboardKeys.size());
            }
            
            log.info("任务相关缓存清除完成");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            // 缓存清除失败不应该影响主要业务逻辑
        }
    }
    
    /**
     * 更新主任务状态（简洁版本）
     * @param subTaskId 子任务ID
     */
    private void updateMainTaskStatusSimple(Long subTaskId) {
        try {
            // 1. 通过SQL直接获取taskId - 需要添加一个简单的查询
            // 先从sub_task表获取task_id
            Long taskId = getTaskIdFromSubTask(subTaskId);
            if (taskId == null) {
                log.warn("无法通过子任务ID: {} 获取主任务ID", subTaskId);
                return;
            }
            
            // 2. 获取任务详情和所有子任务的提交状态
            Task task = taskMapper.getTaskDetail(taskId);
            List<Integer> submissionStatuses = taskMapper.getSubTaskSubmissionStatuses(taskId);
            
            if (task == null || submissionStatuses == null || submissionStatuses.isEmpty()) {
                log.info("任务ID: {} 信息不完整，跳过状态更新", taskId);
                return;
            }
            
            // 3. 计算新状态（复用现有逻辑）
            String newStatus = calculateTaskStatus(submissionStatuses, task.getEndTime());
            
            // 4. 仅在状态发生变化时更新
            if (!newStatus.equals(task.getStatus())) {
                Task updateTask = new Task();
                updateTask.setTaskId(taskId);
                updateTask.setStatus(newStatus);
                updateTask.setUpdateTime(LocalDateTime.now());
                taskMapper.update(updateTask);
                
                // 立即清除该任务的详情缓存
                String taskDetailCacheKey = "task:detail:" + taskId;
                redisTemplate.delete(taskDetailCacheKey);
                
                // 清除任务列表缓存
                clearTaskRelatedCache(subTaskId, null);
                
                log.info("主任务状态更新成功，任务ID: {}, {} -> {}, 已清除缓存", taskId, task.getStatus(), newStatus);
            }
            
        } catch (Exception e) {
            log.error("更新主任务状态失败，子任务ID: {}", subTaskId, e);
            // 不抛出异常，避免影响审批流程
        }
    }
    
    /**
     * 通过子任务ID获取主任务ID
     */
    private Long getTaskIdFromSubTask(Long subTaskId) {
        try {
            SubTask subTask = subTaskMapper.findById(subTaskId);
            return subTask != null ? subTask.getTaskId() : null;
        } catch (Exception e) {
            log.error("获取主任务ID失败", e);
            return null;
        }
    }
    
    /**
     * 计算主任务状态（优化版）
     * 优先级：被退回 > 待审核 > 已逾期 > 已完成 > 紧急 > 进行中
     */
    private String calculateTaskStatus(List<Integer> submissionStatuses, LocalDateTime endTime) {
        log.info("计算任务状态，提交状态列表: {}", submissionStatuses);
        
        // 统计各种状态的数量
        long rejectedCount = submissionStatuses.stream().filter(status -> status != null && status == 3).count();
        long pendingCount = submissionStatuses.stream().filter(status -> status != null && status == 2).count();
        long completedCount = submissionStatuses.stream().filter(status -> status != null && status == 1).count();
        long inProgressCount = submissionStatuses.stream().filter(status -> status != null && status == 0).count();
        
        // 1. 最高优先级：有被退回的提交
        if (rejectedCount > 0) {
            log.info("存在被退回的提交 ({}个)，返回REJECTED", rejectedCount);
            return "REJECTED";
        }
        
        // 2. 有待审核的提交
        if (pendingCount > 0) {
            log.info("存在待审核的提交 ({}个)，返回PENDING_REVIEW", pendingCount);
            return "PENDING_REVIEW";
        }
        
        // 3. 检查是否已逾期（且未全部完成）
        if (endTime != null && LocalDateTime.now().isAfter(endTime)) {
            if (completedCount < submissionStatuses.size()) {
                log.info("任务已逾期且未全部完成，返回OVERDUE");
                return "OVERDUE";
            }
        }
        
        // 4. 检查是否全部完成
        if (completedCount == submissionStatuses.size() && submissionStatuses.size() > 0) {
            log.info("所有子任务都已完成 ({}/{}个)，返回COMPLETED", completedCount, submissionStatuses.size());
            return "COMPLETED";
        }
        
        // 5. 检查是否为紧急任务（3天内到期且未全部完成）
        if (endTime != null && !LocalDateTime.now().isAfter(endTime)) {
            LocalDateTime urgentThreshold = LocalDateTime.now().plusDays(3);
            if (endTime.isBefore(urgentThreshold) && completedCount < submissionStatuses.size()) {
                log.info("任务即将到期且未完成，返回URGENT");
                return "URGENT";
            }
        }
        
        // 6. 默认：进行中
        log.info("任务状态默认为进行中，已完成: {}, 进行中: {}, 总数: {}", completedCount, inProgressCount, submissionStatuses.size());
        return "IN_PROGRESS";
    }
    
    /**
     * 智能更新子任务状态（基于所有成员的提交状态）
     * @param subTaskId 子任务ID
     */
    private void updateSubTaskStatusIntelligently(Long subTaskId) {
        try {
            // 获取该子任务的所有提交状态
            List<Integer> submissionStatuses = taskSubmissionMapper.getSubmissionStatusesBySubTaskId(subTaskId);
            
            if (submissionStatuses == null || submissionStatuses.isEmpty()) {
                // 没有提交记录，保持初始状态（进行中）
                updateSubTaskStatus(subTaskId, 0);
                log.info("子任务{}无提交记录，设置为进行中状态", subTaskId);
                return;
            }
            
            // 统计各种状态的数量
            long rejectedCount = submissionStatuses.stream().filter(status -> status != null && status == 3).count();
            long pendingCount = submissionStatuses.stream().filter(status -> status != null && status == 2).count();
            long completedCount = submissionStatuses.stream().filter(status -> status != null && status == 1).count();
            
            // 优先级：被退回 > 待审核 > 已完成 > 进行中
            int newSubTaskStatus;
            if (rejectedCount > 0) {
                newSubTaskStatus = 3; // 已退回
                log.info("子任务{}有被退回的提交，设置为退回状态", subTaskId);
            } else if (pendingCount > 0) {
                newSubTaskStatus = 2; // 待审核
                log.info("子任务{}有待审核的提交，设置为待审核状态", subTaskId);
            } else if (completedCount == submissionStatuses.size() && submissionStatuses.size() > 0) {
                newSubTaskStatus = 1; // 已完成（所有成员都已通过审核）
                log.info("子任务{}所有成员都已通过审核，设置为完成状态", subTaskId);
            } else {
                newSubTaskStatus = 0; // 进行中
                log.info("子任务{}设置为进行中状态", subTaskId);
            }
            
            updateSubTaskStatus(subTaskId, newSubTaskStatus);
            log.info("子任务{}状态智能更新完成，新状态: {}", subTaskId, newSubTaskStatus);
            
        } catch (Exception e) {
            log.error("智能更新子任务状态失败，子任务ID: {}", subTaskId, e);
            // 失败时不抛出异常，避免影响主要业务流程
        }
    }

    /**
     * 获取当前用户角色
     * @param currentUserId 当前用户ID
     * @return 用户角色字符串
     */
    private String getCurrentUserRole(Long currentUserId) {
        if (currentUserId == null) {
            return "student"; // 默认学生角色
        }
        
        try {
            User currentUser = userMapper.getUserById(currentUserId);
            if (currentUser != null && currentUser.getRoleId() != null) {
                Long roleId = currentUser.getRoleId();
                if (roleId == 2L) {
                    return "teacher"; // 老师
                } else if (roleId == 3L || roleId == 4L || roleId == 7L) {
                    return "admin"; // 管理员/超级管理员/工作室管理员
                }
            }
        } catch (Exception e) {
            log.error("获取用户角色失败，用户ID: {}", currentUserId, e);
        }
        
        return "student"; // 默认学生角色
    }
} 