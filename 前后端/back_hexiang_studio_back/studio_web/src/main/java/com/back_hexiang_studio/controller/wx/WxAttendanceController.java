package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.service.AttendanceService;
import com.back_hexiang_studio.service.CommonLocationService;
import com.back_hexiang_studio.dv.dto.AttendanceQueryDto;
import com.back_hexiang_studio.dv.dto.AttendancePlanDto;
import com.back_hexiang_studio.utils.DateTimeUtils;
import com.back_hexiang_studio.GlobalException.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.service.ActivityReservationService;
import com.back_hexiang_studio.dv.dto.ActivityReservationDto;

/**
 * 微信端考勤控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/attendance")
public class WxAttendanceController {

    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private CommonLocationService commonLocationService;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ActivityReservationService activityReservationService;

    /**
     * 获取模块统计
     * @return 模块统计数据
     */
    @GetMapping("/module-stats")
    public Result getModuleStats() {
        log.info("获取考勤模块统计");
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> moduleStats = new HashMap<>();

            // 🔧 统一权限逻辑：与getAttendancePlans保持一致
            User user = userMapper.getUserById(currentUserId);
            boolean isAdmin = user != null && user.getPositionId() != null && 
                            (user.getPositionId() == 8L || user.getPositionId() == 6L || user.getPositionId() == 7L);
            
            long dutyCount, activityCount, courseCount;
            
            if (isAdmin) {
                // 管理员：统计所有有效的考勤计划
                AttendanceQueryDto dutyQuery = new AttendanceQueryDto();
                dutyQuery.setType("duty");
                dutyQuery.setStatus("1");
                dutyQuery.setPage(1);
                dutyQuery.setPageSize(1000);
                PageResult dutyResult = attendanceService.getAttendancePlanList(dutyQuery);
                dutyCount = ((List<?>) dutyResult.getRecords()).size();
                
                AttendanceQueryDto activityQuery = new AttendanceQueryDto();
                activityQuery.setType("activity");
                activityQuery.setStatus("1");
                activityQuery.setPage(1);
                activityQuery.setPageSize(1000);
                PageResult activityResult = attendanceService.getAttendancePlanList(activityQuery);
                activityCount = ((List<?>) activityResult.getRecords()).size();
                
                AttendanceQueryDto courseQuery = new AttendanceQueryDto();
                courseQuery.setType("course");
                courseQuery.setStatus("1");
                courseQuery.setPage(1);
                courseQuery.setPageSize(1000);
                PageResult courseResult = attendanceService.getAttendancePlanList(courseQuery);
                courseCount = ((List<?>) courseResult.getRecords()).size();
                
                log.info("管理员考勤统计: 值班={}, 活动={}, 课程={}, 用户ID={}", 
                        dutyCount, activityCount, courseCount, currentUserId);
            } else {
                // 普通用户：使用权限过滤逻辑
                List<Map<String, Object>> userAvailablePlans = attendanceService.getCurrentUserAvailablePlans(currentUserId);
                
                dutyCount = userAvailablePlans.stream()
                        .filter(plan -> "duty".equals(plan.get("type")))
                        .count();
                
                activityCount = userAvailablePlans.stream()
                        .filter(plan -> "activity".equals(plan.get("type")))
                        .count();
                
                courseCount = userAvailablePlans.stream()
                        .filter(plan -> "course".equals(plan.get("type")))
                        .count();
                
                log.info("普通用户考勤统计: 值班={}, 活动={}, 课程={}, 用户ID={}", 
                        dutyCount, activityCount, courseCount, currentUserId);
            }

            moduleStats.put("dutyCount", dutyCount);
            moduleStats.put("activityCount", activityCount);
            moduleStats.put("courseCount", courseCount);

            return Result.success(moduleStats);
        } catch (Exception e) {
            log.error("获取模块统计失败: {}", e.getMessage());
            return Result.error("获取模块统计失败");
        }
    }

    /**
     * 获取快速签到计划
     * @return 当前可签到的计划
     */
    @GetMapping("/quick-sign-plan")
    public Result getQuickSignPlan() {
        log.info("获取快速签到计划");
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> quickPlan = attendanceService.getQuickSignPlan(currentUserId);
            return Result.success(quickPlan);
        } catch (Exception e) {
            log.error("获取快速签到计划失败: {}", e.getMessage());
            return Result.error("获取快速签到计划失败");
        }
    }

    /**
     * 获取当前用户可参与的考勤计划列表
     * @return 当前用户可签到的考勤计划列表
     */
    @GetMapping("/current-plans")
    public Result getCurrentPlans() {
        log.info("获取当前用户可参与的考勤计划列表");
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            List<Map<String, Object>> availablePlans = attendanceService.getCurrentUserAvailablePlans(currentUserId);
            return Result.success(availablePlans);
        } catch (Exception e) {
            log.error("获取当前用户可参与的考勤计划列表失败: {}", e.getMessage());
            return Result.error("获取考勤计划列表失败");
        }
    }

    /**
     * 获取考勤地点列表
     * @return 地点列表
     */
    @GetMapping("/locations")
    public Result getAttendanceLocations() {
        log.info("获取考勤地点列表");
        try {
            // TODO: 实现获取考勤地点列表的逻辑
            // 可能需要从attendance_plan表中获取不同的地点信息

            return Result.success("考勤地点列表功能待实现");
        } catch (Exception e) {
            log.error("获取考勤地点列表失败: {}", e.getMessage());
            return Result.error("获取考勤地点列表失败");
        }
    }

    /**
     * 获取学生可见的考勤计划列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/plans")
    public Result<PageResult> getAttendancePlans(AttendanceQueryDto queryDto) {
        log.info("获取考勤计划列表，查询参数：{}", queryDto);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 🔧 管理员权限检查：管理员可以查看所有考勤计划
            User user = userMapper.getUserById(currentUserId);
            boolean isAdmin = user != null && user.getPositionId() != null && 
                            (user.getPositionId() == 8L || user.getPositionId() == 6L || user.getPositionId() == 7L);
            
            log.info("用户权限检查: userId={}, positionId={}, isAdmin={}", 
                    currentUserId, user != null ? user.getPositionId() : null, isAdmin);
            
            if (isAdmin) {
                // 管理员直接返回所有符合条件的考勤计划
                queryDto.setStatus("1"); // 只查询有效的考勤计划
                PageResult allPlans = attendanceService.getAttendancePlanList(queryDto);
                log.info("管理员权限: 返回 {} 个考勤计划", 
                        ((List<?>) allPlans.getRecords()).size());
                return Result.success(allPlans);
            } else {
                // 非管理员使用原有的权限过滤逻辑
                queryDto.setStatus("1"); // 只查询有效的考勤计划
                PageResult allPlans = attendanceService.getAttendancePlanList(queryDto);
                
                // 获取原始计划列表
                List<Map<String, Object>> planList = (List<Map<String, Object>>) allPlans.getRecords();
                
                // 🚀 性能优化：一次性获取用户的所有可用计划，避免重复查询
                List<Map<String, Object>> userAvailablePlans = attendanceService.getCurrentUserAvailablePlans(currentUserId);
                Set<Long> availablePlanIds = userAvailablePlans.stream()
                        .map(plan -> (Long) plan.get("planId"))
                        .collect(Collectors.toSet());
                
                // 对每个计划进行权限过滤
                List<Map<String, Object>> filteredPlans = new ArrayList<>();
                for (Map<String, Object> plan : planList) {
                    Long planId = (Long) plan.get("planId");
                    String type = (String) plan.get("type");
                    
                    // 检查用户是否有权限查看此考勤
                    if (hasUserPermissionForPlan(planId, type, availablePlanIds)) {
                        filteredPlans.add(plan);
                    }
                }
                
                log.info("普通用户权限过滤结果: 原始计划数={}, 过滤后计划数={}, 用户ID={}", 
                        planList.size(), filteredPlans.size(), currentUserId);
                
                // 构造过滤后的分页结果
                PageResult filteredResult = new PageResult((long) filteredPlans.size(), filteredPlans);
                return Result.success(filteredResult);
            }
        } catch (Exception e) {
            log.error("获取考勤计划列表失败: {}", e.getMessage());
            return Result.error("获取考勤计划列表失败");
        }
    }

    /**
     * 获取考勤计划详情
     * @param planId 计划ID
     * @return 考勤计划
     */
    @GetMapping("/plan/{planId}")
    public Result getAttendancePlanDetail(@PathVariable Long planId) {
        log.info("获取考勤计划详情，计划ID：{}", planId);
        try {
            return Result.success(attendanceService.getAttendancePlanDetail(planId));
        } catch (Exception e) {
            log.error("获取考勤计划详情失败: {}", e.getMessage());
            return Result.error("获取考勤计划详情失败");
        }
    }

    /**
     * 考勤签到（统一签到接口）
     * @param checkInData 签到数据
     * @return 签到结果
     */
    @PostMapping("/check-in")
    public Result checkIn(@RequestBody Map<String, Object> checkInData) {
        log.info("考勤签到，签到数据: {}", checkInData);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 从请求数据中提取参数
            Object planIdObj = checkInData.get("planId");
            Object latitudeObj = checkInData.get("latitude");
            Object longitudeObj = checkInData.get("longitude");
            String location = (String) checkInData.get("location");

            if (planIdObj == null || latitudeObj == null || longitudeObj == null) {
                return Result.error("签到参数不完整");
            }

            Long planId = Long.valueOf(planIdObj.toString());
            Double latitude = Double.valueOf(latitudeObj.toString());
            Double longitude = Double.valueOf(longitudeObj.toString());

            // 调用服务进行签到
            Map<String, Object> result = attendanceService.studentCheckIn(planId, currentUserId, latitude, longitude, location);

            log.info("用户签到成功，用户ID: {}, 计划ID: {}", currentUserId, planId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("考勤签到失败: {}", e.getMessage());
            return Result.error("考勤签到失败");
        }
    }

    /**
     * 学生签到（兼容旧接口）
     * @param planId 考勤计划ID
     * @param data 签到数据
     * @return 签到结果
     */
    @PostMapping("/check-in/{planId}")
    public Result<Map<String, Object>> checkInByPlan(@PathVariable Long planId,
                                                     @RequestBody Map<String, Object> data) {
        log.info("学生签到，计划ID: {}, 签到数据: {}", planId, data);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Double latitude = Double.valueOf(data.get("latitude").toString());
            Double longitude = Double.valueOf(data.get("longitude").toString());
            String location = (String) data.get("location");

            Map<String, Object> result = attendanceService.studentCheckIn(planId, currentUserId, latitude, longitude, location);

            log.info("学生签到成功，用户ID: {}, 计划ID: {}", currentUserId, planId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("学生签到失败: {}", e.getMessage());
            return Result.error("学生签到失败");
        }
    }

    /**
     * 获取用户考勤统计
     * @return 考勤统计
     */
    @GetMapping("/statistics")
    public Result getAttendanceStatistics() {
        log.info("获取用户考勤统计");
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = new HashMap<>();

            // 🔧 修复权限控制：基于用户权限计算统计数据
            // 获取用户所有可见的考勤计划
            List<Map<String, Object>> userAvailablePlans = attendanceService.getCurrentUserAvailablePlans(currentUserId);
            
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalDate monthStart = today.withDayOfMonth(1); // 本月第一天

            // 计算今日考勤计划数（用户可见的）
            long todayAttendance = userAvailablePlans.stream()
                    .filter(plan -> {
                        try {
                            Object startTimeObj = plan.get("startTime");
                            if (startTimeObj != null) {
                                LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                                return startTime != null && startTime.toLocalDate().equals(today);
                            }
                            return false;
                        } catch (Exception e) {
                            log.warn("解析考勤计划时间失败: {}", e.getMessage());
                            return false;
                        }
                    })
                    .count();

            // 计算本月考勤计划数（用户可见的）
            long monthAttendance = userAvailablePlans.stream()
                    .filter(plan -> {
                        try {
                            Object startTimeObj = plan.get("startTime");
                            if (startTimeObj != null) {
                                LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                                if (startTime != null) {
                                    LocalDate planDate = startTime.toLocalDate();
                                    return !planDate.isBefore(monthStart) && !planDate.isAfter(today);
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            log.warn("解析考勤计划时间失败: {}", e.getMessage());
                            return false;
                        }
                    })
                    .count();

            // 计算当前可签到的考勤计划数（用户可见的，且在时间范围内）
            long pendingPlans = userAvailablePlans.stream()
                    .filter(plan -> {
                try {
                    Object startTimeObj = plan.get("startTime");
                    Object endTimeObj = plan.get("endTime");

                    if (startTimeObj != null && endTimeObj != null) {
                        LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                        LocalDateTime endTime = DateTimeUtils.parseDateTime(endTimeObj.toString());

                                return startTime != null && endTime != null &&
                                       now.isAfter(startTime) && now.isBefore(endTime);
                        }
                            return false;
                } catch (Exception e) {
                    log.warn("解析考勤计划时间失败: {}", e.getMessage());
                            return false;
                        }
                    })
                    .count();

            statistics.put("todayAttendance", todayAttendance);      // 今日考勤计划数
            statistics.put("monthAttendance", monthAttendance);      // 本月考勤计划数
            statistics.put("pendingPlans", pendingPlans);            // 当前可签到计划数

            log.info("用户考勤统计数据: 今日={}, 本月={}, 待签到={}, 用户ID={}",
                    todayAttendance, monthAttendance, pendingPlans, currentUserId);

            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取考勤统计失败: {}", e.getMessage());
            return Result.error("获取考勤统计失败");
        }
    }

    /**
     * 获取学生考勤统计（兼容旧接口）
     * @param studentId 学生ID
     * @return 统计数据
     */
    @GetMapping("/statistics/student/{studentId}")
    public Result getStudentAttendanceStatistics(@PathVariable Long studentId) {
        log.info("获取学生考勤统计，学生ID：{}", studentId);
        try {
            Map<String, Object> statistics = attendanceService.getStudentAttendanceStatistics(studentId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取学生考勤统计失败: {}", e.getMessage());
            return Result.error("获取学生考勤统计失败");
        }
    }

    /**
     * 获取当前值班信息
     * @return 当前值班信息
     */
    @GetMapping("/current-duty")
    public Result getCurrentDuty() {
        log.info("获取当前值班信息");
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 查询当前时间的值班计划
            AttendanceQueryDto queryDto = new AttendanceQueryDto();
            queryDto.setType("duty");
            queryDto.setStatus("1"); // 只查询有效的
            queryDto.setPage(1);
            queryDto.setPageSize(50);

            PageResult planResult = attendanceService.getAttendancePlanList(queryDto);
            List<Map<String, Object>> plans = (List<Map<String, Object>>) planResult.getRecords();

            // 筛选出当前时间范围内的值班计划
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> currentDuty = null;

            for (Map<String, Object> plan : plans) {
                try {
                    Object startTimeObj = plan.get("startTime");
                    Object endTimeObj = plan.get("endTime");

                    if (startTimeObj != null && endTimeObj != null) {
                        // 使用DateTimeUtils进行时间解析
                        LocalDateTime startTime = DateTimeUtils.parseDateTime(startTimeObj.toString());
                        LocalDateTime endTime = DateTimeUtils.parseDateTime(endTimeObj.toString());

                        if (startTime != null && endTime != null) {
                            // 检查当前时间是否在值班时间范围内
                            if (now.isAfter(startTime) && now.isBefore(endTime)) {
                                currentDuty = plan;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析值班计划时间失败: {}", e.getMessage());
                    continue;
                }
            }

            return Result.success(currentDuty);
        } catch (Exception e) {
            log.error("获取当前值班信息失败: {}", e.getMessage());
            return Result.error("获取当前值班信息失败");
        }
    }

    /**
     * 获取考勤记录列表
     * @param queryDto 查询参数
     * @return 考勤记录列表
     */
    @GetMapping("/records")
    public Result getAttendanceRecords(AttendanceQueryDto queryDto) {
        log.info("获取考勤记录列表，查询参数: {}", queryDto);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            // 🔧 学生端自动过滤：只查询当前学生的考勤记录
            Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
            if (studentId != null) {
                queryDto.setStudentId(studentId); // 自动设置学生ID过滤
                log.info("学生端查询，自动过滤学生ID: {}", studentId);
            }

            PageResult records = attendanceService.getAttendanceRecordList(queryDto);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取考勤记录列表失败: {}", e.getMessage());
            return Result.error("获取考勤记录列表失败");
        }
    }

    /**
     * 获取单个考勤计划的详细记录信息
     * @param planId 考勤计划ID
     * @param type 期望的考勤计划类型（activity/course/duty），可选参数，用于类型验证
     * @return 详细记录信息
     */
    @GetMapping("/plan/{planId}/records")
    public Result getPlanAttendanceRecords(@PathVariable Long planId, 
                                         @RequestParam(required = false) String type) {
        log.info("获取考勤计划记录详情，计划ID: {}, 期望类型: {}", planId, type);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 获取考勤计划基本信息（包含records数据）
            Map<String, Object> planDetail = attendanceService.getAttendancePlanDetail(planId);
            
            // 验证考勤计划是否存在
            if (planDetail == null) {
                return Result.error("考勤计划不存在");
            }
            
            // 类型验证：确保访问的是正确类型的考勤计划
            if (type != null && !type.isEmpty()) {
                String planType = (String) planDetail.get("type");
                if (planType == null || !type.equals(planType)) {
                    log.warn("考勤计划类型不匹配，计划ID: {}, 实际类型: {}, 期望类型: {}", 
                            planId, planType, type);
                    return Result.error("无法访问该类型的考勤计划");
                }
            }

            // 直接使用getAttendancePlanDetail返回的records数据，不再进行额外查询
            Object records = planDetail.get("records");
            
            log.info("=== 控制器返回数据检查 ===");
            log.info("planDetail包含的keys: {}", planDetail.keySet());
            log.info("records对象类型: {}", records != null ? records.getClass().getSimpleName() : "null");
            if (records instanceof List) {
                List<?> recordList = (List<?>) records;
                log.info("records列表大小: {}", recordList.size());
                for (int i = 0; i < recordList.size(); i++) {
                    log.info("记录[{}]: {}", i, recordList.get(i));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("planDetail", planDetail);
            result.put("records", records);
            
            log.info("最终返回result的keys: {}", result.keySet());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取考勤计划记录详情失败: {}", e.getMessage());
            return Result.error("获取考勤计划记录详情失败");
        }
    }

    /**
     * 获取考勤计划的签到状态
     * @param planId 考勤计划ID
     * @return 签到状态信息
     */
    @GetMapping("/plan/{planId}/status")
    public Result getAttendanceStatus(@PathVariable Long planId) {
        log.info("获取考勤计划签到状态，计划ID: {}", planId);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> status = attendanceService.getAttendanceStatus(planId, currentUserId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取考勤计划签到状态失败: {}", e.getMessage());
            return Result.error("获取签到状态失败");
        }
    }

    /**
     * 创建考勤计划（微信端）
     * @param planDto 考勤计划数据
     * @return 创建结果
     */
    @PostMapping("/plan")
    public Result<Map<String, Object>> createAttendancePlan(@RequestBody AttendancePlanDto planDto) {
        log.info("微信端创建考勤计划，数据: {}", planDto);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 基本字段验证
            if (planDto.getType() == null || planDto.getType().trim().isEmpty()) {
                return Result.error("考勤类型不能为空");
            }
            
            if (planDto.getName() == null || planDto.getName().trim().isEmpty()) {
                return Result.error("考勤名称不能为空");
            }
            
            if (planDto.getStartTime() == null) {
                return Result.error("开始时间不能为空");
            }
            
            if (planDto.getEndTime() == null) {
                return Result.error("结束时间不能为空");
            }
            
            if (planDto.getLocation() == null || planDto.getLocation().trim().isEmpty()) {
                return Result.error("活动地点不能为空");
            }

            // 验证时间
            if (planDto.getEndTime().isBefore(planDto.getStartTime())) {
                return Result.error("结束时间不能早于开始时间");
            }

            // 设置创建用户
            planDto.setCreateUser(currentUserId);
            
            // 设置默认状态值为有效(1)
            if (planDto.getStatus() == null) {
                planDto.setStatus(1);
            }

            // 设置默认签到半径
            if (planDto.getRadius() == null) {
                planDto.setRadius(100);
            }

            // 创建考勤计划
            Map<String, Object> result = attendanceService.createAttendancePlan(planDto);
            log.info("微信端考勤计划创建成功: {}", result);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("微信端创建考勤计划失败: {}", e.getMessage(), e);
            
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else {
                return Result.error("创建失败: " + e.getMessage());
            }
        }
    }

    /**
     * 更新考勤计划（微信端）
     * @param planId 计划ID
     * @param planDto 考勤计划数据
     * @return 更新结果
     */
    @PutMapping("/plan/{planId}")
    public Result updateAttendancePlan(@PathVariable Long planId, @RequestBody AttendancePlanDto planDto) {
        log.info("微信端更新考勤计划，ID: {}, 数据: {}", planId, planDto);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 验证planId
            if (planId == null || planId <= 0) {
                return Result.error("无效的计划ID");
            }

            // 设置计划ID和更新用户
            planDto.setPlanId(planId);
            // 注意：updateUser字段在DTO中不存在，由Service层处理

            // 基本字段验证
            if (planDto.getName() != null && planDto.getName().trim().isEmpty()) {
                return Result.error("考勤名称不能为空");
            }

            if (planDto.getStartTime() != null && planDto.getEndTime() != null 
                && planDto.getEndTime().isBefore(planDto.getStartTime())) {
                return Result.error("结束时间不能早于开始时间");
            }

            boolean result = attendanceService.updateAttendancePlan(planDto);
            if (result) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("微信端更新考勤计划失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    // ==================== 活动预约相关API ====================
    
    /**
     * 获取所有学生列表（用于预约选择）
     * @param keyword 搜索关键词
     * @return 学生列表
     */
    @GetMapping("/students")
    public Result getAllStudents(@RequestParam(required = false) String keyword) {
        log.info("获取学生列表，关键词: {}", keyword);
        try {
            List<Map<String, Object>> students = activityReservationService.getAllStudents(keyword);
            return Result.success(students);
        } catch (Exception e) {
            log.error("获取学生列表失败: {}", e.getMessage());
            return Result.error("获取学生列表失败");
        }
    }
    
    /**
     * 批量创建活动预约（创建活动时使用）
     * @param reservationDto 预约信息
     * @return 创建结果
     */
    @PostMapping("/reservation/batch")
    public Result batchCreateReservation(@RequestBody ActivityReservationDto reservationDto) {
        log.info("批量创建活动预约，数据: {}", reservationDto);
        try {
            Map<String, Object> result = activityReservationService.batchCreateReservation(reservationDto);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量创建活动预约失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("创建预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 学生预约活动
     * @param planId 活动计划ID
     * @param remark 备注
     * @return 预约结果
     */
    @PostMapping("/reservation/{planId}")
    public Result reserveActivity(@PathVariable Long planId, @RequestParam(required = false) String remark) {
        log.info("学生预约活动，计划ID: {}, 备注: {}", planId, remark);
        try {
            // 获取当前登录用户对应的学生ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
            if (studentId == null) {
                return Result.error("当前用户不是学生");
            }
            
            Map<String, Object> result = activityReservationService.reserveActivity(planId, studentId, remark);
            return Result.success(result);
        } catch (Exception e) {
            log.error("预约活动失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消预约
     * @param reservationId 预约ID
     * @return 取消结果
     */
    @DeleteMapping("/reservation/{reservationId}")
    public Result cancelReservation(@PathVariable Long reservationId) {
        log.info("取消预约，预约ID: {}", reservationId);
        try {
            // 获取当前登录用户对应的学生ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
            if (studentId == null) {
                return Result.error("当前用户不是学生");
            }
            
            Map<String, Object> result = activityReservationService.cancelReservation(reservationId, studentId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("取消预约失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("取消预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活动预约列表
     * @param planId 活动计划ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 搜索关键词
     * @return 预约列表
     */
    @GetMapping("/reservation/list")
    public Result getReservationList(@RequestParam Long planId,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   @RequestParam(required = false) String keyword) {
        log.info("获取活动预约列表，计划ID: {}, 页码: {}, 关键词: {}", planId, pageNum, keyword);
        try {
            PageResult result = activityReservationService.getReservationList(planId, pageNum, pageSize, keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取预约列表失败: {}", e.getMessage());
            return Result.error("获取预约列表失败");
        }
    }
    
    /**
     * 获取学生的预约记录
     * @param status 预约状态
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 学生预约列表
     */
    @GetMapping("/reservation/my")
    public Result getMyReservations(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("获取我的预约记录，状态: {}, 页码: {}", status, pageNum);
        try {
            // 获取当前登录用户对应的学生ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
            if (studentId == null) {
                return Result.error("当前用户不是学生");
            }
            
            PageResult result = activityReservationService.getStudentReservations(studentId, status, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取学生预约列表失败: {}", e.getMessage());
            return Result.error("获取预约列表失败");
        }
    }
    
    /**
     * 获取活动预约统计
     * @param planId 活动计划ID
     * @return 预约统计
     */
    @GetMapping("/reservation/statistics/{planId}")
    public Result getReservationStatistics(@PathVariable Long planId) {
        log.info("获取活动预约统计，计划ID: {}", planId);
        try {
            Map<String, Object> result = activityReservationService.getReservationStatistics(planId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取预约统计失败: {}", e.getMessage());
            return Result.error("获取预约统计失败");
        }
    }
    
    /**
     * 检查学生是否已预约活动
     * @param planId 活动计划ID
     * @return 是否已预约
     */
    @GetMapping("/reservation/check/{planId}")
    public Result checkReservation(@PathVariable Long planId) {
        log.info("检查预约状态，计划ID: {}", planId);
        try {
            // 获取当前登录用户对应的学生ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            
            Long studentId = studentMapper.getStudentIdByUserId(currentUserId);
            if (studentId == null) {
                return Result.error("当前用户不是学生");
            }
            
            boolean isReserved = activityReservationService.isStudentReserved(planId, studentId);
            Map<String, Object> result = new HashMap<>();
            result.put("isReserved", isReserved);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查预约状态失败: {}", e.getMessage());
            return Result.error("检查预约状态失败");
        }
    }
    
    /**
     * 为活动预约学生生成考勤记录（管理员功能）
     * @param planId 活动计划ID
     * @return 生成结果
     */
    @PostMapping("/plan/{planId}/generate-records")
    public Result generateAttendanceRecords(@PathVariable Long planId) {
        log.info("为活动预约学生生成考勤记录，计划ID: {}", planId);
        try {
            Map<String, Object> result = attendanceService.generateAttendanceRecordsForReservedStudents(planId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("生成考勤记录失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("生成考勤记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取活动预约学生列表
     * @param planId 活动计划ID
     * @return 预约学生列表
     */
    @GetMapping("/plan/{planId}/reservations")
    public Result getActivityReservations(@PathVariable Long planId) {
        log.info("获取活动预约学生列表，计划ID: {}", planId);
        try {
            List<Map<String, Object>> reservations = activityReservationService.getReservationsByPlanId(planId);
            return Result.success(reservations);
        } catch (Exception e) {
            log.error("获取活动预约学生列表失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("获取预约学生列表失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否有权限查看指定的考勤计划（优化版本）
     * 复用服务层的权限检查逻辑，避免代码冗余
     * @param planId 计划ID
     * @param type 考勤类型
     * @param availablePlanIds 用户可用的计划ID集合
     * @return 是否有权限
     */
    private boolean hasUserPermissionForPlan(Long planId, String type, Set<Long> availablePlanIds) {
        try {
            if (type == null || planId == null) {
                return false;
            }
            
            // 🔧 优化：直接检查可用计划ID集合，避免重复权限验证
            // 因为 availablePlanIds 已经是通过 getCurrentUserAvailablePlans 获取的
            // 该方法内部已经包含了完整的权限检查逻辑（包括管理员权限）
            boolean hasPermission = availablePlanIds.contains(planId);
            log.debug("考勤权限检查结果: planId={}, type={}, hasPermission={}", planId, type, hasPermission);
            return hasPermission;
            
        } catch (Exception e) {
            log.error("检查用户考勤权限失败: planId={}, error={}", planId, e.getMessage());
            return false;
        }
    }
} 