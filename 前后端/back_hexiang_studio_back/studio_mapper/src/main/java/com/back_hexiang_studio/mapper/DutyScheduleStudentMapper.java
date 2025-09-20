package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.DutyScheduleStudent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DutyScheduleStudentMapper {
    // 新增值班学生关联
    int insert(DutyScheduleStudent dutyScheduleStudent);
    
    // 批量插入值班学生关联
    int batchInsert(List<DutyScheduleStudent> dutyScheduleStudents);
    
//    // 根据值班安排ID查询学生列表
    List<Map<String, Object>> selectStudentsByScheduleId(Long scheduleId);
    
    // 根据值班安排ID和学生ID查询关联
    DutyScheduleStudent selectByScheduleIdAndStudentId(@Param("scheduleId") Long scheduleId, @Param("studentId") Long studentId);
    
    // 更新值班学生关联
    int update(DutyScheduleStudent dutyScheduleStudent);
    
    // 删除值班学生关联
    int deleteById(Long id);
    
    // 根据值班安排ID删除所有关联
    int deleteByScheduleId(Long scheduleId);
    
    // 根据值班安排ID和学生ID删除关联
    int deleteByScheduleIdAndStudentId(@Param("scheduleId") Long scheduleId, @Param("studentId") Long studentId);
    
    // 检查学生是否在值班名单中
    boolean isStudentInDutySchedule(@Param("scheduleId") Long scheduleId, @Param("studentId") Long studentId);

    /**
     * 获取指定值班安排的学生信息
     * @param scheduleId 值班安排ID
     * @return 学生信息列表
     */
    List<Map<String, Object>> getStudentsByScheduleId(@Param("scheduleId") Long scheduleId);

    List<DutyScheduleStudent> findByScheduleId(@Param("scheduleId") Long scheduleId);
} 