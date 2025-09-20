package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.vo.task.MyTaskVO;
import com.back_hexiang_studio.mapper.AttendanceStatisticsMapper;
import com.back_hexiang_studio.mapper.CourseMapper;
import com.back_hexiang_studio.mapper.MaterialMapper;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.mapper.TaskMapper;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.UserService;
import com.github.pagehelper.PageHelper;
import com.back_hexiang_studio.service.AttendanceService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页仪表盘控制器
 * 提供首页所需的各种统计数据
 * 权限：所有人都可以访问
 */
@RestController
@RequestMapping("/admin/dashboard")
@Slf4j
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
public class DashboardController {

    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private AttendanceStatisticsMapper attendanceStatisticsMapper;

    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private UserService userService;

    /**
     * 获取首页概览数据
     * @return 包含学生总数、资料总数、课程总数等信息
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverviewData() {
        Map<String, Object> data = new HashMap<>();
        
        // 获取学生总数
        int studentCount = studentMapper.countActiveStudents();
        data.put("studentCount", studentCount);
        
        // 获取资料总数
        int materialCount = materialMapper.countMaterials();
        data.put("materialCount", materialCount);
        
        // 获取课程总数
        int courseCount = courseMapper.countCourses();
        data.put("courseCount", courseCount);
        
        // 获取当前在线用户数（调用Service层方法）
        long onlineCount = userService.countOnlineUsers();
        data.put("onlineCount", onlineCount);
        
        return Result.success(data);
    }
    
    /**
     * 获取活动数据概览
     * @param type 数据类型：activity(活动考勤)、course(课程考勤)、duty(值班考勤)
     * @return 对应类型的考勤统计数据
     */
    @GetMapping("/activity-data")
    public Result<Map<String, Object>> getActivityData(String type) {
        try {
            if (type == null || type.isEmpty()) {
                type = "activity"; // 默认为活动考勤
            }
            
            // 获取真实的考勤趋势数据
            Map<String, Object> data = getAttendanceTrendData(type);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取活动数据概览失败", e);
            // 返回空数据
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("type", type);
            emptyData.put("dates", new ArrayList<>());
            emptyData.put("values", new ArrayList<>());
            return Result.success(emptyData);
        }
    }

    /**
     * 获取当前考勤周期数据
     * @return 当前考勤周期的统计数据
     */
    @GetMapping("/current-period")
    public Result<Map<String, Object>> getCurrentPeriodData() {
        try {
            log.info("开始获取当前考勤周期数据");
            Map<String, Object> data = new HashMap<>();
            
            // 获取当前周的考勤数据
            Map<String, Object> activityData = getAttendanceTrendData("activity");
            Map<String, Object> courseData = getAttendanceTrendData("course");
            Map<String, Object> dutyData = getAttendanceTrendData("duty");
            
            log.info("获取到活动考勤数据: {}", activityData);
            log.info("获取到课程考勤数据: {}", courseData);
            log.info("获取到值班考勤数据: {}", dutyData);
            
            // 检查并确保数据完整性
            validateAndRepairData(activityData, "activity");
            validateAndRepairData(courseData, "course");
            validateAndRepairData(dutyData, "duty");
            
            data.put("activity", activityData);
            data.put("course", courseData);
            data.put("duty", dutyData);
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取当前考勤周期数据失败", e);
            // 返回空数据
            Map<String, Object> data = new HashMap<>();
            
            Map<String, Object> emptyData = createEmptyData("activity");
            Map<String, Object> emptyCourseData = createEmptyData("course");
            Map<String, Object> emptyDutyData = createEmptyData("duty");
            
            data.put("activity", emptyData);
            data.put("course", emptyCourseData);
            data.put("duty", emptyDutyData);
            
            return Result.success(data);
        }
    }

    /**
     * 获取考勤趋势数据
     * @param type 考勤类型
     * @return 趋势数据
     */
    private Map<String, Object> getAttendanceTrendData(String type) {
        // 创建结果Map
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        
        try {
            // 获取当前日期
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(6); // 最近7天
            
            // 查询数据库获取真实的统计数据
            Map<String, Object> params = new HashMap<>();
            params.put("type", type);
            params.put("startDate", startDate);
            params.put("endDate", today);
            
            List<Map<String, Object>> statisticsData = attendanceStatisticsMapper.selectStatistics(params);
            
            // 日期格式化器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            
            // 准备返回数据
            List<String> dates = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            
            // 如果有数据，处理数据
            if (!statisticsData.isEmpty()) {
                for (Map<String, Object> stat : statisticsData) {
                    // 获取日期并格式化
                    LocalDate date = (LocalDate) stat.get("date");
                    String dateStr = date.format(formatter);
                    
                    // 获取对应的出勤人数
                    Integer presentCount = (Integer) stat.get("present_count");
                    
                    dates.add(dateStr);
                    values.add(presentCount != null ? presentCount : 0);
                }
            } else {
                // 如果没有数据，生成空数据
                for (int i = 0; i < 7; i++) {
                    LocalDate date = startDate.plusDays(i);
                    dates.add(date.format(formatter));
                    values.add(0);
                }
            }
            
            result.put("dates", dates);
            result.put("values", values);
            
            return result;
        } catch (Exception e) {
            log.error("获取考勤趋势数据失败: {}", e.getMessage());
            
            // 返回空数据
            result.put("dates", new ArrayList<>());
            result.put("values", new ArrayList<>());
            return result;
        }
    }

    /**
     * 验证并修复数据，确保数据结构完整
     * @param data 需要验证的数据
     * @param type 数据类型
     */
    private void validateAndRepairData(Map<String, Object> data, String type) {
        if (data == null) {
            data = new HashMap<>();
        }
        
        // 确保type字段存在
        if (!data.containsKey("type")) {
            data.put("type", type);
        }
        
        // 确保dates字段存在且不为空
        if (!data.containsKey("dates") || data.get("dates") == null) {
            data.put("dates", new ArrayList<>());
        }
        
        // 确保values字段存在且不为空
        if (!data.containsKey("values") || data.get("values") == null) {
            data.put("values", new ArrayList<>());
        }
        
        // 如果dates和values都为空或长度不为7，创建默认的一周数据
        List<String> dates = (List<String>) data.get("dates");
        List<Integer> values = (List<Integer>) data.get("values");
        
        if (dates.isEmpty() || values.isEmpty() || dates.size() < 7 || values.size() < 7) {
            log.info("数据长度不足7天，创建默认的一周数据。当前日期数: {}, 当前值数: {}", 
                    dates.size(), values.size());
            
            // 清空现有数据
            dates.clear();
            values.clear();
            
            // 创建当前周的日期标签（周一到周日）
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            
            // 设置为本周的周一
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SUNDAY) {
                dayOfWeek = 8; // 使Sunday为7
            }
            cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY));
            
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
            SimpleDateFormat dayFormat = new SimpleDateFormat("E"); // 获取周几
            
            for (int i = 0; i < 7; i++) {
                Date day = cal.getTime();
                String dateStr = sdf.format(day) + " " + dayFormat.format(day);
                dates.add(dateStr);
                values.add(0);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                
                log.info("添加默认日期: {}", dateStr);
            }
            
            data.put("dates", dates);
            data.put("values", values);
        } else if (dates.size() != values.size()) {
            // 如果日期和值的数量不一致，调整值的数量
            log.info("日期和值的数量不一致，调整值的数量。日期数: {}, 值数: {}", 
                    dates.size(), values.size());
            
            if (dates.size() > values.size()) {
                // 补充缺少的值
                while (values.size() < dates.size()) {
                    values.add(0);
                }
            } else {
                // 截断多余的值
                while (values.size() > dates.size()) {
                    values.remove(values.size() - 1);
                }
            }
            
            data.put("values", values);
        }
    }

    /**
     * 创建空数据
     * @param type 数据类型
     * @return 空数据结构
     */
    private Map<String, Object> createEmptyData(String type) {
        Map<String, Object> emptyData = new HashMap<>();
        emptyData.put("type", type);
        
        List<String> dates = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        
        // 创建当前周的日期标签（周一到周日）
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        
        // 设置为本周的周一
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, Calendar.MONDAY - dayOfWeek);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        SimpleDateFormat dayFormat = new SimpleDateFormat("E"); // 获取周几
        
        for (int i = 0; i < 7; i++) {
            Date day = cal.getTime();
            String dateStr = sdf.format(day) + " " + dayFormat.format(day);
            dates.add(dateStr);
            values.add(0);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        emptyData.put("dates", dates);
        emptyData.put("values", values);
        
        return emptyData;
    }

    /**
     * 获取当前用户的紧急待办任务列表
     * @return 包含任务信息的列表
     */
    @GetMapping("/my-tasks")
    public Result<List<MyTaskVO>> getMyUrgentTasks() {
        Long userId = UserContextHolder.getCurrentId(); // 使用正确的方法获取用户ID
        if (userId == null) {
            return Result.error("无法获取用户信息");
        }
        
        // 使用PageHelper限制返回数量（最多5个任务）
        PageHelper.startPage(1, 5, false);
        
        List<MyTaskVO> tasks = taskMapper.findUrgentTasksByUserId(userId);
        return Result.success(tasks);
    }
} 