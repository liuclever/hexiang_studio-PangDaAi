package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.AttendancePlan;
import com.back_hexiang_studio.entity.AttendanceRecord;
import com.back_hexiang_studio.enumeration.AttendanceStatus;
import com.back_hexiang_studio.mapper.AttendancePlanMapper;
import com.back_hexiang_studio.mapper.AttendanceRecordMapper;
import com.back_hexiang_studio.mapper.DutyScheduleMapper;
import com.back_hexiang_studio.mapper.DutyScheduleStudentMapper;
import com.back_hexiang_studio.service.AttendanceSchedulingTasks;
import com.back_hexiang_studio.service.AttendanceService;
import com.back_hexiang_studio.service.DutyScheduleService;
import com.back_hexiang_studio.utils.NotificationUtils;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.SystemException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.time.DayOfWeek;
import com.back_hexiang_studio.entity.DutySchedule;
import com.back_hexiang_studio.entity.DutyScheduleStudent;
import java.util.ArrayList;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.back_hexiang_studio.mapper.UserMapper;

/**
 * 考勤调度任务服务实现类
 * 
 * 负责处理考勤相关的定时任务，包括：
 * 1. 值班调度任务：自动生成排班、激活考勤计划
 * 2. 考勤状态更新：处理过期考勤、更新状态
 * 3. 统计数据生成：每日考勤统计
 * 4. 通知提醒：值班提醒、任务状态通知
 * 5. 数据维护：备份、报表生成
 * 
 * @author Hexiang
 * @date 2024/09/27
 */
@Slf4j
@Service
public class AttendanceSchedulingTasksimpl implements AttendanceSchedulingTasks {

    // ==================== 时间配置常量 ====================
    
    /** 考勤计划提前激活时间（小时） */
    private static final int ATTENDANCE_ACTIVATION_HOURS = 48;
    
    /** 默认考勤地点经纬度 - 纬度 */
    private static final double DEFAULT_LATITUDE = 29.553017;
    
    /** 默认考勤地点经纬度 - 经度 */
    private static final double DEFAULT_LONGITUDE = 106.237538;
    
    /** 默认考勤半径（米） */
    private static final int DEFAULT_RADIUS = 100;
    
    /** 系统默认用户ID */
    private static final Long SYSTEM_USER_ID = 1L;
    
    /** 重要通知级别 */
    private static final int NOTIFICATION_IMPORTANT = 1;
    
    /** 普通通知级别 */
    private static final int NOTIFICATION_NORMAL = 0;
    
    // ==================== 值班时间段配置 ====================
    
    /** 值班时间段配置 */
    private static final class DutyPeriod {
        static final LocalTime MORNING_START = LocalTime.of(8, 30);
        static final LocalTime MORNING_END = LocalTime.of(10, 0);
        
        static final LocalTime SECOND_START = LocalTime.of(10, 20);
        static final LocalTime SECOND_END = LocalTime.of(11, 50);
        
        static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
        static final LocalTime AFTERNOON_END = LocalTime.of(15, 30);
        
        static final LocalTime FOURTH_START = LocalTime.of(15, 50);
        static final LocalTime FOURTH_END = LocalTime.of(17, 20);
        
        static final LocalTime EVENING_START = LocalTime.of(18, 30);
        static final LocalTime EVENING_END = LocalTime.of(20, 0);
    }

    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private AttendancePlanMapper attendancePlanMapper;
    
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    
    @Autowired
    @Qualifier("dutyScheduleServiceSimple")
    private DutyScheduleService dutyScheduleService;
    
    @Autowired
    private DutyScheduleMapper dutyScheduleMapper;
    
    @Autowired
    private DutyScheduleStudentMapper dutyScheduleStudentMapper;
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 每日值班调度任务编排器
     * 
     * 执行时间：每天凌晨2点
     * 主要职责：
     * 1. 检查并自动生成下周排班表，确保排班表永不"断档"
     * 2. 激活即将在48小时内开始的排班考勤计划，确保可以正常签到
     * 
     * 任务设计理念：
     * - 预防性调度：提前生成排班，避免临时空档
     * - 及时激活：确保考勤功能在适当时机可用
     * - 容错处理：各职责独立执行，单点故障不影响全局
     */
    // 测试阶段暂时禁用自动生成，改为手动操作
    // @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void dailyDutyScheduleOrchestrator() {
        log.info("===== 开始执行每日值班调度任务 =====");

        // 职责一：检查并自动生成下周排班
        try {
            handleWeeklyScheduleGeneration();
        } catch (Exception e) {
            log.error("❌ [职责一] 自动生成下周排班时发生错误", e);
            // 记录但不抛出异常，确保职责二能继续执行
        }

        // 职责二：激活即将开始的考勤计划
        try {
            handleUpcomingAttendancePlans();
        } catch (Exception e) {
            log.error("❌ [职责二] 激活考勤计划时发生错误", e);
        }
        
        log.info("===== 每日值班调度任务执行完毕 =====");
    }

    /**
     * 处理周排班生成逻辑
     */
    private void handleWeeklyScheduleGeneration() {
        try {
            log.info("--- [职责一] 检查是否需要生成下周排班 ---");
            
            LocalDate nextWeekMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
            long scheduleCount = dutyScheduleMapper.countByDateRange(
                    nextWeekMonday.atStartOfDay(), 
                    nextWeekMonday.plusDays(7).atStartOfDay());

            if (scheduleCount == 0) {
                log.info("下周 ({}) 尚无排班，开始自动生成...", nextWeekMonday);
                LocalDate thisWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
                dutyScheduleService.generateNextWeekDutySchedules(thisWeekMonday);
                log.info("✅ 成功生成下周排班");
            } else {
                log.info("下周已有排班，无需自动生成");
            }
            
        } catch (Exception e) {
            log.error("处理周排班生成时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "自动生成排班失败");
        }
        }

    /**
     * 处理即将开始的考勤计划激活
     */
    private void handleUpcomingAttendancePlans() {
        try {
            log.info("--- [职责二] 检查并激活即将开始的考勤计划 ---");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime checkHorizon = now.plusHours(ATTENDANCE_ACTIVATION_HOURS);

            List<DutySchedule> schedulesToActivate = dutyScheduleMapper.findSchedulesWithoutPlans(now, checkHorizon);

            if (schedulesToActivate.isEmpty()) {
                log.info("没有发现需要在{}小时内激活的考勤计划", ATTENDANCE_ACTIVATION_HOURS);
                return;
            }

                log.info("发现 {} 个待激活的排班，开始处理...", schedulesToActivate.size());
            
            int activatedCount = 0;
                for (DutySchedule schedule : schedulesToActivate) {
                try {
                    activateAttendancePlan(schedule);
                    activatedCount++;
                } catch (Exception e) {
                    log.error("激活排班ID {} 的考勤计划失败", schedule.getScheduleId(), e);
                    // 继续处理其他排班
                }
            }
            
            log.info("✅ 成功激活了 {}/{} 个排班的考勤计划", activatedCount, schedulesToActivate.size());
            
        } catch (Exception e) {
            log.error("处理即将开始的考勤计划时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "激活考勤计划失败");
        }
    }

    /**
     * 激活单个排班的考勤计划
     * 
     * @param schedule 排班信息
     */
    private void activateAttendancePlan(DutySchedule schedule) {
        try {
            // 参数校验
            if (schedule == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "排班信息不能为空");
            }
            
            if (schedule.getScheduleId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "排班ID不能为空");
            }

                    // 创建考勤计划
            AttendancePlan plan = createAttendancePlan(schedule);
            attendancePlanMapper.insert(plan);

            // 为学生创建考勤记录
            createAttendanceRecords(plan, schedule);

            log.info("成功为排班ID {} 创建考勤计划", schedule.getScheduleId());
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("为排班 {} 创建考勤计划时发生异常", schedule.getScheduleId(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "创建考勤计划失败");
        }
    }

    /**
     * 根据排班信息创建考勤计划
     * 
     * @param schedule 排班信息
     * @return 考勤计划
     */
    private AttendancePlan createAttendancePlan(DutySchedule schedule) {
                    AttendancePlan plan = new AttendancePlan();
                    plan.setType("duty");
                    plan.setName(schedule.getTitle() + " - 值班考勤");
                    plan.setStartTime(schedule.getStartTime());
                    plan.setEndTime(schedule.getEndTime());
                    plan.setLocation(schedule.getLocation());
        plan.setLocationLat(DEFAULT_LATITUDE);
        plan.setLocationLng(DEFAULT_LONGITUDE);
        plan.setRadius(DEFAULT_RADIUS);
                    plan.setScheduleId(schedule.getScheduleId());
        plan.setCreateUser(SYSTEM_USER_ID);
                    plan.setCreateTime(LocalDateTime.now());
                    plan.setUpdateTime(LocalDateTime.now());
                    plan.setStatus(1);
                    plan.setProcessed(false);
        return plan;
    }

    /**
     * 为考勤计划创建学生考勤记录
     * 
     * @param plan 考勤计划
     * @param schedule 排班信息
     */
    private void createAttendanceRecords(AttendancePlan plan, DutySchedule schedule) {
        try {
                    List<DutyScheduleStudent> students = dutyScheduleStudentMapper.findByScheduleId(schedule.getScheduleId());
            
            if (students.isEmpty()) {
                log.warn("排班ID {} 没有分配学生", schedule.getScheduleId());
                return;
            }

                        List<AttendanceRecord> recordsToInsert = new ArrayList<>();
                        for (DutyScheduleStudent student : students) {
                            AttendanceRecord record = new AttendanceRecord();
                            record.setPlanId(plan.getPlanId());
                            record.setStudentId(student.getStudentId());
                            record.setStatus(AttendanceStatus.pending);
                            record.setCreateTime(LocalDateTime.now());
                            record.setUpdateTime(LocalDateTime.now());
                            recordsToInsert.add(record);
                        }
            
                        attendanceRecordMapper.batchInsert(recordsToInsert);
            log.info("为排班ID {} 的 {} 名学生创建了考勤记录", schedule.getScheduleId(), students.size());
            
        } catch (Exception e) {
            log.error("为排班 {} 创建学生考勤记录时发生异常", schedule.getScheduleId(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "创建学生考勤记录失败");
        }
    }

    /**
     * 每日考勤统计数据更新任务
     * 
     * 执行时间：每天凌晨2点
     * 功能说明：统计前一天的考勤数据，更新考勤统计表
     * 
     * Cron表达式说明：
     * "0 0 2 * * ?" 表示：
     * - 0秒 0分 2点
     * - 每天(*) 每月(*) 每周的任意一天(?)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void dailyAttendanceStatistics() {
        log.info("===== 每日考勤统计数据更新开始 =====");
        
        try {
            // 获取前一天的日期
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // 参数校验
            if (yesterday == null) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, "无法计算前一天日期");
            }

            // 生成指定日期的考勤统计数据
            attendanceService.generateAttendanceStatistics(yesterday);

            log.info("✅ 每日考勤统计数据更新完成，统计日期: {}", yesterday);
            
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 每日考勤统计数据更新失败", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "考勤统计数据更新失败");
        }
        
        log.info("===== 每日考勤统计数据更新任务完毕 =====");
    }

    /**
     * 考勤状态定时更新任务
     * 
     * 执行时间：每5分钟
     * 功能说明：
     * 1. 查找已过期但未处理的考勤计划
     * 2. 将未签到的记录状态更新为缺席
     * 3. 标记考勤计划为已处理
     * 4. 更新考勤统计数据
     * 
     * 处理逻辑：
     * - 值班考勤：开始时间 + 签到限制时间
     * - 课程考勤：结束时间 + 签到限制时间  
     * - 活动考勤：结束时间 + 签到限制时间
     */
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 */5 * * * ?")
    public void updateAttendanceStatus() {
        log.info("===== 考勤状态定时更新开始 =====");
        
        try {
            // 查找已过期但未处理的考勤计划
            List<AttendancePlan> expiredPlans = attendancePlanMapper.findExpiredUnprocessedCoursePlans();
            
            if (expiredPlans.isEmpty()) {
                log.debug("没有发现需要处理的过期考勤计划");
                return;
            }

            log.info("发现 {} 个过期但未处理的考勤计划，开始处理...", expiredPlans.size());
            
            int processedCount = 0;
            for (AttendancePlan plan : expiredPlans) {
                try {
                    processExpiredAttendancePlan(plan);
                    processedCount++;
                } catch (Exception e) {
                    log.error("处理过期考勤计划 {} 时发生异常", plan.getPlanId(), e);
                    // 继续处理其他计划
                }
            }

            log.info("✅ 成功处理了 {}/{} 个过期考勤计划", processedCount, expiredPlans.size());

        } catch (Exception e) {
            log.error("❌ 考勤状态定时更新失败", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "考勤状态更新失败");
        }
        
        log.info("===== 考勤状态定时更新完毕 =====");
    }

    /**
     * 处理单个过期的考勤计划
     * 
     * @param plan 过期的考勤计划
     */
    private void processExpiredAttendancePlan(AttendancePlan plan) {
        try {
            // 参数校验
            if (plan == null || plan.getPlanId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "考勤计划信息不完整");
            }

            // 查找所有未签到的记录
            List<AttendanceRecord> pendingRecords = attendanceRecordMapper.findByPlanAndStatus(
                    plan.getPlanId(), AttendanceStatus.pending);

            // 更新为缺席状态
            int updatedRecords = 0;
            for (AttendanceRecord record : pendingRecords) {
                record.setStatus(AttendanceStatus.absent);
                record.setUpdateTime(LocalDateTime.now());
                attendanceRecordMapper.update(record);
                updatedRecords++;
            }

            // 标记考勤计划为已处理
            plan.setProcessed(true);
            attendancePlanMapper.update(plan);

            // 更新考勤统计数据
            attendanceService.updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());

            log.info("处理过期考勤计划 {} 完成，更新了 {} 条记录为缺席状态", 
                    plan.getPlanId(), updatedRecords);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("处理过期考勤计划时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "处理过期考勤计划失败");
        }
    }

    /**
     * 值班考勤状态特殊处理任务
     * 
     * 执行时间：每日10:05, 12:05, 15:05, 17:05
     * 功能说明：处理特定时间段结束后的值班考勤状态更新
     * 
     * 时间段配置：
     * - 8:30-10:00 (10:05处理)
     * - 10:20-11:50 (12:05处理)  
     * - 14:00-15:30 (15:05处理)
     * - 15:50-17:20 (17:05处理)
     * - 18:30-20:00 (需要添加20:05的处理)
     */
    @Scheduled(cron = "0 5 10,12,15,17 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateDutyAttendanceStatus() {
        log.info("===== 值班考勤状态特殊处理开始 =====");
        
        try {
        LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();
            
            // 根据当前时间确定要处理的时间段
            DutyTimeRange timeRange = determineDutyTimeRange(currentTime);
            
            if (timeRange == null) {
                log.warn("当前时间 {} 不在预定的值班处理时间段内", currentTime);
                return;
            }

            log.info("开始处理 {}-{} 时间段的值班考勤", timeRange.startTime, timeRange.endTime);
            
            processDutyAttendanceForPeriod(timeRange, now.toLocalDate());

        } catch (Exception e) {
            log.error("❌ 值班考勤状态特殊处理失败", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "值班考勤状态处理失败");
        }
        
        log.info("===== 值班考勤状态特殊处理完毕 =====");
    }

    /**
     * 根据当前时间确定值班时间段
     * 
     * @param currentTime 当前时间
     * @return 值班时间段，如果不在处理时间则返回null
     */
    private DutyTimeRange determineDutyTimeRange(LocalTime currentTime) {
        int hour = currentTime.getHour();
        
        switch (hour) {
            case 10:
                return new DutyTimeRange(DutyPeriod.MORNING_START, DutyPeriod.MORNING_END);
            case 12:
                return new DutyTimeRange(DutyPeriod.SECOND_START, DutyPeriod.SECOND_END);
            case 15:
                return new DutyTimeRange(DutyPeriod.AFTERNOON_START, DutyPeriod.AFTERNOON_END);
            case 17:
                return new DutyTimeRange(DutyPeriod.FOURTH_START, DutyPeriod.FOURTH_END);
            default:
                return null;
        }
    }

    /**
     * 处理指定时间段的值班考勤
     * 
     * @param timeRange 时间段
     * @param date 日期
     */
    private void processDutyAttendanceForPeriod(DutyTimeRange timeRange, LocalDate date) {
        try {
            // 构建完整的时间范围
            LocalDateTime periodStart = LocalDateTime.of(date, timeRange.startTime);
            LocalDateTime periodEnd = LocalDateTime.of(date, timeRange.endTime);

            // 查找这个时间段的值班考勤计划
                List<AttendancePlan> dutyPlans = attendancePlanMapper.findDutyPlanByTimeRange(periodStart, periodEnd);

            if (dutyPlans.isEmpty()) {
                log.info("时间段 {}-{} 没有值班考勤计划", timeRange.startTime, timeRange.endTime);
                return;
            }

                int processedCount = 0;
                for (AttendancePlan plan : dutyPlans) {
                    if (plan.getProcessed()) {
                    log.debug("考勤计划 {} 已处理，跳过", plan.getPlanId());
                        continue;
                }

                try {
                    processExpiredAttendancePlan(plan);
                    processedCount++;
                } catch (Exception e) {
                    log.error("处理值班考勤计划 {} 时发生异常", plan.getPlanId(), e);
                }
            }

            log.info("✅ 时间段 {}-{} 成功处理了 {} 个值班考勤计划", 
                    timeRange.startTime, timeRange.endTime, processedCount);

        } catch (Exception e) {
            log.error("处理时间段 {}-{} 的值班考勤时发生异常", timeRange.startTime, timeRange.endTime, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "处理值班考勤失败");
        }
    }

/**
     * 值班时间段配置类
     */
    private static class DutyTimeRange {
        final LocalTime startTime;
        final LocalTime endTime;

        public DutyTimeRange(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * 周期性报表生成任务
     * 
     * 执行时间：每周日凌晨3点
     * 功能说明：生成过去一周的考勤、任务等相关报表
     * 
     * TODO: 当前为预留接口，具体报表生成逻辑待实现
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional(rollbackFor = Exception.class)
public void generateWeeklyReport() {
        log.info("===== 开始生成周期性报表 =====");
        
        try {
            // 计算报表时间范围（过去一周）
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(6);

            log.info("报表生成时间范围：{} 至 {}", startDate, endDate);

            // TODO: 实现具体的报表生成逻辑
            // 1. 考勤报表
            // 2. 任务完成报表  
            // 3. 活跃度报表
            // 4. 生成PDF文件
            // 5. 发送通知

            log.info("✅ 周期性报表生成成功");

        } catch (Exception e) {
            log.error("❌ 生成周期性报表时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "报表生成失败");
        }
        
        log.info("===== 周期性报表生成任务完毕 =====");
    }

    /**
     * 数据备份任务
     * 
     * 执行时间：每周日凌晨5点
     * 功能说明：备份重要的业务数据，防止数据丢失
     * 
     * TODO: 当前为预留接口，具体备份逻辑待实现
 */
@Scheduled(cron = "0 0 5 ? * SUN")
    @Transactional(rollbackFor = Exception.class)
public void backupData() {
        log.info("===== 开始数据备份任务 =====");
        
        try {
            // 计算备份时间范围
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
            
            log.info("开始备份数据，本周起始日期: {}", startOfWeek);

            // TODO: 实现具体的数据备份逻辑
            // 1. 备份考勤数据
            // 2. 备份任务数据
            // 3. 备份用户数据
            // 4. 备份系统配置
            // 5. 压缩并存储到指定位置

            log.info("✅ 数据备份成功");

        } catch (Exception e) {
            log.error("❌ 数据备份时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "数据备份失败");
        }
        
        log.info("===== 数据备份任务完毕 =====");
    }

    /**
     * 值班安排提醒任务
     * 
     * 执行时间：每晚22:00
     * 功能说明：提醒明天有值班安排的相关人员
 */
@Scheduled(cron = "0 0 22 ? * *")
    @Transactional(rollbackFor = Exception.class)
public void remindDutyPlan() {
        log.info("===== 开始值班安排提醒任务 =====");
        
    try {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

            // 查询明天的值班计划
        List<AttendancePlan> dutyPlans = attendancePlanMapper.findDutyPlanByDate(tomorrow);

            if (dutyPlans.isEmpty()) {
                log.info("明天({})没有值班安排", tomorrow);
                return;
            }

            log.info("明天({})有 {} 个值班安排，开始发送提醒", tomorrow, dutyPlans.size());

        int notificationCount = 0;
        for (AttendancePlan plan : dutyPlans) {
            try {
                    sendDutyReminder(plan, tomorrow);
                    notificationCount++;
                } catch (Exception e) {
                    log.error("为值班计划 {} 发送提醒失败", plan.getPlanId(), e);
                }
            }

            log.info("✅ 值班提醒发送完成，共创建了 {} 条通知", notificationCount);

        } catch (Exception e) {
            log.error("❌ 值班安排提醒任务失败", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "值班提醒任务失败");
        }
        
        log.info("===== 值班安排提醒任务完毕 =====");
    }

    /**
     * 发送值班提醒通知
     * 
     * @param plan 值班计划
     * @param date 值班日期
     */
    private void sendDutyReminder(AttendancePlan plan, LocalDate date) {
        try {
            // 参数校验
            if (plan == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "值班计划不能为空");
            }

                String title = "明日值班提醒: " + plan.getName();
            String content = buildDutyReminderContent(plan);

            // 创建系统通知
                NotificationUtils.createSystemNotification(
                    title,
                    content,
                    null, // 全局通知
                    NOTIFICATION_IMPORTANT
            );

            log.info("成功发送值班提醒：{} - {}", date, plan.getName());

        } catch (BusinessException e) {
            throw e;
            } catch (Exception e) {
            log.error("发送值班提醒时发生异常", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "发送值班提醒失败");
        }
    }

    /**
     * 构建值班提醒内容
     * 
     * @param plan 值班计划
     * @return 提醒内容
     */
    private String buildDutyReminderContent(AttendancePlan plan) {
        return String.format(
                "明天有值班安排: %s\n" +
                "开始时间: %s\n" +
                "结束时间: %s\n" +
                "地点: %s\n" +
                "请准时到岗并记得签到",
                plan.getName(),
                plan.getStartTime().toString(),
                plan.getEndTime().toString(),
                plan.getLocation()
        );
    }

    /**
     * 更新逾期任务状态任务
     * 
     * 执行时间：每天凌晨1点
     * 功能说明：将所有未完成且已过截止时间的任务状态更新为"OVERDUE"
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateOverdueTasks() {
        log.info("===== 开始更新逾期任务状态任务 =====");
        
        try {
            // 更新逾期任务状态
            taskMapper.updateOverdueTasksStatus();
            log.info("✅ 成功更新逾期任务状态");

            // 发送系统通知
            sendOverdueTaskNotification();

        } catch (Exception e) {
            log.error("❌ 更新逾期任务状态时发生错误", e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "更新逾期任务状态失败");
        }
        
        log.info("===== 更新逾期任务状态任务完毕 =====");
    }

    /**
     * 发送逾期任务通知
     */
    private void sendOverdueTaskNotification() {
        try {
                String title = "系统任务状态更新";
                String content = "系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。";
                
                NotificationUtils.createSystemNotification(
                    title,
                    content,
                    null, // 全局通知
                    NOTIFICATION_NORMAL
            );

            log.info("成功创建逾期任务系统通知");

        } catch (Exception e) {
            log.error("创建逾期任务通知时发生错误", e);
            // 不抛出异常，通知失败不应影响主任务
        }
    }
}