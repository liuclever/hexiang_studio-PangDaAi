package com.back_hexiang_studio.mapper;


import com.back_hexiang_studio.entity.ActivityReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 活动预约Mapper接口
 */
@Mapper
public interface ActivityReservationMapper {
    
    /**
     * 新增预约记录
     * @param reservation 预约记录
     * @return 影响行数
     */
    int insert(ActivityReservation reservation);
    
    /**
     * 批量新增预约记录
     * @param reservations 预约记录列表
     * @return 影响行数
     */
    int batchInsert(@Param("reservations") List<ActivityReservation> reservations);
    
    /**
     * 根据ID查询预约记录
     * @param reservationId 预约ID
     * @return 预约记录
     */
    ActivityReservation selectById(@Param("reservationId") Long reservationId);
    
    /**
     * 根据计划ID和学生ID查询预约记录
     * @param planId 计划ID
     * @param studentId 学生ID
     * @return 预约记录
     */
    ActivityReservation selectByPlanIdAndStudentId(@Param("planId") Long planId, @Param("studentId") Long studentId);
    
    /**
     * 更新预约记录
     * @param reservation 预约记录
     * @return 影响行数
     */
    int update(ActivityReservation reservation);
    
    /**
     * 取消预约（软删除）
     * @param reservationId 预约ID
     * @return 影响行数
     */
    int cancelReservation(@Param("reservationId") Long reservationId);
    
    /**
     * 批量取消预约
     * @param planId 计划ID
     * @param studentIds 学生ID列表
     * @return 影响行数
     */
    int batchCancelReservation(@Param("planId") Long planId, @Param("studentIds") List<Long> studentIds);
    
    /**
     * 删除预约记录
     * @param reservationId 预约ID
     * @return 影响行数
     */
    int deleteById(@Param("reservationId") Long reservationId);
    
    /**
     * 根据计划ID删除所有预约记录
     * @param planId 计划ID
     * @return 影响行数
     */
    int deleteByPlanId(@Param("planId") Long planId);
    
    /**
     * 分页查询预约记录（带学生信息）
     * @param params 查询参数
     * @return 预约记录列表
     */
    List<Map<String, Object>> selectByPage(@Param("params") Map<String, Object> params);
    
    /**
     * 查询预约记录总数
     * @param params 查询参数
     * @return 总数
     */
    int selectCount(@Param("params") Map<String, Object> params);
    
    /**
     * 根据计划ID查询已预约的学生列表
     * @param planId 计划ID
     * @return 学生信息列表
     */
    List<Map<String, Object>> selectReservedStudentsByPlanId(@Param("planId") Long planId);
    
    /**
     * 根据计划ID查询已预约的学生ID列表
     * @param planId 计划ID
     * @return 学生ID列表
     */
    List<Long> selectReservedStudentIdsByPlanId(@Param("planId") Long planId);
    
    /**
     * 根据学生ID查询其预约的活动列表
     * @param studentId 学生ID
     * @param status 预约状态（可选）
     * @return 活动预约列表
     */
    List<Map<String, Object>> selectReservationsByStudentId(@Param("studentId") Long studentId, @Param("status") String status);
    
    /**
     * 统计活动的预约数量
     * @param planId 计划ID
     * @return 预约统计信息
     */
    Map<String, Object> getReservationStatistics(@Param("planId") Long planId);
    
    /**
     * 检查学生是否已预约某活动
     * @param planId 计划ID
     * @param studentId 学生ID
     * @return 是否已预约（true/false）
     */
    boolean isStudentReserved(@Param("planId") Long planId, @Param("studentId") Long studentId);
    
    /**
     * 更新学生预约状态（用于签到时更新）
     * @param planId 计划ID
     * @param studentId 学生ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateReservationStatus(@Param("planId") Long planId, @Param("studentId") Long studentId, @Param("status") String status);
} 