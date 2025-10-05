package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.service.ActivityReservationService;
import com.back_hexiang_studio.mapper.ActivityReservationMapper;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.mapper.AttendancePlanMapper;
import com.back_hexiang_studio.entity.ActivityReservation;
import com.back_hexiang_studio.entity.AttendancePlan;
import com.back_hexiang_studio.dv.dto.ActivityReservationDto;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.NotFoundException;
import com.back_hexiang_studio.GlobalException.ParamException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 活动预约服务实现类
 * 
 * @author Hexiang
 * @date 2024/09/27
 */
@Slf4j
@Service
public class ActivityReservationServiceImpl implements ActivityReservationService {
    
    @Autowired
    private ActivityReservationMapper activityReservationMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private AttendancePlanMapper attendancePlanMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCreateReservation(ActivityReservationDto reservationDto) {
        try {
            log.info("批量创建活动预约，计划ID: {}, 学生数量: {}", 
                    reservationDto.getPlanId(), 
                    reservationDto.getStudentIds() != null ? reservationDto.getStudentIds().size() : 0);
            
            // 参数校验
            validateReservationDto(reservationDto);
            
            // 验证活动计划
            AttendancePlan plan = validateAndGetActivityPlan(reservationDto.getPlanId());
            
            // 检查活动是否已开始
            if (plan.getStartTime().isBefore(LocalDateTime.now())) {
                log.warn("活动已开始，无法预约，计划ID: {}", reservationDto.getPlanId());
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "活动已开始，无法预约");
            }
            
            List<Long> studentIds = reservationDto.getStudentIds();
            
            // 准备批量插入的预约记录
            List<ActivityReservation> reservations = new ArrayList<>();
            List<String> conflictStudents = new ArrayList<>();
            
            for (Long studentId : studentIds) {
                try {
                    // 检查学生是否存在
                    Boolean studentExists = studentMapper.selectById(studentId);
                    if (!studentExists) {
                        log.warn("学生不存在，跳过处理: {}", studentId);
                        continue;
                    }
                    
                    // 检查是否已预约
                    if (activityReservationMapper.isStudentReserved(reservationDto.getPlanId(), studentId)) {
                        conflictStudents.add(studentId.toString());
                        log.debug("学生{}已预约此活动，跳过", studentId);
                        continue;
                    }
                    
                    ActivityReservation reservation = new ActivityReservation();
                    reservation.setPlanId(reservationDto.getPlanId());
                    reservation.setStudentId(studentId);
                    reservation.setStatus("reserved");
                    reservation.setReservationTime(LocalDateTime.now());
                    reservation.setRemark(reservationDto.getRemark());
                    reservations.add(reservation);
                    
                } catch (Exception e) {
                    log.warn("处理学生{}的预约时发生异常: {}", studentId, e.getMessage());
                    continue;
                }
            }
            
            int successCount = 0;
            if (!reservations.isEmpty()) {
                successCount = activityReservationMapper.batchInsert(reservations);
                log.info("成功批量插入{}条预约记录", successCount);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", studentIds.size());
            result.put("conflictStudents", conflictStudents);
            result.put("activityName", plan.getName());
            
            log.info("批量创建活动预约完成，成功: {}, 总数: {}, 冲突: {}", 
                    successCount, studentIds.size(), conflictStudents.size());
            
            return result;
            
        } catch (ParamException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量创建活动预约失败，计划ID: {}", reservationDto.getPlanId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量创建预约失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reserveActivity(Long planId, Long studentId, String remark) {
        try {
            log.info("学生预约活动，计划ID: {}, 学生ID: {}", planId, studentId);
            
            // 参数校验
            validateReserveParams(planId, studentId);
            
            // 验证活动计划
            AttendancePlan plan = validateAndGetActivityPlan(planId);
            
            // 检查活动是否已开始
            if (plan.getStartTime().isBefore(LocalDateTime.now())) {
                log.warn("活动已开始，无法预约，计划ID: {}, 学生ID: {}", planId, studentId);
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "活动已开始，无法预约");
            }
            
            // 检查学生是否存在
            Boolean studentExists = studentMapper.selectById(studentId);
            if (!studentExists) {
                log.warn("学生不存在，学生ID: {}", studentId);
                throw new NotFoundException(ErrorCode.USER_NOT_FOUND, "学生不存在");
            }
            
            // 检查是否已预约
            if (activityReservationMapper.isStudentReserved(planId, studentId)) {
                log.warn("学生已预约此活动，计划ID: {}, 学生ID: {}", planId, studentId);
                throw new BusinessException(ErrorCode.DATA_CONFLICT, "您已预约此活动");
            }
            
            // 创建预约记录
            ActivityReservation reservation = new ActivityReservation();
            reservation.setPlanId(planId);
            reservation.setStudentId(studentId);
            reservation.setStatus("reserved");
            reservation.setReservationTime(LocalDateTime.now());
            reservation.setRemark(remark);
            
            int result = activityReservationMapper.insert(reservation);
            if (result <= 0) {
                log.error("插入预约记录失败，计划ID: {}, 学生ID: {}", planId, studentId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "预约失败");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activityName", plan.getName());
            response.put("reservationTime", reservation.getReservationTime());
            
            log.info("学生预约活动成功，计划ID: {}, 学生ID: {}", planId, studentId);
            return response;
            
        } catch (ParamException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("预约活动失败，计划ID: {}, 学生ID: {}", planId, studentId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "预约失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelReservation(Long reservationId, Long studentId) {
        try {
            log.info("取消活动预约，预约ID: {}, 学生ID: {}", reservationId, studentId);
            
            // 参数校验
            if (reservationId == null || reservationId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "预约ID不能为空且必须大于0");
            }
            if (studentId == null || studentId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "学生ID不能为空且必须大于0");
            }
            
            ActivityReservation reservation = activityReservationMapper.selectById(reservationId);
            if (reservation == null) {
                log.warn("预约记录不存在，预约ID: {}", reservationId);
                throw new NotFoundException(ErrorCode.NOT_FOUND, "预约记录不存在");
            }
            
            // 验证学生权限
            if (!reservation.getStudentId().equals(studentId)) {
                log.warn("学生无权限取消此预约，预约ID: {}, 学生ID: {}, 预约归属学生: {}", 
                        reservationId, studentId, reservation.getStudentId());
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限取消此预约");
            }
            
            if ("cancelled".equals(reservation.getStatus())) {
                log.warn("预约已取消，预约ID: {}", reservationId);
                throw new BusinessException(ErrorCode.BUSINESS_STATE_ERROR, "预约已取消");
            }
            
            // 检查活动是否已开始
            AttendancePlan plan = attendancePlanMapper.selectById(reservation.getPlanId());
            if (plan != null && plan.getStartTime().isBefore(LocalDateTime.now())) {
                log.warn("活动已开始，无法取消预约，预约ID: {}", reservationId);
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "活动已开始，无法取消预约");
            }
            
            int result = activityReservationMapper.cancelReservation(reservationId);
            if (result <= 0) {
                log.error("取消预约失败，预约ID: {}", reservationId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消预约失败");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activityName", plan != null ? plan.getName() : "");
            
            log.info("取消活动预约成功，预约ID: {}, 学生ID: {}", reservationId, studentId);
            return response;
            
        } catch (ParamException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消预约失败，预约ID: {}, 学生ID: {}", reservationId, studentId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消预约失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCancelReservation(Long planId, List<Long> studentIds) {
        try {
            log.info("批量取消预约，计划ID: {}, 学生数量: {}", planId, 
                    studentIds != null ? studentIds.size() : 0);
            
            // 参数校验
            if (planId == null || planId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
            }
            if (studentIds == null || studentIds.isEmpty()) {
                throw new ParamException(ErrorCode.PARAM_MISSING, "请选择要取消预约的学生");
            }
            
            int result = activityReservationMapper.batchCancelReservation(planId, studentIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("successCount", result);
            response.put("totalCount", studentIds.size());
            
            log.info("批量取消预约完成，成功: {}, 总数: {}", result, studentIds.size());
            return response;
            
        } catch (ParamException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量取消预约失败，计划ID: {}, 学生数量: {}", planId, 
                    studentIds != null ? studentIds.size() : 0, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量取消预约失败");
        }
    }
    
    @Override
    public PageResult getReservationList(Long planId, Integer pageNum, Integer pageSize, String keyword) {
        try {
            log.info("获取预约列表，计划ID: {}, 页码: {}, 页大小: {}", planId, pageNum, pageSize);
            
            // 参数校验
            validatePaginationParams(pageNum, pageSize);
            if (planId == null || planId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
            }
            
            PageHelper.startPage(pageNum, pageSize);
            
            Map<String, Object> params = new HashMap<>();
            params.put("planId", planId);
            if (StringUtils.hasText(keyword)) {
                params.put("keyword", keyword);
            }
            
            List<Map<String, Object>> list = activityReservationMapper.selectByPage(params);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
            
            log.info("获取预约列表成功，总数: {}, 当前页数据: {}", pageInfo.getTotal(), list.size());
            return new PageResult(pageInfo.getTotal(), pageInfo.getList());
            
        } catch (ParamException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取预约列表失败，计划ID: {}", planId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取预约列表失败");
        }
    }
    
    @Override
    public PageResult getStudentReservations(Long studentId, String status, Integer pageNum, Integer pageSize) {
        try {
            log.info("获取学生预约列表，学生ID: {}, 状态: {}, 页码: {}, 页大小: {}", 
                    studentId, status, pageNum, pageSize);
            
            // 参数校验
            validatePaginationParams(pageNum, pageSize);
            if (studentId == null || studentId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "学生ID不能为空且必须大于0");
            }
            
            PageHelper.startPage(pageNum, pageSize);
            
            List<Map<String, Object>> list = activityReservationMapper.selectReservationsByStudentId(studentId, status);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
            
            log.info("获取学生预约列表成功，学生ID: {}, 总数: {}", studentId, pageInfo.getTotal());
            return new PageResult(pageInfo.getTotal(), pageInfo.getList());
            
        } catch (ParamException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取学生预约列表失败，学生ID: {}", studentId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取学生预约列表失败");
        }
    }
    
    @Override
    public Map<String, Object> getReservationStatistics(Long planId) {
        try {
            log.info("获取预约统计，计划ID: {}", planId);
            
            // 参数校验
            if (planId == null || planId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
            }
            
            Map<String, Object> statistics = activityReservationMapper.getReservationStatistics(planId);
            
            // 如果统计结果为空，返回默认值
            if (statistics == null) {
                statistics = new HashMap<>();
                statistics.put("totalReservations", 0);
                statistics.put("activeReservations", 0);
                statistics.put("cancelledReservations", 0);
            }
            
            log.info("获取预约统计成功，计划ID: {}", planId);
            return statistics;
            
        } catch (ParamException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取预约统计失败，计划ID: {}", planId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取预约统计失败");
        }
    }
    
    @Override
    public boolean isStudentReserved(Long planId, Long studentId) {
        try {
            log.debug("检查预约状态，计划ID: {}, 学生ID: {}", planId, studentId);
            
            // 参数校验
            if (planId == null || planId <= 0 || studentId == null || studentId <= 0) {
                log.warn("参数无效，计划ID: {}, 学生ID: {}", planId, studentId);
                return false;
            }
            
            boolean reserved = activityReservationMapper.isStudentReserved(planId, studentId);
            log.debug("预约状态检查结果：{}", reserved);
            return reserved;
            
        } catch (Exception e) {
            log.error("检查预约状态失败，计划ID: {}, 学生ID: {}", planId, studentId, e);
            return false;
        }
    }
    
    @Override
    public List<Long> getReservedStudentIds(Long planId) {
        try {
            log.debug("获取已预约学生列表，计划ID: {}", planId);
            
            // 参数校验
            if (planId == null || planId <= 0) {
                log.warn("计划ID无效: {}", planId);
                return new ArrayList<>();
            }
            
            List<Long> studentIds = activityReservationMapper.selectReservedStudentIdsByPlanId(planId);
            log.debug("获取到{}个已预约学生", studentIds != null ? studentIds.size() : 0);
            
            return studentIds != null ? studentIds : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取已预约学生列表失败，计划ID: {}", planId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> getAllStudents(String keyword) {
        try {
            log.info("获取学生列表，关键词: {}", keyword);
            
            List<Map<String, Object>> students;
            if (StringUtils.hasText(keyword)) {
                students = studentMapper.searchStudents(keyword);
            } else {
                students = studentMapper.selectStudentsWithNames();
            }
            
            log.info("获取学生列表成功，数量: {}", students != null ? students.size() : 0);
            return students != null ? students : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取学生列表失败，关键词: {}", keyword, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取学生列表失败");
        }
    }
    
    @Override
    public List<Map<String, Object>> getReservationsByPlanId(Long planId) {
        try {
            log.info("获取活动预约学生列表，计划ID: {}", planId);
            
            // 参数校验
            if (planId == null || planId <= 0) {
                throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
            }
            
            // 验证活动计划是否存在
            AttendancePlan plan = validateAndGetActivityPlan(planId);
            
            // 获取预约学生信息
            List<Map<String, Object>> reservations = activityReservationMapper.selectReservedStudentsByPlanId(planId);
            
            log.info("获取活动预约学生列表成功，计划ID: {}, 数量: {}", planId, 
                    reservations != null ? reservations.size() : 0);
            
            return reservations != null ? reservations : new ArrayList<>();
            
        } catch (ParamException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取活动预约学生列表失败，计划ID: {}", planId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取预约学生列表失败");
        }
    }
    
    @Override
    public boolean updateReservationStatus(Long planId, Long studentId, String status) {
        try {
            log.info("更新预约状态，计划ID: {}, 学生ID: {}, 状态: {}", planId, studentId, status);
            
            // 参数校验
            if (planId == null || planId <= 0) {
                log.warn("计划ID无效: {}", planId);
                return false;
            }
            if (studentId == null || studentId <= 0) {
                log.warn("学生ID无效: {}", studentId);
                return false;
            }
            if (!StringUtils.hasText(status)) {
                log.warn("状态无效: {}", status);
                return false;
            }
            
            int result = activityReservationMapper.updateReservationStatus(planId, studentId, status);
            boolean success = result > 0;
            
            log.info("更新预约状态结果: {}, 计划ID: {}, 学生ID: {}", success, planId, studentId);
            return success;
            
        } catch (Exception e) {
            log.error("更新预约状态失败，计划ID: {}, 学生ID: {}, 状态: {}", planId, studentId, status, e);
            return false;
        }
    }

    /**
     * 校验预约DTO参数
     * 
     * @param reservationDto 预约信息
     */
    private void validateReservationDto(ActivityReservationDto reservationDto) {
        if (reservationDto == null) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "预约信息不能为空");
        }
        
        if (reservationDto.getPlanId() == null || reservationDto.getPlanId() <= 0) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
        }
        
        List<Long> studentIds = reservationDto.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "请选择至少一个学生");
        }
        
        if (studentIds.size() > 100) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "单次批量预约学生数量不能超过100个");
        }
    }

    /**
     * 校验预约参数
     * 
     * @param planId 计划ID
     * @param studentId 学生ID
     */
    private void validateReserveParams(Long planId, Long studentId) {
        if (planId == null || planId <= 0) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "计划ID不能为空且必须大于0");
        }
        
        if (studentId == null || studentId <= 0) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "学生ID不能为空且必须大于0");
        }
    }

    /**
     * 校验分页参数
     * 
     * @param pageNum 页码
     * @param pageSize 页大小
     */
    private void validatePaginationParams(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "页码必须大于0");
        }
        
        if (pageSize == null || pageSize < 1 || pageSize > 1000) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "页大小必须在1-1000之间");
        }
    }

    /**
     * 验证并获取活动计划
     * 
     * @param planId 计划ID
     * @return 活动计划
     */
    private AttendancePlan validateAndGetActivityPlan(Long planId) {
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            log.warn("活动计划不存在，计划ID: {}", planId);
            throw new NotFoundException(ErrorCode.NOT_FOUND, "活动计划不存在");
        }
        
        if (!"activity".equals(plan.getType())) {
            log.warn("计划类型不是活动，计划ID: {}, 类型: {}", planId, plan.getType());
            throw new BusinessException(ErrorCode.BUSINESS_VALIDATION_FAILED, "只能为活动类型的计划创建预约");
        }
        
        return plan;
    }
} 