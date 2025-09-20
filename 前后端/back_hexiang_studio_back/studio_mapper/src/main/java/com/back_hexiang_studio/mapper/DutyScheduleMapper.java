package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.DutySchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 值班安排 Mapper 接口
 */
@Mapper
public interface DutyScheduleMapper {
    /**
     * 获取所有值班安排（不限制日期）
     * @return 所有值班安排
     */
    List<Map<String, Object>> getAllDutySchedules();
    
    /**
     * 获取指定日期范围内的值班考勤状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 值班考勤状态
     */
    List<Map<String, Object>> getDutyAttendanceStatus(@Param("startDate") String startDate, @Param("endDate") String endDate);
    
    // 新增值班安排
    int insert(DutySchedule schedule);
    
    // 根据ID查询值班安排
    DutySchedule selectById(Long scheduleId);
    
    // 更新值班安排
    int update(DutySchedule schedule);
    
    // 删除值班安排
    int deleteById(Long scheduleId);
    
    // 分页查询值班安排
    List<Map<String, Object>> selectByPage(Map<String, Object> params);
    
    // 查询值班安排总数
    int selectCount(Map<String, Object> params);
    
    // 查询指定日期范围内的值班安排
    List<Map<String, Object>> selectByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 查询学生的值班安排
    List<Map<String, Object>> selectByStudentId(@Param("studentId") Long studentId);

    // 检查指定日期和时间段是否已有值班安排
    boolean existsByDateAndTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    List<Map<String, Object>> findAttendanceStatusByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 专门用于获取日期范围内的考勤状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 考勤状态数据
     */
    List<Map<String, Object>> getDutyAttendanceStatusInRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 根据日期和时间段查询值班安排
     * @param startOfDay 当天的开始时间
     * @param endOfDay 当天的结束时间
     * @param timeSlot 时间段
     * @return 值班安排列表
     */
    List<DutySchedule> findByDutyDateAndTimeSlot(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("timeSlot") String timeSlot
    );

    List<DutySchedule> findSchedulesWithoutPlans(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定日期范围内的值班表（不包含学生信息）
     * @param startDate 开始日期时间
     * @param endDate 结束日期时间
     * @return 值班安排基础信息列表
     */
    List<Map<String, Object>> getDutyScheduleByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 