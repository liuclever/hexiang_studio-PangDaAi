package com.back_hexiang_studio.pangDaAi.tool.workflow;

import com.back_hexiang_studio.pangDaAi.service.PermissionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 【工作流工具】考勤管理工具
 * 将考勤查询、统计分析、请假管理等操作封装成完整的工作流工具。
 * 提供考勤记录查询、考勤统计、缺勤学生查询、请假审批等功能。
 */
@Service
@Slf4j
public class AttendanceManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    // ====================================================================================
    // 1. 考勤记录查询工具 (Read-Only)
    // ====================================================================================

    @Tool("查询指定用户的考勤记录，需要有相应的查看权限")
    public String getUserAttendanceRecords(
            @P("用户姓名") String userName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查询用户 '{}' 的考勤记录", userName);
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询考勤记录。";
        }
        
        if (!StringUtils.hasText(userName)) {
            return "  用户姓名不能为空。";
        }
        
        // 权限检查
        if (!permissionService.canViewUserAttendance(currentUserId, userName)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看用户 '%s' 的考勤记录。", roleName, userName);
        }
        
        try {
            String sql = "SELECT ar.sign_in_time, ar.status, " +
                         "DATE(ap.start_time) as attendance_date, ap.start_time, ap.end_time, ap.name as plan_name " +
                         "FROM attendance_record ar " +
                         "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                         "JOIN student s ON ar.student_id = s.student_id " +
                         "JOIN user u ON s.user_id = u.user_id " +
                         "WHERE u.name = ? " +
                         "ORDER BY ap.start_time DESC LIMIT 10";
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, userName);
            
            if (records.isEmpty()) {
                return "  用户 '" + userName + "' 暂无考勤记录。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  用户 **").append(userName).append("** 的考勤记录（最近10条）：\n\n");
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String status = getAttendanceStatusText((String) record.get("status"));
                String statusIcon = getAttendanceStatusIcon((String) record.get("status"));
                
                result.append(String.format("%s %d. %s - **%s**\n", 
                    statusIcon, i + 1, record.get("attendance_date"), status));
                
                if (record.get("plan_name") != null) {
                    result.append(String.format("     计划：%s\n", record.get("plan_name")));
                }
                
                if (record.get("sign_in_time") != null) {
                    result.append(String.format("     签到时间：%s\n", record.get("sign_in_time")));
                } else {
                    result.append("     签到时间：未签到\n");
                }
                
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询用户考勤记录失败: {}", e.getMessage(), e);
            return "  查询考勤记录时出现系统错误，请稍后重试。";
        }
    }

    @Tool("统计指定日期的考勤情况，包括正常、迟到、缺勤等各种状态的人数统计")
    public String getAttendanceStatistics(
            @P("查询日期，格式：YYYY-MM-DD (可选，留空则查询今天)") String date,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        String queryDate = StringUtils.hasText(date) ? date : "今天";
        log.info("  AI Workflow Tool: 统计考勤情况，日期: {}", queryDate);
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询考勤统计。";
        }
        
        // 权限检查
        if (!permissionService.canViewAttendanceStatistics(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看考勤统计。只有管理员和超级管理员可以查看全部考勤统计信息。", roleName);
        }
        
        try {
            // 获取统计数据
            String statsSql;
            List<Map<String, Object>> stats;
            
            if (StringUtils.hasText(date)) {
                statsSql = "SELECT ar.status, COUNT(*) as count FROM attendance_record ar " +
                          "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                          "WHERE DATE(ap.start_time) = ? GROUP BY ar.status";
                stats = jdbcTemplate.queryForList(statsSql, date);
            } else {
                statsSql = "SELECT ar.status, COUNT(*) as count FROM attendance_record ar " +
                          "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                          "WHERE DATE(ap.start_time) = CURDATE() GROUP BY ar.status";
                stats = jdbcTemplate.queryForList(statsSql);
            }
            
            if (stats.isEmpty()) {
                return "  " + queryDate + " 暂无考勤数据。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **").append(queryDate).append("** 考勤统计报告：\n\n");
            
            int total = 0;
            
            // 显示统计数字
            for (Map<String, Object> stat : stats) {
                String status = (String) stat.get("status");
                Object countObj = stat.get("count");
                int count = countObj instanceof BigInteger ? 
                    ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                total += count;
                
                String statusText = getAttendanceStatusText(status);
                String statusIcon = getAttendanceStatusIcon(status);
                result.append(String.format("%s **%s**：%d人\n", statusIcon, statusText, count));
            }
            
            result.append("  **总计**：").append(total).append("人次考勤记录\n\n");
            
            // 获取并显示具体人员名单
            String detailSql;
            List<Map<String, Object>> details;
            
            if (StringUtils.hasText(date)) {
                detailSql = "SELECT u.name, s.student_number, s.majorClass, ar.status, " +
                           "ar.sign_in_time, ap.start_time, ap.name as plan_name " +
                           "FROM attendance_record ar " +
                           "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                           "JOIN student s ON ar.student_id = s.student_id " +
                           "JOIN user u ON s.user_id = u.user_id " +
                           "WHERE DATE(ap.start_time) = ? " +
                           "ORDER BY ar.status, s.majorClass, u.name";
                details = jdbcTemplate.queryForList(detailSql, date);
            } else {
                detailSql = "SELECT u.name, s.student_number, s.majorClass, ar.status, " +
                           "ar.sign_in_time, ap.start_time, ap.name as plan_name " +
                           "FROM attendance_record ar " +
                           "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                           "JOIN student s ON ar.student_id = s.student_id " +
                           "JOIN user u ON s.user_id = u.user_id " +
                           "WHERE DATE(ap.start_time) = CURDATE() " +
                           "ORDER BY ar.status, s.majorClass, u.name";
                details = jdbcTemplate.queryForList(detailSql);
            }
            
            // 按状态分组显示人员名单
            String currentStatus = "";
            for (Map<String, Object> detail : details) {
                String status = (String) detail.get("status");
                String statusText = getAttendanceStatusText(status);
                String statusIcon = getAttendanceStatusIcon(status);
                
                if (!status.equals(currentStatus)) {
                    currentStatus = status;
                    result.append("### ").append(statusIcon).append(" ").append(statusText).append("人员\n");
                }
                
                String timeInfo = "";
                if ("late".equals(status) && detail.get("sign_in_time") != null) {
                    timeInfo = String.format(" (签到: %s, 应到: %s)", 
                        detail.get("sign_in_time"), detail.get("start_time"));
                }
                
                result.append(String.format("- **%s** (%s班)%s\n", 
                    detail.get("name"), detail.get("majorClass"), timeInfo));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询考勤统计失败: {}", e.getMessage(), e);
            return "  查询考勤统计时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询指定日期的缺勤学生列表，用于跟踪学生出勤情况")
    public String getAbsentStudents(
            @P("查询日期，格式：YYYY-MM-DD (可选，留空则查询今天)") String date,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        String queryDate = StringUtils.hasText(date) ? date : "今天";
        log.info("  AI Workflow Tool: 查询缺勤学生列表，日期: {}", queryDate);
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询缺勤学生列表。";
        }
        
        // 权限检查
        if (!permissionService.canViewAbsentStudents(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看缺勤学生列表。只有老师、管理员和超级管理员可以查看。", roleName);
        }
        
        try {
            String sql;
            List<Map<String, Object>> absentStudents;
            
            if (StringUtils.hasText(date)) {
                sql = "SELECT u.name, s.student_number, s.majorClass, ap.name as plan_name " +
                      "FROM attendance_record ar " +
                      "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                      "JOIN student s ON ar.student_id = s.student_id " +
                      "JOIN user u ON s.user_id = u.user_id " +
                      "WHERE ar.status = 'absent' AND DATE(ap.start_time) = ? " +
                      "AND s.student_number IS NOT NULL " +
                      "ORDER BY s.majorClass, u.name";
                absentStudents = jdbcTemplate.queryForList(sql, date);
            } else {
                sql = "SELECT u.name, s.student_number, s.majorClass, ap.name as plan_name " +
                      "FROM attendance_record ar " +
                      "JOIN attendance_plan ap ON ar.plan_id = ap.plan_id " +
                      "JOIN student s ON ar.student_id = s.student_id " +
                      "JOIN user u ON s.user_id = u.user_id " +
                      " WHERE ar.status = 'absent' AND DATE(ap.start_time) = CURDATE() " +
                      "AND s.student_number IS NOT NULL " +
                      "ORDER BY s.majorClass, u.name";
                absentStudents = jdbcTemplate.queryForList(sql);
            }
            
            if (absentStudents.isEmpty()) {
                return " " + queryDate + " 无缺勤学生记录，出勤情况良好！";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **").append(queryDate).append("** 缺勤学生名单（共 ").append(absentStudents.size()).append(" 人）：\n\n");
            
            for (int i = 0; i < absentStudents.size(); i++) {
                Map<String, Object> student = absentStudents.get(i);
                result.append(String.format("%d. **%s**\n", i + 1, student.get("name")));
                result.append(String.format("    学号：%s\n", student.get("student_number")));
                result.append(String.format("    班级：%s\n", student.get("majorClass")));
                
                if (student.get("plan_name") != null) {
                    result.append(String.format("     考勤计划：%s\n", student.get("plan_name")));
                }
                
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询缺勤学生列表失败: {}", e.getMessage(), e);
            return "  查询缺勤学生列表时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询今日的考勤计划安排")
    public String getTodayAttendancePlan() {
        log.info("  AI Workflow Tool: 查询今日考勤计划");
        
        try {
            String sql = "SELECT name, start_time, end_time, location, note " +
                         "FROM attendance_plan " +
                         "WHERE DATE(start_time) = CURDATE() AND status = 1 " +
                         "ORDER BY start_time";
            
            List<Map<String, Object>> plans = jdbcTemplate.queryForList(sql);
            
            if (plans.isEmpty()) {
                return "  今日暂无考勤计划安排。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **今日考勤计划安排**（共 ").append(plans.size()).append(" 项）：\n\n");
            
            for (int i = 0; i < plans.size(); i++) {
                Map<String, Object> plan = plans.get(i);
                result.append(String.format("  %d. **%s**\n", i + 1, plan.get("name")));
                result.append(String.format("    时间：%s - %s\n", plan.get("start_time"), plan.get("end_time")));
                result.append(String.format("    地点：%s\n", plan.get("location")));
                
                if (plan.get("note") != null) {
                    result.append(String.format("     说明：%s\n", plan.get("note")));
                }
                
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询今日考勤计划失败: {}", e.getMessage(), e);
            return "  查询今日考勤计划时出现系统错误，请稍后重试。";
        }
    }

    // ====================================================================================
    // 2. 请假管理工具
    // ====================================================================================

    @Tool("查询待审批的请假申请列表")
    public String getPendingLeaveRequests(@P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId) {
        log.info("  AI Workflow Tool: 查询待审批的请假申请");
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询请假申请。";
        }
        
        try {
            String sql = "SELECT lr.type, lr.start_time, lr.end_time, lr.reason, " +
                         "u.name as applicant_name, s.student_number, lr.create_time " +
                         "FROM leave_request lr " +
                         "LEFT JOIN student s ON lr.student_id = s.student_id " +
                         "LEFT JOIN user u ON s.user_id = u.user_id " +
                         "WHERE lr.status = 'pending' " +
                         "ORDER BY lr.create_time ASC";
            
            List<Map<String, Object>> requests = jdbcTemplate.queryForList(sql);
            
            if (requests.isEmpty()) {
                return "  当前没有待审批的请假申请。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **待审批的请假申请**（共 ").append(requests.size()).append(" 件）：\n\n");
            
            for (int i = 0; i < requests.size(); i++) {
                Map<String, Object> request = requests.get(i);
                String leaveType = getLeaveTypeText((String) request.get("type"));
                
                result.append(String.format("  %d. **%s** (%s)\n", i + 1, 
                    request.get("applicant_name"), 
                    request.get("student_number") != null ? "学号: " + request.get("student_number") : "教职工"));
                result.append(String.format("     类型：%s\n", leaveType));
                result.append(String.format("     时间：%s 至 %s\n", request.get("start_time"), request.get("end_time")));
                result.append(String.format("   💬 原因：%s\n\n", request.get("reason")));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询待审批请假申请失败: {}", e.getMessage(), e);
            return "  查询待审批请假申请时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询指定用户的请假历史记录")
    public String getUserLeaveHistory(@P("用户姓名") String userName) {
        log.info("  AI Workflow Tool: 查询用户 '{}' 的请假历史", userName);
        
        if (!StringUtils.hasText(userName)) {
            return "  用户姓名不能为空。";
        }
        
        try {
            String sql = "SELECT lr.type, lr.start_time, lr.end_time, lr.reason, " +
                         "lr.status, lr.create_time, lr.approved_at " +
                         "FROM leave_request lr " +
                         "LEFT JOIN student s ON lr.student_id = s.student_id " +
                         "LEFT JOIN user u ON s.user_id = u.user_id " +
                         "WHERE u.name = ? " +
                         "ORDER BY lr.create_time DESC LIMIT 10";
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, userName);
            
            if (records.isEmpty()) {
                return "  用户 '" + userName + "' 暂无请假记录。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  用户 **").append(userName).append("** 的请假记录（最近10条）：\n\n");
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String leaveType = getLeaveTypeText((String) record.get("type"));
                String status = getLeaveStatusText((String) record.get("status"));
                String statusIcon = getLeaveStatusIcon((String) record.get("status"));
                
                result.append(String.format("%s %d. %s - %s [**%s**]\n", 
                    statusIcon, i + 1, record.get("start_time"), record.get("end_time"), status));
                result.append(String.format("     类型：%s\n", leaveType));
                result.append(String.format("   💬 原因：%s\n\n", record.get("reason")));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询用户请假历史失败: {}", e.getMessage(), e);
            return "  查询用户请假历史时出现系统错误，请稍后重试。";
        }
    }

    @Tool("根据学生姓名快速查询其请假情况")
    public String getStudentLeaveStatus(@P("学生姓名") String studentName) {
        log.info("  AI Workflow Tool: 查询学生 '{}' 的请假情况", studentName);
        
        if (!StringUtils.hasText(studentName)) {
            return "  学生姓名不能为空。";
        }
        
        try {
            String sql = "SELECT lr.type, lr.start_time, lr.end_time, lr.status, " +
                         "s.student_number, s.majorClass " +
                         "FROM leave_request lr " +
                         "LEFT JOIN student s ON lr.student_id = s.student_id " +
                         "LEFT JOIN user u ON s.user_id = u.user_id " +
                         "WHERE u.name = ? AND s.student_number IS NOT NULL " +
                         "ORDER BY lr.create_time DESC LIMIT 5";
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, studentName);
            
            if (records.isEmpty()) {
                return "  学生 '" + studentName + "' 暂无请假记录。";
            }
            
            Map<String, Object> studentInfo = records.get(0);
            StringBuilder result = new StringBuilder();
            result.append(" 学生 **").append(studentName).append("** 的请假情况：\n");
            result.append(String.format(" 班级：%s |  学号：%s\n\n",
                studentInfo.get("majorClass"), studentInfo.get("student_number")));
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String leaveType = getLeaveTypeText((String) record.get("type"));
                String status = getLeaveStatusText((String) record.get("status"));
                String statusIcon = getLeaveStatusIcon((String) record.get("status"));
                
                result.append(String.format("%s %d. %s - %s [**%s**]\n", 
                    statusIcon, i + 1, record.get("start_time"), record.get("end_time"), status));
                result.append(String.format("     类型：%s\n\n", leaveType));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询学生请假情况失败: {}", e.getMessage(), e);
            return "  查询学生请假情况时出现系统错误，请稍后重试。";
        }
    }

    // ====================================================================================
    // 3. 工具辅助方法
    // ====================================================================================

    private String getAttendanceStatusText(String status) {
        if (status == null) return "未知";
        switch (status.toLowerCase()) {
            case "present": return "正常";
            case "late": return "迟到";
            case "absent": return "缺勤";
            case "leave": return "请假";
            case "pending": return "待签到";
            case "early_leave": return "早退";
            case "sick_leave": return "病假";
            case "personal_leave": return "事假";
            default: return status;
        }
    }
    
    private String getAttendanceStatusIcon(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "present": return " ";
            case "late": return "";
            case "absent": return " ";
            case "leave": return " ";
            case "pending": return "";
            case "early_leave": return "";
            default: return "";
        }
    }

    private String getLeaveTypeText(String type) {
        if (type == null) return "未知";
        switch (type.toLowerCase()) {
            case "sick_leave": return "病假";
            case "personal_leave": return "事假";
            case "public_leave": return "公假";
            case "annual_leave": return "年假";
            default: return type;
        }
    }
    
    private String getLeaveStatusText(String status) {
        if (status == null) return "未知";
        switch (status.toLowerCase()) {
            case "pending": return "待审批";
            case "approved": return "已通过";
            case "rejected": return "已驳回";
            case "cancelled": return "已取消";
            default: return status;
        }
    }
    
    private String getLeaveStatusIcon(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "pending": return "";
            case "approved": return " ";
            case "rejected": return " ";
            case "cancelled": return "";
            default: return "";
        }
    }
} 