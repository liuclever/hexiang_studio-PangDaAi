package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.AttendancePlanDto;
import com.back_hexiang_studio.dv.dto.AttendanceQueryDto;
import com.back_hexiang_studio.result.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    /**
     * 创建考勤计划
     * @param planDto 考勤计划DTO
     * @return 创建结果
     */
    Map<String, Object> createAttendancePlan(AttendancePlanDto planDto);
    
    /**
     * 获取考勤计划列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    PageResult getAttendancePlanList(AttendanceQueryDto queryDto);
    
    /**
     * 获取考勤计划详情
     * @param planId 计划ID
     * @return 考勤计划详情
     */
    Map<String, Object> getAttendancePlanDetail(Long planId);
    
    /**
     * 更新考勤计划
     * @param planDto 考勤计划DTO
     * @return 更新结果
     */
    boolean updateAttendancePlan(AttendancePlanDto planDto);
    
    /**
     * 删除考勤计划
     * @param planId 计划ID
     * @return 删除结果
     */
    boolean deleteAttendancePlan(Long planId);
    
    /**
     * 学生签到
     * @param planId 考勤计划ID
     * @param userId 用户ID（将自动转换为studentId）
     * @param latitude 签到纬度
     * @param longitude 签到经度
     * @param location 签到位置描述
     * @return 签到结果
     */
    Map<String, Object> studentCheckIn(Long planId, Long userId, Double latitude, Double longitude, String location);
    
    /**
     * 获取考勤记录列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    PageResult getAttendanceRecordList(AttendanceQueryDto queryDto);

    /**
     * 更新考勤记录状态
     * @param recordId 记录ID
     * @param status 新状态
     * @param remark 备注
     */
    void updateAttendanceStatus(Long recordId, String status, String remark);
    
    /**
     * 为活动预约的学生生成考勤记录
     * @param planId 活动计划ID
     * @return 生成结果
     */
    Map<String, Object> generateAttendanceRecordsForReservedStudents(Long planId);
    
    /**
     * 删除考勤记录
     * @param recordId 记录ID
     * @return 删除结果
     */
    boolean deleteAttendanceRecord(Long recordId);
    
    /**
     * 获取当前用户可参与的考勤计划列表
     * @param userId 用户ID
     * @return 可参与的考勤计划列表
     */
    List<Map<String, Object>> getCurrentUserAvailablePlans(Long userId);
    
    /**
     * 获取快速签到计划
     * @param userId 用户ID
     * @return 当前可签到的计划
     */
    Map<String, Object> getQuickSignPlan(Long userId);

    /**
     * 获取学生考勤统计
     * @param studentId 学生ID
     * @return 统计数据
     */
    Map<String, Object> getStudentAttendanceStatistics(Long studentId);
    
    /**
     * 获取课程考勤统计
     * @param courseId 课程ID
     * @return 统计数据
     */
    Map<String, Object> getCourseAttendanceStatistics(Long courseId);
    
    /**
     * 生成考勤统计数据
     * @param date 统计日期
     */
    void generateAttendanceStatistics(LocalDate date);

    /**
     * 更新考勤统计数据
     * @param type 考勤类型
     * @param date 统计日期
     */
    void updateAttendanceStatistics(String type, LocalDate date);

    /**
     * 为值班安排创建考勤记录
     * @param planId 考勤计划ID
     * @param studentIds 学生ID列表
     * @return 创建结果
     */
    Map<String, Object> createAttendanceRecordsForDuty(Long planId, List<Long> studentIds);

    /**
     * 根据值班安排ID查找考勤计划ID
     * @param scheduleId 值班安排ID
     * @return 考勤计划ID，如果不存在则返回null
     */
    Long findPlanIdByScheduleId(Long scheduleId);
    
    /**
     * 同步考勤记录与值班安排（删除多余记录，添加缺失记录）
     * @param planId 考勤计划ID
     * @param currentStudentIds 当前值班的学生ID列表
     * @return 同步结果
     */
    Map<String, Object> syncAttendanceRecordsForDuty(Long planId, List<Long> currentStudentIds);

    /**
     * 更新考勤计划状态
     * @param planId 考勤计划ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateAttendancePlanStatus(Long planId, boolean status);

    /**
     * 获取总体统计数据
     * @param queryDto 查询参数
     * @return 统计数据
     */
    Map<String, Object> getOverallStatistics(AttendanceQueryDto queryDto);

    /**
     * 获取考勤趋势数据
     * @param queryDto 查询参数
     * @return 趋势数据
     */
    List<Map<String, Object>> getAttendanceTrends(AttendanceQueryDto queryDto);

    /**
     * 获取考勤类型分布数据
     * @param queryDto 查询参数
     * @return 类型分布数据
     */
    List<Map<String, Object>> getAttendanceTypeDistribution(AttendanceQueryDto queryDto);

    /**
     * 获取学生考勤统计列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    PageResult getStudentAttendanceStatisticsList(AttendanceQueryDto queryDto);

    /**
     * 获取考勤计划的签到状态
     * @param planId 考勤计划ID
     * @param userId 用户ID
     * @return 签到状态信息
     */
    Map<String, Object> getAttendanceStatus(Long planId, Long userId);
}