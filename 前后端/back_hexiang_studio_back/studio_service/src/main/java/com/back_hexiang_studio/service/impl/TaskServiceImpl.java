package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.GlobalException.BaseException;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.dto.task.PageTaskPageDto;

import com.back_hexiang_studio.dv.dto.task.TaskAddDto;
import com.back_hexiang_studio.dv.dto.task.TaskStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.task.TaskUpdateDto;
import com.back_hexiang_studio.dv.vo.task.*;
import com.back_hexiang_studio.entity.SubTask;
import com.back_hexiang_studio.entity.Task;
import com.back_hexiang_studio.entity.TaskAttachment;
import com.back_hexiang_studio.entity.SubTaskMember;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.mapper.SubTaskMapper;
import com.back_hexiang_studio.mapper.SubTaskMemberMapper;
import com.back_hexiang_studio.mapper.TaskAttachmentMapper;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.TaskService;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.back_hexiang_studio.utils.FileValidationManager;
import com.back_hexiang_studio.utils.NotificationUtils;
import com.back_hexiang_studio.context.UserContextHolder;
import com.fasterxml.jackson.databind.ser.Serializers;
import java.time.LocalDateTime;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.enumeration.FileType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import java.util.logging.Handler;
import com.back_hexiang_studio.context.UserContextHolder;
import java.util.stream.Collectors;
import java.io.IOException;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private SubTaskMemberMapper subTaskMemberMapper;
    @Autowired
    private SubTaskMapper subTaskMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private TaskAttachmentMapper taskAttachmentMapper;
    @Autowired
    private FileValidationManager fileValidationManager;

    /**
     * 获取任务列表
     * @param pageTaskPageDto
     * @return
     */
    @Override
    public PageResult getTasks(PageTaskPageDto pageTaskPageDto) {
        // 获取当前用户角色（基于职位ID判断）
        Long currentUserId = pageTaskPageDto.getCurrentUserId();
        String userRole = "student"; // 默认学生角色
        
        if (currentUserId != null) {
            // 获取用户信息
            User currentUser = userMapper.getUserById(currentUserId);
            if (currentUser != null && currentUser.getPositionId() != null) {
                Long positionId = currentUser.getPositionId();
                // 根据职位ID判断角色 - 与CourseServiceImpl和其他Service保持一致
                if (positionId == 8L) {
                    userRole = "admin"; // 超级管理员
                } else if (positionId == 6L || positionId == 7L) {
                    userRole = "manager"; // 主任、副主任
                } else if (positionId == 5L) {
                    userRole = "teacher"; // 老师
                } else {
                    userRole = "student"; // 学生（部长、副部长、普通学员）
                }
            }
        }
        
        // 缓存键需要包含用户角色，确保不同角色的缓存分离
        String cacheKey = "task:list:" + userRole + ":" + currentUserId + ":" + pageTaskPageDto.getPage() + ":" + pageTaskPageDto.getSize();
        if (pageTaskPageDto.getKeyword() != null) {
            cacheKey += ":keyWord:" + pageTaskPageDto.getKeyword();
        }
        if (pageTaskPageDto.getStartTime() != null) {
            cacheKey += ":startTime:" + pageTaskPageDto.getStartTime();
        }
        if (pageTaskPageDto.getEndTime() != null) {
            cacheKey += ":endTime:" + pageTaskPageDto.getEndTime();
        }
        if (pageTaskPageDto.getStatus() != null) {
            cacheKey += ":taskStatus:" + pageTaskPageDto.getStatus();
        }

        // 尝试从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
             // 如果从缓存获取，也需要设置PageHelper以确保后续逻辑一致
            PageHelper.startPage(pageTaskPageDto.getPage(), pageTaskPageDto.getSize());
            return (PageResult) cacheResult;
        }
        
        PageHelper.startPage(pageTaskPageDto.getPage(), pageTaskPageDto.getSize());

        // 构造查询条件
        Task taskQuery = new Task();
        if (pageTaskPageDto.getStartTime() != null && !pageTaskPageDto.getStartTime().isEmpty()) {
            // 将 YYYY-MM-DD 转换为 YYYY-MM-DD 00:00:00
            taskQuery.setStartTime(DateTimeUtils.parseDateTime(pageTaskPageDto.getStartTime() + " 00:00:00"));
        }
        if (pageTaskPageDto.getEndTime() != null && !pageTaskPageDto.getEndTime().isEmpty()) {
            // 将 YYYY-MM-DD 转换为 YYYY-MM-DD 23:59:59
            taskQuery.setEndTime(DateTimeUtils.parseDateTime(pageTaskPageDto.getEndTime() + " 23:59:59"));
        }
        if (pageTaskPageDto.getKeyword() != null && !pageTaskPageDto.getKeyword().isEmpty()) {
            taskQuery.setTitle(pageTaskPageDto.getKeyword());
        }
        if (pageTaskPageDto.getStatus() != null && !pageTaskPageDto.getStatus().isEmpty()) {
            taskQuery.setStatus(pageTaskPageDto.getStatus());
        }

        // 调用带角色过滤的查询方法
        List<TasksVo> tasks = taskMapper.getTasksWithRole(taskQuery, currentUserId, userRole);
        if (tasks == null||tasks.isEmpty()) {
            log.info("未查询到任务列表");
            // 即使没有数据，也应该返回一个空的分页结果，而不是null
            return new PageResult(0, Collections.emptyList(), pageTaskPageDto.getPage(), pageTaskPageDto.getSize(), 0);
        }

        // 先用原始查询结果创建PageInfo以获取正确的分页信息
        PageInfo<TasksVo> originalPageInfo = new PageInfo<>(tasks);

        // SQL查询已经格式化了时间字段，直接使用
        for(TasksVo tasksVo : tasks){
            log.info("任务列表：{}",tasksVo.toString());
        }

        // 使用 originalPageInfo 中的正确总数来构建 PageResult
        PageResult pageResult=new PageResult(
                originalPageInfo.getTotal(), // 使用正确的总记录数
                tasks,                  // 当前页数据
                originalPageInfo.getPageNum(),  // 当前页码
                originalPageInfo.getPageSize(), // 每页大小
                originalPageInfo.getPages()     // 总页数
        );

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, pageResult, 300, TimeUnit.SECONDS); // 设置5分钟过期

        return pageResult;
    }

    /**
     * 添加任务
     * @param taskAddDto
     * @return
     */
    @Override
    @Transactional
    @AutoFill(value = OperationType.INSERT)
    public Boolean addTask(TaskAddDto taskAddDto) {
        if (taskAddDto == null) {
            throw new BusinessException("添加任务参数为空");
        }
        try {
            // 调试信息：检查当前用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            log.info("创建任务时的当前用户ID: {}", currentUserId);
            
            Task task = new Task();
            BeanUtils.copyProperties(taskAddDto, task);
            task.setStatus("IN_PROGRESS");
            task.setStartTime(DateTimeUtils.parseDateTime(taskAddDto.getStartTime()));
            task.setEndTime(DateTimeUtils.parseDateTime(taskAddDto.getEndTime()));
            
            // 手动设置创建人和创建时间，防止AOP失效
            if (currentUserId != null) {
                task.setCreateUser(currentUserId);
                task.setUpdateUser(currentUserId);
                task.setCreateTime(LocalDateTime.now());
                task.setUpdateTime(LocalDateTime.now());
                log.info("手动设置任务创建信息: 创建人={}, 创建时间={}", currentUserId, task.getCreateTime());
            } else {
                log.warn("当前用户ID为null，无法设置创建人信息");
            }
            
                taskMapper.insert(task);
                Long taskId = task.getTaskId();
            taskAddDto.setTaskId(taskId); // 回填ID
            
            // 验证创建人是否成功保存
            log.info("任务插入数据库后，任务ID: {}, 创建人: {}, 创建时间: {}", 
                taskId, task.getCreateUser(), task.getCreateTime());

                if (taskAddDto.getSubTasks() != null) {
                    processSubTasks(taskAddDto.getSubTasks(), taskId);
                }
                clearTaskCache();
                return true;
            } catch (Exception e) {
                log.info("添加任务失败: {}", e.getMessage());
                throw new BusinessException("添加任务失败");
            }
    }
    
    @Override
    @Transactional
    public void addTaskWithAttachments(TaskAddDto taskAddDto, List<MultipartFile> files) {
        // 1. 先保存任务基本信息
        addTask(taskAddDto);
        Long taskId = taskAddDto.getTaskId();

        // 2. 保存附件
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                try {
                    Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                    if (!validationResult.isSuccess()) {
                        throw new BusinessException("文件验证失败: " + validationResult.getMsg());
                    }
                    String filePath = FileUtils.saveFile(file, FileType.TASK_ATTACHMENT);
                    TaskAttachment attachment = new TaskAttachment();
                    attachment.setTaskId(taskId);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFilePath(filePath);
                    attachment.setFileSize(file.getSize());
                    attachment.setFileType(file.getContentType());
                    attachment.setUploaderId(UserContextHolder.getCurrentId());
                    attachment.setUploadTime(LocalDateTime.now());
                    taskAttachmentMapper.insert(attachment);
                } catch (IOException e) {
                    log.error("任务附件保存失败: {}", file.getOriginalFilename(), e);
                    throw new BusinessException("任务附件保存失败");
                }
            }
        }
        
        // 3. 创建任务通知
        try {
            // 获取当前用户ID作为发送者
            Long senderId = UserContextHolder.getCurrentId();
            
            // 为每个子任务成员创建通知
            if (taskAddDto.getSubTasks() != null) {
                for (SubTaskVo subTask : taskAddDto.getSubTasks()) {
                    if (subTask.getMembers() != null) {
                        for (SubTaskMemberVo member : subTask.getMembers()) {
                            // 构建通知内容
                            String title = "新任务分配: " + taskAddDto.getTitle();
                            String content = "您被分配了一个新任务: " + taskAddDto.getTitle() + 
                                "\n子任务: " + subTask.getTitle() + 
                                "\n开始时间: " + taskAddDto.getStartTime() + 
                                "\n截止时间: " + taskAddDto.getEndTime();
                            
                            // 创建通知
                            NotificationUtils.createTaskNotification(
                                title,
                                content,
                                taskId,
                                senderId,
                                member.getUserId(),
                                1  // 重要程度: 1表示重要
                            );
                        }
                    }
                }
            }
            log.info("成功为任务ID: {} 创建任务通知", taskId);
        } catch (Exception e) {
            log.error("为任务创建通知失败: {}", e.getMessage(), e);
            // 通知创建失败不影响任务创建
        }
    }


    /**
     * 获任务详细信息
     * @param taskId
     * @return
     */
    @Override
    public TaskDetailVo detail(Long taskId) {
        if (taskId == null){
            log.info("任务ID为空");
            throw new BusinessException("任务ID为空");
        }

        //定义缓存key
        String tasKey = "task:detail:" + taskId;
        //从缓存获取
        Object taskResult=redisTemplate.opsForValue().get(tasKey);
        if (taskResult != null){
            log.info("从缓存获取任务详情");
            return (TaskDetailVo) taskResult;
        }
        log.info("缓存未命中，从数据库查询任务详情");
        TaskDetailVo taskDetail=new TaskDetailVo();
        try{
            //获取主任务

             Task task =taskMapper.getTaskDetail(taskId);
            BeanUtils.copyProperties(task,taskDetail);
            Long createUserId=task.getCreateUser();
            String creatUserName=userMapper.getUserNameById(createUserId);
            taskDetail.setCreatUserName(creatUserName);

            //获取相关子任务
            List<SubTaskDetailVo> subTasks=taskMapper.getSubTasks(taskId);
            //获取相关成员
            for (SubTaskDetailVo subTaskDetailVo :subTasks){
                List<SubTaskMemberVo> subTaskMembers=taskMapper.getSubMembers(subTaskDetailVo.getSubTaskId());
                subTaskDetailVo.setMembers(subTaskMembers);
            }
            taskDetail.setSubTasks(subTasks);
            //获取子任务
        }catch (Exception e){
            log.info("获取任务详情失败: {}", e.getMessage());
            throw new BusinessException("获取任务详情失败");
        }
        
        // 获取附件
        List<TaskAttachmentVo> attachments = taskAttachmentMapper.getByTaskId(taskId);
        taskDetail.setAttachments(attachments);


        //        //将结果存入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(tasKey, taskDetail, 5, TimeUnit.MINUTES);
        return taskDetail ;
    }

    /**
     * 更新任务
     * @param taskUpdateDto
     */
    @Override
    @Transactional
    @AutoFill(value = OperationType.UPDATE)
    public void updateTask(TaskUpdateDto taskUpdateDto) {
        try {
            // 1. 更新主任务
            Task task = new Task();
            BeanUtils.copyProperties(taskUpdateDto, task);
            task.setStartTime(DateTimeUtils.parseDateTime(taskUpdateDto.getStartTime()));
            task.setEndTime(DateTimeUtils.parseDateTime(taskUpdateDto.getEndTime()));
            taskMapper.update(task);

            Long taskId = taskUpdateDto.getTaskId();

            // 2. 删除旧的子任务成员
            subTaskMemberMapper.deleteByTaskId(taskId);

            // 3. 删除旧的子任务
            subTaskMapper.deleteByTaskId(taskId);

            // 4. 插入新的子任务和成员
            if (taskUpdateDto.getSubTasks() != null) {
                processSubTasks(taskUpdateDto.getSubTasks(), taskId);
            }

            // 5. 清理缓存
            clearTaskCache();
        } catch (Exception e) {
            log.error("更新任务失败: {}", e.getMessage(), e);
            throw new BusinessException("更新任务失败");
        }
    }

    @Override
    @Transactional
    public void updateTaskWithAttachments(TaskUpdateDto taskUpdateDto, List<MultipartFile> newFiles, List<Long> keepAttachmentIds) {
        // 1. 更新任务基本信息
        updateTask(taskUpdateDto);
        Long taskId = taskUpdateDto.getTaskId();

        // 2. 清理不再需要的旧附件
        List<TaskAttachmentVo> oldAttachments = taskAttachmentMapper.getByTaskId(taskId);
        if (oldAttachments != null && !oldAttachments.isEmpty()) {
            List<Long> attachmentsToDeleteIds = oldAttachments.stream()
                .map(TaskAttachmentVo::getAttachmentId)
                .filter(id -> keepAttachmentIds == null || !keepAttachmentIds.contains(id))
                .collect(Collectors.toList());
            
            if (!attachmentsToDeleteIds.isEmpty()) {
                List<String> filesToDelete = attachmentsToDeleteIds.stream()
                    .map(id -> taskAttachmentMapper.getById(id).getFilePath())
                    .collect(Collectors.toList());
                taskAttachmentMapper.deleteBatch(attachmentsToDeleteIds);
                filesToDelete.forEach(FileUtils::deleteFile);
            }
        }

        // 3. 插入新上传的附件
        if (newFiles != null && !newFiles.isEmpty()) {
            for (MultipartFile file : newFiles) {
                 try {
                     Result<?> validationResult = fileValidationManager.validateFile(file, "attachment");
                     if (!validationResult.isSuccess()) {
                         throw new BusinessException("文件验证失败: " + validationResult.getMsg());
                     }
                    String filePath = FileUtils.saveFile(file, FileType.TASK_ATTACHMENT);
                    TaskAttachment attachment = new TaskAttachment();
                    attachment.setTaskId(taskId);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFilePath(filePath);
                    attachment.setFileSize(file.getSize());
                    attachment.setFileType(file.getContentType());
                    attachment.setUploaderId(UserContextHolder.getCurrentId());
                    attachment.setUploadTime(LocalDateTime.now());
                    taskAttachmentMapper.insert(attachment);
                } catch (IOException e) {
                    log.error("任务附件保存失败: {}", file.getOriginalFilename(), e);
                    throw new BusinessException("任务附件保存失败");
                }
            }
        }
        
        // 4. 创建任务更新通知
        try {
            // 获取当前用户ID作为发送者
            Long senderId = UserContextHolder.getCurrentId();
            
            // 为每个子任务成员创建通知
            if (taskUpdateDto.getSubTasks() != null) {
                for (SubTaskVo subTask : taskUpdateDto.getSubTasks()) {
                    if (subTask.getMembers() != null) {
                        for (SubTaskMemberVo member : subTask.getMembers()) {
                            // 构建通知内容
                            String title = "任务更新: " + taskUpdateDto.getTitle();
                            String content = "您参与的任务已更新: " + taskUpdateDto.getTitle() + 
                                "\n子任务: " + subTask.getTitle() + 
                                "\n开始时间: " + taskUpdateDto.getStartTime() + 
                                "\n截止时间: " + taskUpdateDto.getEndTime();
                            
                            // 创建通知
                            NotificationUtils.createTaskNotification(
                                title,
                                content,
                                taskId,
                                senderId,
                                member.getUserId(),
                                1  // 重要程度: 1表示重要
                            );
                        }
                    }
                }
            }
            log.info("成功为更新的任务ID: {} 创建任务通知", taskId);
        } catch (Exception e) {
            log.error("为更新的任务创建通知失败: {}", e.getMessage(), e);
            // 通知创建失败不影响任务更新
        }
    }

    /**
     * 删除任务
     * @param taskId
     */
    @Override
    public void deleteTask(Long taskId) {
        if (taskId==null){
            log.error("任务ID不能为空");
            throw new BusinessException("任务ID不能为空");
        }
        // 检查任务是否存在等逻辑可以根据需要添加
        Long isExist=taskMapper.isTaskExist(taskId);
        if (isExist==0){
            log.error("任务不存在");
            throw new BusinessException("任务不存在");
        }
        taskMapper.deleteById(taskId);
        // 清理缓存
        clearTaskCache();
    }

    /**
     * 更新任务状态
     * @param taskStatusUpdateDto
     */
    @Override
    public void updateTaskStatus(TaskStatusUpdateDto taskStatusUpdateDto) {
        Task task = new Task();
        task.setTaskId(taskStatusUpdateDto.getTaskId());
        task.setStatus(taskStatusUpdateDto.getStatus());

        if (taskStatusUpdateDto.getEndTime() != null && !taskStatusUpdateDto.getEndTime().isEmpty()) {
            task.setEndTime(DateTimeUtils.parseDateTime(taskStatusUpdateDto.getEndTime()));
        }

        // Apply AutoFill logic manually for update user and time
        task.setUpdateTime(LocalDateTime.now());
        task.setUpdateUser(UserContextHolder.getCurrentId());

        taskMapper.update(task);
        
        // 创建任务状态变更通知
        try {
            Long taskId = taskStatusUpdateDto.getTaskId();
            // 获取任务详情
            TaskDetailVo taskDetail = detail(taskId);
            if (taskDetail != null) {
                // 获取当前用户ID作为发送者
                Long senderId = UserContextHolder.getCurrentId();
                
                // 为每个子任务成员创建通知
                if (taskDetail.getSubTasks() != null) {
                    for (SubTaskDetailVo subTask : taskDetail.getSubTasks()) {
                        if (subTask.getMembers() != null) {
                            for (SubTaskMemberVo member : subTask.getMembers()) {
                                // 构建通知内容
                                String title = "任务状态变更: " + taskDetail.getTitle();
                                String content = "您参与的任务状态已变更: " + taskDetail.getTitle() + 
                                    "\n状态: " + getStatusDisplayName(taskStatusUpdateDto.getStatus());
                                
                                if (taskStatusUpdateDto.getEndTime() != null && !taskStatusUpdateDto.getEndTime().isEmpty()) {
                                    content += "\n新截止时间: " + taskStatusUpdateDto.getEndTime();
                                }
                                
                                // 创建通知
                                NotificationUtils.createTaskNotification(
                                    title,
                                    content,
                                    taskId,
                                    senderId,
                                    member.getUserId(),
                                    1  // 重要程度: 1表示重要
                                );
                            }
                        }
                    }
                }
                log.info("成功为任务状态变更ID: {} 创建任务通知", taskId);
            }
        } catch (Exception e) {
            log.error("为任务状态变更创建通知失败: {}", e.getMessage(), e);
            // 通知创建失败不影响任务状态更新
        }
        
        clearTaskCache();
    }
    
    /**
     * 获取任务状态显示名称
     */
    private String getStatusDisplayName(String status) {
        switch (status) {
            case "NOT_STARTED":
                return "未开始";
            case "IN_PROGRESS":
                return "进行中";
            case "COMPLETED":
                return "已完成";
            case "DELAYED":
                return "已延期";
            default:
                return status;
        }
    }


    /**
     * 获取任务统计数据
     */
    @Override
    public List<SubTaskMemberVo> getSubMembers(Long subTaskId) {
        return taskMapper.getSubMembers(subTaskId);
    }



    /**
     * 添加相关子任务
     * @param subTasks
     * @param taskId
     */
    public void processSubTasks(List<SubTaskVo> subTasks, Long taskId) {

            for (SubTaskVo subTaskVo : subTasks) {
                // 2a. 准备并插入子任务
                subTaskVo.setTaskId(taskId); // 关联主任务ID
                subTaskVo.setStatus(0L); // 设置默认状态
                LocalDateTime currentTime= LocalDateTime.now();
                Long currentUserId = UserContextHolder.getCurrentId();
                subTaskVo.setCreateTime(currentTime);
                subTaskVo.setCreateUser(currentUserId);
                subTaskVo.setUpdateTime(currentTime);
                subTaskVo.setUpdateUser(currentUserId);
                subTaskMapper.insert(subTaskVo); // 传入VO对象，ID会自动回填
                Long subTaskId = subTaskVo.getSubTaskId();

                // 2b. 添加子任务成员
                if (subTaskVo.getMembers() != null) {
                    for (SubTaskMemberVo memberVo : subTaskVo.getMembers()) {
                        memberVo.setSubTaskId(subTaskId); // 关联子任务ID
                        subTaskMemberMapper.insert(memberVo);
                    }
                }
            }



    }




    /**
     * 获取学生老师管理员列表（任务管理）- 带分页
     * @param name 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页后的用户列表
     */
    @Override
    public List<UserList> getUserListPage(String name, Integer page, Integer pageSize) {
        // 缓存key
        String cacheKey = "task:userlist:" + page + ":" + pageSize;
        if (name != null && !name.isEmpty()) {
            cacheKey += ":" + name;
        }

        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<UserList>) cached;
        }

        // 使用PageHelper进行分页
        PageHelper.startPage(page, pageSize);
        
        // 获取用户列表，使用带搜索功能的方法
        List<UserList> userLists = userMapper.getUSerListOfTaskWithKeyword(name);

        // 缓存结果，设置过期时间
        redisTemplate.opsForValue().set(cacheKey, userLists, 300, TimeUnit.SECONDS); // 5分钟

        return userLists;
    }


    /**
     * 获取任务统计数据
     * @return
     */
    @Override
    public TaskStatisticsVo getTaskStatistics() {
        String cacheKey = "task:statistics";

        // 1. 尝试从缓存获取
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取任务统计数据");
            return (TaskStatisticsVo) cachedResult;
        }

        // 2. 从数据库查询
        log.info("缓存未命中，从数据库查询任务统计数据");
        TaskStatisticsVo statistics = taskMapper.getTaskStatistics();

        // 3. 将结果存入缓存，设置5分钟过期
        if (statistics != null) {
            redisTemplate.opsForValue().set(cacheKey, statistics, 5, TimeUnit.MINUTES);
        }

        return statistics;
    }

    /**
     * 根据用户ID获取个人任务统计
     * @param userId 用户ID
     * @return 个人任务统计数据
     */
    @Override
    public TaskStatisticsVo getUserTaskStatistics(Long userId) {
        String cacheKey = "task:user:statistics:" + userId;

        // 1. 尝试从缓存获取
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取用户{}的任务统计数据", userId);
            return (TaskStatisticsVo) cachedResult;
        }

        // 2. 从数据库查询
        log.info("缓存未命中，从数据库查询用户{}的任务统计数据", userId);
        TaskStatisticsVo statistics = taskMapper.getUserTaskStatistics(userId);

        // 3. 将结果存入缓存，设置3分钟过期（用户数据更新频繁）
        if (statistics != null) {
            redisTemplate.opsForValue().set(cacheKey, statistics, 3, TimeUnit.MINUTES);
        }

        return statistics;
    }

    @Override
    public List<MyTaskVO> getUserTaskList(Long userId) {
        log.info("获取用户{}的所有任务列表", userId);
        return taskMapper.getUserTaskList(userId);
    }

    @Override
    public List<MyTaskVO> getUserTaskListByStatus(Long userId, List<String> statusList) {
        log.info("获取用户{}状态为{}的任务列表", userId, statusList);
        return taskMapper.getUserTaskListByStatus(userId, statusList);
    }


    //清理任务管理缓存
    private void clearTaskCache() {
        //获取任务相关缓存关键字
        Set<String> listKeys=redisTemplate.keys("task:*");
        if(listKeys!=null&&!listKeys.isEmpty()){
            redisTemplate.delete(listKeys);
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
            
            // 默认返回进行中
            return "IN_PROGRESS";
            
        } catch (Exception e) {
            log.error("计算任务状态失败，任务ID: {}", taskId, e);
            return originalStatus; // 出错时返回原状态
        }
    }

    /**
     * 审批任务（通过）
     * @param taskId 任务ID
     * @param approvalComment 审批意见
     */
    @Override
    @Transactional
    public void approveTask(Long taskId, String approvalComment) {
        log.info("开始审批任务，任务ID: {}, 审批意见: {}", taskId, approvalComment);
        
        // 检查任务是否存在
        Long taskExists = taskMapper.isTaskExist(taskId);
        if (taskExists == null || taskExists == 0) {
            throw new BusinessException("任务不存在");
        }
        
        // 获取任务详情
        Task task = taskMapper.getTaskDetail(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        // 检查任务状态是否为待审核
        if (!"PENDING_REVIEW".equals(task.getStatus())) {
            throw new BusinessException("只能审批待审核状态的任务");
        }
        
        try {
            // 更新任务状态为已完成
            task.setStatus("COMPLETED");
            task.setUpdateTime(LocalDateTime.now());
            task.setUpdateUser(UserContextHolder.getCurrentId());
            
            taskMapper.update(task);
            
            // 创建审批通知
            createApprovalNotification(taskId, task.getCreateUser(), true, approvalComment);
            
            // 清除任务缓存
            clearTaskCache();
            
            log.info("任务审批成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("审批任务失败，任务ID: {}", taskId, e);
            throw new BusinessException("审批任务失败: " + e.getMessage());
        }
    }

    /**
     * 退回任务
     * @param taskId 任务ID
     * @param rejectionReason 退回原因
     */
    @Override
    @Transactional
    public void rejectTask(Long taskId, String rejectionReason) {
        log.info("开始退回任务，任务ID: {}, 退回原因: {}", taskId, rejectionReason);
        
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BusinessException("退回原因不能为空");
        }
        
        // 检查任务是否存在
        Long taskExists = taskMapper.isTaskExist(taskId);
        if (taskExists == null || taskExists == 0) {
            throw new BusinessException("任务不存在");
        }
        
        // 获取任务详情
        Task task = taskMapper.getTaskDetail(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        // 检查任务状态是否为待审核
        if (!"PENDING_REVIEW".equals(task.getStatus())) {
            throw new BusinessException("只能退回待审核状态的任务");
        }
        
        try {
            // 更新任务状态为被退回
            task.setStatus("REJECTED");
            task.setUpdateTime(LocalDateTime.now());
            task.setUpdateUser(UserContextHolder.getCurrentId());
            
            taskMapper.update(task);
            
            // 创建退回通知
            createApprovalNotification(taskId, task.getCreateUser(), false, rejectionReason);
            
            // 清除任务缓存
            clearTaskCache();
            
            log.info("任务退回成功，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("退回任务失败，任务ID: {}", taskId, e);
            throw new BusinessException("退回任务失败: " + e.getMessage());
        }
    }

    /**
     * 创建审批通知
     * @param taskId 任务ID
     * @param receiverId 接收者ID
     * @param isApproved 是否通过
     * @param comment 意见
     */
    private void createApprovalNotification(Long taskId, Long receiverId, boolean isApproved, String comment) {
        try {
            Long senderId = UserContextHolder.getCurrentId();
            String title = isApproved ? "任务审批通过" : "任务审批退回";
            String content = isApproved ? 
                "您的任务已审批通过" + (comment != null ? "，审批意见：" + comment : "") :
                "您的任务被退回，原因：" + comment;
                
            NotificationUtils.createTaskNotification(
                title,
                content,
                taskId,
                senderId,
                receiverId,
                1 // 重要程度
            );
            
            log.info("审批通知创建成功，任务ID: {}, 接收者: {}, 是否通过: {}", taskId, receiverId, isApproved);
            
        } catch (Exception e) {
            log.error("创建审批通知失败: {}", e.getMessage(), e);
            // 通知创建失败不影响审批流程
        }
    }

}
