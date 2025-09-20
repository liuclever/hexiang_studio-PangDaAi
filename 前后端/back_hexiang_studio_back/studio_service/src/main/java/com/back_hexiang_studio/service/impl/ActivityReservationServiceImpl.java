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
            // 验证活动计划
            AttendancePlan plan = attendancePlanMapper.selectById(reservationDto.getPlanId());
            if (plan == null) {
                throw new BusinessException("活动计划不存在");
            }
            
            if (!"activity".equals(plan.getType())) {
                throw new BusinessException("只能为活动类型的计划创建预约");
            }
            
            // 检查活动是否已开始
            if (plan.getStartTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("活动已开始，无法预约");
            }
            
            List<Long> studentIds = reservationDto.getStudentIds();
            if (studentIds == null || studentIds.isEmpty()) {
                throw new BusinessException("请选择至少一个学生");
            }
            
            // 准备批量插入的预约记录
            List<ActivityReservation> reservations = new ArrayList<>();
            List<String> conflictStudents = new ArrayList<>();
            
            for (Long studentId : studentIds) {
                // 检查学生是否存在
                Boolean studentExists = studentMapper.selectById(studentId);
                if (!studentExists) {
                    log.warn("学生不存在: {}", studentId);
                    continue;
                }
                
                // 检查是否已预约
                if (activityReservationMapper.isStudentReserved(reservationDto.getPlanId(), studentId)) {
                    conflictStudents.add(studentId.toString());
                    continue;
                }
                
                ActivityReservation reservation = new ActivityReservation();
                reservation.setPlanId(reservationDto.getPlanId());
                reservation.setStudentId(studentId);
                reservation.setStatus("reserved");
                reservation.setReservationTime(LocalDateTime.now());
                reservation.setRemark(reservationDto.getRemark());
                reservations.add(reservation);
            }
            
            int successCount = 0;
            if (!reservations.isEmpty()) {
                successCount = activityReservationMapper.batchInsert(reservations);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", studentIds.size());
            result.put("conflictStudents", conflictStudents);
            result.put("activityName", plan.getName());
            
            return result;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量创建活动预约失败", e);
            throw new BusinessException("批量创建预约失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reserveActivity(Long planId, Long studentId, String remark) {
        try {
            // 验证活动计划
            AttendancePlan plan = attendancePlanMapper.selectById(planId);
            if (plan == null) {
                throw new BusinessException("活动计划不存在");
            }
            
            if (!"activity".equals(plan.getType())) {
                throw new BusinessException("只能预约活动类型的计划");
            }
            
            // 检查活动是否已开始
            if (plan.getStartTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("活动已开始，无法预约");
            }
            
            // 检查学生是否存在
            Boolean studentExists = studentMapper.selectById(studentId);
            if (!studentExists) {
                throw new BusinessException("学生不存在");
            }
            
            // 检查是否已预约
            if (activityReservationMapper.isStudentReserved(planId, studentId)) {
                throw new BusinessException("您已预约此活动");
            }
            
            // 创建预约记录
            ActivityReservation reservation = new ActivityReservation();
            reservation.setPlanId(planId);
            reservation.setStudentId(studentId);
            reservation.setStatus("reserved");
            reservation.setReservationTime(LocalDateTime.now());
            reservation.setRemark(remark);
            
            int result = activityReservationMapper.insert(reservation);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result > 0);
            response.put("activityName", plan.getName());
            response.put("reservationTime", reservation.getReservationTime());
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("预约活动失败", e);
            throw new BusinessException("预约失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelReservation(Long reservationId, Long studentId) {
        try {
            ActivityReservation reservation = activityReservationMapper.selectById(reservationId);
            if (reservation == null) {
                throw new BusinessException("预约记录不存在");
            }
            
            // 验证学生权限
            if (!reservation.getStudentId().equals(studentId)) {
                throw new BusinessException("无权限取消此预约");
            }
            
            if ("cancelled".equals(reservation.getStatus())) {
                throw new BusinessException("预约已取消");
            }
            
            // 检查活动是否已开始
            AttendancePlan plan = attendancePlanMapper.selectById(reservation.getPlanId());
            if (plan != null && plan.getStartTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("活动已开始，无法取消预约");
            }
            
            int result = activityReservationMapper.cancelReservation(reservationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result > 0);
            response.put("activityName", plan != null ? plan.getName() : "");
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消预约失败", e);
            throw new BusinessException("取消预约失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCancelReservation(Long planId, List<Long> studentIds) {
        try {
            if (studentIds == null || studentIds.isEmpty()) {
                throw new BusinessException("请选择要取消预约的学生");
            }
            
            int result = activityReservationMapper.batchCancelReservation(planId, studentIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("successCount", result);
            response.put("totalCount", studentIds.size());
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量取消预约失败", e);
            throw new BusinessException("批量取消预约失败: " + e.getMessage());
        }
    }
    
    @Override
    public PageResult getReservationList(Long planId, Integer pageNum, Integer pageSize, String keyword) {
        try {
            PageHelper.startPage(pageNum, pageSize);
            
            Map<String, Object> params = new HashMap<>();
            params.put("planId", planId);
            if (StringUtils.hasText(keyword)) {
                params.put("keyword", keyword);
            }
            
            List<Map<String, Object>> list = activityReservationMapper.selectByPage(params);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
            
            return new PageResult(pageInfo.getTotal(), pageInfo.getList());
            
        } catch (Exception e) {
            log.error("获取预约列表失败", e);
            throw new BusinessException("获取预约列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public PageResult getStudentReservations(Long studentId, String status, Integer pageNum, Integer pageSize) {
        try {
            PageHelper.startPage(pageNum, pageSize);
            
            List<Map<String, Object>> list = activityReservationMapper.selectReservationsByStudentId(studentId, status);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
            
            return new PageResult(pageInfo.getTotal(), pageInfo.getList());
            
        } catch (Exception e) {
            log.error("获取学生预约列表失败", e);
            throw new BusinessException("获取学生预约列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getReservationStatistics(Long planId) {
        try {
            return activityReservationMapper.getReservationStatistics(planId);
        } catch (Exception e) {
            log.error("获取预约统计失败", e);
            throw new BusinessException("获取预约统计失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isStudentReserved(Long planId, Long studentId) {
        try {
            return activityReservationMapper.isStudentReserved(planId, studentId);
        } catch (Exception e) {
            log.error("检查预约状态失败", e);
            return false;
        }
    }
    
    @Override
    public List<Long> getReservedStudentIds(Long planId) {
        try {
            return activityReservationMapper.selectReservedStudentIdsByPlanId(planId);
        } catch (Exception e) {
            log.error("获取已预约学生列表失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> getAllStudents(String keyword) {
        try {
            if (StringUtils.hasText(keyword)) {
                return studentMapper.searchStudents(keyword);
            } else {
                return studentMapper.selectStudentsWithNames();
            }
        } catch (Exception e) {
            log.error("获取学生列表失败", e);
            throw new BusinessException("获取学生列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Map<String, Object>> getReservationsByPlanId(Long planId) {
        try {
            // 验证活动计划是否存在
            AttendancePlan plan = attendancePlanMapper.selectById(planId);
            if (plan == null) {
                throw new BusinessException("活动计划不存在");
            }
            
            if (!"activity".equals(plan.getType())) {
                throw new BusinessException("只能查询活动类型的预约信息");
            }
            
            // 获取预约学生信息
            List<Map<String, Object>> reservations = activityReservationMapper.selectReservedStudentsByPlanId(planId);
            
            return reservations;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取活动预约学生列表失败", e);
            throw new BusinessException("获取预约学生列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean updateReservationStatus(Long planId, Long studentId, String status) {
        try {
            int result = activityReservationMapper.updateReservationStatus(planId, studentId, status);
            return result > 0;
        } catch (Exception e) {
            log.error("更新预约状态失败，计划ID: {}, 学生ID: {}, 状态: {}", planId, studentId, status, e);
            return false;
        }
    }
} 