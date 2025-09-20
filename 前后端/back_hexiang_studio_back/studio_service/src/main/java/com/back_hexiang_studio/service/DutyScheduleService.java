package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.DutyScheduleSyncDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 值班安排服务接口
 */
public interface DutyScheduleService {
    
    /**
     * 获取值班表结构（不包含考勤状态）
     * @return 值班表结构数据
     */
    Map<String, Object> getDutyScheduleStructure();
    
    /**
     * 获取考勤状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 考勤状态数据
     */
    List<Map<String, Object>> getAttendanceStatus(LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定日期范围内的值班表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 值班表数据
     */
    Map<String, Object> getWeeklyDutyTable(LocalDate startDate, LocalDate endDate);

    /**
     * 生成下一周值班安排
     * 将当前周的值班安排复制到下一周，保持相同的时间段和值班人员
     * @param currentWeekStart 当前周的开始日期（周一）
     * @return 生成结果，包含生成的值班安排数量和ID列表
     */
    Map<String, Object> generateNextWeekDutySchedules(LocalDate currentWeekStart);

    /**
     * 手动复制当前周值班安排到下一周
     * 复制当前周的所有值班安排（包括时间段、地点、值班人员）到下一周对应的日期
     * @return 复制结果，包含复制的数量和详细信息
     */
    Map<String, Object> copyCurrentWeekToNext();
    
    /**
     * 批量更新值班安排学生（专门处理前端编辑模式的数据格式）
     * 高效处理多个值班安排的学生变更，支持同时处理多个时间段的学生调整
     * @param updateData 更新数据，格式：[{scheduleId, all_schedule_ids, studentIds, dutyDate, timeSlot}]
     * @return 更新结果，包含成功更新的值班安排数量和详细信息
     */
    Map<String, Object> batchUpdateDutyScheduleStudents(List<Map<String, Object>> updateData);
    
    /**
     * 批量同步值班安排
     * 根据日期和时间段同步值班安排，支持增删改一体化操作
     * 如果指定日期和时间段不存在排班，则创建新的排班
     * 如果存在排班但学生列表为空，则删除该排班
     * 如果存在排班且学生列表不为空，则更新该排班的学生
     * 
     * @param syncDataList 同步数据列表，每项包含日期、时间段和学生ID列表
     * @return 同步结果，包含成功同步的数量和详情
     */
    Map<String, Object> batchSyncDutySchedules(List<DutyScheduleSyncDto> syncDataList);
} 