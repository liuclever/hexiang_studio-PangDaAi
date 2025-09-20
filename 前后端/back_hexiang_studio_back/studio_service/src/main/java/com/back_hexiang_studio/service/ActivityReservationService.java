package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.ActivityReservationDto;
import com.back_hexiang_studio.result.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 活动预约服务接口
 */
public interface ActivityReservationService {
    
    /**
     * 批量创建活动预约
     * @param reservationDto 预约信息（包含学生ID列表）
     * @return 创建结果
     */
    Map<String, Object> batchCreateReservation(ActivityReservationDto reservationDto);
    
    /**
     * 学生预约活动
     * @param planId 活动计划ID
     * @param studentId 学生ID
     * @param remark 备注
     * @return 预约结果
     */
    Map<String, Object> reserveActivity(Long planId, Long studentId, String remark);
    
    /**
     * 取消预约
     * @param reservationId 预约ID
     * @param studentId 学生ID（用于权限验证）
     * @return 取消结果
     */
    Map<String, Object> cancelReservation(Long reservationId, Long studentId);
    
    /**
     * 批量取消预约
     * @param planId 计划ID
     * @param studentIds 学生ID列表
     * @return 取消结果
     */
    Map<String, Object> batchCancelReservation(Long planId, List<Long> studentIds);
    
    /**
     * 获取活动预约列表（分页）
     * @param planId 活动计划ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 搜索关键词（学生姓名或学号）
     * @return 分页结果
     */
    PageResult getReservationList(Long planId, Integer pageNum, Integer pageSize, String keyword);
    
    /**
     * 获取学生预约的活动列表
     * @param studentId 学生ID
     * @param status 预约状态（可选）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    PageResult getStudentReservations(Long studentId, String status, Integer pageNum, Integer pageSize);
    
    /**
     * 获取活动预约统计信息
     * @param planId 活动计划ID
     * @return 统计信息
     */
    Map<String, Object> getReservationStatistics(Long planId);
    
    /**
     * 检查学生是否已预约某活动
     * @param planId 活动计划ID
     * @param studentId 学生ID
     * @return 是否已预约
     */
    boolean isStudentReserved(Long planId, Long studentId);
    
    /**
     * 获取已预约学生列表（用于考勤记录生成）
     * @param planId 活动计划ID
     * @return 学生ID列表
     */
    List<Long> getReservedStudentIds(Long planId);
    
    /**
     * 获取所有学生列表（用于选择器）
     * @param keyword 搜索关键词
     * @return 学生列表
     */
    List<Map<String, Object>> getAllStudents(String keyword);
    
    /**
     * 根据计划ID获取预约学生详细信息
     * @param planId 活动计划ID
     * @return 预约学生列表（包含学生详细信息）
     */
    List<Map<String, Object>> getReservationsByPlanId(Long planId);
    
    /**
     * 更新学生预约状态（用于签到时更新）
     * @param planId 活动计划ID
     * @param studentId 学生ID
     * @param status 新状态（checked_in-已签到, cancelled-已取消）
     * @return 是否更新成功
     */
    boolean updateReservationStatus(Long planId, Long studentId, String status);
} 