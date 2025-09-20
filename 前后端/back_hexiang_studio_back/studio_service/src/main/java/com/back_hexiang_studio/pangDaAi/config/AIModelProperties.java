package com.back_hexiang_studio.pangDaAi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

/**
 * AI模型配置属性类
 * 从application.yml的ai配置中读取模型相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIModelProperties {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 模型配置
     */
    private Map<String, ModelConfig> models;

    /**
     * 问题类型配置
     */
    private Map<String, QuestionTypeConfig> questionTypes;

    /**
     * 智能选择规则
     */
    private SelectionRules selectionRules;

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring;

    /**
     * 单个模型配置
     */
    @Data
    public static class ModelConfig {
        private String name;
        private String displayName;
        private String description;
        private Double costRate;
        private Integer maxTokens;
        private Integer maxInputTokens;
        private String category;
        private List<String> features;
        private List<String> usageScenarios;
    }

    /**
     * 问题类型配置
     */
    @Data
    public static class QuestionTypeConfig {
        private String description;
        private String defaultModel;
        private List<String> keywords;
        private Integer priority;
    }

    /**
     * 智能选择规则配置
     */
    @Data
    public static class SelectionRules {
        private LengthThresholds lengthThresholds;
        private ComplexityThresholds complexityThresholds;
        private CostControl costControl;
        private Performance performance;
    }

    /**
     * 长度阈值配置
     */
    @Data
    public static class LengthThresholds {
        private Integer longContent;
        private Integer mediumContent;
        private Integer shortContent;
    }

    /**
     * 复杂度阈值配置
     */
    @Data
    public static class ComplexityThresholds {
        private Integer veryHigh;
        private Integer high;
        private Integer medium;
        private Integer low;
    }

    /**
     * 成本控制配置
     */
    @Data
    public static class CostControl {
        private Double highCostThreshold;
        private Double dailyCostLimit;
        private String emergencyModel;
    }

    /**
     * 性能配置
     */
    @Data
    public static class Performance {
        private Integer responseTimeLimit;
        private Double failureRateThreshold;
        private String fallbackModel;
    }

    /**
     * 监控配置
     */
    @Data
    public static class MonitoringConfig {
        private StatsPeriods statsPeriods;
        private Alerts alerts;
        private AutoOptimization autoOptimization;
    }

    /**
     * 统计周期配置
     */
    @Data
    public static class StatsPeriods {
        private Integer realtime;
        private Integer daily;
        private Integer weekly;
        private Integer monthly;
    }

    /**
     * 预警配置
     */
    @Data
    public static class Alerts {
        private Double highCostUserThreshold;
        private Integer anomalousUsageThreshold;
        private Double failureRateThreshold;
        private Integer responseTimeThreshold;
    }

    /**
     * 自动优化配置
     */
    @Data
    public static class AutoOptimization {
        private Boolean enabled;
        private Double modelSwitchThreshold;
        private Boolean costOptimizationEnabled;
        private Boolean performanceOptimizationEnabled;
    }

    /**
     * 根据模型名称获取模型配置
     */
    public ModelConfig getModelConfig(String modelName) {
        return models != null ? models.get(modelName) : null;
    }

    /**
     * 根据问题类型获取配置
     */
    public QuestionTypeConfig getQuestionTypeConfig(String questionType) {
        return questionTypes != null ? questionTypes.get(questionType) : null;
    }

    /**
     * 获取所有模型名称列表
     */
    public List<String> getAllModelNames() {
        return models != null ? new ArrayList<>(models.keySet()) : Collections.emptyList();
    }

    /**
     * 检查模型是否存在
     */
    public boolean isModelExists(String modelName) {
        return models != null && models.containsKey(modelName);
    }
} 