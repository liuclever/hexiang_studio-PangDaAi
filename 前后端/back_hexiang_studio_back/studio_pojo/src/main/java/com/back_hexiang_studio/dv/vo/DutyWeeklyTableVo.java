package com.back_hexiang_studio.dv.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 值班周表VO
 * 优化后的值班表数据结构，减少不必要的字段传输
 */
@Data
public class DutyWeeklyTableVo {
    /**
     * 周开始日期 (格式: yyyy-MM-dd)
     */
    private String weekStart;
    
    /**
     * 周结束日期 (格式: yyyy-MM-dd)
     */
    private String weekEnd;
    
    /**
     * 时间段列表
     * 例如: ["08:30-10:00", "10:20-11:50", ...]
     */
    private List<String> timeSlots;
    
    /**
     * 周天数据
     * 包含每天的日期、星期几和值班安排
     */
    private List<DayScheduleVo> weekDays;
    
    /**
     * 单天值班安排VO
     */
    @Data
    public static class DayScheduleVo {
        /**
         * 星期几 (1-7, 1表示周一)
         */
        private Integer dayOfWeek;
        
        /**
         * 日期 (格式: yyyy-MM-dd)
         */
        private String date;
        
        /**
         * 值班安排列表
         */
        private List<Map<String, Object>> schedules;
    }
    
    /**
     * 值班学生VO
     */
    @Data
    public static class DutyStudentVo {
        /**
         * 学生ID
         */
        private Long studentId;
        
        /**
         * 学生姓名
         */
        private String studentName;
        
        /**
         * 考勤状态
         * normal: 未开始考勤
         * present: 已签到
         * late: 迟到
         * absent: 缺勤
         * leave: 请假
         */
        private String attendanceStatus;
    }
} 