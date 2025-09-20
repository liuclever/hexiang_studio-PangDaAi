package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.dv.dto.DutyScheduleBatchDto;
import com.back_hexiang_studio.dv.dto.DutyScheduleDto;
import com.back_hexiang_studio.dv.dto.DutyScheduleSyncDto;
import com.back_hexiang_studio.entity.DutySchedule;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.DutyScheduleService;
import com.back_hexiang_studio.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 值班管理控制器
 * 权限：所有人可查看，主任、副主任、超级管理员可修改
 */
@RestController
@RequestMapping("/admin/duty-schedule")
@Slf4j
public class DutyScheduleController{
    
    // 自动注入Primary Bean（简化版）
    @Autowired
    private DutyScheduleService dutyScheduleService;

    /**
     * 批量更新值班安排学生
     */
    @PostMapping("/batch-update-students")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ATTENDANCE_MANAGE')")
    public Result<Map<String, Object>> batchUpdateDutyScheduleStudentsOld(@RequestBody List<Map<String, Object>> updateData) {
        try {
            log.info("批量更新值班安排学生: {}", updateData);
            Map<String, Object> result = dutyScheduleService.batchUpdateDutyScheduleStudents(updateData);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量更新值班安排学生失败", e);
            return Result.error("批量更新值班安排学生失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量同步值班安排
     * 根据日期和时间段同步值班安排，支持增删改一体化操作
     */
    @PostMapping("/batch-sync")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ATTENDANCE_MANAGE')")
    public Result<Map<String, Object>> batchSyncDutySchedules(@RequestBody List<DutyScheduleSyncDto> syncDataList) {
        try {
            log.info("批量同步值班安排: {}", syncDataList);
            Map<String, Object> result = dutyScheduleService.batchSyncDutySchedules(syncDataList);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量同步值班安排失败", e);
            return Result.error("批量同步值班安排失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取值班表结构
     */
    @GetMapping("/structure")
    public Result<Map<String, Object>> getDutyScheduleStructure() {
        log.info("获取值班表结构");
        Map<String, Object> result = dutyScheduleService.getDutyScheduleStructure();
        return Result.success(result);
    }

    /**
     * 获取考勤状态
     */
    @GetMapping("/attendance-status")
    public Result<List<Map<String, Object>>> getAttendanceStatus(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("获取考勤状态: startDate={}, endDate={}", startDate, endDate);
        List<Map<String, Object>> result = dutyScheduleService.getAttendanceStatus(startDate, endDate);
        return Result.success(result);
    }
    
    /**
     * 周值班表方法 - 简化版优化
     */
    @GetMapping("/weekly-table")
    public Result<Map<String, Object>> getWeeklyDutyTable(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(name = "weekStart", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        
        log.info("【简化版-工作日】获取周值班表: startDate={}, endDate={}, weekStart={}, offset={}", startDate, endDate, weekStart, offset);
        
        try {
            // 确定日期范围 - 简化逻辑
            if (startDate == null || endDate == null) {
                if (weekStart == null) {
                    weekStart = LocalDate.now();
                    // 调整为本周一
                    while (weekStart.getDayOfWeek().getValue() != 1) {
                        weekStart = weekStart.minusDays(1);
                    }
                }
                
                // 应用偏移量
                if (offset != null && offset != 0) {
                    weekStart = weekStart.plusWeeks(offset);
                }
                
                startDate = weekStart;
                endDate = weekStart.plusDays(4); // 工作日5天（周一到周五）
            }
            
            // 直接调用简化的Service方法
            Map<String, Object> result = dutyScheduleService.getWeeklyDutyTable(startDate, endDate);
            
            log.info("【简化版】成功返回周值班表数据");
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("【简化版】获取周值班表失败", e);
            return Result.error("获取周值班表失败: " + e.getMessage());
        }
    }

    /**
     * 手动复制当前周值班安排到下一周
     */
    @PostMapping("/copy-to-next-week")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ATTENDANCE_MANAGE')")
    public Result<Map<String, Object>> copyCurrentWeekToNext() {
        log.info("【手动复制】开始复制当前周值班安排到下一周");
        try {
            Map<String, Object> result = dutyScheduleService.copyCurrentWeekToNext();
            return Result.success(result);
        } catch (Exception e) {
            log.error("【手动复制】复制失败", e);
            return Result.error("复制失败: " + e.getMessage());
        }
    }
    
    /**
     * 版本切换接口 - 用于测试
     */
    @PostMapping("/switch-version")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Map<String, Object>> switchVersion(@RequestParam String version) {
        Map<String, Object> result = new HashMap<>();
        result.put("currentVersion", "simple"); // 当前使用简化版
        result.put("message", "当前使用简化版Service，如需切换请修改@Qualifier注解");
        return Result.success(result);
    }
} 