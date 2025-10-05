package com.back_hexiang_studio.pangDaAi.service;

import com.back_hexiang_studio.pangDaAi.config.AIModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 智能AI模型路由服务
 * 根据问题类型、复杂度、长度等因素智能选择最适合的AI模型
 * 优化版本：支持会话粘性和多轮对话连续性
 */
@Service
@Slf4j
public class ModelRouterService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private AIModelProperties aiModelProperties;
    
    // 关键词匹配规则缓存
    private Map<Pattern, String> keywordPatterns;
    
    // 会话状态缓存
    private Map<String, SessionState> sessionStateCache = new HashMap<>();
    
    /**
     * 会话状态类
     */
    public static class SessionState {
        private String currentModel;
        private String currentQuestionType;
        private LocalDateTime lastInteraction;
        private int turnCount;
        private boolean modelLocked;
        private String lockReason;
        private List<String> recentQuestionTypes = new ArrayList<>();
        
        public SessionState(String model, String questionType) {
            this.currentModel = model;
            this.currentQuestionType = questionType;
            this.lastInteraction = LocalDateTime.now();
            this.turnCount = 1;
            this.modelLocked = false;
            this.recentQuestionTypes.add(questionType);
        }
        
        // getters and setters
        public String getCurrentModel() { return currentModel; }
        public void setCurrentModel(String currentModel) { this.currentModel = currentModel; }
        public String getCurrentQuestionType() { return currentQuestionType; }
        public void setCurrentQuestionType(String currentQuestionType) { 
            this.currentQuestionType = currentQuestionType;
            this.recentQuestionTypes.add(currentQuestionType);
            // 保持最近5个问题类型
            if (this.recentQuestionTypes.size() > 5) {
                this.recentQuestionTypes.remove(0);
            }
        }
        public LocalDateTime getLastInteraction() { return lastInteraction; }
        public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
        public int getTurnCount() { return turnCount; }
        public void incrementTurnCount() { this.turnCount++; }
        public boolean isModelLocked() { return modelLocked; }
        public void lockModel(String reason) { 
            this.modelLocked = true; 
            this.lockReason = reason;
        }
        public void unlockModel() { 
            this.modelLocked = false; 
            this.lockReason = null;
        }
        public String getLockReason() { return lockReason; }
        public List<String> getRecentQuestionTypes() { return new ArrayList<>(recentQuestionTypes); }
    }
    
    /**
     * AI模型信息类
     */
    public static class AIModel {
        private final String modelName;
        private final String displayName;
        private final String description;
        private final double costRate;
        private final int maxTokens;
        private final String category;
        
        public AIModel(String modelName, AIModelProperties.ModelConfig config) {
            this.modelName = modelName;
            this.displayName = config != null ? config.getDisplayName() : modelName;
            this.description = config != null ? config.getDescription() : "";
            this.costRate = config != null && config.getCostRate() != null ? config.getCostRate() : 1.0;
            this.maxTokens = config != null && config.getMaxTokens() != null ? config.getMaxTokens() : 4000;
            this.category = config != null ? config.getCategory() : "GENERAL";
        }
        
        public String getModelName() { return modelName; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getCostRate() { return costRate; }
        public int getMaxTokens() { return maxTokens; }
        public String getCategory() { return category; }
    }
    
    /**
     * 智能选择AI模型 - 优化版本
     * 支持会话粘性，避免频繁切换模型
     */
    public AIModel selectModel(String userMessage, Long userId, String sessionId) {
        try {
            log.info(" 智能模型选择开始 - 用户ID: {}, 会话: {}", userId, sessionId);
            
            // 1. 获取或创建会话状态
            SessionState sessionState = getSessionState(sessionId);
            
            // 2. 清理过期会话状态
            cleanExpiredSessions();
            
            // 3. 分析当前问题
            String currentQuestionType = classifyQuestion(userMessage);
            int messageLength = userMessage.length();
            int contextComplexity = analyzeContextComplexity(userMessage, userId, sessionId);
            
            log.debug("  消息长度: {} 字符,   问题类型: {},  复杂度: {}", 
                     messageLength, currentQuestionType, contextComplexity);
            
            // 4. 判断是否需要切换模型
            String selectedModelName = selectModelWithStickiness(
                sessionState, currentQuestionType, messageLength, contextComplexity, userMessage
            );
            
            // 5. 更新会话状态
            updateSessionState(sessionState, selectedModelName, currentQuestionType);
            
            // 6. 创建模型实例
            AIModel selectedModel = createAIModel(selectedModelName);
            
            // 7. 记录选择结果
            recordModelSelection(userId, sessionId, userMessage, selectedModel, currentQuestionType, sessionState);
            
            log.info("  选择模型: {} ({}) - 会话轮次: {}, 模型锁定: {}", 
                    selectedModel.getModelName(), selectedModel.getDescription(),
                    sessionState.getTurnCount(), sessionState.isModelLocked());
                    
            return selectedModel;
            
        } catch (Exception e) {
            log.error("  模型选择失败，使用默认模型: {}", e.getMessage(), e);
            return createAIModel("qwen-plus"); // 默认模型
        }
    }
    
    /**
     * 获取或创建会话状态
     */
    private SessionState getSessionState(String sessionId) {
        SessionState state = sessionStateCache.get(sessionId);
        
        if (state == null || isSessionExpired(state)) {
            log.debug("  创建新的会话状态: {}", sessionId);
            // 创建新会话状态，使用默认模型
            state = new SessionState("qwen-plus", "CASUAL");
            sessionStateCache.put(sessionId, state);
        }
        
        return state;
    }
    
    /**
     * 检查会话是否过期（30分钟无活动）
     */
    private boolean isSessionExpired(SessionState state) {
        return state.getLastInteraction().isBefore(LocalDateTime.now().minusMinutes(30));
    }
    
    /**
     * 清理过期会话状态
     */
    private void cleanExpiredSessions() {
        Iterator<Map.Entry<String, SessionState>> iterator = sessionStateCache.entrySet().iterator();
        int cleanedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, SessionState> entry = iterator.next();
            if (isSessionExpired(entry.getValue())) {
                iterator.remove();
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            log.debug("  清理过期会话: {} 个", cleanedCount);
        }
    }
    
    /**
     * 带粘性的模型选择
     */
    private String selectModelWithStickiness(SessionState sessionState, String questionType, 
                                           int messageLength, int contextComplexity, String userMessage) {
        
        // 1. 如果模型被锁定，继续使用当前模型
        if (sessionState.isModelLocked()) {
            log.debug("  模型已锁定: {} (原因: {})", sessionState.getCurrentModel(), sessionState.getLockReason());
            return sessionState.getCurrentModel();
        }
        
        // 2. 检查是否需要强制切换模型
        String forceModel = checkForceModelSwitch(questionType, messageLength, contextComplexity);
        if (forceModel != null) {
            log.info(" 强制切换模型: {} → {}", sessionState.getCurrentModel(), forceModel);
            return forceModel;
        }
        
        // 3. 判断问题类型是否发生显著变化
        boolean shouldSwitchModel = shouldSwitchModel(sessionState, questionType, messageLength, contextComplexity, userMessage);
        
        if (!shouldSwitchModel) {
            // 保持当前模型
            log.debug(" 保持当前模型: {} (会话粘性)", sessionState.getCurrentModel());
            return sessionState.getCurrentModel();
        }
        
        // 4. 需要切换时，选择新模型
        String newModel = intelligentModelSelection(questionType, messageLength, contextComplexity);
        log.info(" 切换模型: {} → {} (问题类型: {})",
                sessionState.getCurrentModel(), newModel, questionType);
        
        return newModel;
    }
    
    /**
     * 检查是否需要强制切换模型
     */
    private String checkForceModelSwitch(String questionType, int messageLength, int contextComplexity) {
        // 超长内容或极高复杂度，强制使用最强模型
        if (messageLength > 3000 || contextComplexity >= 9) {
            return "qwen3-max-preview";
        }
        
        // 代码生成任务，强制使用代码模型
        if ("CODE_GENERATION".equals(questionType)) {
            return "qwen3-coder-plus-2025-07-22";
        }
        
        return null;
    }
    
    /**
     * 判断是否应该切换模型
     */
    private boolean shouldSwitchModel(SessionState sessionState, String questionType, 
                                    int messageLength, int contextComplexity, String userMessage) {
        
        // 1. 新会话或前3轮对话，允许切换
        if (sessionState.getTurnCount() <= 3) {
            return !questionType.equals(sessionState.getCurrentQuestionType());
        }
        
        // 2. 检查用户是否明确要求切换话题
        if (isExplicitTopicSwitch(userMessage)) {
            log.debug(" 用户明确切换话题");
            return true;
        }
        
        // 3. 问题类型发生重大变化
        if (isMajorQuestionTypeChange(sessionState, questionType)) {
            log.debug(" 问题类型发生重大变化");
            return true;
        }
        
        // 4. 复杂度显著提升，需要更强模型
        if (needsStrongerModel(sessionState.getCurrentModel(), questionType, contextComplexity)) {
            log.debug("️ 需要更强的模型");
            return true;
        }
        
        // 5. 默认保持粘性
        return false;
    }
    
    /**
     * 检查用户是否明确要求切换话题
     */
    private boolean isExplicitTopicSwitch(String message) {
        String[] switchPhrases = {
            "换个话题", "说点别的", "不聊这个了", "我们聊聊", 
            "另外", "还有个问题", "顺便问一下", "对了",
            "帮我写", "帮我做", "现在我想", "我还需要"
        };
        
        String lowerMessage = message.toLowerCase();
        for (String phrase : switchPhrases) {
            if (lowerMessage.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断问题类型是否发生重大变化
     */
    private boolean isMajorQuestionTypeChange(SessionState sessionState, String newQuestionType) {
        List<String> recentTypes = sessionState.getRecentQuestionTypes();
        
        // 如果新问题类型与最近的问题类型完全不同
        boolean isDifferentFromRecent = !recentTypes.contains(newQuestionType);
        
        // 特定类型的重大变化
        String currentType = sessionState.getCurrentQuestionType();
        
        // 从一般问题转向专业问题
        if ("CASUAL".equals(currentType) && 
            ("CODE_GENERATION".equals(newQuestionType) || "COMPLEX_ANALYSIS".equals(newQuestionType))) {
            return true;
        }
        
        // 从专业问题转向一般问题
        if (("CODE_GENERATION".equals(currentType) || "COMPLEX_ANALYSIS".equals(currentType)) && 
            "CASUAL".equals(newQuestionType)) {
            return true;
        }
        
        return isDifferentFromRecent && recentTypes.size() >= 3;
    }
    
    /**
     * 判断是否需要更强的模型
     */
    private boolean needsStrongerModel(String currentModel, String questionType, int contextComplexity) {
        // 当前使用轻量模型，但复杂度较高
        if ("qwen-flash".equals(currentModel) && contextComplexity >= 4) {
            return true;
        }
        
        if ("qwen-plus".equals(currentModel) && contextComplexity >= 7) {
            return true;
        }
        
        // 特定问题类型需要特定模型
        if ("CODE_GENERATION".equals(questionType) && !currentModel.contains("coder")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新会话状态
     */
    private void updateSessionState(SessionState sessionState, String selectedModel, String questionType) {
        sessionState.setLastInteraction(LocalDateTime.now());
        sessionState.incrementTurnCount();
        
        // 如果模型发生变化，更新状态
        if (!selectedModel.equals(sessionState.getCurrentModel())) {
            sessionState.unlockModel(); // 解锁以允许后续切换
            sessionState.setCurrentModel(selectedModel);
        }
        
        // 更新问题类型
        sessionState.setCurrentQuestionType(questionType);
        
        // 连续相同类型的问题，可以锁定模型
        if (sessionState.getTurnCount() >= 3 && 
            sessionState.getRecentQuestionTypes().stream()
                .skip(Math.max(0, sessionState.getRecentQuestionTypes().size() - 3))
                .allMatch(type -> type.equals(questionType))) {
            sessionState.lockModel("连续相同类型问题");
            log.debug("  锁定模型 {} (连续相同类型: {})", selectedModel, questionType);
        }
    }
    
    /**
     * 创建AI模型实例
     */
    private AIModel createAIModel(String modelName) {
        AIModelProperties.ModelConfig config = aiModelProperties.getModelConfig(modelName);
        return new AIModel(modelName, config);
    }
    
    /**
     * 问题类型分类
     */
    private String classifyQuestion(String message) {
        // 初始化关键词模式（懒加载）
        if (keywordPatterns == null) {
            initKeywordPatterns();
        }
        
        for (Map.Entry<Pattern, String> entry : keywordPatterns.entrySet()) {
            if (entry.getKey().matcher(message).find()) {
                return entry.getValue();
            }
        }
        
        // 默认为普通问题
        return "CASUAL";
    }
    
    /**
     * 初始化关键词匹配模式
     */
    private void initKeywordPatterns() {
        keywordPatterns = new HashMap<>();
        
        if (aiModelProperties.getQuestionTypes() != null) {
            for (Map.Entry<String, AIModelProperties.QuestionTypeConfig> entry : 
                 aiModelProperties.getQuestionTypes().entrySet()) {
                
                String questionType = entry.getKey();
                AIModelProperties.QuestionTypeConfig config = entry.getValue();
                
                if (config.getKeywords() != null) {
                    String keywordPattern = "(?i).*(" + String.join("|", config.getKeywords()) + ").*";
                    keywordPatterns.put(Pattern.compile(keywordPattern), questionType);
                }
            }
        }
        
        // 如果没有配置，使用默认模式
        if (keywordPatterns.isEmpty()) {
            keywordPatterns.put(Pattern.compile("(?i).*(你好|hello|hi|谢谢|再见|天气|新闻|时间).*"), "CASUAL");
            keywordPatterns.put(Pattern.compile("(?i).*(查询|查看|显示|列表|谁是|我是谁|成员|人员|统计).*"), "STUDIO_QUERY");
            keywordPatterns.put(Pattern.compile("(?i).*(任务|考勤|请假|审批|权限|角色|部门|管理|分配).*"), "STUDIO_MANAGEMENT");
            keywordPatterns.put(Pattern.compile("(?i).*(分析|对比|建议|优化|决策|策略|评估|如何提高).*"), "COMPLEX_ANALYSIS");
            keywordPatterns.put(Pattern.compile("(?i).*(计划|方案|总结|报告|详细说明|完整介绍|制定.*计划).*"), "LONG_CONTENT");
            keywordPatterns.put(Pattern.compile("(?i).*(代码|编程|开发|bug|调试|系统设计|架构|算法|数据库|API).*"), "CODE_GENERATION");
        }
    }
    
    /**
     * 上下文复杂度分析
     */
    private int analyzeContextComplexity(String message, Long userId, String sessionId) {
        int complexity = 0;
        
        // 长度因子
        if (message.length() > 500) complexity += 2;
        else if (message.length() > 200) complexity += 1;
        
        // 包含特殊符号（表格、代码等）
        if (message.contains("|") || message.contains("```") || message.contains("SELECT")) {
            complexity += 2;
        }
        
        // 多个问句
        long questionCount = message.chars().filter(ch -> ch == '?' || ch == '？').count();
        complexity += (int) Math.min(questionCount, 3);
        
        // 近期对话复杂度
        try {
            String sql = "SELECT COUNT(*) FROM ai_conversation_log " +
                        "WHERE user_id = ? AND session_id = ? " +
                        "AND create_time > DATE_SUB(NOW(), INTERVAL 10 MINUTE)";
            Integer recentCount = jdbcTemplate.queryForObject(sql, Integer.class, userId, sessionId);
            if (recentCount != null && recentCount > 5) {
                complexity += 1;
            }
        } catch (Exception e) {
            log.debug("分析会话复杂度失败: {}", e.getMessage());
        }
        
        return Math.min(complexity, 10); // 最大复杂度10
    }
    
    /**
     * 智能模型选择逻辑
     */
    private String intelligentModelSelection(String questionType, int messageLength, int contextComplexity) {
        // 长文优先判断
        if (messageLength > 2000 || (messageLength > 1000 && contextComplexity >= 5)) {
            return "qwen3-max-preview";
        }
        
        // 复杂度升级判断
        if (contextComplexity >= 8) {
            return "qwen-max-latest";
        } else if (contextComplexity >= 5) {
            return "qwen-max";
        }
        
        // 根据问题类型选择模型
        AIModelProperties.QuestionTypeConfig config = aiModelProperties.getQuestionTypeConfig(questionType);
        if (config != null && config.getDefaultModel() != null) {
            String defaultModel = config.getDefaultModel();
            
            // 根据复杂度和长度进行微调
            if ("STUDIO_QUERY".equals(questionType)) {
                if (messageLength > 500 || contextComplexity >= 3) {
                    return "qwen-plus-latest";
                }
                return defaultModel;
            }
            
            return defaultModel;
        }
        
        // 默认模型选择
        switch (questionType) {
            case "CASUAL":
                return "qwen-flash";
            case "STUDIO_QUERY":
                return "qwen-plus";
            case "STUDIO_MANAGEMENT":
                return "qwen-max";
            case "COMPLEX_ANALYSIS":
                return "qwen-max-latest";
            case "LONG_CONTENT":
                return "qwen3-max-preview";
            case "CODE_GENERATION":
                return "qwen3-coder-plus-2025-07-22";
            default:
                return "qwen-plus";
        }
    }
    
    /**
     * 记录模型选择
     */
    private void recordModelSelection(Long userId, String sessionId, String message, 
                                    AIModel model, String questionType, SessionState sessionState) {
        try {
            String sql = "INSERT INTO model_selection_log " +
                        "(user_id, session_id, message_length, question_type, " +
                        "selected_model, model_cost_rate, turn_count, model_locked, create_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql, 
                userId, sessionId, message.length(), questionType,
                model.getModelName(), model.getCostRate(), 
                sessionState.getTurnCount(), sessionState.isModelLocked(),
                LocalDateTime.now());
                
        } catch (Exception e) {
            log.error("记录模型选择失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取会话状态信息（用于调试）
     */
    public Map<String, Object> getSessionInfo(String sessionId) {
        SessionState state = sessionStateCache.get(sessionId);
        Map<String, Object> info = new HashMap<>();
        
        if (state != null) {
            info.put("currentModel", state.getCurrentModel());
            info.put("questionType", state.getCurrentQuestionType());
            info.put("turnCount", state.getTurnCount());
            info.put("modelLocked", state.isModelLocked());
            info.put("lockReason", state.getLockReason());
            info.put("recentQuestionTypes", state.getRecentQuestionTypes());
            info.put("lastInteraction", state.getLastInteraction());
        } else {
            info.put("status", "no_session_found");
        }
        
        return info;
    }
    
    /**
     * 手动重置会话状态
     */
    public void resetSession(String sessionId) {
        SessionState oldState = sessionStateCache.remove(sessionId);
        log.info("手动重置会话状态: {} (之前模型: {})",
                sessionId, oldState != null ? oldState.getCurrentModel() : "无");
    }
    
    /**
     * 获取所有活跃会话统计
     */
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveSessions", sessionStateCache.size());
        
        Map<String, Integer> modelUsage = new HashMap<>();
        Map<String, Integer> questionTypeUsage = new HashMap<>();
        int lockedSessions = 0;
        
        for (SessionState state : sessionStateCache.values()) {
            // 统计模型使用情况
            modelUsage.merge(state.getCurrentModel(), 1, Integer::sum);
            
            // 统计问题类型
            questionTypeUsage.merge(state.getCurrentQuestionType(), 1, Integer::sum);
            
            // 统计锁定的会话
            if (state.isModelLocked()) {
                lockedSessions++;
            }
        }
        
        stats.put("modelUsage", modelUsage);
        stats.put("questionTypeUsage", questionTypeUsage);
        stats.put("lockedSessions", lockedSessions);
        
        return stats;
    }
    
    /**
     * 获取可用模型列表
     */
    public List<AIModel> getAvailableModels() {
        List<AIModel> models = new ArrayList<>();
        
        if (aiModelProperties.getModels() != null) {
            for (Map.Entry<String, AIModelProperties.ModelConfig> entry : aiModelProperties.getModels().entrySet()) {
                models.add(new AIModel(entry.getKey(), entry.getValue()));
            }
        }
        
        return models;
    }
    
    /**
     * 根据模型名称获取模型信息
     */
    public AIModel getModelByName(String modelName) {
        return createAIModel(modelName);
    }
} 