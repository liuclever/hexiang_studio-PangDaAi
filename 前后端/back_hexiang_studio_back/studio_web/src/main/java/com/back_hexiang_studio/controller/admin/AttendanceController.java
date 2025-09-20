package com.back_hexiang_studio.controller.admin;



import com.back_hexiang_studio.dv.dto.ActivityReservationDto;

import com.back_hexiang_studio.dv.dto.AttendancePlanDto;
import com.back_hexiang_studio.dv.dto.AttendanceQueryDto;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.AttendanceService;
import com.back_hexiang_studio.service.ActivityReservationService;
import com.back_hexiang_studio.GlobalException.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员考勤控制器
 * 权限：超级管理员或只有副主任、主任可以访问
 */
@Slf4j
@RestController
@RequestMapping("/admin/attendance")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ATTENDANCE_MANAGE')")
public class AttendanceController {
    
    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private ActivityReservationService activityReservationService;
    
    /**
     * 创建考勤计划
     * @param planDto 考勤计划数据
     * @return 创建结果
     */
    @PostMapping("/plan")
    public Result<Map<String, Object>> createAttendancePlan(@RequestBody AttendancePlanDto planDto) {
        try {
            // 记录请求信息
            log.info("接收到创建考勤计划请求: {}", planDto);
            log.info("考勤开始时间: {}, 类型: {}", planDto.getStartTime(), planDto.getStartTime() != null ? planDto.getStartTime().getClass().getName() : "null");
            log.info("考勤结束时间: {}, 类型: {}", planDto.getEndTime(), planDto.getEndTime() != null ? planDto.getEndTime().getClass().getName() : "null");
            log.info("创建用户ID: {}", planDto.getCreateUser());
            
            // 检查必要字段
            if (planDto.getType() == null) {
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
            
            // 处理日期比较
            if (planDto.getEndTime().isBefore(planDto.getStartTime())) {
                return Result.error("结束时间不能早于开始时间");
            }
            
            // 设置默认状态值为有效(1)
            if (planDto.getStatus() == null) {
                planDto.setStatus(1);
                log.info("设置默认状态值为1");
            }
            
            // 检查用户ID并设置默认值
            if (planDto.getCreateUser() == null) {
                // 尝试从UserContextHolder获取
                Long userId = com.back_hexiang_studio.context.UserContextHolder.getCurrentId();
                log.info("从UserContextHolder获取用户ID: {}", userId);
                
                if (userId == null) {
                    // 如果无法获取，则设置默认值
                    userId = 1L;
                    log.info("设置默认用户ID: {}", userId);
                }
                
                planDto.setCreateUser(userId);
            }
            
            // 如果是课程考勤，检查课程ID
            if ("course".equals(planDto.getType())) {
                log.info("课程考勤，课程ID: {}", planDto.getCourseId());
                if (planDto.getCourseId() == null) {
                    return Result.error("课程考勤必须指定课程ID");
                }
            }
            
            // 创建考勤计划
        Map<String, Object> result = attendanceService.createAttendancePlan(planDto);
            log.info("考勤计划创建成功: {}", result);
        return Result.success(result);
        } catch (Exception e) {
            log.error("创建考勤计划时发生系统异常", e);
            
            // 提供更详细的错误信息
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else if (e.getCause() != null) {
                return Result.error("系统异常: " + e.getCause().getMessage());
            } else {
                return Result.error("系统异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取考勤计划列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/plans")
    public Result<PageResult> getAttendancePlans(AttendanceQueryDto queryDto) {
        PageResult plans = attendanceService.getAttendancePlanList(queryDto);
        return Result.success(plans);
    }
    
    /**
     * 获取考勤计划详情
     * @param planId 计划ID
     * @return 考勤计划详情
     */
    @GetMapping("/plan/{planId}")
    public Result<Map<String, Object>> getAttendancePlanDetail(@PathVariable Long planId) {
        Map<String, Object> plan = attendanceService.getAttendancePlanDetail(planId);
        return Result.success(plan);
    }
    
    /**
     * 更新考勤计划
     * @param planId 计划ID
     * @param planDto 考勤计划数据
     * @return 更新结果
     */
    @PutMapping("/plan/{planId}")
    public Result updateAttendancePlan(@PathVariable Long planId, @RequestBody AttendancePlanDto planDto) {
        try {
            log.info("更新考勤计划，ID: {}, 数据: {}", planId, planDto);
            
            // 验证planId
            if (planId == null || planId <= 0) {
                return Result.error("无效的计划ID");
            }
            
            // 设置计划ID
            planDto.setPlanId(planId);
            
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
            log.error("更新考勤计划失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新考勤计划状态
     * @param planId 计划ID
     * @param data 包含新状态的Map
     * @return 更新结果
     */
    @PutMapping("/plan/{planId}/status")
    public Result updateAttendancePlanStatus(@PathVariable Long planId, @RequestBody Map<String, Boolean> data) {
        Boolean status = data.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        boolean result = attendanceService.updateAttendancePlanStatus(planId, status);
        return Result.success(result);
    }
    
    /**
     * 删除考勤计划
     * @param planId 计划ID
     * @return 删除结果
     */
    @DeleteMapping("/plan/{planId}")
    public Result deleteAttendancePlan(@PathVariable Long planId) {
        boolean result = attendanceService.deleteAttendancePlan(planId);
        return Result.success();
    }
    
    /**
     * 获取考勤记录列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/records")
    public Result<PageResult> getAttendanceRecords(AttendanceQueryDto queryDto) {
        PageResult records = attendanceService.getAttendanceRecordList(queryDto);
        return Result.success(records);
    }
    
    /**
     * 更新考勤记录状态
     * @param recordId 记录ID
     * @param
     * @return 更新结果
     *
     * 【未使用方法】- 前端API列表中未包含此接口
     */

    @PutMapping("/records/{recordId}/status")
    public Result updateRecordStatus(
            @PathVariable Long recordId,
            @RequestBody Map<String, String> requestBody) {
        String status = requestBody.get("status");
        String remark = requestBody.get("remark");
        attendanceService.updateAttendanceStatus(recordId, status, remark);
        return Result.success("状态更新成功");
    }

    
    /**
     * 删除考勤记录
     * @param recordId 记录ID
     * @return 删除结果
     */
    @DeleteMapping("/record/{recordId}")
    public Result deleteAttendanceRecord(@PathVariable Long recordId) {
        boolean result = attendanceService.deleteAttendanceRecord(recordId);
        return Result.success();
    }
    
    /**
     * 获取课程考勤统计
     * @param courseId 课程ID
     * @return 统计数据
     * 
     * 【未使用方法】- 前端API列表中未包含此接口
     */
    /*
    @GetMapping("/statistics/course/{courseId}")
    public Result<Map<String, Object>> getCourseAttendanceStatistics(@PathVariable Long courseId) {
        Map<String, Object> statistics = attendanceService.getCourseAttendanceStatistics(courseId);
        return Result.success(statistics);
    }
    */
    
    /**
     * 获取学生考勤统计
     * @param studentId 学生ID
     * @return 统计数据
     * 
     * 【未使用方法】- 前端API列表中未包含此接口
     */
    /*
    @GetMapping("/statistics/student/{studentId}")
    public Result<Map<String, Object>> getStudentAttendanceStatistics(@PathVariable Long studentId) {
        Map<String, Object> statistics = attendanceService.getStudentAttendanceStatistics(studentId);
        return Result.success(statistics);
    }
    */
    
    /**
     * 手动生成考勤统计数据
     * @param date 统计日期
     * @return 结果
     * 
     * 【未使用方法】- 前端API列表中未包含此接口
     */
    /*
    @PostMapping("/statistics/generate")
    public Result generateAttendanceStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        attendanceService.generateAttendanceStatistics(date);
        return Result.success();
    }
    */

    /**
     * 获取总体统计数据
     * @param queryDto 查询参数
     * @return 统计数据
     */
    @GetMapping("/statistics/overall")
    public Result<Map<String, Object>> getOverallStatistics(AttendanceQueryDto queryDto) {
        try {
            log.info("接收到获取总体统计数据请求: {}", queryDto);
            Map<String, Object> statistics = attendanceService.getOverallStatistics(queryDto);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取总体统计数据时发生系统异常", e);
            
            // 提供更详细的错误信息
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else if (e.getCause() != null) {
                return Result.error("系统异常: " + e.getCause().getMessage());
            } else {
                return Result.error("系统异常: " + e.getMessage());
            }
        }
    }

    /**
     * 获取考勤趋势数据
     * @param queryDto 查询参数
     * @return 趋势数据
     */
    @GetMapping("/statistics/trends")
    public Result<List<Map<String, Object>>> getAttendanceTrends(AttendanceQueryDto queryDto) {
        try {
            log.info("接收到获取考勤趋势数据请求: {}", queryDto);
            List<Map<String, Object>> trends = attendanceService.getAttendanceTrends(queryDto);
            return Result.success(trends);
        } catch (Exception e) {
            log.error("获取考勤趋势数据时发生系统异常", e);
            
            // 提供更详细的错误信息
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else if (e.getCause() != null) {
                return Result.error("系统异常: " + e.getCause().getMessage());
            } else {
                return Result.error("系统异常: " + e.getMessage());
            }
        }
    }

    /**
     * 获取考勤类型分布数据
     * @param queryDto 查询参数
     * @return 类型分布数据
     */
    @GetMapping("/statistics/type-distribution")
    public Result<List<Map<String, Object>>> getAttendanceTypeDistribution(AttendanceQueryDto queryDto) {
        try {
            log.info("接收到获取考勤类型分布数据请求: {}", queryDto);
            List<Map<String, Object>> distribution = attendanceService.getAttendanceTypeDistribution(queryDto);
            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取考勤类型分布数据时发生系统异常", e);
            
            // 提供更详细的错误信息
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else if (e.getCause() != null) {
                return Result.error("系统异常: " + e.getCause().getMessage());
            } else {
                return Result.error("系统异常: " + e.getMessage());
            }
        }
    }

    /**
     * 获取学生考勤统计列表
     * @param queryDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/statistics/student-list")
    public Result<PageResult> getStudentAttendanceStatisticsList(AttendanceQueryDto queryDto) {
        try {
            log.info("接收到获取学生考勤统计列表请求: {}", queryDto);
            PageResult result = attendanceService.getStudentAttendanceStatisticsList(queryDto);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取学生考勤统计列表时发生系统异常", e);
            
            // 提供更详细的错误信息
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            } else if (e.getCause() != null) {
                return Result.error("系统异常: " + e.getCause().getMessage());
            } else {
                return Result.error("系统异常: " + e.getMessage());
            }
        }
    }
    
    // ============= 活动预约相关接口 =============
    
    /**
     * 批量创建活动预约
     * @param reservationDto 预约数据
     * @return 创建结果
     */
    @PostMapping("/activity/reservation/batch")
    public Result<Map<String, Object>> batchCreateActivityReservation(@RequestBody ActivityReservationDto reservationDto) {
        try {
            log.info("接收到批量创建活动预约请求: {}", reservationDto);
            Map<String, Object> result = activityReservationService.batchCreateReservation(reservationDto);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量创建活动预约失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("批量创建预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活动预约学生列表
     * @param planId 活动计划ID
     * @return 预约学生列表
     */
    @GetMapping("/activity/plan/{planId}/reservations")
    public Result<List<Map<String, Object>>> getActivityReservations(@PathVariable Long planId) {
        try {
            log.info("获取活动预约学生列表，计划ID: {}", planId);
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
     * 取消活动预约
     * @param data 取消预约数据
     * @return 取消结果
     */
    @PostMapping("/activity/reservation/cancel")
    public Result<Map<String, Object>> cancelActivityReservation(@RequestBody Map<String, Object> data) {
        try {
            Long planId = Long.valueOf(data.get("planId").toString());
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) data.get("studentIds");
            
            log.info("取消活动预约，计划ID: {}, 学生IDs: {}", planId, studentIds);
            Map<String, Object> result = activityReservationService.batchCancelReservation(planId, studentIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("取消活动预约失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("取消预约失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活动预约列表（分页）
     * @param planId 活动计划ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param keyword 搜索关键词
     * @return 预约列表
     */
    @GetMapping("/activity/reservation/list")
    public Result<PageResult> getActivityReservationList(@RequestParam Long planId,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword) {
        try {
            log.info("获取活动预约列表，计划ID: {}, 页码: {}, 关键词: {}", planId, pageNum, keyword);
            PageResult result = activityReservationService.getReservationList(planId, pageNum, pageSize, keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取活动预约列表失败: {}", e.getMessage());
            if (e instanceof BusinessException) {
                return Result.error(((BusinessException) e).getMessage());
            }
            return Result.error("获取预约列表失败: " + e.getMessage());
        }
    }
} 