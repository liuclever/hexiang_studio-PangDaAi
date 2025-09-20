package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.AttendancePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendancePlanMapper {
    // 新增考勤计划
    int insert(AttendancePlan plan);
    
    // 根据ID查询考勤计划
    AttendancePlan selectById(Long planId);
    
    // 更新考勤计划
    int update(AttendancePlan plan);
    
    // 删除考勤计划
    int deleteById(Long planId);
    
    // 分页查询考勤计划
    List<Map<String, Object>> selectByPage(Map<String, Object> params);
    
    // 查询考勤计划总数
    int selectCount(Map<String, Object> params);
    
    // 查询已结束但未处理的课程考勤计划
    List<AttendancePlan> findExpiredUnprocessedCoursePlans();
    
    // 查询指定日期的考勤计划
    List<AttendancePlan> selectByDate(@Param("date") LocalDate date);

    //查找时间段中考勤计划
    List<AttendancePlan> findDutyPlanByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    //查找即将开始考勤的计划（30分钟后）
    List<AttendancePlan> findUpcomingAttendancePlans( @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    //查询指定日期的值班计划
    List<AttendancePlan> findDutyPlanByDate(@Param("date")  LocalDate date);
    
    // 根据scheduleId查询考勤计划
    AttendancePlan findByScheduleId(@Param("scheduleId") Long scheduleId);
    
    // 查询指定时间段和值班安排ID的值班考勤计划
    List<AttendancePlan> findDutyPlansByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("scheduleId") Long scheduleId);
        
    // 根据scheduleId查询考勤计划及关联的学生信息
    Map<String, Object> findByScheduleIdWithStudents(@Param("scheduleId") Long scheduleId);
    
    // 根据值班安排ID查找考勤计划ID
    Long findPlanIdByScheduleId(@Param("scheduleId") Long scheduleId);
}