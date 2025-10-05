package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.AttendancePlanDto;
import com.back_hexiang_studio.dv.dto.DutyScheduleSyncDto;
import com.back_hexiang_studio.entity.DutySchedule;
import com.back_hexiang_studio.entity.DutyScheduleStudent;
import com.back_hexiang_studio.mapper.DutyScheduleMapper;
import com.back_hexiang_studio.mapper.DutyScheduleStudentMapper;
import com.back_hexiang_studio.service.AttendanceService;
import com.back_hexiang_studio.service.DutyScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 值班管理服务 - 核心功能，简单实现
 */
@Slf4j
@Primary
@Service("dutyScheduleServiceSimple")
public class DutyScheduleServiceImpl implements DutyScheduleService {

    @Autowired
    private DutyScheduleMapper dutyScheduleMapper;

    @Autowired
    private DutyScheduleStudentMapper dutyScheduleStudentMapper;

    @Autowired
    private AttendanceService attendanceService;

    // 固定时间段配置
    private static final List<String> TIME_SLOTS = Arrays.asList(
            "08:30-10:00", "10:20-11:50", "14:00-15:30", "15:50-17:20", "18:30-20:00"
    );

    /**
     * 获取周值班表 - 核心方法
     */
    @Override
    public Map<String, Object> getWeeklyDutyTable(LocalDate startDate, LocalDate endDate) {
        log.info(" 查询值班表: {} 到 {}", startDate, endDate);
        
        try {
            // 1. 检查并生成数据（如果需要）
            checkAndGenerateData(startDate, endDate);
            
            // 2. 查询值班数据
            List<Map<String, Object>> dutyData = queryWeeklyDuty(startDate, endDate);
            
            // 3. 构建返回结果
            Map<String, Object> result = buildWeeklyResponse(dutyData, startDate, endDate);
            
            log.info("返回 {} 条值班记录", dutyData.size());
            return result;
            
                } catch (Exception e) {
            log.error("查询值班表失败", e);
            return createEmptyResponse(startDate, endDate);
        }
    }

    /**
     * 检查并生成数据（如果需要）
     */
    private void checkAndGenerateData(LocalDate startDate, LocalDate endDate) {
        long count = dutyScheduleMapper.countByDateRange(
            startDate.atStartOfDay(), 
            endDate.plusDays(1).atStartOfDay()
        );
        
        //  如果查询的周没有数据，且是未来的周，尝试从前一周复制
        if (count == 0 && !startDate.isBefore(LocalDate.now())) {
            log.info("未来周无数据，尝试自动复制前一周: {}", startDate);
            
            try {
                // 计算前一周的日期范围
                LocalDate prevWeekStart = startDate.minusWeeks(1);
                LocalDate prevWeekEnd = endDate.minusWeeks(1);
                
                // 查询前一周是否有数据
                long prevWeekCount = dutyScheduleMapper.countByDateRange(
                    prevWeekStart.atStartOfDay(), 
                    prevWeekEnd.plusDays(1).atStartOfDay()
                );
                
                if (prevWeekCount > 0) {
                    // 有前一周数据，执行复制
                    List<Map<String, Object>> prevWeekData = queryWeeklyDuty(prevWeekStart, prevWeekEnd);
                    int copiedCount = copyWeeklyDutyData(prevWeekData, prevWeekStart, startDate);
                    log.info("自动复制成功: 从 {} 复制 {} 个值班安排到 {}", prevWeekStart, copiedCount, startDate);
                } else {
                    log.info("前一周也无数据，跳过自动复制: {}", prevWeekStart);
                }
            } catch (Exception e) {
                log.error("自动复制失败: {}", e.getMessage());
                // 自动复制失败不影响查询，只记录错误
            }
        }
    }

    /**
     * 手动复制当前周值班数据到下一周
     */
    @Transactional
    public Map<String, Object> copyCurrentWeekToNext() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate currentWeekMonday = today.with(DayOfWeek.MONDAY);
            LocalDate nextWeekMonday = currentWeekMonday.plusWeeks(1);
            
            log.info("从当前周 {} 复制到下一周 {}", currentWeekMonday, nextWeekMonday);
            
            // 1. 查询当前周的值班数据
            List<Map<String, Object>> currentWeekData = queryWeeklyDuty(
                currentWeekMonday, currentWeekMonday.plusDays(4)
            );
            
            if (currentWeekData.isEmpty()) {
                throw new RuntimeException("当前周没有值班数据，无法复制");
            }
            
            // 2. 检查下一周是否已有数据
            long nextWeekCount = dutyScheduleMapper.countByDateRange(
                nextWeekMonday.atStartOfDay(), 
                nextWeekMonday.plusDays(5).atStartOfDay()
            );
            
            if (nextWeekCount > 0) {
                throw new RuntimeException("下一周已有值班数据，请先清空再复制");
            }
            
            // 3. 复制数据到下一周
            int copiedCount = copyWeeklyDutyData(currentWeekData, currentWeekMonday, nextWeekMonday);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", String.format("成功复制 %d 个值班安排到下一周", copiedCount));
            result.put("sourceWeek", currentWeekMonday.toString());
            result.put("targetWeek", nextWeekMonday.toString());
            result.put("copiedCount", copiedCount);
            
            log.info("成功复制 {} 个值班安排", copiedCount);
            return result;
            
        } catch (Exception e) {
            log.error("复制失败", e);
            throw new RuntimeException("复制失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制值班数据的具体实现
     */
    private int copyWeeklyDutyData(List<Map<String, Object>> sourceData, 
                                   LocalDate sourceWeekStart, LocalDate targetWeekStart) {
        int copiedCount = 0;
        Map<String, List<Long>> scheduleStudentMap = new HashMap<>();
        
        // 按照时间段和日期组织数据
        Map<String, DutySchedule> scheduleMap = new HashMap<>();
        
        for (Map<String, Object> dutyItem : sourceData) {
            String dutyDate = (String) dutyItem.get("duty_date");
            String timeSlot = (String) dutyItem.get("time_slot");
            
            if (dutyDate == null || timeSlot == null) continue;
            
            // 计算目标日期（加7天）
            LocalDate sourceDate = LocalDate.parse(dutyDate);
            LocalDate targetDate = sourceDate.plusWeeks(1);
            String targetDateStr = targetDate.toString();
            
            String scheduleKey = targetDateStr + "_" + timeSlot;
            
            // 创建新的值班安排（如果还不存在）
            if (!scheduleMap.containsKey(scheduleKey)) {
                DutySchedule newSchedule = new DutySchedule();
                newSchedule.setTitle(timeSlot + " 值班");
                newSchedule.setTimeSlot(timeSlot);
                newSchedule.setLocation("工作室");
                
                // 解析时间段设置开始结束时间
                String[] timeParts = timeSlot.split("-");
                if (timeParts.length == 2) {
                    String[] startParts = timeParts[0].split(":");
                    String[] endParts = timeParts[1].split(":");
                    
                    newSchedule.setStartTime(targetDate.atTime(
                        Integer.parseInt(startParts[0]), Integer.parseInt(startParts[1])
                    ));
                    newSchedule.setEndTime(targetDate.atTime(
                        Integer.parseInt(endParts[0]), Integer.parseInt(endParts[1])
                    ));
                }
                
                newSchedule.setStatus(1);
                newSchedule.setCreateTime(LocalDateTime.now());
                newSchedule.setUpdateTime(LocalDateTime.now());
                
                dutyScheduleMapper.insert(newSchedule);
                scheduleMap.put(scheduleKey, newSchedule);
                scheduleStudentMap.put(scheduleKey, new ArrayList<>());
                copiedCount++;
            }
            
            // 收集学生信息
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> students = (List<Map<String, Object>>) dutyItem.get("students");
            if (students != null) {
                for (Map<String, Object> student : students) {
                    Object studentIdObj = student.get("studentId");
                    if (studentIdObj != null) {
                        Long studentId = ((Number) studentIdObj).longValue();
                        scheduleStudentMap.get(scheduleKey).add(studentId);
                    }
                }
            }
        }
        
        // 创建学生关联
        for (Map.Entry<String, List<Long>> entry : scheduleStudentMap.entrySet()) {
            String scheduleKey = entry.getKey();
            List<Long> studentIds = entry.getValue();
            DutySchedule schedule = scheduleMap.get(scheduleKey);
            
            if (schedule != null && !studentIds.isEmpty()) {
                for (Long studentId : studentIds) {
                    DutyScheduleStudent relation = new DutyScheduleStudent();
                    relation.setScheduleId(schedule.getScheduleId());
                    relation.setStudentId(studentId);
                    relation.setCreateTime(LocalDateTime.now());
                    dutyScheduleStudentMapper.insert(relation);
                }
            }
        }
        
        // 🔧 重要：为每个新复制的值班安排创建考勤计划
        log.info("【复制功能】开始为 {} 个新值班安排创建考勤计划", scheduleMap.size());
        for (Map.Entry<String, DutySchedule> entry : scheduleMap.entrySet()) {
            String scheduleKey = entry.getKey();
            DutySchedule schedule = entry.getValue();
            List<Long> studentIds = scheduleStudentMap.get(scheduleKey);
            
            if (schedule != null && studentIds != null && !studentIds.isEmpty()) {
                try {
                    // 构建DutyScheduleSyncDto用于创建考勤计划
                    DutyScheduleSyncDto syncData = new DutyScheduleSyncDto();
                    syncData.setDutyDate(schedule.getStartTime().toLocalDate().toString());
                    syncData.setTimeSlot(schedule.getTimeSlot());
                    syncData.setDutyName(schedule.getTitle());
                    syncData.setLocation(schedule.getLocation());
                    syncData.setStudentIds(studentIds);
                    
                    // 创建考勤计划（使用独立事务）
                    createNewAttendancePlan(schedule.getScheduleId(), syncData);
                    log.info("为值班安排 {} 创建了考勤计划", schedule.getScheduleId());
                    
                            } catch (Exception e) {
                    log.error("为值班安排 {} 创建考勤计划失败: {}", schedule.getScheduleId(), e.getMessage());
                    // 不影响值班安排的复制，只记录错误
                }
            }
        }
        
        return copiedCount;
    }
    
    /**
     * 生成未来周的值班数据 - 现在调用手动复制
     */
    @Transactional
    public void generateWeeklyDuty(LocalDate weekStart) {
        // 现在暂时不自动生成，改为手动操作
        log.info(" 不再自动生成，请使用手动复制功能");
    }

    /**
     * 查询周值班数据 - 包含学生信息（修复版）
     */
    private List<Map<String, Object>> queryWeeklyDuty(LocalDate startDate, LocalDate endDate) {
        // 🔧 使用分离查询避免数据重复
        // 1. 先查询基础值班安排数据（不JOIN学生）
        List<Map<String, Object>> schedules = dutyScheduleMapper.getDutyScheduleByDateRange(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );
        
        log.info(" 查询到值班安排: {} 个", schedules.size());
        
        // 2. 为每个值班安排单独查询学生信息
        for (Map<String, Object> schedule : schedules) {
            Object scheduleIdObj = schedule.get("schedule_id");
            if (scheduleIdObj != null) {
                Long scheduleId = ((Number) scheduleIdObj).longValue();
                
                // 查询该值班安排的学生（包含考勤状态）
                List<Map<String, Object>> students = dutyScheduleStudentMapper.getStudentsByScheduleId(scheduleId);
                schedule.put("students", students);
                
                log.info(" 值班安排 {} 包含 {} 名学生", scheduleId, students.size());
            } else {
                schedule.put("students", new ArrayList<>());
            }
        }
        
        return schedules;
    }

    /**
     * 构建前端需要的响应格式
     */
    private Map<String, Object> buildWeeklyResponse(List<Map<String, Object>> dutyData, 
                                                  LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        
        // 构建工作日结构（周一到周五）
        List<Map<String, Object>> weekDays = new ArrayList<>();
        for (int i = 0; i < 5; i++) {  // 只生成5个工作日
            LocalDate date = startDate.plusDays(i);
            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("dayOfWeek", date.getDayOfWeek().getValue()); // 1=周一, 2=周二, ..., 5=周五
            weekDays.add(day);
        }
        
        // 包装为tableData格式（兼容前端）
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("weekDays", weekDays);
        tableData.put("timeSlots", TIME_SLOTS);
        tableData.put("dutyData", dutyData);  // 🔧 关键修复：把查询到的值班数据放进去！
        
        // 获取考勤状态数据
        List<Map<String, Object>> statusData = getAttendanceStatus(startDate, endDate);
        
        result.put("tableData", tableData);
        result.put("statusData", statusData);
        result.put("structure", getDutyScheduleStructure());
        result.put("weekStart", startDate.toString());
        result.put("weekEnd", endDate.toString());
        
        // 🔍 添加调试日志
        log.info(" 返回值班数据: {} 条记录", dutyData.size());
        
        return result;
    }

    /**
     * 创建空响应（当出错时）
     */
    private Map<String, Object> createEmptyResponse(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("tableData", new HashMap<>());
        result.put("statusData", new ArrayList<>());
        result.put("structure", getDutyScheduleStructure());
        result.put("weekStart", startDate.toString());
        result.put("weekEnd", endDate.toString());
        return result;
    }

    /**
     * 批量同步值班安排 - 简化版
     */
    @Override
    @Transactional
    public Map<String, Object> batchSyncDutySchedules(List<DutyScheduleSyncDto> syncDataList) {
        log.info(" 批量同步 {} 条值班安排", syncDataList.size());

        int successCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (DutyScheduleSyncDto syncData : syncDataList) {
            try {
                // 🔧 时间限制：禁止编辑过去的时间段 (测试阶段注释掉)
                LocalDate dutyDate = LocalDate.parse(syncData.getDutyDate());
                /*
                LocalDateTime now = LocalDateTime.now();
                
                // 解析时间段的开始时间
                String[] timeParts = syncData.getTimeSlot().split("-");
                if (timeParts.length == 2) {
                    String[] startParts = timeParts[0].split(":");
                    if (startParts.length == 2) {
                        LocalDateTime dutyStartTime = dutyDate.atTime(
                            Integer.parseInt(startParts[0]), 
                            Integer.parseInt(startParts[1])
                        );
                        
                        // 🔧 15分钟缓冲时间：值班开始后15分钟内还可以编辑
                        LocalDateTime cutoffTime = now.minusMinutes(15);
                        if (dutyStartTime.isBefore(cutoffTime)) {
                            String reason = String.format("时间段 %s %s 已过期，无法编辑", syncData.getDutyDate(), syncData.getTimeSlot());
                            log.warn(" {}", reason);
                            skippedCount++;
                            skippedReasons.add(reason);
                            continue; // 跳过这条记录
                        }
                    }
                }
                */
                
                // 根据日期和时间段查找现有的值班安排
                LocalDateTime dayStart = dutyDate.atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                List<DutySchedule> existingSchedules = dutyScheduleMapper.findByDutyDateAndTimeSlot(
                    dayStart, dayEnd, syncData.getTimeSlot()
                );
                
                Long scheduleId;
                boolean isNewSchedule = false;
                
                if (existingSchedules.isEmpty()) {
                    // 创建新的值班安排
                    DutySchedule newSchedule = createDutySchedule(syncData);
                    dutyScheduleMapper.insert(newSchedule);
                    // 获取插入后的ID（可能需要在mapper中设置useGeneratedKeys）
                    scheduleId = newSchedule.getScheduleId();
                    if (scheduleId == null) {
                        log.warn("插入值班安排后未获取到ID，跳过学生关联");
                        continue;
                    }
                    isNewSchedule = true;
                } else {
                    // 使用现有的值班安排
                    scheduleId = existingSchedules.get(0).getScheduleId();
                }
                
                // 更新学生关联
                updateScheduleStudents(scheduleId, syncData.getStudentIds());
                
                // 🔧 重要：不管新建还是编辑，都要确保每个学生都有考勤计划和记录
                if (syncData.getStudentIds() != null && !syncData.getStudentIds().isEmpty()) {
                    try {
                        ensureAttendancePlanForDuty(scheduleId, syncData, isNewSchedule);
                        log.info(" 为值班安排 {} 确保了考勤计划，涉及 {} 名学生", scheduleId, syncData.getStudentIds().size());
                                    } catch (Exception e) {
                        log.error(" 为值班安排 {} 处理考勤计划失败，但不影响值班安排保存: {}", scheduleId, e.getMessage());
                        // 不重新抛出异常，避免影响值班安排的保存
                    }
                }
                
                successCount++;
                
            } catch (Exception e) {
                log.error(" 同步单条数据失败: {}", syncData, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("totalCount", syncDataList.size());
        
        // 🔧 更详细的返回信息
        if (skippedCount > 0) {
            result.put("message", String.format("同步完成：成功 %d 条，跳过 %d 条过期记录", successCount, skippedCount));
            result.put("skippedReasons", skippedReasons);
        } else {
            result.put("message", String.format("同步完成：成功 %d 条", successCount));
        }
        
        return result;
    }

    /**
     * 创建值班安排
     */
    private DutySchedule createDutySchedule(DutyScheduleSyncDto syncData) {
        DutySchedule schedule = new DutySchedule();
        // 🔧 如果 dutyName 为空，生成默认名称
        String dutyName = syncData.getDutyName();
        if (dutyName == null || dutyName.trim().isEmpty()) {
            dutyName = syncData.getTimeSlot() + " 值班";
        }
        schedule.setTitle(dutyName);
        schedule.setTimeSlot(syncData.getTimeSlot());
        schedule.setLocation(syncData.getLocation());
        
        // 解析时间段
        LocalDate dutyDate = LocalDate.parse(syncData.getDutyDate());
        
        // 简化处理：设置开始和结束时间
        schedule.setStartTime(dutyDate.atTime(8, 30));
        schedule.setEndTime(dutyDate.atTime(10, 0));
        
        schedule.setStatus(1);
        schedule.setCreateTime(LocalDateTime.now());
        schedule.setUpdateTime(LocalDateTime.now());
        
        return schedule;
    }

    /**
     * 更新值班安排的学生关联
     */
    private void updateScheduleStudents(Long scheduleId, List<Long> studentIds) {
        // 删除现有关联
        dutyScheduleStudentMapper.deleteByScheduleId(scheduleId);
        
        // 添加新关联
        if (studentIds != null && !studentIds.isEmpty()) {
            for (Long studentId : studentIds) {
                DutyScheduleStudent relation = new DutyScheduleStudent();
                relation.setScheduleId(scheduleId);
                relation.setStudentId(studentId);
                dutyScheduleStudentMapper.insert(relation);
            }
        }
    }

    // 其他必需方法的简化实现
    @Override
    public Map<String, Object> getDutyScheduleStructure() {
        Map<String, Object> result = new HashMap<>();
        result.put("timeSlots", TIME_SLOTS);
        return result;
    }

    @Override
    public List<Map<String, Object>> getAttendanceStatus(LocalDate startDate, LocalDate endDate) {
        return dutyScheduleMapper.findAttendanceStatusByDateRange(startDate, endDate);
    }

    @Override
    public Map<String, Object> generateNextWeekDutySchedules(LocalDate currentWeekStart) {
        generateWeeklyDuty(currentWeekStart.plusDays(7));
        Map<String, Object> result = new HashMap<>();
        result.put("message", "生成成功");
            return result;
        }

    @Override
    public Map<String, Object> batchUpdateDutyScheduleStudents(List<Map<String, Object>> updateData) {
        // 转换为统一格式
        List<DutyScheduleSyncDto> syncList = new ArrayList<>();
        for (Map<String, Object> data : updateData) {
            DutyScheduleSyncDto dto = new DutyScheduleSyncDto();
            // 这里需要根据实际数据格式来转换
            // 暂时返回空实现
        }
        return batchSyncDutySchedules(syncList);
    }

    /**
     * 确保值班安排有考勤计划，每个学生都有考勤记录（独立事务）
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void ensureAttendancePlanForDuty(Long scheduleId, DutyScheduleSyncDto syncData, boolean isNewSchedule) {
        try {
            // 检查是否已经存在考勤计划
            Long existingPlanId = attendanceService.findPlanIdByScheduleId(scheduleId);
            
            if (existingPlanId == null) {
                // 没有考勤计划，创建新的
                log.info(" 值班安排 {} 没有考勤计划，开始创建", scheduleId);
                createNewAttendancePlan(scheduleId, syncData);
            } else {
                // 已有考勤计划，同步考勤记录（删除多余，添加缺失）
                log.info(" 值班安排 {} 已有考勤计划 {}，同步考勤记录", scheduleId, existingPlanId);
                Map<String, Object> syncResult = attendanceService.syncAttendanceRecordsForDuty(existingPlanId, syncData.getStudentIds());
                log.info(" 考勤记录同步结果: {}", syncResult.get("message"));
            }
            
        } catch (Exception e) {
            log.error(" 为值班安排 {} 处理考勤计划失败", scheduleId, e);
            // 独立事务，异常不会影响主事务
            throw new RuntimeException("考勤计划处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建新的考勤计划（内部方法）
     */
    private void createNewAttendancePlan(Long scheduleId, DutyScheduleSyncDto syncData) {
        AttendancePlanDto planDto = new AttendancePlanDto();
        planDto.setType("duty");
        planDto.setName(syncData.getDutyName() + " - 考勤");
        planDto.setLocation(syncData.getLocation());
        planDto.setScheduleId(scheduleId);
        
        // 解析时间段，设置考勤时间
        LocalDate dutyDate = LocalDate.parse(syncData.getDutyDate());
        String[] timeParts = syncData.getTimeSlot().split("-");
        if (timeParts.length == 2) {
            String[] startParts = timeParts[0].split(":");
            String[] endParts = timeParts[1].split(":");
            
            planDto.setStartTime(dutyDate.atTime(
                Integer.parseInt(startParts[0]), 
                Integer.parseInt(startParts[1])
            ));
            planDto.setEndTime(dutyDate.atTime(
                Integer.parseInt(endParts[0]), 
                Integer.parseInt(endParts[1])
            ));
        }
        
        // 设置默认签到参数
        planDto.setRadius(50); // 50米签到半径
        planDto.setStatus(1);
        planDto.setNote("系统自动创建的值班考勤计划");
        
        // 调用考勤服务创建计划（会自动为学生创建考勤记录）
        attendanceService.createAttendancePlan(planDto);
    }
} 