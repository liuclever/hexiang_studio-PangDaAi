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
 * 【工作流工具】请假管理工具
 * 将请假申请查询、审批状态分析、请假统计等操作封装成完整的工作流工具。
 * 提供请假记录查询、请假统计、待审批申请查询等功能。
 */
@Service
@Slf4j
public class LeaveManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    // ====================================================================================
    // 1. 请假查询工具 (Read-Only)
    // ====================================================================================

    @Tool("查询待审批的请假申请列表，需要管理员权限")
    public String getPendingLeaveRequests(
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查询待审批的请假申请");
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询待审批请假申请。";
        }
        
        // 权限检查
        if (!permissionService.canViewAbsentStudents(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看请假审批信息。只有老师、管理员和超级管理员可以查看。", roleName);
        }
        
        try {
            String sql = "SELECT lr.leave_id, lr.leave_type, lr.start_date, lr.end_date, lr.reason, " +
                        "u.name as applicant_name, s.student_number, s.majorClass, lr.apply_time " +
                        "FROM leave_request lr " +
                        "LEFT JOIN student s ON lr.student_id = s.student_id " +
                        "LEFT JOIN user u ON s.user_id = u.user_id " +
                        "WHERE lr.status = 'PENDING' " +
                        "ORDER BY lr.apply_time ASC";
            
            List<Map<String, Object>> requests = jdbcTemplate.queryForList(sql);
            
            if (requests.isEmpty()) {
                return "  当前没有待审批的请假申请。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **待审批的请假申请**：\n\n");
            result.append("共有 ").append(requests.size()).append(" 条待审批申请\n\n");
            
            for (int i = 0; i < requests.size(); i++) {
                Map<String, Object> request = requests.get(i);
                String leaveType = getLeaveTypeText((String) request.get("leave_type"));
                String leaveIcon = getLeaveTypeIcon((String) request.get("leave_type"));
                
                result.append("### ").append(i + 1).append(". ").append(leaveIcon).append(" ").append(leaveType).append("\n");
                result.append("**申请人**：").append(request.get("applicant_name"));
                if (request.get("student_number") != null) {
                    result.append(" (学号: ").append(request.get("student_number")).append(", ").append(request.get("majorClass")).append("班)");
                }
                result.append("\n");
                result.append("**时间**：").append(request.get("start_date")).append(" 至 ").append(request.get("end_date")).append("\n");
                result.append("**原因**：").append(request.get("reason")).append("\n");
                result.append("**申请时间**：").append(request.get("apply_time")).append("\n\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询待审批请假申请失败: {}", e.getMessage(), e);
            return "抱歉，当前网络不佳，请稍后重试。";
        }
    }

    @Tool("查询指定用户的请假历史记录")
    public String getUserLeaveHistory(
            @P("用户姓名") String userName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查询用户 '{}' 的请假历史", userName);
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询请假历史。";
        }
        
        if (!StringUtils.hasText(userName)) {
            return "  用户姓名不能为空。";
        }
        
        // 权限检查
        if (!permissionService.canViewAbsentStudents(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看请假记录。只有老师、管理员和超级管理员可以查看。", roleName);
        }
        
        try {
            String sql = "SELECT lr.leave_type, lr.start_date, lr.end_date, lr.reason, " +
                        "lr.status, lr.apply_time, lr.approve_time " +
                        "FROM leave_request lr " +
                        "LEFT JOIN student s ON lr.student_id = s.student_id " +
                        "LEFT JOIN user u ON s.user_id = u.user_id " +
                        "WHERE u.name = ? " +
                        "ORDER BY lr.apply_time DESC LIMIT 10";
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, userName);
            
            if (records.isEmpty()) {
                return "  用户 '" + userName + "' 暂无请假记录。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **").append(userName).append("** 的请假记录：\n\n");
            result.append("最近 ").append(records.size()).append(" 条记录\n\n");
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String leaveType = getLeaveTypeText((String) record.get("leave_type"));
                String leaveIcon = getLeaveTypeIcon((String) record.get("leave_type"));
                String status = getLeaveStatusText((String) record.get("status"));
                String statusIcon = getLeaveStatusIcon((String) record.get("status"));
                
                result.append("### ").append(i + 1).append(". ").append(leaveIcon).append(" ").append(leaveType).append("\n");
                result.append("**状态**：").append(statusIcon).append(" ").append(status).append("\n");
                result.append("**时间**：").append(record.get("start_date")).append(" 至 ").append(record.get("end_date")).append("\n");
                result.append("**原因**：").append(record.get("reason")).append("\n");
                result.append("**申请时间**：").append(record.get("apply_time")).append("\n");
                if (record.get("approve_time") != null) {
                    result.append("**审批时间**：").append(record.get("approve_time")).append("\n");
                }
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询用户请假历史失败: {}", e.getMessage(), e);
            return "抱歉，当前网络不佳，请稍后重试。";
        }
    }

    @Tool("根据学生姓名快速查询其请假情况")
    public String getStudentLeaveStatus(
            @P("学生姓名") String studentName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查询学生 '{}' 的请假情况", studentName);
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询学生请假情况。";
        }
        
        if (!StringUtils.hasText(studentName)) {
            return "  学生姓名不能为空。";
        }
        
        // 权限检查
        if (!permissionService.canViewAbsentStudents(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看学生请假情况。只有老师、管理员和超级管理员可以查看。", roleName);
        }
        
        try {
            String sql = "SELECT lr.leave_type, lr.start_date, lr.end_date, lr.status, lr.reason, " +
                        "s.student_number, s.majorClass, lr.apply_time " +
                        "FROM leave_request lr " +
                        "LEFT JOIN student s ON lr.student_id = s.student_id " +
                        "LEFT JOIN user u ON s.user_id = u.user_id " +
                        "WHERE u.name = ? AND s.student_number IS NOT NULL " +
                        "ORDER BY lr.apply_time DESC LIMIT 5";
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, studentName);
            
            if (records.isEmpty()) {
                return "  学生 '" + studentName + "' 暂无请假记录，或该用户不是学生。";
            }
            
            Map<String, Object> firstRecord = records.get(0);
            StringBuilder result = new StringBuilder();
            result.append("  **学生信息**：").append(studentName)
                  .append(" (学号: ").append(firstRecord.get("student_number"))
                  .append(", 班级: ").append(firstRecord.get("majorClass")).append(")\n\n");
            result.append("  **请假情况**：\n\n");
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String leaveType = getLeaveTypeText((String) record.get("leave_type"));
                String leaveIcon = getLeaveTypeIcon((String) record.get("leave_type"));
                String status = getLeaveStatusText((String) record.get("status"));
                String statusIcon = getLeaveStatusIcon((String) record.get("status"));
                
                result.append("### ").append(i + 1).append(". ").append(leaveIcon).append(" ").append(leaveType).append("\n");
                result.append("**状态**：").append(statusIcon).append(" ").append(status).append("\n");
                result.append("**时间**：").append(record.get("start_date")).append(" 至 ").append(record.get("end_date")).append("\n");
                result.append("**原因**：").append(record.get("reason")).append("\n\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询学生请假情况失败: {}", e.getMessage(), e);
            return "抱歉，当前网络不佳，请稍后重试。";
        }
    }

    @Tool("统计请假申请的状态分布和类型分析")
    public String getLeaveStatistics(
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 统计请假申请状态分布");
        
        if (currentUserId == null) {
            return "  用户未登录，无法查询请假统计。";
        }
        
        // 权限检查
        if (!permissionService.canViewAttendanceStatistics(currentUserId)) {
            Map<String, Object> userInfo = permissionService.getUserInfo(currentUserId);
            String roleName = userInfo != null ? 
                permissionService.getRoleName((Long) userInfo.get("role_id")) : "未知";
            return String.format("  权限不足：您当前是【%s】身份，无权查看请假统计。只有管理员和超级管理员可以查看统计信息。", roleName);
        }
        
        try {
            // 状态统计
            String statusSql = "SELECT status, COUNT(*) as count FROM leave_request GROUP BY status";
            List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql);
            
            // 类型统计
            String typeSql = "SELECT leave_type, COUNT(*) as count FROM leave_request GROUP BY leave_type";
            List<Map<String, Object>> typeStats = jdbcTemplate.queryForList(typeSql);
            
            if (statusStats.isEmpty() && typeStats.isEmpty()) {
                return "  暂无请假申请数据。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **请假申请统计报告**\n\n");
            
            // 状态分布
            if (!statusStats.isEmpty()) {
                result.append("###   审批状态分布\n");
                int totalStatus = 0;
                for (Map<String, Object> stat : statusStats) {
                    String status = (String) stat.get("status");
                    Object countObj = stat.get("count");
                    int count = countObj instanceof BigInteger ? 
                        ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                    totalStatus += count;
                    
                    String statusText = getLeaveStatusText(status);
                    String statusIcon = getLeaveStatusIcon(status);
                    result.append(String.format("%s **%s**：%d个\n", statusIcon, statusText, count));
                }
                result.append("**总计**：").append(totalStatus).append("个申请\n\n");
            }
            
            // 类型分布
            if (!typeStats.isEmpty()) {
                result.append("###   请假类型分布\n");
                for (Map<String, Object> stat : typeStats) {
                    String type = (String) stat.get("leave_type");
                    Object countObj = stat.get("count");
                    int count = countObj instanceof BigInteger ? 
                        ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                    
                    String typeText = getLeaveTypeText(type);
                    String typeIcon = getLeaveTypeIcon(type);
                    result.append(String.format("%s **%s**：%d次\n", typeIcon, typeText, count));
                }
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询请假统计失败: {}", e.getMessage(), e);
            return "抱歉，当前网络不佳，请稍后重试。";
        }
    }

    // ====================================================================================
    // 工具方法
    // ====================================================================================

    private String getLeaveTypeText(String leaveType) {
        if (leaveType == null) return "未知类型";
        switch (leaveType.toUpperCase()) {
            case "SICK": return "病假";
            case "PERSONAL": return "事假";
            case "ANNUAL": return "年假";
            case "MATERNITY": return "产假";
            case "COMPASSIONATE": return "丧假";
            case "STUDY": return "学习假";
            default: return leaveType;
        }
    }

    private String getLeaveTypeIcon(String leaveType) {
        if (leaveType == null) return "";
        switch (leaveType.toUpperCase()) {
            case "SICK": return "🤒";
            case "PERSONAL": return "📝";
            case "ANNUAL": return "🌴";
            case "MATERNITY": return "👶";
            case "COMPASSIONATE": return "🙏";
            case "STUDY": return "📚";
            default: return "📄";
        }
    }

    private String getLeaveStatusText(String status) {
        if (status == null) return "未知状态";
        switch (status.toUpperCase()) {
            case "PENDING": return "待审批";
            case "APPROVED": return "已批准";
            case "REJECTED": return "已拒绝";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }

    private String getLeaveStatusIcon(String status) {
        if (status == null) return "❓";
        switch (status.toUpperCase()) {
            case "PENDING": return "⏳";
            case "APPROVED": return "✅";
            case "REJECTED": return " ";
            case "CANCELLED": return "🚫";
            default: return "❓";
        }
    }
} 