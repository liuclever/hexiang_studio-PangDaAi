package com.back_hexiang_studio.dv.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表板统计数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsVo {
    /**
     * 当前在线人数
     */
    private Integer activeToday;
    
    /**
     * 课程总数
     */
    private Integer totalCourses;
} 