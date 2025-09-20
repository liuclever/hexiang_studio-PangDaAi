package com.back_hexiang_studio.pangDaAi.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 工具分类服务 - 实现智能工具路由和分类存储
 * 
 * 🎯 核心目标：
 * - 减少RAG检索范围，提升响应速度
 * - 智能识别用户查询意图，定位到具体工具类别
 * - 支持多级分类和交叉分类检索
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-18
 */
@Service
@Slf4j
public class ToolClassificationService {

    /**
     * 工具分类枚举 - 基于AssistantAgent的38个工具方法
     */
    public enum ToolCategory {
        // 一级分类：按功能模块
        USER_MANAGEMENT("用户管理", "user", Arrays.asList(
            "用户", "成员", "档案", "添加用户", "删除用户", "修改用户", "用户信息", "权限"
        )),
        
        STUDIO_INFO("工作室信息", "studio", Arrays.asList(
            "工作室", "部门", "统计", "成员名单", "部门信息", "组织架构"
        )),
        
        NOTICE_MANAGEMENT("公告管理", "notice", Arrays.asList(
            "公告", "通知", "消息", "发布", "公告管理", "通知管理"
        )),
        
        ATTENDANCE_MANAGEMENT("考勤管理", "attendance", Arrays.asList(
            "考勤", "签到", "出勤", "考勤统计", "签到记录"
        )),
        
        COURSE_MANAGEMENT("课程管理", "course", Arrays.asList(
            "课程", "培训", "上课", "教学", "培训方向", "课程安排"
        )),
        
        TASK_MANAGEMENT("任务管理", "task", Arrays.asList(
            "任务", "作业", "项目", "待办", "未完成任务"
        )),
        
        MATERIAL_MANAGEMENT("资料管理", "material", Arrays.asList(
            "资料", "材料", "文档", "文件", "资料分类", "文档管理"
        )),
        
        AI_MODEL_MANAGEMENT("AI模型管理", "ai_model", Arrays.asList(
            "模型", "会话", "AI状态", "模型优化", "会话统计"
        )),
        
        DATA_PROCESSING("数据处理", "data", Arrays.asList(
            "表格", "转换", "格式化", "数据处理", "JSON"
        )),
        
        EXTERNAL_API("外部API", "external", Arrays.asList(
            "天气", "新闻", "时事", "预报", "今日新闻"
        ));
        
        private final String displayName;
        private final String code;
        private final List<String> keywords;
        
        ToolCategory(String displayName, String code, List<String> keywords) {
            this.displayName = displayName;
            this.code = code;
            this.keywords = keywords;
        }
        
        public String getDisplayName() { return displayName; }
        public String getCode() { return code; }
        public List<String> getKeywords() { return keywords; }
    }

    /**
     * 使用场景分类 - 二级分类维度
     */
    public enum UsageScenario {
        QUERY("查询类", Arrays.asList("查询", "获取", "查看", "显示", "统计")),
        MANAGEMENT("管理类", Arrays.asList("添加", "删除", "修改", "更新", "管理")),
        PERMISSION("权限类", Arrays.asList("权限", "检查", "验证", "确认")),
        PROCESSING("处理类", Arrays.asList("转换", "处理", "格式化", "分析"));
        
        private final String displayName;
        private final List<String> keywords;
        
        UsageScenario(String displayName, List<String> keywords) {
            this.displayName = displayName;
            this.keywords = keywords;
        }
        
        public String getDisplayName() { return displayName; }
        public List<String> getKeywords() { return keywords; }
    }

    /**
     * 🧠 智能工具路由 - 根据用户查询分析最可能的工具类别
     */
    public List<ToolClassificationResult> classifyUserQuery(String userQuery) {
        log.debug("🔍 开始工具分类分析: {}", userQuery);
        
        String normalizedQuery = userQuery.toLowerCase();
        List<ToolClassificationResult> results = new ArrayList<>();
        
        // 1. 功能模块匹配
        for (ToolCategory category : ToolCategory.values()) {
            double score = calculateCategoryScore(normalizedQuery, category);
            if (score > 0.1) { // 置信度阈值
                results.add(new ToolClassificationResult(category, null, score, "功能匹配"));
            }
        }
        
        // 2. 使用场景匹配
        for (ToolClassificationResult result : results) {
            UsageScenario bestScenario = findBestScenario(normalizedQuery);
            if (bestScenario != null) {
                result.setUsageScenario(bestScenario);
                result.setScore(result.getScore() * 1.2); // 场景匹配加权
            }
        }
        
        // 3. 特殊查询模式识别
        enhanceWithSpecialPatterns(normalizedQuery, results);
        
        // 4. 排序并返回
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        log.info("🎯 工具分类完成，匹配到{}个类别", results.size());
        for (ToolClassificationResult result : results) {
            log.debug("- {} (置信度: {:.2f})", result.getCategory().getDisplayName(), result.getScore());
        }
        
        return results;
    }

    /**
     * 计算类别匹配分数
     */
    private double calculateCategoryScore(String query, ToolCategory category) {
        double score = 0.0;
        int totalKeywords = category.getKeywords().size();
        int matchedKeywords = 0;
        
        for (String keyword : category.getKeywords()) {
            if (query.contains(keyword)) {
                matchedKeywords++;
                // 关键词长度权重
                score += keyword.length() > 2 ? 0.3 : 0.2;
            }
        }
        
        // 覆盖率加权
        double coverage = (double) matchedKeywords / totalKeywords;
        score += coverage * 0.5;
        
        return Math.min(score, 1.0);
    }

    /**
     * 查找最佳使用场景
     */
    private UsageScenario findBestScenario(String query) {
        double maxScore = 0.0;
        UsageScenario bestScenario = null;
        
        for (UsageScenario scenario : UsageScenario.values()) {
            double score = 0.0;
            for (String keyword : scenario.getKeywords()) {
                if (query.contains(keyword)) {
                    score += 0.3;
                }
            }
            
            if (score > maxScore) {
                maxScore = score;
                bestScenario = scenario;
            }
        }
        
        return maxScore > 0.2 ? bestScenario : null;
    }

    /**
     * 特殊查询模式增强
     */
    private void enhanceWithSpecialPatterns(String query, List<ToolClassificationResult> results) {
        // 身份查询模式
        if (Pattern.matches(".*[我你他她它].*", query)) {
            enhanceCategory(results, ToolCategory.USER_MANAGEMENT, 0.3, "身份查询");
        }
        
        // 统计查询模式
        if (query.contains("统计") || query.contains("多少") || query.contains("几个")) {
            enhanceCategory(results, ToolCategory.STUDIO_INFO, 0.2, "统计查询");
            enhanceCategory(results, ToolCategory.ATTENDANCE_MANAGEMENT, 0.2, "统计查询");
        }
        
        // 管理操作模式
        if (query.contains("添加") || query.contains("删除") || query.contains("修改")) {
            results.forEach(result -> {
                if (result.getUsageScenario() == UsageScenario.MANAGEMENT) {
                    result.setScore(result.getScore() * 1.5);
                }
            });
        }
        
        // 时间相关查询
        if (query.contains("今天") || query.contains("最新") || query.contains("最近")) {
            enhanceCategory(results, ToolCategory.NOTICE_MANAGEMENT, 0.2, "时间查询");
            enhanceCategory(results, ToolCategory.ATTENDANCE_MANAGEMENT, 0.2, "时间查询");
            enhanceCategory(results, ToolCategory.EXTERNAL_API, 0.3, "时间查询");
        }
    }

    /**
     * 增强特定类别得分
     */
    private void enhanceCategory(List<ToolClassificationResult> results, ToolCategory targetCategory, 
                                double bonus, String reason) {
        for (ToolClassificationResult result : results) {
            if (result.getCategory() == targetCategory) {
                result.setScore(result.getScore() + bonus);
                result.setReason(result.getReason() + "+" + reason);
                return;
            }
        }
        
        // 如果没找到，添加新的结果
        results.add(new ToolClassificationResult(targetCategory, null, bonus, reason));
    }

    /**
     * 🎯 生成分类检索策略
     */
    public RetrievalStrategy generateRetrievalStrategy(String userQuery) {
        List<ToolClassificationResult> classifications = classifyUserQuery(userQuery);
        
        RetrievalStrategy strategy = new RetrievalStrategy();
        strategy.setOriginalQuery(userQuery);
        
        if (classifications.isEmpty()) {
            // 兜底策略：全局检索
            strategy.setStrategyType("GLOBAL");
            strategy.setPrioritizedCategories(Arrays.asList(ToolCategory.values()));
            log.info("🌐 使用全局检索策略");
        } else if (classifications.size() == 1) {
            // 单类别检索
            strategy.setStrategyType("SINGLE_CATEGORY");
            strategy.setPrioritizedCategories(Collections.singletonList(classifications.get(0).getCategory()));
            log.info("🎯 使用单类别检索策略: {}", classifications.get(0).getCategory().getDisplayName());
        } else {
            // 多类别优先检索
            strategy.setStrategyType("MULTI_CATEGORY");
            strategy.setPrioritizedCategories(
                classifications.stream()
                    .filter(r -> r.getScore() > 0.3) // 高置信度类别
                    .map(ToolClassificationResult::getCategory)
                    .collect(java.util.stream.Collectors.toList())
            );
            log.info("🔄 使用多类别检索策略，优先级类别: {}", 
                strategy.getPrioritizedCategories().size());
        }
        
        return strategy;
    }

    /**
     * 工具分类结果
     */
    public static class ToolClassificationResult {
        private ToolCategory category;
        private UsageScenario usageScenario;
        private double score;
        private String reason;
        
        public ToolClassificationResult(ToolCategory category, UsageScenario usageScenario, 
                                      double score, String reason) {
            this.category = category;
            this.usageScenario = usageScenario;
            this.score = score;
            this.reason = reason;
        }
        
        // Getters and Setters
        public ToolCategory getCategory() { return category; }
        public UsageScenario getUsageScenario() { return usageScenario; }
        public double getScore() { return score; }
        public String getReason() { return reason; }
        
        public void setUsageScenario(UsageScenario usageScenario) { this.usageScenario = usageScenario; }
        public void setScore(double score) { this.score = score; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 检索策略
     */
    public static class RetrievalStrategy {
        private String originalQuery;
        private String strategyType;
        private List<ToolCategory> prioritizedCategories;
        
        // Getters and Setters
        public String getOriginalQuery() { return originalQuery; }
        public String getStrategyType() { return strategyType; }
        public List<ToolCategory> getPrioritizedCategories() { return prioritizedCategories; }
        
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
        public void setPrioritizedCategories(List<ToolCategory> prioritizedCategories) { 
            this.prioritizedCategories = prioritizedCategories; 
        }
    }
} 