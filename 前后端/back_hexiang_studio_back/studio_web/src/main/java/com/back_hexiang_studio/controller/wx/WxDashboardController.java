package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.UserService;
import com.back_hexiang_studio.service.ActivityTrendService;
import com.back_hexiang_studio.securuty.TokenService;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.mapper.CourseMapper;
import com.back_hexiang_studio.dv.vo.basicUserVo;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.dv.vo.dashboard.DashboardStatisticsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 微信端仪表盘控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/dashboard")
public class WxDashboardController {

    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private ActivityTrendService activityTrendService;

    /**
     * 获取仪表板统计数据
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Result<DashboardStatisticsVo> getStatistics() {
        log.info("获取微信端仪表板统计数据");
        try {
            // 获取当前在线用户数（真正的活跃用户统计）
            int activeToday = getCurrentOnlineUserCount();
            
            // 获取课程总数
            int totalCourses = courseMapper.countCourses();
            
            DashboardStatisticsVo statistics = DashboardStatisticsVo.builder()
                    .activeToday(activeToday)
                    .totalCourses(totalCourses)
                    .build();
            
            log.info("仪表板统计 - 当前在线用户: {}, 课程总数: {}", activeToday, totalCourses);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败: {}", e.getMessage());
            return Result.error("获取统计数据失败");
        }
    }
    
    /**
     * 获取当前在线用户数量
     * @return 在线用户数量
     */
    private int getCurrentOnlineUserCount() {
        try {
            // 获取所有用户列表
            PageDto pageDto = new PageDto();
            pageDto.setPage(1);
            pageDto.setPageSize(999); // 获取所有用户
            PageResult result = userService.list(pageDto);
            
            if (result == null || result.getRecords() == null) {
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            List<basicUserVo> users = (List<basicUserVo>) result.getRecords();
            
            // 统计在线用户数量
            int onlineCount = 0;
            for (basicUserVo user : users) {
                if (user.getUserId() != null && tokenService.isUserOnline(user.getUserId())) {
                    onlineCount++;
                }
            }
            
            log.debug("统计在线用户 - 总用户数: {}, 在线用户数: {}", users.size(), onlineCount);
            return onlineCount;
        } catch (Exception e) {
            log.error("统计在线用户失败: {}", e.getMessage());
            return 0; // 出错时返回0
        }
    }

    /**
     * 获取工作室活跃度趋势（近5天）
     * @return 活跃度趋势数据
     */
    @GetMapping("/activity-trend")
    public Result<Map<String, Object>> getActivityTrend() {
        log.info("获取工作室活跃度趋势（近5天）");
        try {
            Map<String, Object> result = activityTrendService.getActivityTrend();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取活跃度趋势失败: {}", e.getMessage(), e);
            return Result.error("获取活跃度趋势失败");
        }
    }
} 