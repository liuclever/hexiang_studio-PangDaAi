package com.back_hexiang_studio.pangDaAi.tool.workflow;

import com.back_hexiang_studio.context.UserContextHolder;
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
 * 【工作流工具】任务管理工具
 * 将任务查询和管理操作封装成完整的工作流工具。
 * 提供任务列表查询、任务详情查看、任务状态跟踪等功能。
 */
@Service
@Slf4j
public class TaskManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    // ====================================================================================
    // 1. 任务查询工具 (Read-Only)
    // ====================================================================================

    @Tool("查询当前登录用户自己的未完成任务列表，包括进行中、紧急、待审核等状态的任务")
    public String getCurrentUserUncompletedTasks(@P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId) {
        log.info("🤖 AI Workflow Tool: 查询用户的未完成任务，用户ID: {}", currentUserId);
        
        if (currentUserId == null) {
            return "❌ 用户未登录，无法查询任务列表。";
        }
        
        try {
            // 通过sub_member -> sub_task -> task的关联关系查询用户参与的未完成任务
            String sql = "SELECT DISTINCT t.task_id, t.title, t.description, t.status, t.start_time, t.end_time, " +
                         "t.create_time, u.name as creator, " +
                         "CASE WHEN t.status = 'URGENT' THEN 'HIGH' " +
                         "     WHEN t.status = 'OVERDUE' THEN 'HIGH' " +
                         "     WHEN t.status = 'PENDING_REVIEW' THEN 'MEDIUM' " +
                         "     ELSE 'MEDIUM' END as priority " +
                         "FROM task t " +
                         "INNER JOIN sub_task st ON t.task_id = st.task_id " +
                         "INNER JOIN sub_member sm ON st.sub_task_id = sm.sub_task_id " +
                         "LEFT JOIN user u ON t.create_user = u.user_id " +
                         "WHERE sm.user_id = ? " +
                         "  AND t.status IN ('IN_PROGRESS', 'URGENT', 'PENDING_REVIEW', 'OVERDUE') " +
                         "ORDER BY " +
                         "  CASE t.status " +
                         "    WHEN 'URGENT' THEN 1 " +
                         "    WHEN 'OVERDUE' THEN 2 " +
                         "    WHEN 'IN_PROGRESS' THEN 3 " +
                         "    WHEN 'PENDING_REVIEW' THEN 4 " +
                         "    ELSE 5 END, " +
                         "  t.end_time ASC";
            
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql, currentUserId);
            
            if (tasks.isEmpty()) {
                return "🎉 太好了！您目前没有待完成的任务，可以休息一下了~";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("📝 您的未完成任务列表（共 ").append(tasks.size()).append(" 个）：\n\n");
            
            for (int i = 0; i < tasks.size(); i++) {
                Map<String, Object> task = tasks.get(i);
                String title = (String) task.get("title");
                String description = (String) task.get("description");
                String status = (String) task.get("status");
                String priority = (String) task.get("priority");
                String creator = (String) task.get("creator");
                Object endTimeObj = task.get("end_time");
                
                result.append(getPriorityIcon(priority)).append(" ").append(i + 1).append(". **").append(title).append("**\n");
                result.append("   📊 状态：").append(getTaskStatusText(status)).append("\n");
                
                if (StringUtils.hasText(description)) {
                    String shortDesc = description.length() > 50 ? description.substring(0, 50) + "..." : description;
                    result.append("   📄 描述：").append(shortDesc).append("\n");
                }
                
                if (creator != null) {
                    result.append("   👤 创建者：").append(creator).append("\n");
                }
                
                if (endTimeObj != null) {
                    result.append("   ⏰ 截止：").append(endTimeObj).append("\n");
                }
                
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("❌ 查询用户未完成任务失败: {}", e.getMessage(), e);
            return "❌ 查询任务时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询指定用户的任务列表，需要有相应权限")
    public String getUserTasks(
            @P("用户姓名") String userName,
            @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("🤖 AI Workflow Tool: 查询用户 '{}' 的任务列表", userName);
        
        if (currentUserId == null) {
            return "❌ 用户未登录，无法查询任务列表。";
        }
        
        if (!StringUtils.hasText(userName)) {
            return "❌ 用户姓名不能为空。";
        }
        
        try {
            // 这里可以添加权限检查
            String sql = "SELECT t.title, t.status, t.start_time, t.end_time " +
                         "FROM task t " +
                         "LEFT JOIN user u ON t.assignee_id = u.user_id " +
                         "WHERE u.name = ? " +
                         "ORDER BY t.end_time ASC";
            
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql, userName);
            
            if (tasks.isEmpty()) {
                return "📋 用户 '" + userName + "' 暂无分配的任务。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("📋 用户 ").append(userName).append(" 的任务列表：\n\n");
            
            for (int i = 0; i < tasks.size(); i++) {
                Map<String, Object> task = tasks.get(i);
                result.append(String.format("%d. **%s**\n", i + 1, task.get("title")));
                result.append(String.format("   📊 状态：%s\n", getTaskStatusText((String) task.get("status"))));
                result.append(String.format("   ⏰ 截止：%s\n\n", task.get("end_time")));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("❌ 查询用户任务列表失败: {}", e.getMessage(), e);
            return "❌ 查询用户任务时出现系统错误，请稍后重试。";
        }
    }

    @Tool("获取指定任务的详细信息")
    public String getTaskDetails(@P("任务标题") String taskTitle) {
        log.info("🤖 AI Workflow Tool: 查询任务 '{}' 的详细信息", taskTitle);
        
        if (!StringUtils.hasText(taskTitle)) {
            return "❌ 任务标题不能为空。";
        }
        
        try {
            String sql = "SELECT t.title, t.description, t.status, t.start_time, t.end_time, " +
                         "u.name as assignee_name " +
                         "FROM task t " +
                         "LEFT JOIN user u ON t.assignee_id = u.user_id " +
                         "WHERE t.title = ?";
            
            Map<String, Object> task = jdbcTemplate.queryForMap(sql, taskTitle);
            
            StringBuilder result = new StringBuilder();
            result.append("📋 任务详细信息：\n\n");
            result.append("📝 **").append(task.get("title")).append("**\n\n");
            
            if (task.get("description") != null) {
                result.append("📄 描述：\n").append(task.get("description")).append("\n\n");
            }
            
            result.append("📊 状态：").append(getTaskStatusText((String) task.get("status"))).append("\n");
            
            if (task.get("assignee_name") != null) {
                result.append("👤 负责人：").append(task.get("assignee_name")).append("\n");
            }
            
            if (task.get("start_time") != null) {
                result.append("🚀 开始时间：").append(task.get("start_time")).append("\n");
            }
            
            if (task.get("end_time") != null) {
                result.append("⏰ 截止时间：").append(task.get("end_time")).append("\n");
            }
            
            return result.toString().trim();
                
        } catch (EmptyResultDataAccessException e) {
            return "❌ 未找到标题为 '" + taskTitle + "' 的任务。";
        } catch (Exception e) {
            log.error("❌ 查询任务详情失败: {}", e.getMessage(), e);
            return "❌ 查询任务详情时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询即将到期的任务（3天内到期且未完成）")
    public String getUpcomingTasks() {
        log.info("🤖 AI Workflow Tool: 查询即将到期的任务");
        
        try {
            String sql = "SELECT t.title, t.status, t.end_time, u.name as assignee_name " +
                         "FROM task t " +
                         "LEFT JOIN user u ON t.assignee_id = u.user_id " +
                         "WHERE t.end_time >= NOW() AND t.end_time <= DATE_ADD(NOW(), INTERVAL 3 DAY) " +
                         "AND t.status NOT IN ('COMPLETED', 'CANCELLED') " +
                         "ORDER BY t.end_time ASC";
            
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql);
            
            if (tasks.isEmpty()) {
                return "✅ 近3天内没有即将到期的任务，工作安排良好！";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("⚠️ 即将到期的任务（3天内，共 ").append(tasks.size()).append(" 个）：\n\n");
            
            for (int i = 0; i < tasks.size(); i++) {
                Map<String, Object> task = tasks.get(i);
                result.append(String.format("🔴 %d. **%s**\n", i + 1, task.get("title")));
                
                if (task.get("assignee_name") != null) {
                    result.append(String.format("   👤 负责人：%s\n", task.get("assignee_name")));
                }
                
                result.append(String.format("   ⏰ 截止：%s\n", task.get("end_time")));
                result.append(String.format("   📊 状态：%s\n\n", getTaskStatusText((String) task.get("status"))));
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("❌ 查询即将到期任务失败: {}", e.getMessage(), e);
            return "❌ 查询即将到期任务时出现系统错误，请稍后重试。";
        }
    }

    // ====================================================================================
    // 2. 工具辅助方法
    // ====================================================================================

    /**
     * 获取优先级图标
     */
    private String getPriorityIcon(String priority) {
        if (priority == null) return "📋";
        switch (priority.toUpperCase()) {
            case "HIGH": return "🔴";
            case "MEDIUM": return "🟡";
            case "LOW": return "🟢";
            default: return "📋";
        }
    }
    
    /**
     * 获取任务状态文本
     */
    private String getTaskStatusText(String status) {
        if (status == null) return "未知";
        switch (status.toUpperCase()) {
            case "TODO": return "待开始";
            case "IN_PROGRESS": return "进行中";
            case "URGENT": return "紧急";
            case "PENDING": return "等待中";
            case "PENDING_REVIEW": return "待审核";
            case "ON_HOLD": return "暂停";
            case "OVERDUE": return "逾期";
            case "COMPLETED": return "已完成";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
} 