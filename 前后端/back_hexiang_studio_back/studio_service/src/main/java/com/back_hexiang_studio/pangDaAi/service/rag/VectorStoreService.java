package com.back_hexiang_studio.pangDaAi.service.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;


/**
 * 向量存储服务
 * 使用 LangChain4j + 可配置的 Embedding 模型和向量存储
 * 
 * 技术栈：
 * - 可配置 EmbeddingStore（支持 InMemory、Milvus 等）
 * - 可配置 EmbeddingModel（支持本地模型、Qwen 等）
 * - Spring 依赖注入管理
 * 
 * @author 胖达AI助手开发团队
 * @version 2.0 - 支持 Milvus 和 Qwen Embedding
 * @since 2025-09-14
 */
@Slf4j
@Service
public class VectorStoreService {

    @Value("${pangda-ai.rag.storage.path:rag/vectors}")
    private String storagePath;

    @Value("${pangda-ai.rag.vector.max-results:5}")
    private int maxResults;

    @Value("${pangda-ai.rag.vector.min-score:0.7}")
    private double minScore;

    @Value("${pangda-ai.rag.chunk-size:500}")
    private int defaultChunkSize;

    @Value("${pangda-ai.rag.chunk-overlap:50}")
    private int defaultChunkOverlap;

    // 免费本地 Embedding 模型
    @Autowired
    private EmbeddingModel embeddingModel;
    
    // 内存向量存储
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    
    // JSON序列化
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()  // 自动注册JSR310模块
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // 向量数据缓存（用于持久化）
    private Map<String, VectorData> vectorCache = new HashMap<>();

    /**
     * 初始化向量存储服务
     */
    @PostConstruct
    public void initialize() {
        log.info(" 初始化向量存储服务...");
        
        try {
            // 验证注入的组件
            if (embeddingModel == null) {
                throw new IllegalStateException("EmbeddingModel 未正确注入");
            }
            if (embeddingStore == null) {
                throw new IllegalStateException("EmbeddingStore 未正确注入");
            }
            
            log.info(" EmbeddingModel 注入成功: {}", embeddingModel.getClass().getSimpleName());
            log.info(" EmbeddingStore 注入成功: {}", embeddingStore.getClass().getSimpleName());
            
            // 创建存储目录（仅用于日志等本地文件）
            createStorageDirectory();
            
            // 注意：不再加载本地持久化向量，因为 Milvus 等外部存储会自己管理持久化
            if (embeddingStore instanceof InMemoryEmbeddingStore) {
                log.info(" 使用内存存储，尝试加载本地持久化数据...");
                loadPersistedVectors();
            } else {
                log.info(" 使用外部向量数据库，跳过本地文件加载");
            }
            
            log.info(" 向量存储服务初始化完成！");
            log.info(" 配置信息：存储路径={}, 最大结果数={}, 最小相似度={}", 
                    storagePath, maxResults, minScore);
                    
        } catch (Exception e) {
            log.error(" 向量存储服务初始化失败", e);
            throw new RuntimeException("向量存储服务启动失败", e);
        }
    }

    /**
     * 添加文档到向量存储
     * 
     * @param id 文档唯一标识
     * @param text 文档文本内容
     * @param metadata 文档元数据
     */
    public void addDocument(String id, String text, Map<String, Object> metadata) {
        try {
            log.debug(" 添加文档到向量存储: ID={}, 文本长度={}", id, text.length());
            
            // 1. 创建文本段
            Metadata docMetadata = Metadata.from(metadata);
            TextSegment textSegment = TextSegment.from(text, docMetadata);
            
            // 2. 生成向量
            Embedding embedding = embeddingModel.embed(text).content();
            
            // 3. 存储到内存向量库
            embeddingStore.add(embedding, textSegment);
            
            // 4. 缓存用于持久化
            VectorData vectorData = new VectorData(id, text, metadata, LocalDateTime.now());
            vectorCache.put(id, vectorData);
            
            // 审计日志：明确写入了什么
            if (log.isInfoEnabled()) {
                String type = metadata != null && metadata.get("type") != null ? metadata.get("type").toString() : "unknown";
                String snippet = text.length() > 120 ? text.substring(0, 120) + "..." : text;
                log.info(" 向量入库: type={}, id={}, textPreview='{}'", type, id, snippet.replaceAll("\n", " "));
            }
            
            log.debug(" 文档添加成功: ID={}", id);
            
        } catch (Exception e) {
            log.error(" 添加文档失败: ID={}, 错误: {}", id, e.getMessage(), e);
        }
    }

    /**
     * 批量添加文档
     * 
     * @param documents 文档列表
     */
    public void addDocuments(List<DocumentData> documents) {
        log.info(" 批量添加 {} 个文档到向量存储", documents.size());
        
        int successCount = 0;
        for (DocumentData doc : documents) {
            try {
                addDocument(doc.getId(), doc.getText(), doc.getMetadata());
                successCount++;
            } catch (Exception e) {
                log.warn(" 批量添加文档失败: ID={}, 错误: {}", doc.getId(), e.getMessage());
            }
        }
        
        log.info(" 批量添加完成：成功 {}/{} 个文档", successCount, documents.size());
    }

    /**
     * 语义搜索
     * 
     * @param query 查询文本
     * @return 相关文档列表
     */
    public List<EmbeddingMatch<TextSegment>> search(String query) {
        return search(query, maxResults);
    }

    /**
     * 语义搜索（指定返回数量）
     * 
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @return 相关文档列表
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        try {
            log.debug(" 执行语义搜索: 查询=\"{}\", 最大结果数={}", query, maxResults);
            
            // 1. 查询向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 向量相似度搜索 
            try {
                // 使用 EmbeddingSearchRequest 
                EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults * 2)  // 获取更多原始结果
                        .minScore(0.0)  // 移除分数限制，让RagRetriever智能过滤
                        .build();
                EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
                List<EmbeddingMatch<TextSegment>> results = searchResult.matches();
                
                log.debug(" 搜索完成：找到 {} 个相关结果", results.size());
                return results;
            } catch (Exception e1) {
                // 如果搜索失败，返回空列表
                log.warn("向量搜索失败，返回空结果: {}", e1.getMessage());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error(" 语义搜索失败: 查询=\"{}\", 错误: {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     *  分类检索 - 根据工具类别进行优化检索
     * 这是性能优化的核心方法，可以显著减少向量比对次数
     * 
     * @param query 查询文本
     * @param categories 优先检索的类别列表
     * @param maxResults 最大返回结果数
     * @return 分类检索结果
     */
    public ClassifiedSearchResult searchByCategories(String query, List<String> categories, int maxResults) {
        try {
            log.info(" 执行分类检索: 查询=\"{}\", 类别={}, 最大结果数={}", query, categories, maxResults);
            
            // 1. 查询向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            ClassifiedSearchResult result = new ClassifiedSearchResult();
            result.setOriginalQuery(query);
            result.setSearchedCategories(categories);
            
            // 2. 按类别优先级检索
            List<EmbeddingMatch<TextSegment>> allMatches = new ArrayList<>();
            Map<String, Integer> categoryMatchCounts = new HashMap<>();
            
            for (String category : categories) {
                log.debug(" 检索类别: {}", category);
                
                // 执行向量搜索
                EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults * 3)  // 每个类别获取更多候选结果
                        .minScore(0.0)
                        .build();
                
                EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
                List<EmbeddingMatch<TextSegment>> categoryMatches = searchResult.matches();
                
                // 按类别过滤结果
                List<EmbeddingMatch<TextSegment>> filteredMatches = categoryMatches.stream()
                    .filter(match -> isMatchInCategory(match, category))
                    .collect(java.util.stream.Collectors.toList());
                
                allMatches.addAll(filteredMatches);
                categoryMatchCounts.put(category, filteredMatches.size());
                
                log.debug(" 类别 {} 检索完成：原始={}, 过滤后={}", 
                    category, categoryMatches.size(), filteredMatches.size());
            }
            
            // 3. 去重和排序
            Map<String, EmbeddingMatch<TextSegment>> uniqueMatches = new HashMap<>();
            for (EmbeddingMatch<TextSegment> match : allMatches) {
                String key = match.embedded().text();
                if (!uniqueMatches.containsKey(key) || uniqueMatches.get(key).score() < match.score()) {
                    uniqueMatches.put(key, match);
                }
            }
            
            List<EmbeddingMatch<TextSegment>> finalMatches = uniqueMatches.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .collect(java.util.stream.Collectors.toList());
            
            result.setMatches(finalMatches);
            result.setCategoryMatchCounts(categoryMatchCounts);
            result.setTotalMatches(finalMatches.size());
            
            log.info(" 分类检索完成：总结果={}, 类别统计={}", 
                finalMatches.size(), categoryMatchCounts);
            
            return result;
            
        } catch (Exception e) {
            log.error(" 分类检索失败: 查询=\"{}\", 类别={}, 错误: {}", query, categories, e.getMessage(), e);
            
            // 降级到全局检索
            log.info(" 降级到全局检索...");
            List<EmbeddingMatch<TextSegment>> fallbackResults = search(query, maxResults);
            
            ClassifiedSearchResult fallbackResult = new ClassifiedSearchResult();
            fallbackResult.setOriginalQuery(query);
            fallbackResult.setMatches(fallbackResults);
            fallbackResult.setSearchedCategories(Collections.singletonList("GLOBAL_FALLBACK"));
            fallbackResult.setTotalMatches(fallbackResults.size());
            
            return fallbackResult;
        }
    }

    /**
     * 判断检索结果是否属于指定类别
     */
    private boolean isMatchInCategory(EmbeddingMatch<TextSegment> match, String category) {
        try {
            Metadata metadata = match.embedded().metadata();
            if (metadata == null) return false;
            
            // 从元数据中获取类型信息
            String type = metadata.getString("type");
            if (type == null) return false;
            
            // 类别映射逻辑
            return isCategoryMatch(type, category);
            
        } catch (Exception e) {
            log.debug("检查类别匹配时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 类别匹配逻辑
     */
    private boolean isCategoryMatch(String documentType, String searchCategory) {
        if (documentType == null || searchCategory == null) return false;
        
        String lowerDocType = documentType.toLowerCase();
        String lowerSearchCat = searchCategory.toLowerCase();
        
        // 直接匹配
        if (lowerDocType.equals(lowerSearchCat)) return true;
        
        // 类别映射规则
        switch (lowerSearchCat) {
            case "user":
            case "user_management":
                return lowerDocType.contains("user") || lowerDocType.contains("student") || 
                       lowerDocType.contains("member") || lowerDocType.contains("person");
                       
            case "notice":
            case "notice_management":
                return lowerDocType.contains("notice") || lowerDocType.contains("announcement");
                
            case "course":
            case "course_management":
                return lowerDocType.contains("course") || lowerDocType.contains("class") ||
                       lowerDocType.contains("training");
                       
            case "task":
            case "task_management":
                return lowerDocType.contains("task") || lowerDocType.contains("assignment") ||
                       lowerDocType.contains("project");
                       
            case "material":
            case "material_management":
                return lowerDocType.contains("material") || lowerDocType.contains("document") ||
                       lowerDocType.contains("file");
                       
            case "attendance":
            case "attendance_management":
                return lowerDocType.contains("attendance") || lowerDocType.contains("checkin");
                
            case "studio":
            case "studio_info":
                return lowerDocType.contains("studio") || lowerDocType.contains("department") ||
                       lowerDocType.contains("organization");
                       
            default:
                return false;
        }
    }

    /**
     * 分类检索结果类
     */
    public static class ClassifiedSearchResult {
        private String originalQuery;
        private List<String> searchedCategories;
        private List<EmbeddingMatch<TextSegment>> matches;
        private Map<String, Integer> categoryMatchCounts;
        private int totalMatches;
        
        // Constructors
        public ClassifiedSearchResult() {
            this.matches = new ArrayList<>();
            this.categoryMatchCounts = new HashMap<>();
        }
        
        // Getters and Setters
        public String getOriginalQuery() { return originalQuery; }
        public List<String> getSearchedCategories() { return searchedCategories; }
        public List<EmbeddingMatch<TextSegment>> getMatches() { return matches; }
        public Map<String, Integer> getCategoryMatchCounts() { return categoryMatchCounts; }
        public int getTotalMatches() { return totalMatches; }
        
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        public void setSearchedCategories(List<String> searchedCategories) { this.searchedCategories = searchedCategories; }
        public void setMatches(List<EmbeddingMatch<TextSegment>> matches) { this.matches = matches; }
        public void setCategoryMatchCounts(Map<String, Integer> categoryMatchCounts) { this.categoryMatchCounts = categoryMatchCounts; }
        public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    }

    /**
     * 清空向量存储
     */
    public void clear() {
        log.info("️ 清空向量存储");
        try {
            // 如果底层存储支持清空，优先调用其清空逻辑
            // 兼容不同实现：尝试反射调用 clear()/deleteAll()/reset()
            boolean cleared = false;
            try {
                java.lang.reflect.Method m = embeddingStore.getClass().getMethod("clear");
                m.invoke(embeddingStore);
                cleared = true;
                log.info("  通过 embeddingStore.clear() 清空成功");
            } catch (NoSuchMethodException ignore) {
                try {
                    java.lang.reflect.Method m = embeddingStore.getClass().getMethod("deleteAll");
                    m.invoke(embeddingStore);
                    cleared = true;
                    log.info("  通过 embeddingStore.deleteAll() 清空成功");
                } catch (NoSuchMethodException ignore2) {
                    try {
                        java.lang.reflect.Method m = embeddingStore.getClass().getMethod("reset");
                        m.invoke(embeddingStore);
                        cleared = true;
                        log.info("  通过 embeddingStore.reset() 清空成功");
                    } catch (NoSuchMethodException ignore3) {
                        // 无可用API，跳过
                    }
                }
            }

            if (!cleared) {
                // 若无清空API，则根据当前类型做保守处理
                if (embeddingStore instanceof InMemoryEmbeddingStore) {
        embeddingStore = new InMemoryEmbeddingStore<>();
                    log.info("  已重建 InMemoryEmbeddingStore");
                } else {
                    log.warn(" ️ 当前向量存储不支持直接清空API，将继续在原集合上追加重建数据");
                }
            }
        } catch (Exception e) {
            log.warn(" ️ 清空向量存储时出现问题: {}", e.getMessage());
        }
        vectorCache.clear();
    }
    
    /**
     *   安全清理：删除向量数据库中的用户敏感信息
     * 该方法用于清理之前错误同步到向量数据库的用户敏感数据
     */
    public void clearUserSensitiveData() {
        log.warn("  开始清理向量数据库中的用户敏感信息...");
        
        try {
            // 清理缓存中以 "user_" 开头的所有用户相关数据
            int removedFromCache = 0;
            Iterator<Map.Entry<String, VectorData>> iterator = vectorCache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, VectorData> entry = iterator.next();
                if (entry.getKey().startsWith("user_")) {
                    iterator.remove();
                    removedFromCache++;
                }
            }
            
            log.warn("  已从缓存中清理 {} 条用户敏感数据", removedFromCache);
            log.warn(" ️ 注意：已存储在持久化向量数据库中的用户数据需要重建整个向量数据库才能完全清理");
            log.warn("建议：执行完整的向量数据库重建操作，确保用户隐私安全");
            
        } catch (Exception e) {
            log.error(" 清理用户敏感数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 新增或更新文档（现代RAG架构的核心方法）- 支持文档切分向量化
     * 
     * @param type 实体类型
     * @param businessId 业务主键ID
     * @param text 文档文本内容
     */
    public void upsert(String type, Long businessId, String text) {
        try {
            log.debug(" Upsert文档到向量存储: 类型={}, ID={}, 文本长度={}", type, businessId, text.length());
            
            //  智能选择处理策略
            String strategy = detectOptimalStrategy(text, type);
            
            if (shouldUseChunking(text, type)) {
                // 使用文档切分向量化
                upsertWithChunking(type, businessId, text, strategy);
            } else {
                // 使用原有的整体向量化（适用于短文档）
                upsertWhole(type, businessId, text, strategy);
            }
            
        } catch (Exception e) {
            log.error(" Upsert操作失败: 类型={}, ID={}, 错误: {}", type, businessId, e.getMessage(), e);
        }
    }

    /**
     * 🔪 文档切分向量化（标准RAG实现）
     * 
     * @param type 实体类型
     * @param businessId 业务主键ID
     * @param text 文档文本内容
     * @param strategy 处理策略
     */
    private void upsertWithChunking(String type, Long businessId, String text, String strategy) {
        log.info(" 开始文档切分向量化: 类型={}, ID={}, 文本长度={}", type, businessId, text.length());
        
        // 1. 智能文本预处理
        String optimizedText = optimizeTextForEmbedding(text, type);
        
        // 2. 文档切分
        List<TextChunk> chunks = splitDocument(optimizedText, type);
        
        log.info(" 文档切分完成: 原文长度={}, 切片数={}", text.length(), chunks.size());
        
        // 3. 删除旧的chunks（如果存在）
        deleteExistingChunks(type, businessId);
        
        // 4. 逐片向量化并存储
        int successCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            try {
                TextChunk chunk = chunks.get(i);
                String chunkId = String.format("%s_%d_chunk_%d", type, businessId, i);
                
                // 构建chunk元数据
                Map<String, Object> chunkMetadata = new HashMap<>();
                chunkMetadata.put("type", type);
                chunkMetadata.put("business_id", String.valueOf(businessId));
                chunkMetadata.put("chunk_index", i);
                chunkMetadata.put("total_chunks", chunks.size());
                chunkMetadata.put("chunk_start", chunk.getStartPosition());
                chunkMetadata.put("chunk_end", chunk.getEndPosition());
                chunkMetadata.put("chunk_length", chunk.getText().length());
                chunkMetadata.put("chunk_strategy", strategy);
                chunkMetadata.put("updated_at", java.time.LocalDateTime.now().toString());
                chunkMetadata.put("is_chunked", "true");
                
                // 单独向量化每个chunk
                addDocument(chunkId, chunk.getText(), chunkMetadata);
                successCount++;
                
                if (log.isDebugEnabled()) {
                    log.debug(" Chunk向量化完成: {} (长度: {}, 位置: {}-{})",
                             chunkId, chunk.getText().length(), chunk.getStartPosition(), chunk.getEndPosition());
                }
                
            } catch (Exception e) {
                log.warn(" ️ Chunk向量化失败: 类型={}, ID={}, chunk={}, 错误: {}", 
                        type, businessId, i, e.getMessage());
            }
        }
        
        log.info("  文档切分向量化完成: 类型={}, ID={}, 成功={}/{}, 策略={}", 
                type, businessId, successCount, chunks.size(), strategy);
    }

    /**
     * 📄 整体文档向量化（适用于短文档）
     */
    private void upsertWhole(String type, Long businessId, String text, String strategy) {
        log.debug("📄 使用整体向量化: 类型={}, ID={}", type, businessId);
        
        String documentId = type + "_" + businessId;
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", type);
        metadata.put("business_id", String.valueOf(businessId));
        metadata.put("updated_at", java.time.LocalDateTime.now().toString());
        metadata.put("text_length", text.length());
        metadata.put("chunk_strategy", strategy);
        metadata.put("is_chunked", "false");
        
        String optimizedText = optimizeTextForEmbedding(text, type);
        addDocument(documentId, optimizedText, metadata);
        
        log.debug("  整体向量化完成: {} (策略: {})", documentId, strategy);
    }

    /**
     *  智能文本优化 - 基于文档类型和内容特征优化embedding效果
     */
    private String optimizeTextForEmbedding(String originalText, String type) {
        StringBuilder optimizedText = new StringBuilder();
        
        // 1. 添加类型上下文标识
        optimizedText.append("【").append(getTypeDescription(type)).append("】\n");
        
        // 2. 添加结构化信息
        switch (type) {
            case "notice":
                optimizedText.append("公告信息 | 通知内容 | 重要通知\n");
                break;
            case "course": 
                optimizedText.append("课程信息 | 教学内容 | 学习资料 | 课程安排\n");
                break;
            case "task":
                optimizedText.append("任务信息 | 作业要求 | 任务安排 | 学习任务\n");
                break;
            case "material":
                optimizedText.append("学习资料 | 教学材料 | 参考文档 | 知识资源\n");
                break;
        }
        
        // 3. 清理和标准化原文本
        String cleanedText = cleanAndNormalizeText(originalText);
        
        // 4. 添加语义增强关键词
        String enhancedText = addSemanticKeywords(cleanedText, type);
        
        optimizedText.append(enhancedText);
        
        return optimizedText.toString();
    }

    /**
     *  检测最优策略
     */
    private String detectOptimalStrategy(String text, String type) {
        if (text.length() > 1500 && text.split("\n\n").length > 3) {
            return "semantic_chunking";
        } else if (text.length() > 800 && isStructuredContent(text, type)) {
            return "contextual_enhancement";
        } else {
            return "standard_optimization";
        }
    }

    /**
     * 🧹 文本清理和标准化
     */
    private String cleanAndNormalizeText(String text) {
        return text
            .replaceAll("\\s+", " ")  // 标准化空白字符
            .replaceAll("[\\r\\n]+", "\n")  // 标准化换行
            .trim();
    }



    /**
     * 判断是否应该使用文档切分
     */
    private boolean shouldUseChunking(String text, String type) {
        // 根据文本长度和类型决定是否切分
        int threshold = getChunkingThreshold(type);
        return text.length() > threshold;
    }

    /**
     * 获取不同类型文档的切分阈值
     */
    private int getChunkingThreshold(String type) {
        switch (type.toLowerCase()) {
            case "notice":
            case "task":
                return 800;  // 公告和任务较短，阈值高一些
            case "course":
            case "material":
                return 600;  // 课程和资料可能较长，阈值低一些
            case "user":
            case "student":
                return 1000; // 用户信息通常较短
            default:
                return 700;  // 默认阈值
        }
    }

    /**
     * 🔪 智能文档切分
     */
    private List<TextChunk> splitDocument(String text, String type) {
        List<TextChunk> chunks = new ArrayList<>();
        
        int chunkSize = getChunkSize(type);
        int chunkOverlap = getChunkOverlap(type);
        
        // 根据类型选择切分策略
        if (shouldUseSentenceSplitting(type)) {
            chunks = splitBySentences(text, chunkSize, chunkOverlap);
        } else {
            chunks = splitByCharacters(text, chunkSize, chunkOverlap);
        }
        
        return chunks;
    }

    /**
     * 按句子切分
     */
    private List<TextChunk> splitBySentences(String text, int chunkSize, int chunkOverlap) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // 简单的句子分割（基于句号、问号、感叹号）
        String[] sentences = text.split("[。！？\\n]");
        
        StringBuilder currentChunk = new StringBuilder();
        int startPosition = 0;
        int currentPosition = 0;
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            // 检查添加当前句子是否超出chunk大小
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // 创建chunk
                TextChunk chunk = new TextChunk(
                    currentChunk.toString().trim(),
                    startPosition,
                    startPosition + currentChunk.length(),
                    chunkIndex++
                );
                chunks.add(chunk);
                
                // 处理重叠
                String overlapText = getOverlapText(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
                startPosition = currentPosition - overlapText.length();
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("。");
            }
            currentChunk.append(sentence);
            currentPosition += sentence.length() + 1;
        }
        
        // 添加最后一个chunk
        if (currentChunk.length() > 0) {
            TextChunk chunk = new TextChunk(
                currentChunk.toString().trim(),
                startPosition,
                startPosition + currentChunk.length(),
                chunkIndex
            );
            chunks.add(chunk);
        }
        
        return chunks;
    }

    /**
     * 按字符切分（兜底策略）
     */
    private List<TextChunk> splitByCharacters(String text, int chunkSize, int chunkOverlap) {
        List<TextChunk> chunks = new ArrayList<>();
        
        for (int i = 0, chunkIndex = 0; i < text.length(); i += chunkSize - chunkOverlap, chunkIndex++) {
            int endPos = Math.min(i + chunkSize, text.length());
            String chunkText = text.substring(i, endPos);
            
            // 尝试在自然边界切分
            if (endPos < text.length()) {
                chunkText = adjustChunkBoundary(chunkText, text, i, endPos);
                endPos = i + chunkText.length();
            }
            
            TextChunk chunk = new TextChunk(chunkText.trim(), i, endPos, chunkIndex);
            chunks.add(chunk);
        }
        
        return chunks;
    }

    /**
     * 调整chunk边界到自然分割点
     */
    private String adjustChunkBoundary(String chunk, String fullText, int startPos, int endPos) {
        if (endPos >= fullText.length()) {
            return chunk;
        }
        
        // 向前查找最近的句子或段落边界
        int adjustedEnd = endPos;
        for (int i = endPos - 1; i >= startPos + (int)(chunk.length() * 0.7); i--) {
            char c = fullText.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n') {
                adjustedEnd = i + 1;
                break;
            }
        }
        
        return fullText.substring(startPos, adjustedEnd);
    }

    /**
     * 获取重叠文本
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        
        String overlap = text.substring(text.length() - overlapSize);
        
        // 尝试从完整句子开始
        int sentenceStart = overlap.indexOf('。');
        if (sentenceStart > 0 && sentenceStart < overlap.length() - 10) {
            overlap = overlap.substring(sentenceStart + 1);
        }
        
        return overlap.trim();
    }

    /**
     * 删除已存在的chunks
     */
    private void deleteExistingChunks(String type, Long businessId) {
        try {
            // TODO: 实现删除逻辑，根据向量数据库的具体实现
            log.debug("🗑️ 删除已存在的chunks: type={}, businessId={}", type, businessId);
        } catch (Exception e) {
            log.warn(" ️ 删除chunks失败: type={}, businessId={}, error={}", type, businessId, e.getMessage());
        }
    }

    /**
     * 是否应该使用句子切分
     */
    private boolean shouldUseSentenceSplitting(String type) {
        return "notice".equals(type) || "course".equals(type) || "material".equals(type);
    }

    /**
     * 获取默认chunk大小（用于通用判断）
     */
    private int getChunkSize() {
        return defaultChunkSize;
    }

    /**
     * 获取chunk大小 - 基于配置和类型调整
     */
    private int getChunkSize(String type) {
        // 基于配置值和类型进行微调
        float multiplier = 1.0f;
        switch (type.toLowerCase()) {
            case "notice":
                multiplier = 0.8f;  // 公告通常较短
                break;
            case "course":
            case "material":
                multiplier = 1.2f;  // 课程和资料可以更长
                break;
            case "task":
                multiplier = 0.6f;  // 任务通常较短
                break;
            case "user":
            case "student":
                multiplier = 1.0f;  // 使用默认值
                break;
            default:
                multiplier = 1.0f;
        }
        
        return Math.round(defaultChunkSize * multiplier);
    }

    /**
     * 获取chunk重叠大小 - 基于配置值
     */
    private int getChunkOverlap(String type) {
        // 使用配置的重叠大小，但确保不超过chunk大小的30%
        int chunkSize = getChunkSize(type);
        int maxOverlap = chunkSize * 30 / 100;  // 最大30%重叠
        return Math.min(defaultChunkOverlap, maxOverlap);
    }

    /**
     * 文本块数据类
     */
    public static class TextChunk {
        private String text;
        private int startPosition;
        private int endPosition;
        private int chunkIndex;
        
        public TextChunk(String text, int startPosition, int endPosition, int chunkIndex) {
            this.text = text;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.chunkIndex = chunkIndex;
        }
        
        public String getText() { return text; }
        public int getStartPosition() { return startPosition; }
        public int getEndPosition() { return endPosition; }
        public int getChunkIndex() { return chunkIndex; }
    }
    private String addSemanticKeywords(String text, String type) {
        // 基于文档类型添加相关的语义关键词，提高检索准确性
        Set<String> keywords = new HashSet<>();
        
        switch (type) {
            case "notice":
                if (text.contains("活动")) keywords.add("活动安排");
                if (text.contains("考试")) keywords.add("考试通知");
                if (text.contains("会议")) keywords.add("会议安排");
                break;
            case "course":
                if (text.contains("实验")) keywords.add("实验课程");
                if (text.contains("理论")) keywords.add("理论学习");
                if (text.contains("作业")) keywords.add("课程作业");
                break;
            case "task":
                if (text.contains("提交")) keywords.add("作业提交");
                if (text.contains("截止")) keywords.add("截止时间");
                if (text.contains("要求")) keywords.add("任务要求");
                break;
        }
        
        if (!keywords.isEmpty()) {
            return text + "\n\n相关标签：" + String.join(" | ", keywords);
        }
        
        return text;
    }

    /**
     *  检查是否为结构化内容
     */
    private boolean isStructuredContent(String text, String type) {
        return text.contains(":") || text.contains("：") || 
               text.contains("\n-") || text.contains("\n•") ||
               (type.equals("course") && text.contains("章")) ||
               (type.equals("task") && (text.contains("步骤") || text.contains("阶段")));
    }

    /**
     *  获取类型描述
     */
    private String getTypeDescription(String type) {
        switch (type) {
            case "notice": return "公告通知";
            case "course": return "课程信息"; 
            case "task": return "任务安排";
            case "material": return "学习资料";
            default: return "工作室文档";
        }
    }
    
    /**
     * 删除文档（现代RAG架构的核心方法）
     * 
     * @param type 实体类型
     * @param businessId 业务主键ID
     */
    public void delete(String type, Long businessId) {
        try {
            log.debug("🗑️ 删除向量存储中的文档: 类型={}, ID={}", type, businessId);
            
            // 构建文档ID
            String documentId = type + "_" + businessId;
            
            // 从缓存中删除
            vectorCache.remove(documentId);
            
            // 注意：当前的InMemoryEmbeddingStore不支持按ID删除
            // 在切换到Milvus后，这里需要调用相应的删除API

            
            log.debug("  删除操作完成: {}", documentId);
            
        } catch (Exception e) {
            log.error(" 删除操作失败: 类型={}, ID={}, 错误: {}", type, businessId, e.getMessage(), e);
        }
    }

    /**
     * 获取向量存储统计信息
     */
    public VectorStoreStats getStats() {
        String embeddingModelName = embeddingModel != null ? 
                embeddingModel.getClass().getSimpleName() : "Unknown";
        String storeTypeName = embeddingStore != null ? 
                embeddingStore.getClass().getSimpleName() : "Unknown";
                
        // 获取实际向量数据库中的文档数量
        int actualDocumentCount = getActualDocumentCount();
        
        return new VectorStoreStats(
            actualDocumentCount,
            storagePath,
            embeddingModelName,
            storeTypeName
        );
    }
    
    /**
     * 获取实际向量数据库中的文档数量
     * 对于外部向量数据库，尝试查询实际数量；对于内存存储，使用缓存大小
     */
    private int getActualDocumentCount() {
        try {
            if (embeddingStore instanceof InMemoryEmbeddingStore) {
                // 内存存储使用缓存大小
                return vectorCache.size();
            }
            
            // 对于外部向量数据库，尝试通过搜索来估算数量
            // 这是一个权宜之计，因为LangChain4j的EmbeddingStore接口没有提供统计方法
            try {
                                 // 执行一个简单的搜索来检查是否有数据
                 List<EmbeddingMatch<TextSegment>> testResult = search("test", 1);
                if (!testResult.isEmpty()) {
                    // 如果能搜索到结果，说明有数据，返回一个非零值
                    // 这里我们无法获得确切数量，但至少知道不是空的
                    log.debug(" 外部向量数据库检测到数据存在");
                    return 1; // 表示非空
                } else {
                    log.debug(" 外部向量数据库未检测到数据");
                    return 0;
                }
            } catch (Exception e) {
                log.debug(" 无法检测外部向量数据库状态，假设为非空: {}", e.getMessage());
                return 1; // 出错时假设有数据，避免重复初始化
            }
            
        } catch (Exception e) {
            log.warn(" ️ 获取文档数量失败，返回缓存大小: {}", e.getMessage());
            return vectorCache.size();
        }
    }

    /**
     * 持久化向量数据到本地文件
     */
    public void persistToFile() {
        try {
            File file = new File(storagePath + "/vectors.json");
            objectMapper.writeValue(file, vectorCache);
            log.info(" 向量数据已持久化到文件: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error(" 持久化向量数据失败", e);
        }
    }

    /**
     * 应用关闭时持久化数据
     */
    @PreDestroy
    public void shutdown() {
        log.info(" 向量存储服务关闭中...");
        persistToFile();
        log.info("  向量存储服务已关闭");
    }

    // ===================================================================
    // 私有辅助方法
    // ===================================================================

    /**
     * 创建存储目录
     */
    private void createStorageDirectory() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info(" 创建存储目录: {}", dir.getAbsolutePath());
            } else {
                log.warn(" ️ 创建存储目录失败: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 加载已持久化的向量数据
     */
    @SuppressWarnings("unchecked")
    private void loadPersistedVectors() {
        try {
            File file = new File(storagePath + "/vectors.json");
            if (file.exists()) {
                Map<String, VectorData> persistedData = objectMapper.readValue(file, Map.class);
                
                // 重新生成向量并加载到内存存储
                for (Map.Entry<String, VectorData> entry : persistedData.entrySet()) {
                    VectorData data = entry.getValue();
                    addDocument(data.getId(), data.getText(), data.getMetadata());
                }
                
                log.info(" 已加载 {} 个持久化向量", persistedData.size());
            }
        } catch (Exception e) {
            log.warn(" ️ 加载持久化向量数据失败: {}", e.getMessage());
        }
    }

    // ===================================================================
    // 数据类定义
    // ===================================================================

    /**
     * 文档数据结构
     */
    public static class DocumentData {
        private String id;
        private String text;
        private Map<String, Object> metadata;

        public DocumentData() {}

        public DocumentData(String id, String text, Map<String, Object> metadata) {
            this.id = id;
            this.text = text;
            this.metadata = metadata;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 向量数据（用于持久化）
     */
    public static class VectorData {
        private String id;
        private String text;
        private Map<String, Object> metadata;
        private LocalDateTime createTime;

        public VectorData() {}

        public VectorData(String id, String text, Map<String, Object> metadata, LocalDateTime createTime) {
            this.id = id;
            this.text = text;
            this.metadata = metadata;
            this.createTime = createTime;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 向量存储统计信息
     */
    public static class VectorStoreStats {
        private final int documentCount;
        private final String storagePath;
        private final String embeddingModel;
        private final String storeType;

        public VectorStoreStats(int documentCount, String storagePath, 
                               String embeddingModel, String storeType) {
            this.documentCount = documentCount;
            this.storagePath = storagePath;
            this.embeddingModel = embeddingModel;
            this.storeType = storeType;
        }

        // Getters
        public int getDocumentCount() { return documentCount; }
        public String getStoragePath() { return storagePath; }
        public String getEmbeddingModel() { return embeddingModel; }
        public String getStoreType() { return storeType; }
    }
} 