package com.back_hexiang_studio.pangDaAi.tool.workflow;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.back_hexiang_studio.pangDaAi.service.ModelRouterService;
import com.back_hexiang_studio.pangDaAi.service.PermissionService;
import com.back_hexiang_studio.context.UserContextHolder;

import java.util.Map;

/**
 *   AI模型管理工作流工具
 * 提供模型切换、会话状态监控、统计分析等智能管理功能
 * 
 * @author Hexiang Studio
 * @version 1.0 - Workflow架构
 */
@Service
@Slf4j
public class ModelManagementTools {
    
    @Autowired
    private ModelRouterService modelRouterService;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * 查看当前会话的AI模型使用状态
     */
    @Tool("查看当前会话的AI模型使用状态和统计信息，了解正在使用的模型类型、对话轮次等详细信息")
    public String getCurrentSessionStatus(
        @P("会话ID (可选，留空则使用当前会话)") String sessionId,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查看会话模型状态 - 会话ID: {}, 用户ID: {}", sessionId, currentUserId);
        
        // 如果没有提供sessionId，使用默认当前会话
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "current";
        }
        
        try {
            Map<String, Object> sessionInfo = modelRouterService.getSessionInfo(sessionId);
            
            if ("no_session_found".equals(sessionInfo.get("status"))) {
                return "  当前会话还没有模型选择记录，可能是新的对话会话。\n\n" +
                       "  提示：AI会根据您的问题类型自动选择最适合的模型进行回答。";
            }
            
            StringBuilder status = new StringBuilder();
            status.append("  当前会话AI模型状态报告：\n\n");
            status.append("  正在使用的模型：").append(sessionInfo.get("currentModel")).append("\n");
            status.append("  当前问题类型：").append(getQuestionTypeDescription((String) sessionInfo.get("questionType"))).append("\n");
            status.append("  对话轮次：").append(sessionInfo.get("turnCount")).append("\n");
            
            boolean isLocked = (Boolean) sessionInfo.get("modelLocked");
            if (isLocked) {
                status.append("  模型状态：已锁定（").append(sessionInfo.get("lockReason")).append("）\n");
                status.append("  说明：模型已锁定为当前类型，确保对话的连贯性。\n");
            } else {
                status.append("  模型状态：智能切换模式\n");
                status.append("  说明：AI会根据您的问题自动选择最佳模型。\n");
            }
            
            status.append( " 最近问题类型：").append(sessionInfo.get("recentQuestionTypes")).append("\n");
            status.append("  最后交互时间：").append(sessionInfo.get("lastInteraction")).append("\n\n");
            status.append("  您可以继续提问，AI会自动选择最适合的模型为您服务！");
            
            return status.toString();
            
        } catch (Exception e) {
            log.error("  查看会话模型状态失败: {}", e.getMessage(), e);
            return "  查看会话状态时发生错误，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    /**
     * 查看系统AI模型使用统计（管理员功能）
     */
    @Tool("查看系统所有AI模型的使用统计信息，包括活跃会话数量、模型分布等（需要管理员权限）")
    public String getSystemModelStatistics(
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 查看系统模型统计 - 用户ID: {}", currentUserId);
        
        // 检查管理员权限
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        
        try {
            Map<String, Object> stats = modelRouterService.getSessionStatistics();
            
            StringBuilder report = new StringBuilder();
            report.append("  AI模型使用统计报告\n");
            report.append("=====================================\n\n");
            
            report.append("  活跃会话总数：").append(stats.get("totalActiveSessions")).append("\n");
            report.append("  锁定会话数量：").append(stats.get("lockedSessions")).append("\n\n");
            
            // 模型使用情况
            @SuppressWarnings("unchecked")
            Map<String, Integer> modelUsage = (Map<String, Integer>) stats.get("modelUsage");
            if (!modelUsage.isEmpty()) {
                report.append("  AI模型使用分布：\n");
                report.append("─────────────────────────────────────\n");
                modelUsage.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(entry -> 
                            report.append("    ").append(entry.getKey())
                                  .append("：").append(entry.getValue()).append(" 个会话\n"));
                report.append("\n");
            }
            
            // 问题类型分布
            @SuppressWarnings("unchecked")
            Map<String, Integer> questionTypeUsage = (Map<String, Integer>) stats.get("questionTypeUsage");
            if (!questionTypeUsage.isEmpty()) {
                report.append("  用户问题类型分布：\n");
                report.append("─────────────────────────────────────\n");
                questionTypeUsage.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(entry -> 
                            report.append("    ").append(getQuestionTypeDescription(entry.getKey()))
                                  .append("：").append(entry.getValue()).append(" 个会话\n"));
                report.append("\n");
            }
            
            report.append("  统计报告生成完成！");
            return report.toString();
            
        } catch (Exception e) {
            log.error("  获取系统模型统计失败: {}", e.getMessage(), e);
            return "  获取统计信息时发生错误，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    /**
     * 重置会话AI模型状态（管理员功能）
     */
    @Tool("重置指定会话的AI模型选择状态，清除模型锁定和历史记录（需要管理员权限）")
    public String resetSessionModelState(
        @P("要重置的会话ID") String sessionId,
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 重置会话模型状态 - 会话ID: {}, 操作者: {}", sessionId, currentUserId);
        
        // 检查管理员权限
        if (!permissionService.canManageUsers(currentUserId)) {
            return permissionService.getUserManagementPermissionInfo(currentUserId);
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return "  参数错误：请提供有效的会话ID。\n\n" +
                   "  提示：您可以先使用'查看系统模型统计'工具获取活跃会话列表。";
        }
        
        try {
            // 获取重置前的状态
            Map<String, Object> beforeReset = modelRouterService.getSessionInfo(sessionId);
            
            // 执行重置操作
            modelRouterService.resetSession(sessionId);
            
            if ("no_session_found".equals(beforeReset.get("status"))) {
                return "  会话 '" + sessionId + "' 不存在或已过期，无需重置。\n\n" +
                       "  可能原因：\n" +
                       "• 会话ID输入错误\n" +
                       "• 会话已经自动过期清理\n" +
                       "• 会话从未被创建";
            }
            
            return String.format("  会话模型状态重置成功！\n\n" +
                               "  重置前状态：\n" +
                               "─────────────────────────────────────\n" +
                               "  使用模型：%s\n" +
                               "  问题类型：%s\n" +
                               "  对话轮次：%s\n" +
                               "  锁定状态：%s\n\n" +
                               "  该会话下次交互时将重新进行智能模型选择。",
                               beforeReset.get("currentModel"),
                               getQuestionTypeDescription((String) beforeReset.get("questionType")),
                               beforeReset.get("turnCount"),
                               beforeReset.get("modelLocked"));
            
        } catch (Exception e) {
            log.error("  重置会话模型状态失败: {}", e.getMessage(), e);
            return "  重置会话状态时发生错误，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    /**
     * 获取AI模型使用优化建议
     */
    @Tool("获取AI模型使用的优化建议和性能分析，帮助了解系统运行状况")
    public String getModelOptimizationAdvice(
        @P("当前用户的ID，这个ID由系统在后台自动提供，AI需要直接传递它") Long currentUserId
    ) {
        log.info("  AI Workflow Tool: 获取模型优化建议 - 用户ID: {}", currentUserId);
        
        try {
            Map<String, Object> stats = modelRouterService.getSessionStatistics();
            
            StringBuilder advice = new StringBuilder();
            advice.append("  AI模型使用优化建议报告\n");
            advice.append("=====================================\n\n");
            
            int totalSessions = (Integer) stats.get("totalActiveSessions");
            int lockedSessions = (Integer) stats.get("lockedSessions");
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> modelUsage = (Map<String, Integer>) stats.get("modelUsage");
            
            // 基于统计数据提供智能建议
            if (totalSessions == 0) {
                advice.append("  系统当前没有活跃会话，运行状态良好。\n\n");
                advice.append("  建议：\n");
                advice.append("• 系统处于空闲状态，这是正常现象\n");
                advice.append("• 可以进行系统维护或更新操作\n");
                advice.append("• 检查日志确保所有服务正常运行");
                return advice.toString();
            }
            
            // 会话锁定情况分析
            double lockRatio = (double) lockedSessions / totalSessions;
            advice.append("  会话锁定分析：\n");
            if (lockRatio > 0.8) {
                advice.append("   高锁定率（").append(String.format("%.1f%%", lockRatio * 100))
                      .append("）- 用户在进行深度连续对话\n");
                advice.append("    这表明模型切换机制工作正常，用户体验良好\n\n");
            } else if (lockRatio < 0.2) {
                advice.append("    低锁定率（").append(String.format("%.1f%%", lockRatio * 100))
                      .append("）- 模型保持灵活切换\n");
                advice.append("    系统能够根据用户需求动态调整模型\n\n");
            } else {
                advice.append("    正常锁定率（").append(String.format("%.1f%%", lockRatio * 100))
                      .append("）- 锁定与切换平衡良好\n\n");
            }
            
            // 模型使用分布分析
            if (!modelUsage.isEmpty()) {
                String mostUsedModel = modelUsage.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("unknown");
                
                int mostUsedCount = modelUsage.get(mostUsedModel);
                double usage = (double) mostUsedCount / totalSessions;
                
                advice.append("  模型使用分析：\n");
                if (usage > 0.7) {
                    advice.append("   ").append(mostUsedModel).append(" 模型使用率较高（")
                          .append(String.format("%.1f%%", usage * 100))
                          .append("）\n");
                    advice.append("    建议监控该模型的负载和响应时间\n\n");
                } else {
                    advice.append("    模型使用分布较为均衡\n");
                    advice.append("    负载分散良好，系统运行稳定\n\n");
                }
            }
            
            // 系统优化建议
            advice.append("  系统优化建议：\n");
            advice.append("─────────────────────────────────────\n");
            advice.append("  性能优化：\n");
            advice.append("  • 定期清理过期会话释放内存\n");
            advice.append("  • 监控各模型的响应时间和成功率\n");
            advice.append("  • 在高峰期考虑增加模型实例\n\n");
            advice.append("  数据分析：\n");
            advice.append("  • 根据用户问题类型优化模型配置\n");
            advice.append("  • 分析用户行为模式调整切换策略\n");
            advice.append("  • 收集用户反馈持续改进\n\n");
            advice.append("⚡ 当前系统总体运行状况：良好  ");
            
            return advice.toString();
            
        } catch (Exception e) {
            log.error("  获取模型优化建议失败: {}", e.getMessage(), e);
            return "  获取优化建议时发生错误，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    /**
     * 获取问题类型的中文描述
     */
    private String getQuestionTypeDescription(String questionType) {
        if (questionType == null) return "未知类型";
        
        switch (questionType) {
            case "CASUAL": return " 日常对话";
            case "STUDIO_QUERY": return " 工作室查询";
            case "STUDIO_MANAGEMENT": return " 工作室管理";
            case "COMPLEX_ANALYSIS": return " 复杂分析";
            case "LONG_CONTENT": return   "长内容生成";
            case "CODE_GENERATION": return " 代码生成";
            default: return  questionType;
        }
    }
} 