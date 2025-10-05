package com.back_hexiang_studio.pangDaAi.config;

import com.back_hexiang_studio.pangDaAi.service.memory.PersistentChatMemoryStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 1.4.0 核心配置类
 * 使用OpenAI兼容方式访问DashScope（阿里云通义千问）
 * 支持Milvus向量数据库和持久化对话记忆
 */
@Configuration
@Slf4j
public class LangChainConfig {

    @Autowired
    private PersistentChatMemoryStore persistentChatMemoryStore;

    // ===================================================================
    // 从 application.yml 读取配置
    // ===================================================================

    @Value("${pangda-ai.memory.max-messages:20}")
    private Integer maxMessages;

    @Value("${pangda-ai.enabled:true}")
    private Boolean aiEnabled;
    
    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String dashscopeApiKey;
    
    @Value("${langchain4j.open-ai.chat-model.provider:openai}")
    private String chatProvider;
    
    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-plus}")
    private String modelName;
    
    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;
    
    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${langchain4j.open-ai.chat-model.timeout:60s}")
    private String timeoutStr;
    
    @Value("${langchain4j.open-ai.chat-model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${langchain4j.embedding-model.model-name:text-embedding-v2}")
    private String embeddingModelName;

    @Value("${langchain4j.embedding-store.provider:inmemory}")
    private String embeddingStoreProvider;

    @Value("${langchain4j.embedding-store.host:localhost}")
    private String milvusHost;

    @Value("${langchain4j.embedding-store.port:19530}")
    private Integer milvusPort;

    @Value("${langchain4j.embedding-store.database-name:hexiang_studio}")
    private String milvusDatabaseName;

    @Value("${langchain4j.embedding-store.collection-name:hexiang_studio_rag}")
    private String milvusCollectionName;

    @Value("${langchain4j.embedding-store.dimension:1536}")
    private Integer milvusDimension;

    // ===================================================================
    // Chat / StreamingChat 模型配置验证
    // 依赖 Spring Boot Starter 自动装配，但验证工具调用支持
    // ===================================================================
    
    @Autowired(required = false)
    private ChatModel injectedChatModel;
    
    @Autowired(required = false) 
    private StreamingChatModel injectedStreamingChatModel;
    
    @Bean
    public String chatModelValidation() {
        log.info(" 验证 ChatModel 和 StreamingChatModel 注入状态");
        
        if (injectedChatModel != null) {
            log.info(" ChatModel 已正确注入: {}", injectedChatModel.getClass().getSimpleName());
        } else {
            log.error(" ChatModel 注入失败 - 请检查 application.yml 中的 langchain4j.open-ai.chat-model 配置");
        }
        
        if (injectedStreamingChatModel != null) {
            log.info(" StreamingChatModel 已正确注入: {}", injectedStreamingChatModel.getClass().getSimpleName());
        } else {
            log.error(" StreamingChatModel 注入失败 - 请检查 application.yml 中的 langchain4j.open-ai.streaming-chat-model 配置");
        }
        
        // 检查配置参数
        log.info(" 模型配置参数验证:");
        log.info("   - API Key: {}", dashscopeApiKey != null && !dashscopeApiKey.trim().isEmpty() ? " 已配置" : " 未配置");
        log.info("   - Base URL: {}", baseUrl);
        log.info("   - Model Name: {}", modelName);
        log.info("   - Temperature: {}", temperature);
        log.info("   - Max Tokens: {}", maxTokens);
        log.info("   - Timeout: {}", timeoutStr);
        
        return "ChatModel validation completed";
    }

    // ===================================================================
    // 向量存储配置
    // ===================================================================

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info(" 配置向量存储服务 ");
        //   选择muilvus作为向量存储服务
        if ("milvus".equalsIgnoreCase(embeddingStoreProvider)) {
            return createMilvusEmbeddingStore();
        }
        return new InMemoryEmbeddingStore<>();
    }

    private EmbeddingStore<TextSegment> createMilvusEmbeddingStore() {
        log.info(" Milvus配置 - Host: {}, Port: {}, Database: {}, Collection: {}, Dimension: {}",
                milvusHost, milvusPort, milvusDatabaseName, milvusCollectionName, milvusDimension);
        try {
            log.info(" 开始创建 Milvus 连接...");


            // 创建 MilvusEmbeddingStore
            Class<?> milvusClass = Class.forName("dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore");
            Object builderObj = milvusClass.getDeclaredMethod("builder").invoke(null);

            Class<?> builderClass = builderObj.getClass();

            // 优先使用 host/port 组合；若不支持再回退到 uri()
            boolean addressConfigured = false;
            try {
                builderObj = builderClass.getMethod("host", String.class).invoke(builderObj, milvusHost);
                try {
                    builderObj = builderClass.getMethod("port", Integer.TYPE).invoke(builderObj, milvusPort);
                } catch (NoSuchMethodException ignore) {
                    builderObj = builderClass.getMethod("port", Integer.class).invoke(builderObj, milvusPort);
                }
                addressConfigured = true;
                log.debug(" 使用 host/port 方法设置连接地址");
            } catch (NoSuchMethodException nsmeHost) {
                // 回退到 uri
                String milvusUri = String.format("http://%s:%d", milvusHost, milvusPort);
                log.info(" host/port 不可用，尝试使用 uri: {}", milvusUri);
                try {
                    builderObj = builderClass.getMethod("uri", String.class).invoke(builderObj, milvusUri);
                    addressConfigured = true;
                    log.debug(" 使用 uri() 方法设置连接地址");
                } catch (NoSuchMethodException nsmeUri) {
                    log.warn("️ Milvus builder不支持 host/port 也不支持 uri 方法，依赖版本可能不匹配");
                }
            }

            if (!addressConfigured) {
                log.error(" 无法为 Milvus 配置连接地址，回退到内存存储");
                return new InMemoryEmbeddingStore<>();
            }




            builderClass = builderObj.getClass();
            builderObj = builderClass.getMethod("databaseName", String.class).invoke(builderObj, milvusDatabaseName);
            builderObj = builderClass.getMethod("collectionName", String.class).invoke(builderObj, milvusCollectionName);
            try {
                builderObj = builderClass.getMethod("dimension", Integer.TYPE).invoke(builderObj, milvusDimension);
            } catch (NoSuchMethodException ignore) {
                builderObj = builderClass.getMethod("dimension", Integer.class).invoke(builderObj, milvusDimension);
            }

            
            // 为 lambda 表达式保存 final 引用
            final Object finalBuilder = builderObj;
            final Class<?> finalBuilderClass = builderClass;
            
            // 使用 CompletableFuture 为 build() 添加 15 秒超时保护
            java.util.concurrent.CompletableFuture<EmbeddingStore<TextSegment>> buildFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
            @SuppressWarnings("unchecked")
                        EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) finalBuilderClass.getMethod("build").invoke(finalBuilder);
                        return store;
                    } catch (Exception e) {
                        throw new RuntimeException("Milvus build 失败: " + e.getMessage(), e);
                    }
                });

            EmbeddingStore<TextSegment> store;
            try {
                store = buildFuture.get(15, java.util.concurrent.TimeUnit.SECONDS);
                log.info(" Milvus向量存储创建成功，耗时 < 15秒");
            } catch (java.util.concurrent.TimeoutException e) {
                return new InMemoryEmbeddingStore<>();
            } catch (Exception e) {
                log.error(" Milvus 构建失败: {}", e.getMessage());
                return new InMemoryEmbeddingStore<>();
            }

            log.info(" 开始连接测试...");
            // 简单连接测试（允许失败，不中断）
            try {
                dev.langchain4j.store.embedding.EmbeddingSearchRequest req =
                        dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                        .queryEmbedding(dev.langchain4j.data.embedding.Embedding.from(new float[milvusDimension]))
                        .maxResults(1)
                        .build();
                store.search(req);
                log.info(" Milvus连接成功");
            } catch (Exception testEx) {
                log.warn(" Milvus连接测试失败，但存储实例已创建: {}", testEx.getMessage());
            }
            return store;
        } catch (Exception e) {
            log.error("Milvus配置失败，回退到内存存储", e);
            return new InMemoryEmbeddingStore<>();
        }
    }

    // ===================================================================
    // 对话记忆配置
    // ===================================================================

    @Bean
    @Primary
    public ChatMemory persistentChatMemory() {
        log.info(" 记忆配置 - 最大消息数: {}", maxMessages);
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }


    // ===================================================================
    // 工具方法
    // ===================================================================

    public boolean isAiEnabled() { return aiEnabled; }
    public boolean isDashscopeConfigured() { return dashscopeApiKey != null && !dashscopeApiKey.trim().isEmpty(); }

} 