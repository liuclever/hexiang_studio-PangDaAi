package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.AttendanceRecord;
import com.back_hexiang_studio.enumeration.AttendanceStatus;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考勤记录Mapper接口
 * 最终修复版本：确保所有被调用的方法都有定义，并统一了命名规范
 */
@Mapper
public interface AttendanceRecordMapper {

    // --- 核心方法 ---
    List<Map<String, Object>> findAttendanceRecordsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    void insert(AttendanceRecord record);
    void update(AttendanceRecord attendanceRecord);
    int batchInsert(List<AttendanceRecord> records);
    Page<Map<String, Object>> pageQuery(Map<String, Object> params);
    
    // --- Select (查询) ---
    @Select("select * from attendance_record where id = #{id}")
    AttendanceRecord selectById(Long id);

    @Select("SELECT * FROM attendance_record WHERE plan_id = #{planId} AND student_id = #{studentId}")
    AttendanceRecord findByPlanIdAndStudentId(@Param("planId") Long planId, @Param("studentId") Long studentId);
    
    /**
     * 使用悲观锁查询考勤记录，防止并发冲突
     */
    @Select("SELECT * FROM attendance_record WHERE plan_id = #{planId} AND student_id = #{studentId} FOR UPDATE")
    AttendanceRecord findByPlanIdAndStudentIdForUpdate(@Param("planId") Long planId, @Param("studentId") Long studentId);
    
    @Select("SELECT * FROM attendance_record WHERE plan_id = #{planId}")
    List<AttendanceRecord> findByPlanId(Long planId);

    List<AttendanceRecord> findByPlanAndStatus(@Param("planId") Long planId, @Param("attendanceStatus") AttendanceStatus attendanceStatus);

    @Select("SELECT ar.*, s.student_name, s.student_number, ap.name as plan_name, ap.start_time, ap.end_time " +
            "FROM attendance_record ar " +
            "JOIN student s ON ar.student_id = s.id " +
            "JOIN attendance_plan ap ON ar.plan_id = ap.id " +
            "WHERE ar.id = #{id}")
    Map<String, Object> getAttendanceRecordDetail(Long id);
    
    @Select("SELECT * FROM attendance_record WHERE student_id = #{studentId} AND check_in_time IS NOT NULL " +
            "AND check_in_time >= #{startOfMonth} AND check_in_time < #{endOfMonth}")
    List<AttendanceRecord> findByStudentIdAndMonth(@Param("studentId") Long studentId, @Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);
    
    // --- 为了修复编译错误而添加/恢复的方法 ---
    List<Map<String, Object>> getDutyAttendanceStatusInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<Map<String, Object>> selectByPage(@Param("params") Map<String, Object> params);
    Map<String, Integer> countByTypeAndStatus(@Param("type") String type, @Param("date") LocalDate date);
    Map<String, Object> getStatisticsForPlan(@Param("planId") Long planId);
    List<Map<String, Object>> selectRecordsByPlanId(@Param("planId") Long planId);
    Map<String, Object> getStudentStatistics(@Param("studentId") Long studentId);
    Map<String, Object> getCourseStatistics(@Param("courseId") Long courseId);
    List<AttendanceRecord> selectByPlanId(@Param("planId") Long planId);
    List<Map<String, Object>> getStudentStatisticsList(@Param("params") Map<String, Object> params);
    void updateStatusAndRemark(@Param("recordId") Long recordId, @Param("status") AttendanceStatus status, @Param("remark") String remark);


    // --- Delete (删除) ---
    @Delete("DELETE FROM attendance_record WHERE id = #{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM attendance_record WHERE plan_id = #{planId}")
    int deleteByPlanId(Long planId);
    
    // --- Insert (插入) ---
    @Insert("INSERT INTO attendance_record (plan_id, student_id, attendance_status, check_in_time, create_time, update_time) " +
            "VALUES (#{planId}, #{studentId}, #{attendanceStatus}, #{checkInTime}, #{createTime}, #{updateTime})")
    void insertRecord(AttendanceRecord record);
    
    /**
     * 根据学生ID和时间范围，将考勤记录状态更新为“请假”
     * @param studentId 学生ID
     * @param startTime 请假开始时间
     * @param endTime 请假结束时间
     * @param approverId 审批人ID
     */
    void updateStatusToLeaveByTimeRange(@Param("studentId") Long studentId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("updateUser") Long approverId);

    /**
     * 根据学生ID和考勤计划ID，将考勤记录状态更新为"请假"（审批通过后调用）
     * @param studentId 学生ID
     * @param planId 考勤计划ID
     * @param approverId 审批人ID
     */
    void updateStatusToLeaveByPlanId(@Param("studentId") Long studentId,
                                     @Param("planId") Long planId,
                                     @Param("updateUser") Long approverId);

    /**
     * 根据考勤计划ID和学生ID查询考勤记录
     * @param planId 考勤计划ID
     * @param studentId 学生ID
     * @return 考勤记录列表
     */
    List<AttendanceRecord> getByPlanIdAndStudentId(@Param("planId") Long planId, 
                                                   @Param("studentId") Long studentId);
} 