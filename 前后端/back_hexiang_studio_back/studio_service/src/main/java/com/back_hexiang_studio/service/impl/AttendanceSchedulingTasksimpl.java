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
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.LoaderClassPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import com.back_hexiang_studio.entity.DutySchedule;
import com.back_hexiang_studio.entity.DutyScheduleStudent;
import java.util.ArrayList;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.back_hexiang_studio.mapper.UserMapper;

@Slf4j
@Service
public class AttendanceSchedulingTasksimpl implements AttendanceSchedulingTasks {

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
     * 全能的每日值班调度任务
     * 每天凌晨2点执行，负责两大职责：
     * 1. 自动生成下周排班：确保排班表永不“断档”。
     * 2. 激活考勤计划：为即将在48小时内开始的排班创建考勤，确保可以签到。
     */
    // 🔧 测试阶段暂时禁用自动生成，改为手动操作
    // @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void dailyDutyScheduleOrchestrator() {
        log.info("===== 开始执行每日值班调度任务 =====");

        // 职责一：检查并自动生成下周排班
        try {
            log.info("--- [职责一] 检查是否需要生成下周排班 ---");
            LocalDate nextWeekMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
            long scheduleCount = dutyScheduleMapper.countByDateRange(
                    nextWeekMonday.atStartOfDay(), nextWeekMonday.plusDays(7).atStartOfDay());

            if (scheduleCount == 0) {
                log.info("下周 ({}) 尚无排班，开始自动生成...", nextWeekMonday);
                LocalDate thisWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
                dutyScheduleService.generateNextWeekDutySchedules(thisWeekMonday);
                log.info("✅ 成功生成下周排班。");
            } else {
                log.info("下周已有排班，无需自动生成。");
            }
        } catch (Exception e) {
            log.error("❌ [职责一] 自动生成下周排班时发生错误。", e);
        }

        // 职责二：激活即将开始的考勤计划
        try {
            log.info("--- [职责二] 检查并激活即将开始的考勤计划 ---");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime checkHorizon = now.plusHours(48); // 检查未来48小时

            List<DutySchedule> schedulesToActivate = dutyScheduleMapper.findSchedulesWithoutPlans(now, checkHorizon);

            if (schedulesToActivate.isEmpty()) {
                log.info("没有发现需要在48小时内激活的考勤计划。");
            } else {
                log.info("发现 {} 个待激活的排班，开始处理...", schedulesToActivate.size());
                for (DutySchedule schedule : schedulesToActivate) {
                    // 创建考勤计划
                    AttendancePlan plan = new AttendancePlan();
                    plan.setType("duty");
                    plan.setName(schedule.getTitle() + " - 值班考勤");
                    plan.setStartTime(schedule.getStartTime());
                    plan.setEndTime(schedule.getEndTime());
                    plan.setLocation(schedule.getLocation());
                    plan.setLocationLat(29.553017);
                    plan.setLocationLng(106.237538);
                    plan.setRadius(100);
                    plan.setScheduleId(schedule.getScheduleId());
                    plan.setCreateUser(1L);
                    plan.setCreateTime(LocalDateTime.now());
                    plan.setUpdateTime(LocalDateTime.now());
                    plan.setStatus(1);
                    plan.setProcessed(false);
                    attendancePlanMapper.insert(plan);

                    // 为学生创建考勤记录
                    List<DutyScheduleStudent> students = dutyScheduleStudentMapper.findByScheduleId(schedule.getScheduleId());
                    if (!students.isEmpty()) {
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
                        log.info("为排班ID {} 创建了考勤计划并为 {} 名学生创建了考勤记录。", schedule.getScheduleId(), students.size());
                    }
                }
                log.info("✅ 成功激活了 {} 个排班的考勤计划。", schedulesToActivate.size());
            }
        } catch (Exception e) {
            log.error("❌ [职责二] 激活考勤计划时发生错误。", e);
        }
        log.info("===== 每日值班调度任务执行完毕 =====");
    }

    /**
     * 考勤统计数据-定时任务（每天凌晨2点更新）
     *  cron表达式格式: 秒 分 时 日 月 周
     *      * "0 0 2 * * ?" 表示：
     *      *   - 0秒
     *      *   - 0分
     *      *   - 2点
     *      *   - 每天 (*)
     *      *   - 每月 (*)
     *      *   - 每周的任意一天 (?)
     */


    //不关心星期几
    @Scheduled(cron="0 0 2 * * ? ")
    @Transactional
    public void dailyAttendanceStatistics() {
        log.info("每日考勤统计数据更新开始");
        try{
            //获取前一天的日期
            LocalDate yesterday=LocalDate.now().minusDays(1);

            //用方法获取指定日期的考勤数据
            attendanceService.generateAttendanceStatistics(yesterday);

            // 🔧 优化：规范日志格式，避免过于随意的表述
            log.info("每日考勤统计数据更新完成，日期: {}", yesterday);
        }catch (Exception e){
            log.error("每日考勤数据更新失败", e);
        }
    }

    /**
     * 考勤状态进行更新 - 每5分钟执行
     * cron表达式格式: 秒 分 时 日 月 周
     * -秒 0
     * 分
     * 每小时 *
     * 每天 *
     * 每月 *
     * 每周的任意一天 ?
     */

    @Transactional
    @Scheduled(cron = "0 */5 * * * ?")
    public void updateAttendanceStatus(){
        // 🔧 优化：定时任务日志简化，避免过于随意的表述
        log.info("考勤状态定时更新开始");
        try{
            //逻辑： 先找到已经结束 但是没有处理的 考勤计划
            //1. 值班考勤的逻辑是：开始时间+签到的限制时间
            //2. 课程考勤的逻辑是：结束时间+签到的限制时间
            //3. 活动考的逻辑是： 结束时间+签到的限制时间
            List<AttendancePlan> exp= attendancePlanMapper.findExpiredUnprocessedCoursePlans();

            //记录缺席数量
            int processedCount = 0;
            //每种计划进行判断
            for(AttendancePlan plan:exp){
                //先找为 pending（未签到的记录）
                List<AttendanceRecord> pendingRecords=attendanceRecordMapper.findByPlanAndStatus(plan.getPlanId(), AttendanceStatus.pending);

                //更新为absent
                for(AttendanceRecord record:pendingRecords){
                    record.setStatus(AttendanceStatus.absent);
                    record.setUpdateTime(LocalDateTime.now());
                    //更新记录
                    attendanceRecordMapper.update(record);
                }
                //更新考勤计划的状态,变为已处理
                plan.setProcessed(true);
                attendancePlanMapper.update(plan);

                //并且再更新考勤数据(修改考勤信息和更新开始时间)
                attendanceService.updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());

            }

            if(processedCount>0){
                log.info("成功处理了{}条过期但未处理的考勤计划,几条记录{}", exp.size(),processedCount);

            }

        }catch (Exception e){
            log.error("考勤状态更新失败....");
        }
    }

    /**
     * 值班考勤特殊处理---每个值班时间段5分钟后 执行
     * 0 秒
     * 5 分
     * 10，10，12，15，17 时
     * *每天
     * *每月
     * ？不考虑周
      */

    @Scheduled(cron = "0 5 10,12,15,17 * * ?")
    @Transactional
    public void updateDutyAttendanceStatus() {
        //获取当前具体时间
        LocalDateTime now = LocalDateTime.now();
        //获取时间
        LocalTime crrentTime = now.toLocalTime();
        LocalTime startTime = null;
        LocalTime endTime = null;

        //先确定好时间段 8.30-10.00 10.20-11.50  14.00-15.30  15.50-17.20  18.30-20.00
        if (crrentTime.getHour() == 10) {
            //8.30-10.00
            startTime = LocalTime.of(8, 30);
            endTime = LocalTime.of(10, 0);
        } else if (crrentTime.getHour() == 12) {
            //10.20-11.50
            startTime = LocalTime.of(10, 20);
            endTime = LocalTime.of(11, 50);
        } else if (crrentTime.getHour() == 15) {
            //14.00-15.30
            startTime = LocalTime.of(14, 0);
            endTime = LocalTime.of(15, 30);
        } else if (crrentTime.getHour() == 17) {
            //15.50-17.20
            startTime = LocalTime.of(15, 50);
            endTime = LocalTime.of(17, 20);
        } else if (crrentTime.getHour() == 20) {
            //18.30-20.00
            startTime = LocalTime.of(18, 30);
            endTime = LocalTime.of(20, 0);
        }

        if (startTime != null && endTime != null) {
            log.info("开始处理{}-{}的考勤", startTime, endTime);
            try {

                //获取当前日期
                LocalDate today = LocalDate.now();
                //拼接时间
                LocalDateTime periodStart = LocalDateTime.of(today, startTime);
                LocalDateTime periodEnd = LocalDateTime.of(today, endTime);

                //去找这个时间段的值班考勤计划
                List<AttendancePlan> dutyPlans = attendancePlanMapper.findDutyPlanByTimeRange(periodStart, periodEnd);

                //判断是否已经处理过
                int processedCount = 0;
                for (AttendancePlan plan : dutyPlans) {
                    //判断是否已经处理过
                    if (plan.getProcessed()) {
                        //已经处理过
                        continue;
                    }
                    //找状态为pending的考勤记录
                    List<AttendanceRecord> pendingRecords = attendanceRecordMapper.findByPlanAndStatus(plan.getPlanId(), AttendanceStatus.pending);

                    //时间段还没处理的记录,更新为absent
                    for (AttendanceRecord record : pendingRecords) {
                        record.setStatus(AttendanceStatus.absent);
                        record.setUpdateTime(LocalDateTime.now());
                        attendanceRecordMapper.update(record);
                        //处理数量+1
                        processedCount++;
                    }

                    //更新计划状态为已处理
                    plan.setProcessed(true);
                    //更新考勤计划
                    attendancePlanMapper.update(plan);

                    //更新统计信息
                    attendanceService.updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());
                }

                if (processedCount > 0 || dutyPlans.isEmpty()) {
                    log.info("值班时间段{}-{}的考勤记录处理完成,处理了{}个计划,{}个记录", periodStart, periodEnd, dutyPlans.size(), processedCount);

                }

            } catch (Exception e) {

                log.error("处理值班考勤记录时发生异常", e);

            }
        }
    }

/**
 * 定时通知提醒 ---10分钟一次
 * 提醒同学即将开始考勤
 * cron
 * 0 0/10 * * * ?
 *
 */
@Scheduled(cron = "0 0/10 * * * ?")
public void sendUpcomingAttendanceReminders() {
    log.debug("开始检查即将开始的考勤计划");
    try{
        //获取即将开始考勤的计划（30分钟内的）
        //获取当前时间
        LocalDateTime now = LocalDateTime.now();
        //获取30分钟后的时间
        LocalDateTime thirtyMinutesLater=now.plusMinutes(30);

        //开始查找
        List<AttendancePlan> upcomingPlans=attendancePlanMapper.findUpcomingAttendancePlans(now,thirtyMinutesLater);

        // 创建通知
        int notificationCount = 0;
        for(AttendancePlan plan : upcomingPlans) {
            try {
                // 创建考勤提醒通知 - 全局通知
                String title = "考勤即将开始: " + plan.getName();
                String content = "考勤即将开始: " + plan.getName() + 
                    "\n开始时间: " + plan.getStartTime().toString() + 
                    "\n结束时间: " + plan.getEndTime().toString() + 
                    "\n地点: " + plan.getLocation();
                
                NotificationUtils.createSystemNotification(
                    title,
                    content,
                    null, // null表示全局通知
                    1     // 重要程度: 1表示重要
                );
                
                notificationCount++;
                // 🔧 优化：降级为DEBUG，减少日志噪音
                log.debug("发送考勤提醒通知: {}", plan.getName());
            } catch (Exception e) {
                log.error("为考勤计划 {} 创建通知时发生错误: {}", plan.getPlanId(), e.getMessage());
            }
        }

        // 🔧 优化：只有发送了通知才记录INFO，否则降级为DEBUG
        if (notificationCount > 0) {
            log.info("考勤提醒通知发送完成，共创建{}条通知", notificationCount);
        } else {
            log.debug("无即将开始的考勤计划，未发送通知");
        }
    } catch (Exception e) {
        log.error("发送即将开始考勤的提醒时发生异常", e);
    }
}


/**
 * 生成周期性报表 ， 每周一凌晨4点执行(报表形式)
 * cron 0 0 4 ? * MON
 * 在每日统计之后执行，使用最新的统计数据
 */
@Scheduled(cron = "0 0 4 ? * MON")
public void generateWeeklyReport() {
    log.info("开始生成周期性报表...");
    try{
        //计算时间范围
        //获取昨天日期
        LocalDate endDate=LocalDate.now().minusDays(1);
        //获取一周前日期
        LocalDate startDate=endDate.minusDays(6);

        //TODO: 生成报表（先不写，暂留）
        //生成报表(PDF)

        log.info("周期性报表生成成功");
    }catch (Exception e){
        log.error("生成周期性报表时发生异常", e);
    }
}

/**
 * 数据备份 每周日凌晨5点（防止数据丢失）
 * cron 0 0 5 ? * SUN
 */
@Scheduled(cron = "0 0 5 ? * SUN")
public void backupData() {
    log.info("开始备份数据...");
    try{
        //计算本周日范围
        //获取今天日期
        LocalDate today=LocalDate.now();
        //获取本周日日期
        LocalDate starOfWeek=today.minusDays(today.getDayOfWeek().getValue()-1);
        log.info("开始备份数据，从{}开始的本周",starOfWeek);
        //TODO: 备份数据（先不写，暂留）
        //备份数据
        log.info("备份数据成功");


    }catch (Exception e){
        log.error("备份数据时发生异常", e);
    }
}


/**
 *值班安排提醒 晚上 10点
 * cron 0 0 22 ? * *
 */
@Scheduled(cron = "0 0 22 ? * *")
public void remindDutyPlan() {
    log.info("开始提醒值班安排...");
    try {
        //计算时间
        //今天明天时间
        LocalDate tomorrow = LocalDate.now().plusDays(1);

       //查询明天排班记录
        List<AttendancePlan> dutyPlans = attendancePlanMapper.findDutyPlanByDate(tomorrow);

        // 创建通知
        int notificationCount = 0;
        for (AttendancePlan plan : dutyPlans) {
            try {
                // 创建值班提醒通知 - 全局通知
                String title = "明日值班提醒: " + plan.getName();
                String content = "明天有值班安排: " + plan.getName() + 
                    "\n开始时间: " + plan.getStartTime().toString() + 
                    "\n结束时间: " + plan.getEndTime().toString() + 
                    "\n地点: " + plan.getLocation() + 
                    "\n请准时到岗并记得签到";
                
                NotificationUtils.createSystemNotification(
                    title,
                    content,
                    null, // null表示全局通知
                    1     // 重要程度: 1表示重要
                );
                
                notificationCount++;
                log.info("提醒值班安排，{}的值班安排是{}", tomorrow, plan.getName());
            } catch (Exception e) {
                log.error("为值班计划 {} 创建通知时发生错误: {}", plan.getPlanId(), e.getMessage());
       }
        }

        log.info("值班提醒通知已发送，共创建了{}条通知", notificationCount);
    } catch (Exception e) {
        log.error("提醒值班安排时发生异常", e);
    }
}

    /**
     * 更新逾期任务状态 - 每天凌晨1点执行
     * 将所有未完成且已过截止时间的任务状态更新为 "OVERDUE"
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void updateOverdueTasks() {
        log.info("===== 开始执行更新逾期任务状态的定时任务 =====");
        try {
            taskMapper.updateOverdueTasksStatus();
            log.info("✅ 成功更新逾期任务的状态。");
            
            // 为所有逾期任务创建一个全局通知
            try {
                // 创建任务逾期通知 - 全局通知
                String title = "系统任务状态更新";
                String content = "系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。";
                
                NotificationUtils.createSystemNotification(
                    title,
                    content,
                    null, // null表示全局通知
                    0     // 重要程度: 0表示普通
                );
                
                log.info("成功创建了逾期任务系统通知");
            } catch (Exception e) {
                log.error("为逾期任务创建通知时发生错误: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ 更新逾期任务状态时发生错误。", e);
        }
        log.info("===== 更新逾期任务状态的定时任务执行完毕 =====");
    }
}