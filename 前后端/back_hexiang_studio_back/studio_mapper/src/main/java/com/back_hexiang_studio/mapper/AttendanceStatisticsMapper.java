package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.AttendanceQueryDto;
import com.back_hexiang_studio.entity.AttendanceStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceStatisticsMapper {
    // 新增统计数据
    int insert(AttendanceStatistics statistics);
    
    // 更新统计数据
    int update(AttendanceStatistics statistics);
    
    // 根据类型和日期查询统计数据
    AttendanceStatistics selectByTypeAndDate(@Param("type") String type, @Param("date") LocalDate date);
    
    // 查询指定日期范围的统计数据
    List<AttendanceStatistics> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 查询统计数据
    List<Map<String, Object>> selectStatistics(Map<String, Object> params);

    AttendanceStatistics selectByDateAndType(@Param("date") LocalDate date, @Param("type") String type);

    int batchInsertOrUpdate(List<AttendanceStatistics> records);

    // --- Statistics ---
    Map<String, Object> getOverallStatistics(AttendanceQueryDto queryDto);
    List<Map<String, Object>> getAttendanceTrends(AttendanceQueryDto queryDto);
    List<Map<String, Object>> getAttendanceTypeDistribution(AttendanceQueryDto queryDto);
    
    // 根据类型和日期获取统计数据（用于活跃度计算）
    Map<String, Object> getStatsByTypeAndDate(@Param("type") String type, @Param("date") LocalDate date);
} 