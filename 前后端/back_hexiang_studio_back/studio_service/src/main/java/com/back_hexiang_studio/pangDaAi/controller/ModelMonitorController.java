package com.back_hexiang_studio.pangDaAi.controller;

import com.back_hexiang_studio.pangDaAi.service.ModelRouterService;
import com.back_hexiang_studio.pangDaAi.service.ModelRouterService.AIModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI模型监控管理后台控制器
 */
@RestController
@RequestMapping("/api/model-monitor")
@Slf4j
public class ModelMonitorController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ModelRouterService modelRouterService;

    /**
     * 获取模型使用统计概览
     */
    @GetMapping("/overview")
    public Map<String, Object> getModelUsageOverview(
            @RequestParam(defaultValue = "7") int days) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 总体统计
            String overviewSql = "SELECT " +
                "COUNT(*) as total_requests, " +
                "COUNT(DISTINCT user_id) as unique_users, " +
                "SUM(cost) as total_cost, " +
                "AVG(response_time) as avg_response_time, " +
                "AVG(success) * 100 as success_rate " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)";
                
            Map<String, Object> overview = jdbcTemplate.queryForMap(overviewSql, days);
            result.put("overview", overview);
            
            // 各模型使用分布
            String modelDistSql = "SELECT model_name, " +
                "COUNT(*) as request_count, " +
                "SUM(cost) as total_cost, " +
                "AVG(response_time) as avg_response_time, " +
                "AVG(success) * 100 as success_rate " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY model_name " +
                "ORDER BY request_count DESC";
                
            List<Map<String, Object>> modelDistribution = jdbcTemplate.queryForList(modelDistSql, days);
            result.put("modelDistribution", modelDistribution);
            
            // 问题类型分布
            String questionTypeSql = "SELECT question_type, " +
                "COUNT(*) as count, " +
                "GROUP_CONCAT(DISTINCT selected_model) as models_used " +
                "FROM model_selection_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY question_type " +
                "ORDER BY count DESC";
                
            List<Map<String, Object>> questionTypeStats = jdbcTemplate.queryForList(questionTypeSql, days);
            result.put("questionTypeDistribution", questionTypeStats);
            
            // 每日成本趋势
            String costTrendSql = "SELECT DATE(create_time) as date, " +
                "SUM(cost) as daily_cost, " +
                "COUNT(*) as daily_requests, " +
                "COUNT(DISTINCT user_id) as daily_users " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "GROUP BY DATE(create_time) " +
                "ORDER BY date DESC";
                
            List<Map<String, Object>> costTrend = jdbcTemplate.queryForList(costTrendSql, days);
            result.put("costTrend", costTrend);
            
            result.put("success", true);
            result.put("message", "获取模型使用统计成功");
            
        } catch (Exception e) {
            log.error("获取模型使用统计失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取统计数据失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取用户模型使用排行榜
     */
    @GetMapping("/user-ranking")
    public Map<String, Object> getUserModelRanking(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 用户使用量排行
            String usageRankSql = "SELECT u.name as user_name, " +
                "COUNT(*) as request_count, " +
                "SUM(mul.cost) as total_cost, " +
                "AVG(mul.response_time) as avg_response_time, " +
                "GROUP_CONCAT(DISTINCT mul.model_name) as models_used " +
                "FROM model_usage_log mul " +
                "LEFT JOIN user u ON mul.user_id = u.user_id " +
                "WHERE mul.create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY mul.user_id, u.name " +
                "ORDER BY request_count DESC " +
                "LIMIT ?";
                
            List<Map<String, Object>> userRanking = jdbcTemplate.queryForList(usageRankSql, days, limit);
            result.put("userRanking", userRanking);
            
            // 成本消耗排行
            String costRankSql = "SELECT u.name as user_name, " +
                "SUM(mul.cost) as total_cost, " +
                "COUNT(*) as request_count, " +
                "ROUND(SUM(mul.cost) / COUNT(*), 4) as avg_cost_per_request " +
                "FROM model_usage_log mul " +
                "LEFT JOIN user u ON mul.user_id = u.user_id " +
                "WHERE mul.create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY mul.user_id, u.name " +
                "ORDER BY total_cost DESC " +
                "LIMIT ?";
                
            List<Map<String, Object>> costRanking = jdbcTemplate.queryForList(costRankSql, days, limit);
            result.put("costRanking", costRanking);
            
            result.put("success", true);
            result.put("message", "获取用户排行榜成功");
            
        } catch (Exception e) {
            log.error("获取用户排行榜失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取排行榜数据失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取实时模型状态
     */
    @GetMapping("/real-time-status")
    public Map<String, Object> getRealTimeStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 最近1小时的模型使用情况
            String realtimeSql = "SELECT model_name, " +
                "COUNT(*) as recent_requests, " +
                "AVG(response_time) as avg_response_time, " +
                "AVG(success) * 100 as success_rate, " +
                "MAX(create_time) as last_used " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
                "GROUP BY model_name " +
                "ORDER BY recent_requests DESC";
                
            List<Map<String, Object>> realtimeStats = jdbcTemplate.queryForList(realtimeSql);
            result.put("realtimeStats", realtimeStats);
            
            // 系统健康状况
            String healthSql = "SELECT " +
                "COUNT(*) as total_requests_last_hour, " +
                "AVG(success) * 100 as overall_success_rate, " +
                "AVG(response_time) as avg_response_time, " +
                "COUNT(DISTINCT user_id) as active_users " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)";
                
            Map<String, Object> systemHealth = jdbcTemplate.queryForMap(healthSql);
            result.put("systemHealth", systemHealth);
            
            // 模型配置信息
            List<Map<String, Object>> modelConfigs = new ArrayList<>();
            List<AIModel> availableModels = modelRouterService.getAvailableModels();
            for (AIModel model : availableModels) {
                Map<String, Object> config = new HashMap<>();
                config.put("modelName", model.getModelName());
                config.put("description", model.getDescription());
                config.put("costRate", model.getCostRate());
                config.put("maxTokens", model.getMaxTokens());
                modelConfigs.add(config);
            }
            result.put("modelConfigs", modelConfigs);
            
            result.put("success", true);
            result.put("message", "获取实时状态成功");
            result.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("获取实时状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取实时状态失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取成本预警信息
     */
    @GetMapping("/cost-alerts")
    public Map<String, Object> getCostAlerts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 今日高成本用户
            String highCostUsersSql = "SELECT u.name as user_name, " +
                "SUM(mul.cost) as today_cost, " +
                "COUNT(*) as today_requests, " +
                "ROUND(SUM(mul.cost) / COUNT(*), 4) as avg_cost_per_request " +
                "FROM model_usage_log mul " +
                "LEFT JOIN user u ON mul.user_id = u.user_id " +
                "WHERE DATE(mul.create_time) = CURDATE() " +
                "GROUP BY mul.user_id, u.name " +
                "HAVING today_cost > 10.0 " +  // 成本超过10的用户
                "ORDER BY today_cost DESC " +
                "LIMIT 20";
                
            List<Map<String, Object>> highCostUsers = jdbcTemplate.queryForList(highCostUsersSql);
            result.put("highCostUsers", highCostUsers);
            
            // 异常使用模式检测
            String anomalyDetectionSql = "SELECT u.name as user_name, " +
                "mul.model_name, " +
                "COUNT(*) as usage_count, " +
                "AVG(mul.response_time) as avg_response_time " +
                "FROM model_usage_log mul " +
                "LEFT JOIN user u ON mul.user_id = u.user_id " +
                "WHERE mul.create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
                "GROUP BY mul.user_id, mul.model_name, u.name " +
                "HAVING usage_count > 50 " +  // 1小时内使用超过50次
                "ORDER BY usage_count DESC " +
                "LIMIT 10";
                
            List<Map<String, Object>> anomalousUsage = jdbcTemplate.queryForList(anomalyDetectionSql);
            result.put("anomalousUsage", anomalousUsage);
            
            // 模型失败率预警
            String failureRateSql = "SELECT model_name, " +
                "COUNT(*) as total_requests, " +
                "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_requests, " +
                "ROUND((SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) as failure_rate " +
                "FROM model_usage_log " +
                "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 2 HOUR) " +
                "GROUP BY model_name " +
                "HAVING failure_rate > 5.0 " +  // 失败率超过5%
                "ORDER BY failure_rate DESC";
                
            List<Map<String, Object>> highFailureRateModels = jdbcTemplate.queryForList(failureRateSql);
            result.put("highFailureRateModels", highFailureRateModels);
            
            result.put("success", true);
            result.put("message", "获取成本预警信息成功");
            
        } catch (Exception e) {
            log.error("获取成本预警信息失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取预警信息失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取用户个人统计
     */
    @GetMapping("/user-stats/{userId}")
    public Map<String, Object> getUserPersonalStats(@PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 用户个人使用统计
            String personalStatsSql = "SELECT " +
                "COUNT(*) as total_requests, " +
                "SUM(cost) as total_cost, " +
                "AVG(cost) as avg_cost_per_request, " +
                "AVG(response_time) as avg_response_time, " +
                "AVG(success) * 100 as success_rate, " +
                "COUNT(DISTINCT model_name) as models_used " +
                "FROM model_usage_log " +
                "WHERE user_id = ? AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)";
                
            Map<String, Object> personalStats = jdbcTemplate.queryForMap(personalStatsSql, userId, days);
            result.put("personalStats", personalStats);
            
            // 用户模型使用偏好
            String modelPreferenceSql = "SELECT model_name, " +
                "COUNT(*) as usage_count, " +
                "SUM(cost) as total_cost, " +
                "AVG(response_time) as avg_response_time " +
                "FROM model_usage_log " +
                "WHERE user_id = ? AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY model_name " +
                "ORDER BY usage_count DESC";
                
            List<Map<String, Object>> modelPreference = jdbcTemplate.queryForList(modelPreferenceSql, userId, days);
            result.put("modelPreference", modelPreference);
            
            // 用户问题类型分布
            String questionTypeStatsSql = "SELECT question_type, " +
                "COUNT(*) as count, " +
                "GROUP_CONCAT(DISTINCT selected_model) as models_used " +
                "FROM model_selection_log " +
                "WHERE user_id = ? AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY question_type " +
                "ORDER BY count DESC";
                
            List<Map<String, Object>> questionTypeStats = jdbcTemplate.queryForList(questionTypeStatsSql, userId, days);
            result.put("questionTypeStats", questionTypeStats);
            
            result.put("success", true);
            result.put("message", "获取用户个人统计成功");
            
        } catch (Exception e) {
            log.error("获取用户个人统计失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取个人统计失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 手动触发模型选择测试
     */
    @PostMapping("/test-selection")
    public Map<String, Object> testModelSelection(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String message = (String) request.get("message");
            Long userId = Long.valueOf(request.get("userId").toString());
            String sessionId = (String) request.getOrDefault("sessionId", "test-session");
            
            AIModel selectedModel = modelRouterService.selectModel(message, userId, sessionId);
            
            result.put("success", true);
            result.put("message", "模型选择测试完成");
            result.put("selectedModel", selectedModel.getModelName());
            result.put("modelDescription", selectedModel.getDescription());
            result.put("costRate", selectedModel.getCostRate());
            result.put("maxTokens", selectedModel.getMaxTokens());
            
        } catch (Exception e) {
            log.error("模型选择测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "模型选择测试失败: " + e.getMessage());
        }
        
        return result;
    }
}