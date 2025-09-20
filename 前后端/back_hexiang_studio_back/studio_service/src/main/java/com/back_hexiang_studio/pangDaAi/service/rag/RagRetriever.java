package com.back_hexiang_studio.pangDaAi.service.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索器核心服务
 * 
 * 🔍 功能特性：
 * - 智能语义检索：基于向量相似度匹配最相关内容
 * - 时间权重优化：最新信息获得更高权重
 * - 类型过滤支持：公告、课程、任务等分类检索
 * - 结果重排序：综合相似度、时间、重要性评分
 * - 自动摘要生成：提取关键信息并生成简洁摘要
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0 - 免费RAG实现
 * @since 2025-09-13
 */
@Service
@Slf4j
public class RagRetriever {

    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private ToolClassificationService toolClassificationService;

    // 检索参数配置
    @Value("${pangda-ai.rag.retrieval.max-results:5}")
    private int defaultMaxResults;

    @Value("${pangda-ai.rag.retrieval.min-score:0.7}")
    private double minSimilarityScore;

    @Value("${pangda-ai.rag.retrieval.time-decay-days:30}")
    private int timeDecayDays;

    /**
     * 核心检索方法 - 用户上下文感知
     * 
     * @param query 用户查询
     * @param maxResults 最大结果数
     * @param userId 当前用户ID，用于身份查询等场景
     * @return 检索结果
     */
    public RetrievalResult retrieve(String query, int maxResults, Long userId) {
        log.info("🔍 开始RAG检索 - 查询: '{}', 最大结果: {}, 用户: {}", query, maxResults, userId);
        
        try {
            // 🎯 优先处理身份查询
            if (isIdentityQuery(query) && userId != null) {
                log.info("🔐 检测到身份查询，执行精确用户查询");
                return performUserIdentityQuery(userId, query);
            }
            
            // 🚀 1. 查询预处理和扩展
            String enhancedQuery = enhanceQueryForBetterRetrieval(query);
            log.debug("🔤 查询增强: '{}' -> '{}'", query, enhancedQuery);
            
            // 🎯 2. 智能工具分类路由 - 新增优化点
            ToolClassificationService.RetrievalStrategy strategy = 
                toolClassificationService.generateRetrievalStrategy(query);
            
            // 3. 执行分类优化检索
            List<EmbeddingMatch<TextSegment>> rawResults = performClassifiedSearch(enhancedQuery, query, maxResults, strategy);
            
            if (rawResults.isEmpty()) {
                log.info("未找到匹配结果 - 查询: '{}'", query);
                return RetrievalResult.empty();
            }
            
            // 3. 简化过滤逻辑
            List<EmbeddingMatch<TextSegment>> filteredResults = simpleQualityFilter(rawResults, query, userId);
            
            // 4. 结果排序和限制
            List<EnhancedResult> finalResults = enhanceAndRankResults(filteredResults, query).stream()
                .limit(maxResults)
                .collect(Collectors.toList());
            
            // 5. 打印最终用于回答的向量数据
            if (log.isInfoEnabled()) {
                log.info("RAG使用向量数据（最终{}条）:", finalResults.size());
                for (EnhancedResult er : finalResults) {
                    try {
                        log.info("- {}", er.getDisplayInfo());
                    } catch (Exception ignore) {}
                }
            }
            
            // 6. 生成智能摘要
            String summary = generateSummary(finalResults, query);
            
            RetrievalResult result = new RetrievalResult(finalResults, summary, query);
            
            log.info("✅ RAG检索完成 - 原始: {}, 过滤后: {}, 最终: {}", 
                    rawResults.size(), filteredResults.size(), finalResults.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ RAG检索失败 - 查询: '{}': {}", query, e.getMessage(), e);
            return RetrievalResult.empty();
        }
    }

    /**
     * 按类型检索 - 针对特定内容类型
     */
    public RetrievalResult retrieveByType(String query, String contentType, int maxResults) {
        log.info("🔍 按类型检索 - 查询: '{}', 类型: '{}'", query, contentType);
        
        try {
            // 增强查询，包含类型信息
            String enhancedQuery = contentType + " " + query;
            
            List<EmbeddingMatch<TextSegment>> rawResults = vectorStoreService.search(enhancedQuery, maxResults * 2);
            
            // 过滤指定类型的结果 - 暂时简化处理
            List<EmbeddingMatch<TextSegment>> typeFilteredResults = rawResults.stream()
                .filter(match -> {
                    try {
                        // 暂时跳过类型过`滤，让所有结果通过
                        // TODO: 修复元数据访问方法后恢复类型过滤
                        log.debug("暂时跳过类型过滤，查询类型: {}", contentType);
                        return true;
                    } catch (Exception e) {
                        log.warn("过滤类型时出错: {}", e.getMessage());
                        return true; // 出错时允许通过
                    }
                })
                .filter(match -> match.score() >= minSimilarityScore)
                .collect(Collectors.toList());
            
            List<EnhancedResult> enhancedResults = enhanceAndRankResults(typeFilteredResults, query);
            List<EnhancedResult> finalResults = enhancedResults.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
            
            String summary = generateTypedSummary(finalResults, query, contentType);
            
            log.info("✅ 按类型检索完成 - 类型: '{}', 结果: {}", contentType, finalResults.size());
            
            return new RetrievalResult(finalResults, summary, enhancedQuery);
            
        } catch (Exception e) {
            log.error("❌ 按类型检索失败 - 查询: '{}', 类型: '{}': {}", query, contentType, e.getMessage(), e);
            return RetrievalResult.empty();
        }
    }

    /**
     * 增强结果并重新排序
     */
    private List<EnhancedResult> enhanceAndRankResults(List<EmbeddingMatch<TextSegment>> matches, String query) {
        return matches.stream()
            .map(match -> createEnhancedResult(match, query))
            .filter(Objects::nonNull)
            .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
            .collect(Collectors.toList());
    }

    /**
     * 创建增强结果对象
     */
    private EnhancedResult createEnhancedResult(EmbeddingMatch<TextSegment> match, String query) {
        try {
            // 暂时简化元数据处理
            EnhancedResult result = new EnhancedResult();
            result.setOriginalScore(match.score());
            result.setContent(match.embedded().text());
            result.setSnippet(extractSnippet(match.embedded().text(), query));
            
            // 暂时设置默认值，避免元数据访问问题
            result.setType("unknown");
            result.setTitle("Untitled");
            result.setId(UUID.randomUUID().toString());
            result.setTimestamp(LocalDateTime.now());
            
            // 计算综合评分
            double finalScore = calculateFinalScore(result, query);
            result.setFinalScore(finalScore);
            
            return result;
            
        } catch (Exception e) {
            log.warn("⚠️ 创建增强结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算综合评分
     */
    private double calculateFinalScore(EnhancedResult result, String query) {
        double score = result.getOriginalScore();
        
        // 时间衰减因子
        if (result.getTimestamp() != null) {
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(result.getTimestamp(), LocalDateTime.now());
            double timeWeight = Math.exp(-daysDiff / (double) timeDecayDays);
            score *= (0.7 + 0.3 * timeWeight);
        }
        
        // 类型权重
        double typeWeight = getTypeWeight(result.getType());
        score *= typeWeight;
        
        // 查询词匹配加权
        double keywordBoost = calculateKeywordBoost(result.getContent(), query);
        score += keywordBoost;
        
        return Math.min(score, 1.0);
    }

    /**
     * 获取内容类型权重
     */
    private double getTypeWeight(String type) {
        if (type == null) return 1.0;
        
        switch (type.toLowerCase()) {
            case "notice": case "公告": return 1.2;
            case "course": case "课程": return 1.1;
            case "task": case "任务": return 1.1;
            case "material": case "资料": return 1.0;
            case "attendance": case "考勤": return 0.9;
            default: return 1.0;
        }
    }

    /**
     * 计算关键词匹配加权
     */
    private double calculateKeywordBoost(String content, String query) {
        if (content == null || query == null) return 0.0;
        
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 简单的关键词匹配计分
        String[] queryWords = lowerQuery.split("\\s+");
        int matches = 0;
        
        for (String word : queryWords) {
            if (word.length() > 1 && lowerContent.contains(word)) {
                matches++;
            }
        }
        
        return matches > 0 ? Math.min(0.1 * matches / queryWords.length, 0.1) : 0.0;
    }

    /**
     * 提取关键片段
     */
    private String extractSnippet(String content, String query) {
        if (content == null || content.length() <= 150) {
            return content;
        }
        
        // 查找查询词附近的上下文
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        int bestStart = 0;
        double bestScore = 0;
        
        // 滑动窗口找最佳片段
        int windowSize = 120;
        for (int i = 0; i <= content.length() - windowSize; i += 30) {
            String window = content.substring(i, Math.min(i + windowSize, content.length()));
            String lowerWindow = window.toLowerCase();
            
            double score = 0;
            for (String word : lowerQuery.split("\\s+")) {
                if (word.length() > 1 && lowerWindow.contains(word)) {
                    score += 1.0;
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestStart = i;
            }
        }
        
        int end = Math.min(bestStart + 150, content.length());
        String snippet = content.substring(bestStart, end);
        
        if (bestStart > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        
        return snippet;
    }

    /**
     * 生成智能摘要
     */
    private String generateSummary(List<EnhancedResult> results, String query) {
        if (results.isEmpty()) {
            return "未找到相关信息";
        }
        
        if (results.size() == 1) {
            return "找到1条相关信息：" + results.get(0).getType() + "类型的内容";
        }
        
        Map<String, Long> typeCount = results.stream()
            .collect(Collectors.groupingBy(
                result -> result.getType() != null ? result.getType() : "其他",
                Collectors.counting()
            ));
        
        StringBuilder summary = new StringBuilder();
        summary.append("找到").append(results.size()).append("条相关信息，包括：");
        
        typeCount.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(entry -> summary.append(entry.getValue()).append("条").append(entry.getKey()).append("、"));
        
        if (summary.length() > 0 && summary.charAt(summary.length() - 1) == '、') {
            summary.setLength(summary.length() - 1);
        }
        
        return summary.toString();
    }

    /**
     * 生成分类摘要
     */
    private String generateTypedSummary(List<EnhancedResult> results, String query, String contentType) {
        if (results.isEmpty()) {
            return "未找到相关的" + contentType + "信息";
        }
        
        return String.format("找到%d条%s相关信息", results.size(), contentType);
    }

    /**
     * 解析时间戳
     */
    private LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) return null;
        
        try {
            if (timestamp instanceof String) {
                return LocalDateTime.parse((String) timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (timestamp instanceof LocalDateTime) {
                return (LocalDateTime) timestamp;
            }
        } catch (Exception e) {
            log.debug("时间戳解析失败: {}", timestamp);
        }
        
        return null;
    }

    // ===================================================================
    // 数据传输对象
    // ===================================================================

    /**
     * 检索结果包装类
     */
    @Data
    public static class RetrievalResult {
        private List<EnhancedResult> results;
        private String summary;
        private String query;
        private LocalDateTime retrievalTime;

        public RetrievalResult(List<EnhancedResult> results, String summary, String query) {
            this.results = results != null ? results : new ArrayList<>();
            this.summary = summary != null ? summary : "";
            this.query = query;
            this.retrievalTime = LocalDateTime.now();
        }

        public static RetrievalResult empty() {
            return new RetrievalResult(new ArrayList<>(), "未找到相关信息", "");
        }

        public boolean isEmpty() {
            return results.isEmpty();
        }

        public int size() {
            return results.size();
        }
    }

    /**
     * 增强结果对象
     */
    @Data
    public static class EnhancedResult {
        private String id;
        private String type;
        private String title;
        private String content;
        private String snippet;
        private double originalScore;
        private double finalScore;
        private LocalDateTime timestamp;

        public String getDisplayInfo() {
            StringBuilder info = new StringBuilder();
            if (title != null && !title.trim().isEmpty()) {
                info.append(title).append(" - ");
            }
            if (type != null) {
                info.append("[").append(type).append("] ");
            }
            if (snippet != null) {
                info.append(snippet);
            }
            return info.toString();
        }
    }

    // ===================================================================
    // 🚀 新增优化方法
    // ===================================================================

    /**
     * 🧠 查询增强 - 提高检索准确性
     */
    private String enhanceQueryForBetterRetrieval(String originalQuery) {
        StringBuilder enhanced = new StringBuilder(originalQuery);
        String lowerQuery = originalQuery.toLowerCase();
        
        // 添加同义词和相关词汇
        if (lowerQuery.contains("公告") || lowerQuery.contains("通知")) {
            enhanced.append(" 通知 公告 消息 信息");
        }
        if (lowerQuery.contains("课程") || lowerQuery.contains("课")) {
            enhanced.append(" 课程 教学 学习 上课");
        }
        if (lowerQuery.contains("任务") || lowerQuery.contains("作业")) {
            enhanced.append(" 任务 作业 练习 项目");
        }
        if (lowerQuery.contains("资料") || lowerQuery.contains("材料")) {
            enhanced.append(" 资料 材料 文档 文件");
        }
        if (lowerQuery.contains("考试")) {
            enhanced.append(" 考试 测验 评估 检测");
        }
        
        return enhanced.toString();
    }

    /**
     * 🎯 增强搜索策略（保留向后兼容）
     */
    private List<EmbeddingMatch<TextSegment>> performEnhancedSearch(String enhancedQuery, String originalQuery, int maxResults) {
        List<EmbeddingMatch<TextSegment>> allResults = new ArrayList<>();
        
        // 策略1: 使用增强查询
        List<EmbeddingMatch<TextSegment>> enhancedResults = vectorStoreService.search(enhancedQuery, maxResults * 2);
        allResults.addAll(enhancedResults);
        
        // 策略2: 使用原始查询（确保不遗漏精确匹配）
        List<EmbeddingMatch<TextSegment>> originalResults = vectorStoreService.search(originalQuery, maxResults);
        allResults.addAll(originalResults);
        
        // 去重并按分数排序
        Map<String, EmbeddingMatch<TextSegment>> uniqueResults = new HashMap<>();
        for (EmbeddingMatch<TextSegment> match : allResults) {
            String key = match.embedded().text();
            if (!uniqueResults.containsKey(key) || uniqueResults.get(key).score() < match.score()) {
                uniqueResults.put(key, match);
            }
        }
        
        return uniqueResults.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .collect(Collectors.toList());
    }

    /**
     * 🚀 分类优化检索 - 核心性能优化方法
     * 根据工具分类策略进行智能检索，显著减少向量比对次数
     */
    private List<EmbeddingMatch<TextSegment>> performClassifiedSearch(String enhancedQuery, String originalQuery, 
                                                                   int maxResults, ToolClassificationService.RetrievalStrategy strategy) {
        try {
            log.info("🎯 执行分类优化检索，策略: {}", strategy.getStrategyType());
            
            List<EmbeddingMatch<TextSegment>> allResults = new ArrayList<>();
            
            switch (strategy.getStrategyType()) {
                case "SINGLE_CATEGORY":
                    // 单类别优化检索 - 最高效
                    allResults = performSingleCategorySearch(enhancedQuery, originalQuery, maxResults, strategy);
                    break;
                    
                case "MULTI_CATEGORY":
                    // 多类别优先检索 - 中等效率
                    allResults = performMultiCategorySearch(enhancedQuery, originalQuery, maxResults, strategy);
                    break;
                    
                case "GLOBAL":
                default:
                    // 兜底全局检索 - 保证召回率
                    log.info("🌐 使用全局检索策略");
                    allResults = performEnhancedSearch(enhancedQuery, originalQuery, maxResults);
                    break;
            }
            
            log.info("✅ 分类检索完成，获得 {} 个结果", allResults.size());
            return allResults;
            
        } catch (Exception e) {
            log.error("❌ 分类检索失败，降级到传统检索: {}", e.getMessage());
            return performEnhancedSearch(enhancedQuery, originalQuery, maxResults);
        }
    }

    /**
     * 单类别检索 - 性能最优
     */
    private List<EmbeddingMatch<TextSegment>> performSingleCategorySearch(String enhancedQuery, String originalQuery, 
                                                                        int maxResults, ToolClassificationService.RetrievalStrategy strategy) {
        List<String> categories = strategy.getPrioritizedCategories().stream()
            .map(cat -> cat.getCode())
            .collect(Collectors.toList());
        
        log.info("🎯 单类别检索: {}", categories.get(0));
        
        // 执行分类检索
        VectorStoreService.ClassifiedSearchResult classifiedResult = 
            vectorStoreService.searchByCategories(enhancedQuery, categories, maxResults);
        
        List<EmbeddingMatch<TextSegment>> results = new ArrayList<>(classifiedResult.getMatches());
        
        // 如果结果不足，补充原始查询结果
        if (results.size() < maxResults / 2) {
            log.info("🔄 分类结果不足，补充原始查询");
            List<EmbeddingMatch<TextSegment>> supplementResults = vectorStoreService.search(originalQuery, maxResults);
            
            // 去重合并
            Set<String> existingTexts = results.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toSet());
            
            supplementResults.stream()
                .filter(match -> !existingTexts.contains(match.embedded().text()))
                .limit(maxResults - results.size())
                .forEach(results::add);
        }
        
        return results;
    }

    /**
     * 多类别检索 - 平衡效率和召回率
     */
    private List<EmbeddingMatch<TextSegment>> performMultiCategorySearch(String enhancedQuery, String originalQuery, 
                                                                       int maxResults, ToolClassificationService.RetrievalStrategy strategy) {
        List<String> categories = strategy.getPrioritizedCategories().stream()
            .map(cat -> cat.getCode())
            .collect(Collectors.toList());
        
        log.info("🔄 多类别检索: {}", categories);
        
        // 按优先级逐个检索类别
        List<EmbeddingMatch<TextSegment>> allResults = new ArrayList<>();
        Set<String> seenTexts = new HashSet<>();
        
        for (String category : categories) {
            if (allResults.size() >= maxResults) break;
            
            log.debug("🔍 检索类别: {}", category);
            
            VectorStoreService.ClassifiedSearchResult categoryResult = 
                vectorStoreService.searchByCategories(enhancedQuery, Collections.singletonList(category), 
                                                   maxResults - allResults.size());
            
            // 添加不重复的结果
            for (EmbeddingMatch<TextSegment> match : categoryResult.getMatches()) {
                String text = match.embedded().text();
                if (!seenTexts.contains(text)) {
                    allResults.add(match);
                    seenTexts.add(text);
                    
                    if (allResults.size() >= maxResults) break;
                }
            }
        }
        
        // 按分数重新排序
        allResults.sort((a, b) -> Double.compare(b.score(), a.score()));
        
        return allResults;
    }

    // 删除原有的复杂过滤逻辑，已替换为simpleQualityFilter

    // 删除原有的复杂语义相关性验证，已替换为简单关键词匹配
    
    /**
     * 向后兼容的重载方法
     */
    public RetrievalResult retrieve(String query, int maxResults) {
        return retrieve(query, maxResults, null);
    }

    /**
     * 执行用户身份查询 - 从向量数据库检索
     * 通过增强查询和结果过滤来精确匹配当前用户信息
     */
    private RetrievalResult performUserIdentityQuery(Long userId, String query) {
        log.info("🎯 执行用户身份向量查询: userId={}, query='{}'", userId, query);
        
        try {
            // 构建包含用户ID的增强查询
            String enhancedQuery = buildUserIdentityQuery(userId, query);
            log.info("🔍 身份查询增强: '{}' -> '{}'", query, enhancedQuery);
            
            // 执行向量检索，获取更多候选结果
            List<EmbeddingMatch<TextSegment>> rawResults = performEnhancedSearch(enhancedQuery, query, 20);
            
            if (rawResults.isEmpty()) {
                log.warn("⚠️ 向量检索未找到任何用户相关结果");
                return RetrievalResult.empty();
            }
            
            // 过滤出与当前用户ID相关的结果
            List<EmbeddingMatch<TextSegment>> userResults = filterUserSpecificResults(rawResults, userId);
            
            if (userResults.isEmpty()) {
                log.warn("⚠️ 未找到用户ID {} 的相关信息，但找到了其他用户信息", userId);
                // 返回提示信息，说明找到了其他用户但没找到当前用户
                return buildUserNotFoundResult(userId, rawResults);
            }
            
            // 转换为增强结果
            List<EnhancedResult> enhancedResults = enhanceAndRankResults(userResults, enhancedQuery);
            
            log.info("✅ 成功检索到用户 {} 的 {} 条身份信息", userId, enhancedResults.size());
            
            return new RetrievalResult(
                enhancedResults,
                "当前用户的身份信息", // 🔒 不暴露用户ID
                query
            );
            
        } catch (Exception e) {
            log.error("❌ 用户身份向量查询失败: userId={}, error={}", userId, e.getMessage(), e);
            return RetrievalResult.empty();
        }
    }
    
    /**
     * 构建包含用户ID的身份查询
     */
    private String buildUserIdentityQuery(Long userId, String originalQuery) {
        StringBuilder enhanced = new StringBuilder();
        enhanced.append(originalQuery).append(" ");
        enhanced.append("用户ID ").append(userId).append(" ");
        enhanced.append("个人信息 身份信息 用户资料 ");
        enhanced.append("ID:").append(userId).append(" ");
        enhanced.append("用户编号").append(userId);
        return enhanced.toString();
    }
    
    /**
     * 过滤出与特定用户ID相关的结果
     */
    private List<EmbeddingMatch<TextSegment>> filterUserSpecificResults(
            List<EmbeddingMatch<TextSegment>> results, Long userId) {
        
        String userIdStr = userId.toString();
        List<EmbeddingMatch<TextSegment>> userResults = new ArrayList<>();
        
        for (EmbeddingMatch<TextSegment> match : results) {
            String content = match.embedded().text();
            String lowerContent = content.toLowerCase();
            
            // 🔍 调试：打印所有检索到的内容
            log.info("📄 检索到的内容片段: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);
            
            // 🎯 修复过滤条件：支持各种格式的用户ID
            boolean matches = 
                // 中文冒号格式
                content.contains("用户ID：" + userIdStr) ||
                content.contains("ID：" + userIdStr) ||
                content.contains("用户编号：" + userIdStr) ||
                content.contains("用户标识：" + userIdStr) ||
                
                // 英文冒号格式  
                content.contains("用户ID: " + userIdStr) ||
                content.contains("ID: " + userIdStr) ||
                content.contains("用户编号: " + userIdStr) ||
                content.contains("用户标识: " + userIdStr) ||
                
                // 空格格式
                lowerContent.contains("用户id " + userIdStr) ||
                lowerContent.contains("id " + userIdStr) ||
                lowerContent.contains("userid " + userIdStr) ||
                
                // 直接数字匹配（作为最后的保险）
                content.contains(userIdStr);
            
            // 🔍 调试：显示匹配检查结果
            log.info("🔍 用户ID {} 匹配检查: 包含'{}'? {} | 包含'{}'? {} | 包含数字'{}'? {}", 
                     userId, 
                     "用户ID：" + userIdStr, content.contains("用户ID：" + userIdStr),
                     "ID：" + userIdStr, content.contains("ID：" + userIdStr),
                     userIdStr, content.contains(userIdStr));
            
            if (matches) {
                log.info("✅ 找到匹配用户ID {} 的内容: {}", userId, 
                         content.length() > 100 ? content.substring(0, 100) + "..." : content);
                userResults.add(match);
            } else {
                log.info("❌ 内容不匹配用户ID {}", userId);
            }
        }
        
        log.info("🎯 用户ID过滤结果: 总结果 {}, 匹配用户 {} 的结果 {}", 
                results.size(), userId, userResults.size());
        
        return userResults;
    }
    
    /**
     * 构建"未找到用户信息"的结果
     */
    private RetrievalResult buildUserNotFoundResult(Long userId, List<EmbeddingMatch<TextSegment>> allResults) {
        EnhancedResult result = new EnhancedResult();
        result.setId("user_not_found_" + userId);
        result.setType("user_identity_error");
        result.setTitle("用户身份查询结果");
        
        StringBuilder content = new StringBuilder();
        content.append(String.format("🔒 很抱歉，未找到用户ID %d 的身份信息。\n", userId));
        content.append("建议您：\n");
        content.append("1. 检查用户ID是否正确\n");
        content.append("2. 联系管理员确认账户状态\n");
        content.append("3. 或通过其他方式查询个人信息\n");
        
        // 🚨 安全修复：绝不泄露其他用户信息！
        // 原代码存在严重安全漏洞，会泄露其他用户的敏感信息
        // 现在只返回安全的提示信息，不包含任何其他用户数据
        
        result.setContent(content.toString());
        result.setSnippet("用户身份信息未找到");
        result.setOriginalScore(0.0);
        result.setFinalScore(0.0);
        result.setTimestamp(LocalDateTime.now());
        
        List<EnhancedResult> results = new ArrayList<>();
        results.add(result);
        
        return new RetrievalResult(results, "未找到指定用户身份信息", "用户身份查询");
    }

    /**
     * 简化的质量过滤器
     */
    private List<EmbeddingMatch<TextSegment>> simpleQualityFilter(
            List<EmbeddingMatch<TextSegment>> results, String query, Long userId) {
        
        log.debug("🔍 开始简化质量过滤 - 原始结果数: {}, 查询: '{}'", results.size(), query);
        
        return results.stream()
                .filter(match -> {
                    // 基本相似度过滤
                    if (match.score() < minSimilarityScore) {
                        return false;
                    }
                    
                    // 内容长度过滤
                    String content = match.embedded().text();
                    if (content.length() < 10) {
                        return false;
                    }
                    
                    // 基础信息查询使用宽松匹配
                    if (isBasicInfoQuery(query.toLowerCase())) {
                        return true; // 基础查询直接通过
                    }
                    
                    // 其他查询使用简单关键词匹配
                    return containsKeywords(content, query);
                })
                .collect(Collectors.toList());
    }

    /**
     * 简单关键词匹配
     */
    private boolean containsKeywords(String content, String query) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 移除标点符号后分割
        String[] queryWords = lowerQuery.replaceAll("[，。！？、]", " ").split("\\s+");
        
        int matchCount = 0;
        for (String word : queryWords) {
            if (word.length() > 1 && lowerContent.contains(word)) {
                matchCount++;
            }
        }
        
        // 只要有任何关键词匹配就通过
        return matchCount > 0;
    }

    /**
     * 判断是否为身份查询
     */
    private boolean isIdentityQuery(String query) {
        return query.contains("我是谁") || query.contains("我的身份") || 
               query.contains("我的信息") || query.contains("我的资料") ||
               query.contains("个人信息") || query.contains("用户信息");
    }
    
    /**
     * 判断是否为基础信息查询
     */
    private boolean isBasicInfoQuery(String query) {
        // 基础信息查询模式：概括性询问工作室基本信息
        // 🎯 优化：更准确地匹配“列出全部”的意图
        return query.contains("有哪些") || query.contains("有什么") ||
               query.contains("都有什么") || query.contains("列出所有") ||
               query.contains("介绍一下") || query.contains("告诉我") ||
               query.contains("所有") || query.contains("全部") ||
               (query.contains("介绍") && (query.contains("部门") || query.contains("成员"))) ||
               (query.contains("人") && (query.contains("工作室") || query.contains("成员"))) ||
               (query.contains("部门") || query.contains("组织") || query.contains("团队")) ||
               (query.contains("未完成") && query.contains("任务")) ||
               query.matches(".*有.*人.*") || query.matches(".*什么.*部门.*");
    }
    
    // 删除复杂的基础信息相关性检查方法，已简化
} 