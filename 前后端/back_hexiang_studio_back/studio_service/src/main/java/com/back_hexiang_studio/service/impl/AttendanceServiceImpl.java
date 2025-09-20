package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.dv.dto.AttendancePlanDto;
import com.back_hexiang_studio.dv.dto.AttendanceQueryDto;
import com.back_hexiang_studio.entity.AttendancePlan;
import com.back_hexiang_studio.entity.AttendanceRecord;
import com.back_hexiang_studio.entity.AttendanceStatistics;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.entity.Course;
import com.back_hexiang_studio.enumeration.AttendanceStatus;
import com.back_hexiang_studio.enumeration.AttendanceStatusTransition;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.mapper.*;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.service.AttendanceService;
import com.back_hexiang_studio.service.ActivityReservationService;
import com.back_hexiang_studio.utils.LocationVerificationService;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.back_hexiang_studio.context.UserContextHolder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 考勤服务实现类
 */
@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendancePlanMapper attendancePlanMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceStatisticsMapper attendanceStatisticsMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private LocationVerificationService locationVerificationService;

    @Autowired
    CommonLocationMapper commonLocationMapper;

    @Autowired
    private DutyScheduleStudentMapper dutyScheduleStudentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityReservationService activityReservationService;

    @Autowired
    private ActivityReservationMapper activityReservationMapper;

    /**
     * 创建基础的考勤计划
     * 根据类型创建不同的考勤计划：课程考勤、活动考勤、值班考勤
     */
    @Override
    @Transactional
    public Map<String, Object> createAttendancePlan(AttendancePlanDto planDto) {
        // 处理常用地点信息
        processLocationInfo(planDto);

        // 根据类型分发到不同的处理方法
        switch (planDto.getType()) {
            case "course":
                return createCourseAttendancePlan(planDto);
            case "activity":
                return createActivityAttendancePlan(planDto);
            case "duty":
                return createDutyAttendancePlan(planDto);
            default:
                throw new BusinessException("不支持的考勤类型");
        }
    }

    /**
     * 处理常用地点信息
     * 如果使用常用地点，则从常用地点获取坐标信息
     */
    private void processLocationInfo(AttendancePlanDto planDto) {
        // 如果提供了常用地点ID，则查询常用地点信息并填充到DTO中
        if (planDto.getCommonLocationId() != null) {
            try {
                Map<String, Double> commonLocationInfo = commonLocationMapper.getCommonLocationsById(planDto.getCommonLocationId());

                if (commonLocationInfo != null && commonLocationInfo.containsKey("latitude") && commonLocationInfo.containsKey("longitude")) {
                    // 设置位置坐标
                    planDto.setLocationLat(commonLocationInfo.get("latitude"));
                    planDto.setLocationLng(commonLocationInfo.get("longitude"));

                    // 如果前端没有填写位置名称但有常用地点，可以从常用地点获取名称
                    if ((planDto.getLocation() == null || planDto.getLocation().isEmpty()) && commonLocationInfo.containsKey("name")) {
                        planDto.setLocation(String.valueOf(commonLocationInfo.get("name")));
                    }
                } else {
                    log.warn("未找到常用地点信息或坐标数据不完整，ID={}", planDto.getCommonLocationId());
                }
            } catch (Exception e) {
                log.error("获取常用地点信息失败", e);
            }
        }

        // 确保必要的坐标信息存在
        if (planDto.getLocationLat() == null) {
            planDto.setLocationLat(0.0);
        }
        if (planDto.getLocationLng() == null) {
            planDto.setLocationLng(0.0);
        }

        // 确保必要的半径信息存在
        if (planDto.getRadius() == null) {
            planDto.setRadius(100); // 默认100米
        }

        // 确保位置名称不为空
        if (planDto.getLocation() == null || planDto.getLocation().isEmpty()) {
            planDto.setLocation("未指定位置");
        }
    }

    /**
     * 创建课程考勤计划
     * 特点：只有选修该课程的学生才能签到
     */
    private Map<String, Object> createCourseAttendancePlan(AttendancePlanDto planDto) {
        // 验证课程ID
        if (planDto.getCourseId() == null) {
            throw new BusinessException("课程考勤必须指定课程ID");
        }

        // 验证课程是否存在
        Course course = null;
        try {
            course = courseMapper.selectById(planDto.getCourseId());
        } catch (Exception e) {
            log.error("查询课程时发生异常: courseId={}, error={}", planDto.getCourseId(), e.getMessage(), e);
            throw new BusinessException("查询课程信息失败: " + e.getMessage());
        }

        if (course == null) {
            log.warn("课程不存在: courseId={}", planDto.getCourseId());
            throw new BusinessException("课程不存在，请确认课程ID");
        }

        log.info("查询到课程: {}", course);

        // 创建考勤计划
        AttendancePlan plan = new AttendancePlan();
        BeanUtils.copyProperties(planDto, plan);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        // 设置默认状态为有效(1)
        plan.setStatus(1);

        // 设置创建者ID，从UserContextHolder获取或使用默认值
        Long currentUserId = UserContextHolder.getCurrentId();
        plan.setCreateUser(currentUserId != null ? currentUserId : 1L);  // 默认使用ID为1的用户

        // 保存考勤计划
        attendancePlanMapper.insert(plan);

        // 返回创建结果
        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getPlanId());
        result.put("message", "课程考勤计划创建成功");
        return result;
    }

    /**
     * 创建活动考勤计划
     * 特点：只有预约的学生才能参加
     */
    private Map<String, Object> createActivityAttendancePlan(AttendancePlanDto planDto) {
        // 验证活动时间
        LocalDateTime now = LocalDateTime.now();
        if (planDto.getStartTime() != null && planDto.getStartTime().isBefore(now)) {
            throw new BusinessException("活动开始时间不能是过去时间，请选择未来时间");
        }

        if (planDto.getEndTime() != null && planDto.getStartTime() != null
                && planDto.getEndTime().isBefore(planDto.getStartTime())) {
            throw new BusinessException("活动结束时间必须晚于开始时间");
        }

        // 创建考勤计划实体
        AttendancePlan plan = new AttendancePlan();
        BeanUtils.copyProperties(planDto, plan);
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        // 设置默认状态为有效(1)
        plan.setStatus(1);

        // 设置创建者ID，从UserContextHolder获取或使用默认值
        Long currentUserId = UserContextHolder.getCurrentId();
        plan.setCreateUser(currentUserId != null ? currentUserId : 1L);  // 默认使用ID为1的用户

        // 保存考勤计划
        attendancePlanMapper.insert(plan);

        // 返回创建结果
        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getPlanId());
        result.put("message", "活动考勤计划创建成功");
        return result;
    }

    /**
     * 创建值班考勤计划
     * 特点：只有被安排值班的学生才能签到
     */
    private Map<String, Object> createDutyAttendancePlan(AttendancePlanDto planDto) {
        // 创建考勤计划实体
        AttendancePlan plan = new AttendancePlan();
        BeanUtils.copyProperties(planDto, plan);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        // 设置默认状态为有效(1)
        plan.setStatus(1);

        // 设置创建者ID，从UserContextHolder获取或使用默认值
        Long currentUserId = UserContextHolder.getCurrentId();
        plan.setCreateUser(currentUserId != null ? currentUserId : 1L);  // 默认使用ID为1的用户

        // 保存考勤计划
        attendancePlanMapper.insert(plan);

        // 如果关联了值班安排ID，则为值班学生创建初始考勤记录
        if (planDto.getScheduleId() != null) {
            // 查询值班安排下的所有学生
            List<Map<String, Object>> students = dutyScheduleStudentMapper.selectStudentsByScheduleId(planDto.getScheduleId());

            for(Map<String, Object> student : students){
                // 创建初始考勤记录
                AttendanceRecord record = new AttendanceRecord();
                record.setPlanId(plan.getPlanId());
                record.setStudentId((Long)student.get("studentId"));
                record.setStatus(AttendanceStatus.pending); //先设为代签到
                record.setCreateTime(LocalDateTime.now());
                record.setUpdateTime(LocalDateTime.now());
                record.setLocation("");
                record.setLocationLat(0.0);
                record.setLocationLng(0.0);
                record.setRemark("");
                attendanceRecordMapper.insert(record);
            }
        }
        // 返回创建结果
        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getPlanId());
        result.put("message", "值班考勤计划创建成功");
        return result;
    }

    /**
     * 为活动预约的学生生成考勤记录
     */
    @Override
    @Transactional
    public Map<String, Object> generateAttendanceRecordsForReservedStudents(Long planId) {
        try {
            // 验证考勤计划是否存在且为活动类型
            AttendancePlan plan = attendancePlanMapper.selectById(planId);
            if (plan == null) {
                throw new BusinessException("考勤计划不存在");
            }

            if (!"activity".equals(plan.getType())) {
                throw new BusinessException("只能为活动考勤生成预约学生记录");
            }

            // 获取已预约的学生列表
            List<Long> reservedStudentIds = activityReservationService.getReservedStudentIds(planId);
            if (reservedStudentIds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "暂无预约学生");
                result.put("generatedCount", 0);
                return result;
            }

            int generatedCount = 0;
            for (Long studentId : reservedStudentIds) {
                // 检查是否已存在考勤记录
                AttendanceRecord existingRecord = attendanceRecordMapper.findByPlanIdAndStudentId(planId, studentId);
                if (existingRecord == null) {
                    // 创建初始考勤记录
                    AttendanceRecord record = new AttendanceRecord();
                    record.setPlanId(planId);
                    record.setStudentId(studentId);
                    record.setStatus(AttendanceStatus.pending); // 待签到
                    record.setCreateTime(LocalDateTime.now());
                    record.setUpdateTime(LocalDateTime.now());
                    record.setLocation("");
                    record.setLocationLat(0.0);
                    record.setLocationLng(0.0);
                    record.setRemark("");

                    attendanceRecordMapper.insert(record);
                    generatedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "成功为" + generatedCount + "名预约学生生成考勤记录");
            result.put("generatedCount", generatedCount);
            result.put("totalReserved", reservedStudentIds.size());
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("为预约学生生成考勤记录失败: planId={}, error={}", planId, e.getMessage(), e);
            throw new BusinessException("生成考勤记录失败: " + e.getMessage());
        }
    }

    /**
     * 学生签到
     * 根据考勤类型执行不同的签到逻辑
     */
    @Override
    @Transactional
    public Map<String, Object> studentCheckIn(Long planId, Long userId, Double latitude, Double longitude, String location) {
        // 根据userId获取studentId
        Long studentId = studentMapper.getStudentIdByUserId(userId);
        if (studentId == null) {
            throw new BusinessException("学生信息不存在");
        }

        // 获取考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }

        // 验证考勤时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(plan.getStartTime())) {
            throw new BusinessException("考勤尚未开始");
        }
        if (now.isAfter(plan.getEndTime())) {
            throw new BusinessException("考勤已结束");
        }

        // 验证位置
        boolean locationValid = locationVerificationService.isLocationValid(
                latitude, longitude, plan.getLocationLat(), plan.getLocationLng(), plan.getRadius());
        if (!locationValid) {
            throw new BusinessException("不在有效签到范围内");
        }

        // 根据考勤类型执行不同的签到逻辑
        switch (plan.getType()) {
            case "course":
                return processCourseCheckIn(plan, studentId, latitude, longitude, location, now);
            case "activity":
                return processActivityCheckIn(plan, studentId, latitude, longitude, location, now);
            case "duty":
                return processDutyCheckIn(plan, studentId, latitude, longitude, location, now);
            default:
                throw new BusinessException("不支持的考勤类型");
        }
    }

    /**
     * 处理课程考勤签到
     * 验证学生是否选修该课程
     */
    private Map<String, Object> processCourseCheckIn(AttendancePlan plan, Long studentId, Double latitude, Double longitude, String location, LocalDateTime now) {
        // 验证学生是否选修该课程
        /*
         * 1. 调用courseMapper.isStudentEnrolled(studentId, plan.getCourseId())
         * 2. 如果学生未选修该课程，抛出BusinessException
         */
        if(!(courseMapper.isSrudentEnrolled(studentId, plan.getCourseId()))){
            throw new BusinessException("学生未选修该课程");
        }

        return isAttendance(plan,studentId,  latitude, longitude, location, now);
    }

    /**
     * 处理活动考勤签到
     * 验证学生是否已预约该活动
     */
    private Map<String, Object> processActivityCheckIn(AttendancePlan plan, Long studentId, Double latitude, Double longitude, String location, LocalDateTime now) {
        // 检查学生是否已预约该活动
        boolean isReserved = activityReservationService.isStudentReserved(plan.getPlanId(), studentId);
        if (!isReserved) {
            throw new BusinessException("您未预约此活动，无法签到");
        }

        // 检查是否已签到
        AttendanceRecord existingRecord = attendanceRecordMapper.findByPlanIdAndStudentId(plan.getPlanId(), studentId);
        if (existingRecord != null && !existingRecord.getStatus().equals("absent")) {
            throw new BusinessException("您已完成签到");
        }

        // 执行签到逻辑
        Map<String, Object> result = isAttendance(plan, studentId, latitude, longitude, location, now);

        // 如果签到成功，更新预约状态为"已签到"
        if (result != null && result.get("recordId") != null) {
            try {
                boolean updateSuccess = activityReservationService.updateReservationStatus(plan.getPlanId(), studentId, "checked_in");
                if (updateSuccess) {
                    log.info("学生签到成功，已更新预约状态：计划ID={}, 学生ID={}", plan.getPlanId(), studentId);
                } else {
                    log.warn("学生签到成功，但更新预约状态失败：计划ID={}, 学生ID={}", plan.getPlanId(), studentId);
                }
            } catch (Exception e) {
                log.error("更新预约状态时发生异常：计划ID={}, 学生ID={}", plan.getPlanId(), studentId, e);
            }
        }

        return result;
    }

    /**
     * 处理值班考勤签到
     * 验证学生是否在值班名单中
     */
    private Map<String, Object> processDutyCheckIn(AttendancePlan plan, Long studentId, Double latitude, Double longitude, String location, LocalDateTime now) {
        if (plan.getScheduleId() == null) {
            throw new BusinessException("关联排班不存在");
        }

        // 根据关联id返回学生列表
        List<Map<String, Object>> dutyStudents = dutyScheduleStudentMapper.selectStudentsByScheduleId(plan.getScheduleId());

        // 正确判断学生是否在值班名单内
        boolean isStudentInDuty = dutyStudents.stream()
                .anyMatch(student -> studentId.equals(student.get("studentId")));

        if (!isStudentInDuty) {
            throw new BusinessException("该学生不在值班名单内");
        }

        return isAttendance(plan, studentId, latitude, longitude, location, now);
    }

    /**
     * 考勤签到判断
     */
    private Map<String, Object> isAttendance(AttendancePlan plan, Long studentId, Double latitude, Double longitude, String location, LocalDateTime now) {
        // 🔧 修复：使用悲观锁查找现有的考勤记录，防止并发冲突
        AttendanceRecord existingRecord = attendanceRecordMapper.findByPlanIdAndStudentIdForUpdate(plan.getPlanId(), studentId);

        // 如果记录存在且状态不是"pending"或"absent"，说明已经签过到了
        if (existingRecord != null && !existingRecord.getStatus().equals(AttendanceStatus.pending) && !existingRecord.getStatus().equals(AttendanceStatus.absent)) {
            // 🔧 修复：提供更详细的错误信息
            String currentStatus = existingRecord.getStatus().name();
            String message = getStatusMessage(existingRecord.getStatus());
            throw new BusinessException("无法重复签到，当前状态：" + message);
        }

        // 决定是更新现有记录还是创建新记录
        AttendanceRecord recordToSave = (existingRecord != null) ? existingRecord : new AttendanceRecord();
        if (recordToSave.getRecordId() == null) { // This is a new record
            recordToSave.setPlanId(plan.getPlanId());
            recordToSave.setStudentId(studentId);
            recordToSave.setCreateTime(LocalDateTime.now());
        }

        // 根据考勤类型设置迟到时间限制
        int checkInLimit;
        switch (plan.getType()) {
            case "duty":
            case "daily":
                checkInLimit = 20;
                break;
            case "activity":
                checkInLimit = 20;
                break;
            case "course":
                checkInLimit = 15;
                break;
            default:
                checkInLimit = 15;
        }

        // 如果考勤总时长过短，则调整迟到时间限制
        long totalMinutes = Duration.between(plan.getStartTime(), plan.getEndTime()).toMinutes();
        if (totalMinutes < checkInLimit * 2) {
            checkInLimit = (int) (totalMinutes / 2);
        }

        // 判断是否迟到
        AttendanceStatus status = now.isAfter(plan.getStartTime().plusMinutes(checkInLimit))
                ? AttendanceStatus.late
                : AttendanceStatus.present;
        recordToSave.setStatus(status);
        recordToSave.setSignInTime(now);
        recordToSave.setLocation(location);
        recordToSave.setLocationLat(latitude);
        recordToSave.setLocationLng(longitude);
        recordToSave.setUpdateTime(LocalDateTime.now());

        // 保存或更新签到记录
        if (recordToSave.getRecordId() != null) {
            attendanceRecordMapper.update(recordToSave);
        } else {
            attendanceRecordMapper.insert(recordToSave);
        }

        // 返回签到结果
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", recordToSave.getRecordId());
        result.put("status", status.name());
        result.put("message", status == AttendanceStatus.present ? "签到成功" : "签到成功，但您已迟到");
        return result;
    }

    /**
     * 获取状态对应的中文说明
     */
    private String getStatusMessage(AttendanceStatus status) {
        switch (status) {
            case pending: return "待签到";
            case present: return "已签到";
            case late: return "迟到";
            case absent: return "缺勤";
            case leave: return "请假";
            default: return "未知状态";
        }
    }

    /**
     * 更新考勤统计数据，随着签到记录进行更新
     */
    @Override
    public void updateAttendanceStatistics(String type, LocalDate date) {
        // 查询当前条件的统计数据
        AttendanceStatistics statistics = attendanceStatisticsMapper.selectByTypeAndDate(type, date);

        // 如果不存在则创建新的统计数据
        if (statistics == null) {
            statistics = new AttendanceStatistics();
            statistics.setType(type);
            statistics.setDate(date);
            statistics.setTotalCount(0);
            statistics.setPresentCount(0);
            statistics.setLateCount(0);
            statistics.setAbsentCount(0);
            statistics.setLeaveCount(0);
            statistics.setCreateTime(LocalDateTime.now());
        }

        // 查询最新的统计数据
        Map<String, Integer> counts = attendanceRecordMapper.countByTypeAndStatus(type, date);

        // 更新统计数据
        // 人员总数、签到人数、迟到人数、缺勤人数、请假人数
        // 安全转换Long到Integer
        statistics.setTotalCount(safeLongToInt(counts.getOrDefault("total", 0)));
        statistics.setPresentCount(safeLongToInt(counts.getOrDefault("present", 0)));
        statistics.setLateCount(safeLongToInt(counts.getOrDefault("late", 0)));
        statistics.setAbsentCount(safeLongToInt(counts.getOrDefault("absent", 0)));
        statistics.setLeaveCount(safeLongToInt(counts.getOrDefault("leave_count", 0)));
        statistics.setUpdateTime(LocalDateTime.now());

        // 保存或更新统计数据
        if (statistics.getId() == null) {
            attendanceStatisticsMapper.insert(statistics);
        } else {
            attendanceStatisticsMapper.update(statistics);
        }
    }

    /**
     * 安全地将Long转换为Integer
     */
    private int safeLongToInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Long) {
            Long longValue = (Long) value;
            if (longValue > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return longValue.intValue();
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 生成考勤统计数据
     * 根据指定日期（年月日）生成各类型考勤的统计数据
     */
    @Override

    @Transactional
    public void generateAttendanceStatistics(LocalDate date) {
        for(String type: new String[]{"course", "activity", "duty"}) {
            //查询指定日期的所有类型的考勤记录
            //得到这一天的范围
            LocalDateTime startDateTime = date.atStartOfDay();
            LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

            // 统计各状态的记录数量
            Map<String, Integer> counts = attendanceRecordMapper.countByTypeAndStatus(type, date);

            //通过状态生成统计数据,根据类型和日期查询统计数据
            AttendanceStatistics statistics = attendanceStatisticsMapper.selectByTypeAndDate(type, date);
            //如果为空，就重置
            if (statistics == null) {
                statistics = new AttendanceStatistics();
                statistics.setType(type);
                statistics.setDate(date);
                statistics.setTotalCount(0);
                statistics.setCreateTime(LocalDateTime.now());
            }
            //设置统计数量信息
            // 更新统计数据，从count里面取值，为空就为0
            statistics.setTotalCount(safeLongToInt(counts.getOrDefault("total", 0)));
            statistics.setPresentCount(safeLongToInt(counts.getOrDefault("present", 0)));
            statistics.setLateCount(safeLongToInt(counts.getOrDefault("late", 0)));
            statistics.setAbsentCount(safeLongToInt(counts.getOrDefault("absent", 0)));
            statistics.setLeaveCount(safeLongToInt(counts.getOrDefault("leave_count", 0)));
            statistics.setUpdateTime(LocalDateTime.now());

            // 根据统计记录id保存或更新统计数据
            if (statistics.getId() == null) {
                attendanceStatisticsMapper.insert(statistics);
            } else {
                attendanceStatisticsMapper.update(statistics);
            }
        }
    }

    /**
     * 获取考勤计划列表
     * 根据查询条件分页查询考勤计划
     */
    @Override
    public PageResult getAttendancePlanList(AttendanceQueryDto queryDto) {
        // 准备查询参数
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("type", queryDto.getType());
        queryParams.put("keyword", queryDto.getKeyword());
        queryParams.put("status", queryDto.getStatus());
        queryParams.put("startDate", queryDto.getStartDate());
        queryParams.put("endDate", queryDto.getEndDate());
        queryParams.put("courseName", queryDto.getCourseName());

        params.put("params", queryParams);

        // 使用PageHelper进行分页
        PageHelper.startPage(queryDto.getPage(), queryDto.getPageSize());

        // 执行查询 (这里调用的是 AttendancePlanMapper 的 selectByPage)
        List<Map<String, Object>> list = attendancePlanMapper.selectByPage(params);

        // 为每条记录添加格式化的统计数据
        for (Map<String, Object> plan : list) {
            Map<String, Object> recordStats = new HashMap<>();
            // 从数据库返回的结果中获取统计数据，并处理null值
            recordStats.put("total", plan.get("totalStudents") != null ? plan.get("totalStudents") : 0);
            recordStats.put("presentCount", plan.get("presentCount") != null ? plan.get("presentCount") : 0);
            recordStats.put("lateCount", plan.get("lateCount") != null ? plan.get("lateCount") : 0);
            recordStats.put("absentCount", plan.get("absentCount") != null ? plan.get("absentCount") : 0);
            recordStats.put("leaveCount", plan.get("leaveCount") != null ? plan.get("leaveCount") : 0);

            plan.put("recordStats", recordStats);
        }

        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 获取考勤计划详情
     * 包括考勤计划信息和考勤记录统计
     */
    @Override
    public Map<String, Object> getAttendancePlanDetail(Long planId) {
        log.info("查询考勤计划详情，planId: {}", planId);

        // 查询考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            log.error("考勤计划不存在，planId: {}", planId);
            throw new BusinessException("考勤计划不存在");
        }

        log.info("查询到考勤计划，planId: {}, name: {}, type: {}",
                plan.getPlanId(), plan.getName(), plan.getType());

        // 准备结果
        Map<String, Object> result = new HashMap<>();

        // 确保位置坐标信息完整
        if (plan.getLocationLat() == null) {
            plan.setLocationLat(0.0);
        }
        if (plan.getLocationLng() == null) {
            plan.setLocationLng(0.0);
        }
        if (plan.getRadius() == null) {
            plan.setRadius(0);
        }

        // 将考勤计划信息转换为Map格式
        Map<String, Object> planInfo = new HashMap<>();
        planInfo.put("id", plan.getPlanId());
        planInfo.put("name", plan.getName());
        planInfo.put("type", plan.getType());
        planInfo.put("startTime", plan.getStartTime());
        planInfo.put("endTime", plan.getEndTime());
        planInfo.put("location", plan.getLocation());
        planInfo.put("locationLat", plan.getLocationLat());
        planInfo.put("locationLng", plan.getLocationLng());
        planInfo.put("radius", plan.getRadius());
        planInfo.put("status", plan.getStatus());
        planInfo.put("createUser", plan.getCreateUser());
        planInfo.put("note", plan.getNote());
        planInfo.put("courseId", plan.getCourseId());

        result.put("name", planInfo.get("name"));
        result.put("type", planInfo.get("type"));
        result.put("startTime", planInfo.get("startTime"));
        result.put("endTime", planInfo.get("endTime"));
        result.put("location", planInfo.get("location"));
        result.put("locationLat", planInfo.get("locationLat"));
        result.put("locationLng", planInfo.get("locationLng"));
        result.put("radius", planInfo.get("radius"));
        result.put("status", planInfo.get("status"));
        result.put("createUser", planInfo.get("createUser"));

        // 获取创建者姓名
        Long createUserId = (Long) planInfo.get("createUser");
        if (createUserId != null) {
            String createUserName = userMapper.getRealNameById(createUserId);
            result.put("createUserName", createUserName);
        }

        result.put("note", planInfo.get("note"));
        result.put("courseId", planInfo.get("courseId"));

        // 调用新方法一次性获取所有统计数据
        Map<String, Object> recordStats = attendanceRecordMapper.getStatisticsForPlan(planId);
        // 如果没有任何考勤记录，getStatisticsForPlan可能会返回null或包含null值的map，这里处理一下
        if (recordStats == null) {
            recordStats = new HashMap<>();
        }
        recordStats.putIfAbsent("total", 0L);
        recordStats.putIfAbsent("presentCount", 0L);
        recordStats.putIfAbsent("lateCount", 0L);
        recordStats.putIfAbsent("absentCount", 0L);
        recordStats.putIfAbsent("leaveCount", 0L);

        result.put("recordStats", recordStats);

        // 使用专门的方法，只查询当前计划的考勤记录
        List<Map<String, Object>> records = attendanceRecordMapper.selectRecordsByPlanId(planId);

        log.info("=== 考勤记录查询结果 ===");
        log.info("planId: {}, 查询到记录数量: {}", planId, records.size());
        for (Map<String, Object> record : records) {
            log.info("记录详情: recordId={}, planId={}, studentId={}, status={}",
                    record.get("record_id"), record.get("plan_id"),
                    record.get("student_id"), record.get("status"));
        }

        // 如果是课程考勤，需要特殊处理：只显示选择了该课程的学生信息
        if ("course".equals(plan.getType()) && plan.getCourseId() != null) {
            log.info("课程考勤特殊处理开始: courseId={}", plan.getCourseId());

            // 获取选择了该课程的所有学生
            List<Map<String, Object>> courseStudents = studentMapper.selectStudentsByCourseId(plan.getCourseId());
            log.info("选修课程的学生数量: {}", courseStudents.size());
            for (Map<String, Object> student : courseStudents) {
                log.info("选修学生: studentId={}, studentName={}",
                        student.get("student_id"), student.get("student_name"));
            }

            // 创建学生ID到学生信息的映射
            Map<Long, Map<String, Object>> studentMap = new HashMap<>();
            for (Map<String, Object> student : courseStudents) {
                studentMap.put((Long) student.get("student_id"), student);
            }

            // 过滤签到记录，只保留选择了该课程的学生
            List<Map<String, Object>> filteredRecords = new ArrayList<>();
            log.info("开始过滤考勤记录...");
            for (Map<String, Object> record : records) {
                Long studentId = (Long) record.get("student_id");
                if (studentMap.containsKey(studentId)) {
                    // 确保学生姓名正确
                    record.put("student_name", studentMap.get(studentId).get("student_name"));
                    filteredRecords.add(record);
                } else {
                    log.warn("学生{}不在选修名单中，过滤掉记录", studentId);
                }
            }
            log.info("过滤后的记录数量: {}", filteredRecords.size());

            // 为没有签到记录的学生创建默认记录
            log.info("为未签到学生创建默认记录...");
            for (Map<String, Object> student : courseStudents) {
                Long studentId = (Long) student.get("student_id");
                boolean hasRecord = false;

                for (Map<String, Object> record : filteredRecords) {
                    if (studentId.equals(record.get("student_id"))) {
                        hasRecord = true;
                        break;
                    }
                }

                if (!hasRecord) {
                    log.info("为学生{}创建默认缺勤记录", studentId);
                    // 创建默认的缺勤记录
                    Map<String, Object> defaultRecord = new HashMap<>();
                    defaultRecord.put("record_id", null);
                    defaultRecord.put("plan_id", planId);
                    defaultRecord.put("student_id", studentId);
                    defaultRecord.put("student_name", student.get("student_name"));
                    defaultRecord.put("status", "absent");
                    defaultRecord.put("sign_in_time", null);
                    defaultRecord.put("location", "");
                    defaultRecord.put("remark", "未签到");
                    filteredRecords.add(defaultRecord);
                } else {
                    log.info("学生{}已有签到记录，跳过", studentId);
                }
            }

            log.info("最终记录数量: {}", filteredRecords.size());
            records = filteredRecords;
        }

        // 🔧 修复：统一三种考勤类型的数据结构，前端期望 records.records 格式
        Map<String, Object> recordsWrapper = new HashMap<>();
        recordsWrapper.put("records", records);
        recordsWrapper.put("total", records.size());
        recordsWrapper.put("statistics", recordStats);

        result.put("records", recordsWrapper);

        return result;
    }

    /**
     * 更新考勤计划
     * 修改考勤计划信息
     */
    @Override
    @Transactional
    @AutoFill(value = OperationType.UPDATE)
    public boolean updateAttendancePlan(AttendancePlanDto planDto) {
        // 查询考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planDto.getPlanId());
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }

        // 处理常用地点信息
        processLocationInfo(planDto);

        // 更新基本信息
        BeanUtils.copyProperties(planDto, plan);

        // 如果更新成功（影响的行数大于 0 行）就返回 true，否则返回 false。
        return attendancePlanMapper.update(plan) > 0;
    }

    @Override
    @Transactional
    public boolean updateAttendancePlanStatus(Long planId, boolean status) {
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }
        plan.setStatus(status ? 1 : 0); // 将布尔值转换为整数
        plan.setUpdateTime(LocalDateTime.now());
        return attendancePlanMapper.update(plan) > 0;
    }

    /**
     * 删除考勤计划
     * 同时删除关联的考勤记录和活动预约记录
     */
    @Override
    @Transactional
    public boolean deleteAttendancePlan(Long planId) {
        // 查询考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }

        // 先删除关联的活动预约记录
        int deletedReservations = activityReservationMapper.deleteByPlanId(planId);
        log.info("删除考勤计划 {} 的活动预约记录数: {}", planId, deletedReservations);

        // 删除关联的考勤记录
        int deletedRecords = attendanceRecordMapper.deleteByPlanId(planId);
        log.info("删除考勤计划 {} 的考勤记录数: {}", planId, deletedRecords);

        // 删除考勤计划
        boolean result = attendancePlanMapper.deleteById(planId) > 0;
        log.info("删除考勤计划 {} 结果: {}", planId, result ? "成功" : "失败");

        return result;
    }

    /**
     * 获取考勤记录列表
     * 根据查询条件分页查询考勤记录
     */
    @Override
    public PageResult getAttendanceRecordList(AttendanceQueryDto queryDto) {
        // 准备查询参数 - 按照XML中的结构传递
        Map<String, Object> queryParams = new HashMap<>();

        // 确保planId参数类型正确
        if (queryDto.getPlanId() != null) {
            Long planId = queryDto.getPlanId();
            queryParams.put("planId", planId);
            log.info("设置查询参数planId: {} (类型: {})", planId, planId.getClass().getSimpleName());
        }

        queryParams.put("studentName", queryDto.getStudentName());
        queryParams.put("status", queryDto.getStatus());
        queryParams.put("type", queryDto.getType());
        queryParams.put("keyword", queryDto.getKeyword());

        // 🔧 学生端查询：支持按学生ID过滤
        if (queryDto.getStudentId() != null) {
            queryParams.put("studentId", queryDto.getStudentId());
            log.info("设置学生ID过滤参数: {}", queryDto.getStudentId());
        }

        // 处理日期参数
        queryParams.put("startDate", queryDto.getStartDate());
        queryParams.put("endDate", queryDto.getEndDate());

        // 添加调试日志
        log.info("查询考勤记录，参数: {}", queryParams);

        // 使用PageHelper进行分页
        PageHelper.startPage(queryDto.getPage(), queryDto.getPageSize());

        // 按照XML中的结构传递参数，将查询参数包装在params键下
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("params", queryParams);

        log.info("发送给Mapper的完整参数: {}", paramMap);

        // 在查询前再次验证参数
        Object planIdParam = paramMap.get("params") != null ?
                ((Map<String, Object>)paramMap.get("params")).get("planId") : null;
        log.info("最终传递给MyBatis的planId参数: {} (是否为null: {})",
                planIdParam, planIdParam == null);

        List<Map<String, Object>> list = attendanceRecordMapper.selectByPage(paramMap);

        // 添加详细的调试日志
        log.info("查询到考勤记录数量: {}", list.size());
        for (Map<String, Object> record : list) {
            log.info("记录详情 - record_id: {}, plan_id: {}, student_name: {}, plan_name: {}, plan_type: {}",
                    record.get("record_id"), record.get("plan_id"),
                    record.get("student_name"), record.get("plan_name"), record.get("plan_type"));
        }

        // 处理记录信息，确保数据完整性
        for (Map<String, Object> record : list) {
            // 处理位置坐标信息
            if (record.get("location_lat") == null && record.get("plan_location_lat") != null) {
                record.put("location_lat", record.get("plan_location_lat"));
            } else if (record.get("location_lat") == null) {
                record.put("location_lat", 0.0);
            }
            if (record.get("location_lng") == null && record.get("plan_location_lng") != null) {
                record.put("location_lng", record.get("plan_location_lng"));
            } else if (record.get("location_lng") == null) {
                record.put("location_lng", 0.0);
            }
            if (record.get("radius") == null && record.get("plan_radius") != null) {
                record.put("radius", record.get("plan_radius"));
            } else if (record.get("radius") == null) {
                record.put("radius", 0);
            }

            // 处理学生信息，确保学生姓名和学号的完整性
            Object studentId = record.get("student_id");
            Object studentName = record.get("student_name");

            // 如果没有学生姓名，尝试通过studentId查询或设置默认值
            if (studentName == null || studentName.toString().trim().isEmpty()) {
                if (studentId != null) {
                    // 可以在这里添加通过studentId查询学生信息的逻辑
                    // 暂时使用默认格式
                    record.put("student_name", "学生" + studentId);
                } else {
                    record.put("student_name", "未知学生");
                }
            }

            // 确保student_number字段存在
            if (record.get("student_number") == null) {
                record.put("student_number", "");
            }

            // 添加调试日志，用于排查数据问题
            log.debug("处理考勤记录: studentId={}, studentName={}, studentNumber={}",
                    record.get("student_id"), record.get("student_name"), record.get("student_number"));
        }

        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 获取学生考勤统计
     * 统计学生在不同类型考勤中的出勤情况
     */
    @Override
    public Map<String, Object> getStudentAttendanceStatistics(Long studentId) {
        // 查询学生是否存在
        if(studentMapper.selectById(studentId) == null){
            throw new BusinessException("该学生不存在");
        }

        // 查询学生考勤统计数据
        Map<String, Object> statistics = attendanceRecordMapper.getStudentStatistics(studentId);

        // 如果没有数据则初始化
        if (statistics == null) {
            statistics = new HashMap<>();
            statistics.put("studentId", studentId);
            statistics.put("totalCount", 0);
            statistics.put("presentCount", 0);
            statistics.put("lateCount", 0);
            statistics.put("absentCount", 0);
            statistics.put("leaveCount", 0);

            // 课程考勤数据
            Map<String, Integer> courseStats = new HashMap<>();
            courseStats.put("totalCount", 0);
            courseStats.put("presentCount", 0);
            courseStats.put("lateCount", 0);
            courseStats.put("absentCount", 0);
            courseStats.put("leaveCount", 0);
            statistics.put("courseStatistics", courseStats);

            //活动考勤数据
            Map<String, Integer> activityStats = new HashMap<>();
            activityStats.put("totalCount", 0);
            activityStats.put("presentCount", 0);
            activityStats.put("lateCount", 0);
            activityStats.put("absentCount", 0);
            activityStats.put("leaveCount", 0);
            statistics.put("activityStatistics", activityStats);

            //值班考勤数据
            Map<String, Integer> dutyStats = new HashMap<>();
            dutyStats.put("totalCount", 0);
            dutyStats.put("presentCount", 0);
            dutyStats.put("lateCount", 0);
            dutyStats.put("absentCount", 0);
            dutyStats.put("leaveCount", 0);
            statistics.put("dutyStatistics", dutyStats);
        }

        return statistics;
    }

    /**
     * 获取课程考勤统计
     * 统计课程的考勤情况
     */
    @Override
    public Map<String, Object> getCourseAttendanceStatistics(Long courseId) {
        // 查询课程是否存在
        if(!courseMapper.selectByIdNoNull(courseId)){
            throw new BusinessException("课程不存在");
        }

        // 查询课程考勤统计数据
        Map<String, Object> statistics = attendanceRecordMapper.getCourseStatistics(courseId);

        // 如果没有数据则初始化
        if (statistics == null) {
            statistics = new HashMap<>();
            statistics.put("courseId", courseId);
            statistics.put("totalCount", 0);
            statistics.put("presentCount", 0);
            statistics.put("lateCount", 0);
            statistics.put("absentCount", 0);
            statistics.put("leaveCount", 0);
        }

        return statistics;
    }

    /**
     * 为值班安排创建考勤记录
     * @param planId 考勤计划ID
     * @param studentIds 学生ID列表
     * @return 创建结果
     */
    @Override
    @Transactional
    public Map<String, Object> createAttendanceRecordsForDuty(Long planId, List<Long> studentIds) {
        // 查询考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }

        // 查询已存在的考勤记录，获取已有学生ID
        List<AttendanceRecord> existingRecords = attendanceRecordMapper.selectByPlanId(planId);
        Set<Long> existingStudentIds = new HashSet<>();
        for (AttendanceRecord record : existingRecords) {
            existingStudentIds.add(record.getStudentId());
        }

        // 过滤出需要新增考勤记录的学生
        List<Long> newStudentIds = new ArrayList<>();
        for (Long studentId : studentIds) {
            if (!existingStudentIds.contains(studentId)) {
                newStudentIds.add(studentId);
            }
        }

        if (newStudentIds.isEmpty()) {
            log.info("所有学生都已有考勤记录，无需创建新记录");
            Map<String, Object> result = new HashMap<>();
            result.put("planId", planId);
            result.put("createdCount", 0);
            return result;
        }

        // 创建考勤记录
        List<AttendanceRecord> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long studentId : newStudentIds) {
            AttendanceRecord record = new AttendanceRecord();
            record.setPlanId(planId);
            record.setStudentId(studentId);
            record.setStatus(AttendanceStatus.pending); // 使用pending状态，表示待签到
            record.setCreateTime(now);
            records.add(record);
        }

        // 批量插入记录
        attendanceRecordMapper.batchInsert(records);

        // 更新统计数据
        updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());

        Map<String, Object> result = new HashMap<>();
        result.put("planId", planId);
        result.put("createdCount", records.size());
        return result;
    }

    /**
     * 获取总体统计数据
     * @param queryDto 查询参数
     * @return 统计数据
     */
    @Override
    public Map<String, Object> getOverallStatistics(AttendanceQueryDto queryDto) {
        try {
            log.info("获取总体统计数据: {}", queryDto);
            Map<String, Object> result = attendanceStatisticsMapper.getOverallStatistics(queryDto);

            // 如果结果为null，则返回空的Map
            if (result == null) {
                log.warn("总体统计数据为空");
                result = new HashMap<>();
                // 设置默认值
                result.put("total", 0);
                result.put("present", 0);
                result.put("late", 0);
                result.put("absent", 0);
                result.put("leave", 0);
            }

            log.info("总体统计数据结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取总体统计数据发生异常", e);
            throw new BusinessException("获取总体统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取考勤趋势数据
     * @param queryDto 查询参数
     * @return 趋势数据
     */
    @Override
    public List<Map<String, Object>> getAttendanceTrends(AttendanceQueryDto queryDto) {
        try {
            log.info("获取考勤趋势数据: {}", queryDto);
            List<Map<String, Object>> result = attendanceStatisticsMapper.getAttendanceTrends(queryDto);

            // 如果结果为null，则返回空的List
            if (result == null) {
                log.warn("考勤趋势数据为空");
                result = new ArrayList<>();
            }

            log.info("考勤趋势数据结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取考勤趋势数据发生异常", e);
            throw new BusinessException("获取考勤趋势数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取考勤类型分布数据
     * @param queryDto 查询参数
     * @return 类型分布数据
     */
    @Override
    public List<Map<String, Object>> getAttendanceTypeDistribution(AttendanceQueryDto queryDto) {
        try {
            log.info("获取考勤类型分布数据: {}", queryDto);
            List<Map<String, Object>> result = attendanceStatisticsMapper.getAttendanceTypeDistribution(queryDto);

            // 如果结果为null，则返回空的List
            if (result == null) {
                log.warn("考勤类型分布数据为空");
                result = new ArrayList<>();
            }

            log.info("考勤类型分布数据结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("获取考勤类型分布数据发生异常", e);
            throw new BusinessException("获取考勤类型分布数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取学生考勤统计列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult getStudentAttendanceStatisticsList(AttendanceQueryDto queryDto) {
        try {
            log.info("获取学生考勤统计列表: {}", queryDto);
            // 设置分页
            PageHelper.startPage(queryDto.getPage(), queryDto.getPageSize());

            // 准备查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("type", queryDto.getType());
            params.put("startDate", queryDto.getStartDate());
            params.put("endDate", queryDto.getEndDate());

            // 查询学生统计列表
            List<Map<String, Object>> list = attendanceRecordMapper.getStudentStatisticsList(params);

            // 处理为空的情况
            if (list == null) {
                log.warn("学生统计列表为空");
                list = new ArrayList<>();
            }

            // 处理分页结果
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

            log.info("学生考勤统计列表结果: 总数={}, 当前页数据数={}", pageInfo.getTotal(), list.size());
            return new PageResult(pageInfo.getTotal(), pageInfo.getList());
        } catch (Exception e) {
            log.error("获取学生考勤统计列表发生异常", e);
            throw new BusinessException("获取学生考勤统计列表失败: " + e.getMessage());
        }
    }

    @Override
    public void updateAttendanceStatus(Long recordId, String status, String remark) {
        if (!AttendanceStatus.isValid(status)) {
            throw new BusinessException("无效的考勤状态");
        }
        AttendanceRecord record = attendanceRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("考勤记录不存在");
        }
        attendanceRecordMapper.updateStatusAndRemark(recordId, AttendanceStatus.valueOf(status), remark);
    }

    @Override
    @Transactional
    public boolean deleteAttendanceRecord(Long recordId) {
        // 验证考勤记录是否存在
        AttendanceRecord record = attendanceRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("考勤记录不存在");
        }

        // 删除考勤记录
        int result = attendanceRecordMapper.deleteById(recordId);

        if (result > 0) {
            log.info("成功删除考勤记录: {}", recordId);
            return true;
        } else {
            log.warn("删除考勤记录失败: {}", recordId);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getCurrentUserAvailablePlans(Long userId) {
        try {
            log.info("获取用户可参与的考勤计划列表: userId={}", userId);

            // 查询所有有效的考勤计划
            AttendanceQueryDto queryDto = new AttendanceQueryDto();
            queryDto.setStatus("1"); // 只查询有效的
            queryDto.setPage(1);
            queryDto.setPageSize(100);

            PageResult planResult = getAttendancePlanList(queryDto);
            List<Map<String, Object>> allPlans = (List<Map<String, Object>>) planResult.getRecords();

            // 筛选出符合时间条件和用户权限的计划
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> availablePlans = new ArrayList<>();

            for (Map<String, Object> plan : allPlans) {
                try {
                    Object startTimeObj = plan.get("startTime");
                    Object endTimeObj = plan.get("endTime");
                    String type = (String) plan.get("type");
                    Long planId = (Long) plan.get("planId");

                    log.debug("处理考勤计划: planId={}, type={}, startTime={}, endTime={}",
                            planId, type, startTimeObj, endTimeObj);

                    if (startTimeObj != null && endTimeObj != null) {
                        LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                        LocalDateTime endTime = DateTimeUtils.parseDateTime(endTimeObj.toString());

                        if (startTime != null && endTime != null) {
                            // 🔧 修复：添加时间窗口检查，避免显示已结束的考勤
                            // 考勤开始前1天可以显示，考勤结束后就不再显示
                            LocalDateTime showTime = startTime.minusDays(1);

                            log.debug("时间判断: planId={}, type={}, startTime={}, endTime={}, now={}",
                                    planId, type, startTime, endTime, now);

                            // 🔧 恢复时间窗口限制：只显示未结束的考勤计划
                            if (now.isAfter(showTime) && now.isBefore(endTime)) {
                                log.info("考勤计划在有效时间窗口内: planId={}, type={}", planId, type);
                                // 检查用户权限
                                if (hasAttendancePermission(userId, plan)) {
                                    log.info("用户有权限参与考勤: userId={}, planId={}", userId, planId);

                                    // 🔧 修复：检查学生是否已签到，已签到的不显示在可请假列表
                                    Long studentId = studentMapper.getStudentIdByUserId(userId);
                                    if (studentId != null) {
                                        AttendanceRecord existingRecord = attendanceRecordMapper.findByPlanIdAndStudentId(planId, studentId);
                                        if (existingRecord != null &&
                                                (existingRecord.getStatus() == AttendanceStatus.present || existingRecord.getStatus() == AttendanceStatus.late)) {
                                            log.info("学生已签到，不显示在可请假列表: userId={}, planId={}, status={}",
                                                    userId, planId, existingRecord.getStatus());
                                            continue; // 跳过已签到的考勤计划
                                        }
                                    }

                                    // 添加额外信息
                                    plan.put("timeStatus", getTimeStatus(now, startTime, endTime));
                                    plan.put("canCheckIn", now.isAfter(startTime) && now.isBefore(endTime));
                                    availablePlans.add(plan);
                                } else {
                                    log.info("用户无权限参与考勤: userId={}, planId={}", userId, planId);
                                }
                            } else {
                                log.debug("考勤计划不在有效时间窗口内，已过滤: planId={}, now={}, showTime(开始前1天)={}, endTime={}",
                                        planId, now, showTime, endTime);
                            }
                        } else {
                            log.warn("时间解析失败: planId={}, startTimeObj={}, endTimeObj={}",
                                    planId, startTimeObj, endTimeObj);
                        }
                    } else {
                        log.warn("考勤计划缺少时间信息: planId={}, startTime={}, endTime={}",
                                planId, startTimeObj, endTimeObj);
                    }
                } catch (Exception e) {
                    log.warn("处理考勤计划失败: planId={}, error={}", plan.get("planId"), e.getMessage());
                    continue;
                }
            }

            // 按开始时间排序
            availablePlans.sort((p1, p2) -> {
                try {
                    LocalDateTime time1 = DateTimeUtils.parseDateTime(p1.get("startTime").toString());
                    LocalDateTime time2 = DateTimeUtils.parseDateTime(p2.get("startTime").toString());
                    return time1.compareTo(time2);
                } catch (Exception e) {
                    return 0;
                }
            });

            log.info("返回 {} 个可用考勤计划", availablePlans.size());
            return availablePlans;
        } catch (Exception e) {
            log.error("获取用户可参与的考勤计划列表失败: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException("获取考勤计划列表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getQuickSignPlan(Long userId) {
        try {
            log.info("获取快速签到计划: userId={}", userId);

            // 查询当前时间可用的考勤计划
            AttendanceQueryDto queryDto = new AttendanceQueryDto();
            queryDto.setStatus("1"); // 只查询有效的
            queryDto.setPage(1);
            queryDto.setPageSize(50);

            PageResult planResult = getAttendancePlanList(queryDto);
            List<Map<String, Object>> plans = (List<Map<String, Object>>) planResult.getRecords();

            // 筛选出当前时间范围内的计划
            LocalDateTime now = LocalDateTime.now();

            for (Map<String, Object> plan : plans) {
                try {
                    Object startTimeObj = plan.get("startTime");
                    Object endTimeObj = plan.get("endTime");

                    if (startTimeObj != null && endTimeObj != null) {
                        LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                        LocalDateTime endTime = DateTimeUtils.parseDateTime(endTimeObj.toString());

                        if (startTime != null && endTime != null) {
                            // 检查当前时间是否在考勤时间范围内
                            if (now.isAfter(startTime) && now.isBefore(endTime)) {
                                // 检查用户权限
                                if (hasAttendancePermission(userId, plan)) {
                                    return plan;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析考勤计划时间失败: {}", e.getMessage());
                    continue;
                }
            }

            return null; // 没有找到合适的签到计划
        } catch (Exception e) {
            log.error("获取快速签到计划失败: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException("获取快速签到计划失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否有参与考勤的权限
     */
    private boolean hasAttendancePermission(Long userId, Map<String, Object> plan) {
        try {
            String type = (String) plan.get("type");
            Long planId = (Long) plan.get("planId");

            log.info("检查用户考勤权限: userId={}, planId={}, type={}", userId, planId, type);

            // 🔧 管理员权限检查：管理员可以查看所有考勤计划
            User user = userMapper.getUserById(userId);
            if (user != null && user.getPositionId() != null) {
                Long positionId = user.getPositionId();
                // 超级管理员(8), 主任(6), 副主任(7) 拥有所有考勤权限
                if (positionId == 8L || positionId == 6L || positionId == 7L) {
                    log.info("管理员用户，拥有所有考勤权限: userId={}, positionId={}, planId={}", userId, positionId, planId);
                    return true;
                }
            }

            switch (type) {
                case "activity":
                    // 活动考勤：需要检查学生是否已预约
                    log.info("活动考勤权限检查: userId={}, planId={}", userId, planId);

                    // 先将userId转换为studentId
                    Long activityStudentId = studentMapper.getStudentIdByUserId(userId);
                    if (activityStudentId == null) {
                        log.warn("用户{}不是学生，无法参与活动考勤", userId);
                        return false;
                    }

                    // 检查学生是否已预约该活动
                    boolean isReserved = activityReservationService.isStudentReserved(planId, activityStudentId);
                    log.info("活动预约检查结果: studentId={}, planId={}, isReserved={}", activityStudentId, planId, isReserved);

                    if (!isReserved) {
                        log.info("学生未预约该活动，无法参与: studentId={}, planId={}", activityStudentId, planId);
                        return false;
                    }

                    log.info("学生已预约该活动，可以参与: studentId={}, planId={}", activityStudentId, planId);
                    return true;

                case "course":
                    // 课程考勤：需要检查是否选修了该课程
                    Object courseIdObj = plan.get("courseId");
                    log.info("课程考勤权限检查: userId={}, planId={}, courseId={}", userId, planId, courseIdObj);

                    if (courseIdObj != null) {
                        Long courseId = Long.valueOf(courseIdObj.toString());

                        // 🔧 修复：先将userId转换为studentId
                        Long studentId = studentMapper.getStudentIdByUserId(userId);
                        log.info("userId转换为studentId: userId={}, studentId={}", userId, studentId);

                        if (studentId == null) {
                            log.warn("用户{}不是学生，无法参与课程考勤", userId);
                            return false;
                        }

                        // 调用CourseMapper检查学生是否选修该课程
                        boolean enrolled = courseMapper.isSrudentEnrolled(studentId, courseId);
                        log.info("课程选修检查结果: studentId={}, courseId={}, enrolled={}", studentId, courseId, enrolled);
                        return enrolled;
                    }
                    log.warn("课程考勤缺少courseId: userId={}, planId={}", userId, planId);
                    return false;

                case "duty":
                    // 值班考勤：需要检查是否在值班名单中
                    Object scheduleIdObj = plan.get("scheduleId");
                    log.info("值班考勤权限检查: userId={}, planId={}, scheduleId={}", userId, planId, scheduleIdObj);

                    if (scheduleIdObj != null) {
                        Long scheduleId = Long.valueOf(scheduleIdObj.toString());

                        // 🔧 修复：先将userId转换为studentId
                        Long studentId = studentMapper.getStudentIdByUserId(userId);
                        log.info("userId转换为studentId: userId={}, studentId={}", userId, studentId);

                        if (studentId == null) {
                            log.warn("用户{}不是学生，无法参与值班考勤", userId);
                            return false;
                        }

                        // 检查用户是否在该值班安排中
                        List<Map<String, Object>> dutyStudents = dutyScheduleStudentMapper.selectStudentsByScheduleId(scheduleId);
                        boolean inDuty = dutyStudents.stream()
                                .anyMatch(student -> studentId.equals(student.get("studentId")));
                        log.info("值班安排检查结果: studentId={}, scheduleId={}, inDuty={}", studentId, scheduleId, inDuty);
                        return inDuty;
                    }
                    log.warn("值班考勤缺少scheduleId: userId={}, planId={}", userId, planId);
                    return false;

                default:
                    log.warn("不支持的考勤类型: type={}, userId={}, planId={}", type, userId, planId);
                    return false;
            }
        } catch (Exception e) {
            log.error("检查用户考勤权限失败: userId={}, planId={}, error={}",
                    userId, plan.get("planId"), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取考勤时间状态
     */
    private String getTimeStatus(LocalDateTime now, LocalDateTime startTime, LocalDateTime endTime) {
        if (now.isBefore(startTime)) {
            return "upcoming"; // 即将开始
        } else if (now.isAfter(startTime) && now.isBefore(endTime)) {
            return "active"; // 进行中
        } else {
            return "ended"; // 已结束
        }
    }

    @Override
    public Map<String, Object> getAttendanceStatus(Long planId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 根据userId获取studentId
            Long studentId = studentMapper.getStudentIdByUserId(userId);
            if (studentId == null) {
                result.put("checked", false);
                result.put("checkTime", "");
                result.put("message", "学生信息不存在");
                return result;
            }

            // 查询考勤记录
            AttendanceRecord record = attendanceRecordMapper.findByPlanIdAndStudentId(planId, studentId);

            if (record == null) {
                // 没有记录，未签到
                result.put("checked", false);
                result.put("checkTime", "");
                result.put("status", "pending");
            } else {
                // 有记录，检查状态
                String status = record.getStatus().name();
                boolean isChecked = !status.equals("pending") && !status.equals("absent");

                result.put("checked", isChecked);
                result.put("status", status);

                if (isChecked && record.getSignInTime() != null) {
                    // 格式化签到时间
                    LocalDateTime signInTime = record.getSignInTime();
                    String timeString = String.format("%02d:%02d",
                            signInTime.getHour(), signInTime.getMinute());
                    result.put("checkTime", timeString);
                } else {
                    result.put("checkTime", "");
                }
            }

        } catch (Exception e) {
            log.error("获取签到状态失败: {}", e.getMessage());
            result.put("checked", false);
            result.put("checkTime", "");
            result.put("message", "查询失败");
        }

        return result;
    }

    /**
     * 根据值班安排ID查找考勤计划ID
     */
    @Override
    public Long findPlanIdByScheduleId(Long scheduleId) {
        return attendancePlanMapper.findPlanIdByScheduleId(scheduleId);
    }

    /**
     * 同步考勤记录与值班安排（删除多余记录，添加缺失记录）
     */
    @Override
    @Transactional
    public Map<String, Object> syncAttendanceRecordsForDuty(Long planId, List<Long> currentStudentIds) {
        // 查询考勤计划
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("考勤计划不存在");
        }

        // 查询现有的所有考勤记录
        List<AttendanceRecord> existingRecords = attendanceRecordMapper.selectByPlanId(planId);
        Set<Long> existingStudentIds = new HashSet<>();
        Map<Long, AttendanceRecord> existingRecordMap = new HashMap<>();

        for (AttendanceRecord record : existingRecords) {
            existingStudentIds.add(record.getStudentId());
            existingRecordMap.put(record.getStudentId(), record);
        }

        Set<Long> currentStudentIdSet = new HashSet<>(currentStudentIds);

        // 1. 找出需要删除的考勤记录（不再值班的学生）
        List<Long> toDeleteStudentIds = new ArrayList<>();
        for (Long existingStudentId : existingStudentIds) {
            if (!currentStudentIdSet.contains(existingStudentId)) {
                toDeleteStudentIds.add(existingStudentId);
            }
        }

        // 2. 找出需要新增的考勤记录（新值班的学生）
        List<Long> toAddStudentIds = new ArrayList<>();
        for (Long currentStudentId : currentStudentIds) {
            if (!existingStudentIds.contains(currentStudentId)) {
                toAddStudentIds.add(currentStudentId);
            }
        }

        int deletedCount = 0;
        int addedCount = 0;

        // 3. 删除多余的考勤记录
        if (!toDeleteStudentIds.isEmpty()) {
            for (Long studentId : toDeleteStudentIds) {
                AttendanceRecord recordToDelete = existingRecordMap.get(studentId);
                if (recordToDelete != null) {
                    attendanceRecordMapper.deleteById(recordToDelete.getRecordId());
                    deletedCount++;
                    log.info("删除学生{}的考勤记录，记录ID: {}", studentId, recordToDelete.getRecordId());
                }
            }
        }

        // 4. 新增缺失的考勤记录
        if (!toAddStudentIds.isEmpty()) {
            List<AttendanceRecord> newRecords = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Long studentId : toAddStudentIds) {
                AttendanceRecord record = new AttendanceRecord();
                record.setPlanId(planId);
                record.setStudentId(studentId);
                record.setStatus(AttendanceStatus.pending);
                record.setCreateTime(now);
                record.setUpdateTime(now);
                record.setLocation("");
                record.setLocationLat(0.0);
                record.setLocationLng(0.0);
                record.setRemark("");
                newRecords.add(record);
            }

            if (!newRecords.isEmpty()) {
                attendanceRecordMapper.batchInsert(newRecords);
                addedCount = newRecords.size();
                log.info("为{}名新学生创建考勤记录", addedCount);
            }
        }

        // 5. 更新统计数据
        updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());

        // 6. 返回同步结果
        Map<String, Object> result = new HashMap<>();
        result.put("planId", planId);
        result.put("deletedCount", deletedCount);
        result.put("addedCount", addedCount);
        result.put("totalCurrentStudents", currentStudentIds.size());
        result.put("message", String.format("同步完成：删除%d条记录，新增%d条记录", deletedCount, addedCount));

        log.info("考勤记录同步完成：计划ID={}, 删除{}条, 新增{}条, 当前学生{}名",
                planId, deletedCount, addedCount, currentStudentIds.size());

        return result;
    }

    /**
     * 处理请假审批通过后的考勤记录更新
     * @param studentId 学生ID
     * @param planId 考勤计划ID
     * @param approverId 审批人ID
     */
    @Transactional
    public void handleApprovedLeaveRequest(Long studentId, Long planId, Long approverId) {
        log.info("处理审批通过的请假申请：学生ID={}, 考勤计划ID={}, 审批人ID={}", studentId, planId, approverId);

        // 1. 验证考勤计划是否存在
        AttendancePlan plan = attendancePlanMapper.selectById(planId);
        if (plan == null) {
            log.warn("考勤计划不存在，跳过处理：planId={}", planId);
            return;
        }

        // 2. 检查是否已有考勤记录
        List<AttendanceRecord> existingRecords = attendanceRecordMapper.getByPlanIdAndStudentId(planId, studentId);

        if (existingRecords.isEmpty()) {
            // 3. 如果没有考勤记录，创建一条"请假"状态的记录
            AttendanceRecord leaveRecord = new AttendanceRecord();
            leaveRecord.setPlanId(planId);
            leaveRecord.setStudentId(studentId);
            leaveRecord.setStatus(AttendanceStatus.leave); // "请假"
            leaveRecord.setSignInTime(null); // 请假不需要签到时间
            leaveRecord.setLocation("请假");
            leaveRecord.setRemark("请假申请审批通过");
            leaveRecord.setUpdateUser(approverId);
            leaveRecord.setCreateTime(LocalDateTime.now());
            leaveRecord.setUpdateTime(LocalDateTime.now());

            attendanceRecordMapper.insert(leaveRecord);
            log.info("创建请假考勤记录：学生ID={}, 考勤计划ID={}", studentId, planId);
        } else {
            // 4. 检查现有记录状态，决定是否允许转换为请假状态
            AttendanceRecord existingRecord = existingRecords.get(0);
            AttendanceStatus currentStatus = existingRecord.getStatus();

            // 🔧 修复：使用状态转换规则检查
            if (!AttendanceStatusTransition.isTransitionAllowed(currentStatus, AttendanceStatus.leave)) {
                String transitionDesc = AttendanceStatusTransition.getTransitionDescription(currentStatus, AttendanceStatus.leave);
                log.warn("不允许的状态转换：{} -> leave，学生ID={}, 考勤计划ID={}",
                        currentStatus, studentId, planId);
                throw new BusinessException(String.format("当前状态（%s）无法转换为请假状态：%s",
                        getStatusMessage(currentStatus), transitionDesc));
            }

            // 执行允许的状态转换
            if (currentStatus == AttendanceStatus.pending || currentStatus == AttendanceStatus.absent) {
                attendanceRecordMapper.updateStatusToLeaveByPlanId(studentId, planId, approverId);
                log.info("更新考勤记录为请假状态：学生ID={}, 考勤计划ID={}, 状态转换: {} -> leave",
                        studentId, planId, currentStatus);
            } else if (currentStatus == AttendanceStatus.leave) {
                log.info("考勤记录已是请假状态，无需更新：学生ID={}, 考勤计划ID={}", studentId, planId);
            }
        }

        // 5. 更新考勤统计
        updateAttendanceStatistics(plan.getType(), plan.getStartTime().toLocalDate());
    }

}