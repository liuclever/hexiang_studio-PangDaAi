package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.dv.dto.LeaveRequestQueryDTO;
import com.back_hexiang_studio.dv.dto.LeaveRequestCreateDTO;
import com.back_hexiang_studio.dv.vo.LeaveRequestVO;
import com.back_hexiang_studio.service.LeaveApprovalService;
import com.back_hexiang_studio.mapper.AttendancePlanMapper;
import com.back_hexiang_studio.mapper.LeaveRequestMapper;
import com.back_hexiang_studio.mapper.AttendanceRecordMapper;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.entity.AttendancePlan;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.entity.LeaveRequest;
import com.back_hexiang_studio.context.UserContextHolder;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.back_hexiang_studio.entity.AttendanceRecord;
import com.back_hexiang_studio.enumeration.AttendanceStatus;
import java.util.ArrayList;

/**
 * 请假审批服务实现 (附带Redis缓存)
 */
@Service
@Slf4j
public class LeaveApprovalServiceImpl implements LeaveApprovalService {

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AttendancePlanMapper attendancePlanMapper;
    
    @Autowired
    private AttendanceServiceImpl attendanceService;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询请假申请列表 (带缓存)
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult getLeaveRequests(LeaveRequestQueryDTO queryDTO) {
        // 获取当前用户ID和角色
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        // 根据角色添加查询条件
        if ("teacher".equals(userRole)) {
            // 老师只能看到自己创建的考勤计划相关的请假申请
            queryDTO.setCreatorId(currentUserId);
            log.debug("老师权限：限制查看范围为自己创建的考勤计划");
        } else if ("admin".equals(userRole)) {
            // 管理员可以看到所有请假申请，无需额外限制
            log.debug("管理员权限：可查看所有请假数据");
        } else {
            // 学生角色不应该访问此接口，返回空结果
            log.warn("学生用户尝试访问请假申请列表，拒绝访问，用户ID: {}", currentUserId);
            return new PageResult(0, new ArrayList<>());
        }
        
        // 🔧 优化：删除冗余的查询参数日志
        log.debug("查询请假申请列表，角色: {}", userRole);
        
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<LeaveRequestVO> page = leaveRequestMapper.pageQuery(queryDTO);
        
        // 🔧 修复：过滤掉学生已签到的请假申请
        List<LeaveRequestVO> filteredList = new ArrayList<>();
        for (LeaveRequestVO request : page.getResult()) {
            // 只对待审批的申请进行状态检查
            if ("pending".equals(request.getStatus()) && request.getAttendancePlanId() != null) {
                List<AttendanceRecord> records = attendanceRecordMapper.getByPlanIdAndStudentId(
                    request.getAttendancePlanId(), request.getStudentId());
                if (!records.isEmpty()) {
                    AttendanceRecord record = records.get(0);
                    if (record.getStatus() == AttendanceStatus.present || record.getStatus() == AttendanceStatus.late) {
                        // 🔧 优化：降级为DEBUG，减少日志噪音
                        log.debug("过滤已签到学生的请假申请: requestId={}, status={}", 
                                request.getRequestId(), record.getStatus());
                        continue; // 跳过已签到的请假申请
                    }
                }
            }
            filteredList.add(request);
        }
        
        // 使用过滤后的列表重新构造Page对象
        Page<LeaveRequestVO> filteredPage = new Page<>(page.getPageNum(), page.getPageSize());
        filteredPage.addAll(filteredList);
        filteredPage.setTotal(filteredList.size());
        
        // 使用PageInfo获取完整分页信息
        PageInfo<LeaveRequestVO> pageInfo = new PageInfo<>(filteredPage);
        PageResult result = new PageResult(
            pageInfo.getTotal(),
            pageInfo.getList(),
            pageInfo.getPageNum(),
            pageInfo.getPageSize(),
            pageInfo.getPages()
        );


        log.debug("请假申请查询完成: 总数={}, 当前页数据={}", pageInfo.getTotal(), pageInfo.getList().size());
        
        // 暂时注释缓存逻辑
        // String cacheKey = buildCacheKey(queryDTO);
        // 2. 将结果存入缓存，设置5分钟过期
        // redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 批准请假申请
     * @param requestId 请假申请ID
     * @param approverId 审批人ID
     */
    @Transactional
    @Override
    public void approveRequest(Long requestId, Long approverId) {
        LeaveRequest existingRequest = leaveRequestMapper.getById(requestId);
        if (existingRequest == null) {
            throw new BusinessException("请假申请不存在");
        }
        if (!"pending".equals(existingRequest.getStatus())) {
            throw new BusinessException("该申请已被处理，请勿重复操作");
        }

        LeaveRequest leaveRequestToUpdate = new LeaveRequest();
        leaveRequestToUpdate.setRequestId(requestId);
        leaveRequestToUpdate.setStatus("approved");
        leaveRequestToUpdate.setApproverId(approverId);
        leaveRequestToUpdate.setApprovedAt(LocalDateTime.now());
        leaveRequestMapper.update(leaveRequestToUpdate);

        // 根据考勤计划ID处理考勤记录（审批通过后才处理）
        if (existingRequest.getAttendancePlanId() != null) {
            // 调用考勤服务处理请假审批通过后的考勤记录
            attendanceService.handleApprovedLeaveRequest(
                existingRequest.getStudentId(),
                existingRequest.getAttendancePlanId(),
                approverId
            );
        } else {
            // 兼容旧数据：如果没有考勤计划ID，使用时间范围更新
            attendanceRecordMapper.updateStatusToLeaveByTimeRange(
                existingRequest.getStudentId(),
                existingRequest.getStartTime(),
                existingRequest.getEndTime(),
                approverId
            );
        }
        log.info("学生(ID:{})的请假申请(ID:{})已批准，并更新了相关考勤记录", existingRequest.getStudentId(), requestId);

        // 3. 清理缓存
        clearLeaveRequestCache();
    }

    /**
     * 驳回请假申请
     * @param requestId 请假申请ID
     * @param remark 驳回理由
     * @param approverId 审批人ID
     */
    @Override
    public void rejectRequest(Long requestId, String remark, Long approverId) {
        LeaveRequest existingRequest = leaveRequestMapper.getById(requestId);
        if (existingRequest == null) {
            throw new BusinessException("请假申请不存在");
        }
        if (!"pending".equals(existingRequest.getStatus())) {
            throw new BusinessException("该申请已被处理，请勿重复操作");
        }

        LeaveRequest leaveRequestToUpdate = new LeaveRequest();
        leaveRequestToUpdate.setRequestId(requestId);
        leaveRequestToUpdate.setStatus("rejected");
        leaveRequestToUpdate.setRemark(remark);
        leaveRequestToUpdate.setApproverId(approverId);
        leaveRequestToUpdate.setApprovedAt(LocalDateTime.now());
        leaveRequestMapper.update(leaveRequestToUpdate);
        
        // 3. 清理缓存
        clearLeaveRequestCache();
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(LeaveRequestQueryDTO dto) {
        StringBuilder cacheKeyBuilder = new StringBuilder("leave:request:list:");
        cacheKeyBuilder.append(dto.getPage()).append(":").append(dto.getPageSize());

        if (dto.getStudentName() != null && !dto.getStudentName().isEmpty()) {
            cacheKeyBuilder.append(":name:").append(dto.getStudentName());
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            cacheKeyBuilder.append(":status:").append(dto.getStatus());
        }
        if (dto.getStartDate() != null) {
            cacheKeyBuilder.append(":startDate:").append(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            cacheKeyBuilder.append(":endDate:").append(dto.getEndDate());
        }
        return cacheKeyBuilder.toString();
    }

    /**
     * 清除请假申请列表相关的所有缓存
     */
    private void clearLeaveRequestCache() {
        Set<String> keys = redisTemplate.keys("leave:request:list:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除了 {} 个请假申请列表缓存", keys.size());
        }
    }

    /**
     * 获取单个请假申请详情
     * @param requestId 申请ID
     * @return 详情视图对象
     */
    @Override
    public LeaveRequestVO getLeaveRequestDetail(Long requestId) {
        // 详情信息一般不需要高频访问，可以不加缓存，保证数据实时性
        LeaveRequestVO detail = leaveRequestMapper.getDetailById(requestId);
        if (detail == null) {
            throw new BusinessException("请假申请不存在");
        }
        return detail;
    }

    /**
     * 获取待审批的请假申请数量
     * @return 待审批数量
     */
    @Override
    public int getPendingCount() {
        // 获取当前用户ID和角色
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        log.info("获取待审批请假申请数量，用户ID: {}, 角色: {}", currentUserId, userRole);
        
        if ("teacher".equals(userRole)) {
            // 老师只能看到自己创建的考勤计划相关的请假申请数量
            return leaveRequestMapper.countPendingRequestsByCreator(currentUserId);
        } else if ("admin".equals(userRole)) {
            // 管理员可以看到所有待审批数量
        return leaveRequestMapper.countPendingRequests();
        } else {
            // 学生角色返回0
            return 0;
        }
    }

    /**
     * 获取今日已处理的请假申请数量
     * @return 今日已处理数量
     */
    @Override
    public int getTodayProcessedCount() {
        // 获取当前用户ID和角色
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        // 🔧 优化：频繁查询，降级为DEBUG，减少用户信息泄露
        log.debug("获取今日已处理请假申请数量，角色: {}", userRole);
        
        if ("teacher".equals(userRole)) {
            // 老师只能看到自己创建的考勤计划相关的今日已处理数量
            return leaveRequestMapper.countTodayProcessedRequestsByCreator(currentUserId);
        } else if ("admin".equals(userRole)) {
            // 管理员可以看到所有今日已处理数量
        return leaveRequestMapper.countTodayProcessedRequests();
        } else {
            // 学生角色返回0
            return 0;
        }
    }

    /**
     * 获取已审批的请假申请记录
     * @param days 查询天数
     * @return 已审批记录列表
     */
    @Override
    public List<com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo> getProcessedLeaveRequests(Integer days) {
        // 获取当前用户ID和角色
        Long currentUserId = UserContextHolder.getCurrentId();
        String userRole = getCurrentUserRole(currentUserId);
        
        log.info("获取已审批的请假申请记录，查询天数: {}, 用户ID: {}, 角色: {}", days, currentUserId, userRole);
        
        List<LeaveRequestVO> requests;
        if ("teacher".equals(userRole)) {
            // 老师只能看到自己创建的考勤计划相关的已审批记录
            requests = leaveRequestMapper.findProcessedRequestsByCreator(days, currentUserId);
        } else if ("admin".equals(userRole)) {
            // 管理员可以看到所有已审批记录
            requests = leaveRequestMapper.findProcessedRequests(days);
        } else {
            // 学生角色返回空列表
            requests = new ArrayList<>();
        }
        
        return requests.stream().<com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo>map(request -> {
            String status = "approved".equals(request.getStatus()) ? "approved" : "rejected";
            String leaveTypeText = getLeaveTypeText(request.getType());
            
            return com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo.builder()
                    .recordId(request.getRequestId())
                    .approvalType("leave")
                    .title(leaveTypeText + " - " + request.getReason())
                    .applicantName(request.getStudentName())
                    .applicantAvatar(request.getStudentAvatar())
                    .status(status)
                    .reviewTime(request.getApprovedAt())
                    .reviewComment(request.getRemark())
                    .applicationTime(request.getCreateTime())
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取请假类型文本
     */
    private String getLeaveTypeText(String type) {
        switch (type) {
            case "sick_leave": return "病假";
            case "personal_leave": return "事假";
            case "annual_leave": return "年假";
            case "maternity_leave": return "产假";
            case "paternity_leave": return "陪产假";
            case "bereavement_leave": return "丧假";
            case "marriage_leave": return "婚假";
            default: return "其他假期";
        }
    }

    // ===================== 学生端功能实现 =====================
    
    /**
     * 创建请假申请
     * @param createDTO 请假申请数据
     * @return 请假申请ID
     */
    @Override
    @Transactional
    public Long createLeaveRequest(LeaveRequestCreateDTO createDTO) {
        log.info("创建请假申请: {}", createDTO);
        
        // 1. 参数验证
        if (createDTO.getAttendancePlanId() == null) {
            throw new BusinessException("考勤计划ID不能为空");
        }
        if (createDTO.getType() == null || createDTO.getType().trim().isEmpty()) {
            throw new BusinessException("请假类型不能为空");
        }
        if (createDTO.getReason() == null || createDTO.getReason().trim().isEmpty()) {
            throw new BusinessException("请假原因不能为空");
        }
        if (createDTO.getStartTime() == null) {
            throw new BusinessException("请假开始时间不能为空");
        }
        if (createDTO.getEndTime() == null) {
            throw new BusinessException("请假结束时间不能为空");
        }
        if (createDTO.getStartTime().isAfter(createDTO.getEndTime())) {
            throw new BusinessException("请假开始时间不能晚于结束时间");
        }
        
        // 2. 根据用户ID获取学生ID
        Long studentId = getStudentIdByUserId(createDTO.getApplicantId());
        if (studentId == null) {
            throw new BusinessException("用户不是学生，无法申请请假");
        }
        
        // 3. 验证考勤计划是否存在
        AttendancePlan attendancePlan = attendancePlanMapper.selectById(createDTO.getAttendancePlanId());
        if (attendancePlan == null) {
            throw new BusinessException("考勤计划不存在");
        }
        if (attendancePlan.getStatus() == 0) {
            throw new BusinessException("考勤计划已取消，无法申请请假");
        }
        
                // 4. 验证请假时间是否与考勤计划时间有重叠
        // 修复：允许请假时间与考勤时间有重叠即可，不要求完全包含
        log.info("时间验证 - 考勤计划: {} ~ {}, 请假申请: {} ~ {}", 
                attendancePlan.getStartTime(), attendancePlan.getEndTime(),
                createDTO.getStartTime(), createDTO.getEndTime());
                
        if (createDTO.getEndTime().isBefore(attendancePlan.getStartTime()) ||
            createDTO.getStartTime().isAfter(attendancePlan.getEndTime())) {
            log.warn("请假时间与考勤时间无重叠 - 考勤: {} ~ {}, 请假: {} ~ {}", 
                    attendancePlan.getStartTime(), attendancePlan.getEndTime(),
                    createDTO.getStartTime(), createDTO.getEndTime());
            throw new BusinessException("请假时间与考勤计划时间无重叠，请检查时间设置");
        }
        
        log.info("时间验证通过 - 请假时间与考勤时间有重叠");
        
        // 🔧 修复：检查学生是否已经签到，已签到不允许请假
        List<AttendanceRecord> existingRecords = attendanceRecordMapper.getByPlanIdAndStudentId(
            createDTO.getAttendancePlanId(), studentId);
        if (!existingRecords.isEmpty()) {
            AttendanceRecord record = existingRecords.get(0);
            if (record.getStatus() == AttendanceStatus.present || record.getStatus() == AttendanceStatus.late) {
                throw new BusinessException("您已完成签到，无法申请请假");
            }
        }
        
        // 3. 检查是否有重叠的请假申请 (暂时跳过此检查，可以后续优化)
        // TODO: 实现重叠请假申请检查
        // List<LeaveRequest> overlappingRequests = leaveRequestMapper.findOverlappingRequests(
        //     studentId, 
        //     createDTO.getStartTime(), 
        //     createDTO.getEndTime()
        // );
        // if (!overlappingRequests.isEmpty()) {
        //     throw new BusinessException("该时间段已有请假申请，请检查时间");
        // }
        
        // 5. 创建请假申请对象
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setStudentId(studentId);
        leaveRequest.setAttendancePlanId(createDTO.getAttendancePlanId());
        leaveRequest.setType(createDTO.getType());
        leaveRequest.setReason(createDTO.getReason());
        leaveRequest.setStartTime(createDTO.getStartTime());
        leaveRequest.setEndTime(createDTO.getEndTime());
        // leaveRequest.setRemark(createDTO.getRemark()); // remark字段用于审批人填写，学生申请时不设置
        leaveRequest.setStatus("pending"); // 默认待审批状态
        leaveRequest.setCreateTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        // 处理附件
        if (createDTO.getAttachments() != null && !createDTO.getAttachments().isEmpty()) {
            try {
                // 将附件列表转换为JSON字符串存储
                ObjectMapper objectMapper = new ObjectMapper();
                String attachmentsJson = objectMapper.writeValueAsString(createDTO.getAttachments());
                leaveRequest.setAttachments(attachmentsJson);
            } catch (JsonProcessingException e) {
                log.error("附件列表转换为JSON失败: {}", e.getMessage());
                throw new BusinessException("附件数据处理失败");
            }
        }
        
        // 6. 保存到数据库
        leaveRequestMapper.insert(leaveRequest);
        
        // 7. 清理缓存
        clearLeaveRequestCache();
        
        log.info("请假申请创建成功: requestId={}, studentId={}", leaveRequest.getRequestId(), studentId);
        return leaveRequest.getRequestId();
    }

    /**
     * 撤销请假申请（只能撤销待审批状态的申请）
     * @param requestId 请假申请ID
     * @param userId 用户ID
     * @return 撤销是否成功
     */
    @Override
    @Transactional
    public boolean cancelLeaveRequest(Long requestId, Long userId) {
        log.info("撤销请假申请: requestId={}, userId={}", requestId, userId);
        
        // 1. 获取请假申请
        LeaveRequest existingRequest = leaveRequestMapper.getById(requestId);
        if (existingRequest == null) {
            throw new BusinessException("请假申请不存在");
        }
        
        // 2. 验证权限：只能撤销自己的申请
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null || !studentId.equals(existingRequest.getStudentId())) {
            throw new BusinessException("无权限撤销此申请");
        }
        
        // 3. 验证状态：只能撤销待审批的申请
        if (!"pending".equals(existingRequest.getStatus())) {
            throw new BusinessException("只能撤销待审批状态的申请");
        }
        
        // 4. 删除申请（或者更新状态为已撤销）
        int result = leaveRequestMapper.deleteById(requestId);
        
        // 5. 清理缓存
        if (result > 0) {
            clearLeaveRequestCache();
            log.info("请假申请撤销成功: requestId={}", requestId);
            return true;
        } else {
            log.warn("请假申请撤销失败: requestId={}", requestId);
            return false;
        }
    }

    /**
     * 根据用户ID获取学生ID
     * @param userId 用户ID
     * @return 学生ID
     */
    @Override
    public Long getStudentIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            return studentMapper.getStudentIdByUserId(userId);
        } catch (Exception e) {
            log.error("根据用户ID获取学生ID失败: userId={}, error={}", userId, e.getMessage());
            return null;
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