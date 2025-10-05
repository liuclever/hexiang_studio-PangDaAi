package com.back_hexiang_studio.pangDaAi.service;

import com.back_hexiang_studio.pangDaAi.config.AIModelProperties;
import com.back_hexiang_studio.context.UserContextHolder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态AI服务
 * 根据智能路由选择的模型动态创建和调用AI服务
 */
@Service
@Slf4j
public class DynamicAIService {

    @Autowired
    private ModelRouterService modelRouterService;
    
    @Autowired
    private AIModelProperties aiModelProperties;
    
    // 模型实例缓存，避免重复创建
    private final Map<String, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatModel> streamingChatModelCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("  DynamicAIService 正在初始化...");
        // 延迟预热，避免启动阻塞
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒后开始预热
                warmupModels();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("模型预热线程被中断");
            }
        }).start();
        log.info("  DynamicAIService 初始化完成");
    }
    
    /**
     * 获取动态选择的聊天模型
     */
    public ChatModel getChatModel(String userMessage, Long userId, String sessionId) {
        // 使用智能路由选择模型
        ModelRouterService.AIModel selectedModel = modelRouterService.selectModel(userMessage, userId, sessionId);
        String modelName = selectedModel.getModelName();
        
        // 从缓存获取或创建新实例
        return chatModelCache.computeIfAbsent(modelName, this::createChatModel);
    }
    
    /**
     * 获取动态选择的流式聊天模型
     */
    public StreamingChatModel getStreamingChatModel(String userMessage, Long userId, String sessionId) {
        // 使用智能路由选择模型
        ModelRouterService.AIModel selectedModel = modelRouterService.selectModel(userMessage, userId, sessionId);
        String modelName = selectedModel.getModelName();
        
        log.info("  动态选择模型: {} ({})", modelName, selectedModel.getDescription());
        
        // 从缓存获取或创建新实例
        return streamingChatModelCache.computeIfAbsent(modelName, this::createStreamingChatModel);
    }
    
    /**
     * 根据指定模型名获取聊天模型
     */
    public ChatModel getChatModelByName(String modelName) {
        return chatModelCache.computeIfAbsent(modelName, this::createChatModel);
    }
    
    /**
     * 根据指定模型名获取流式聊天模型
     */
    public StreamingChatModel getStreamingChatModelByName(String modelName) {
        return streamingChatModelCache.computeIfAbsent(modelName, this::createStreamingChatModel);
    }
    
    /**
     * 创建聊天模型实例
     */
    private ChatModel createChatModel(String modelName) {
        try {
            log.debug("🔧 创建聊天模型: {}", modelName);
            
            AIModelProperties.ModelConfig config = aiModelProperties.getModelConfig(modelName);
            int maxTokens = config != null && config.getMaxTokens() != null ? config.getMaxTokens() : 4000;
            
            return OpenAiChatModel.builder()
                    .apiKey(aiModelProperties.getApiKey())
                    .baseUrl(aiModelProperties.getBaseUrl())
                    .modelName(modelName)
                    .temperature(0.8)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(180))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("  创建聊天模型失败: {}, 使用默认模型", e.getMessage());
            // 回退到默认模型
            return OpenAiChatModel.builder()
                    .apiKey(aiModelProperties.getApiKey())
                    .baseUrl(aiModelProperties.getBaseUrl())
                    .modelName("qwen-plus")
                    .temperature(0.8)
                    .maxTokens(4000)
                    .timeout(Duration.ofSeconds(180))
                    .build();
        }
    }
    
    /**
     * 创建流式聊天模型实例
     */
    private StreamingChatModel createStreamingChatModel(String modelName) {
        try {
            log.debug("🔧 创建流式聊天模型: {}", modelName);
            
            AIModelProperties.ModelConfig config = aiModelProperties.getModelConfig(modelName);
            int maxTokens = config != null && config.getMaxTokens() != null ? config.getMaxTokens() : 4000;
            
            return OpenAiStreamingChatModel.builder()
                    .apiKey(aiModelProperties.getApiKey())
                    .baseUrl(aiModelProperties.getBaseUrl())
                    .modelName(modelName)
                    .temperature(0.8)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(180))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("  创建流式聊天模型失败: {}, 使用默认模型", e.getMessage());
            // 回退到默认模型
            return OpenAiStreamingChatModel.builder()
                    .apiKey(aiModelProperties.getApiKey())
                    .baseUrl(aiModelProperties.getBaseUrl())
                    .modelName("qwen-plus")
                    .temperature(0.8)
                    .maxTokens(4000)
                    .timeout(Duration.ofSeconds(180))
                    .build();
        }
    }
    
    /**
     * 预热常用模型（启动时调用）
     */
    public void warmupModels() {
        log.info("  开始预热常用模型...");
        
        String[] commonModels = {"qwen-flash", "qwen-plus", "qwen-plus-latest", "qwen-max"};
        
        for (String modelName : commonModels) {
            try {
                if (aiModelProperties.isModelExists(modelName)) {
                    getChatModelByName(modelName);
                    getStreamingChatModelByName(modelName);
                    log.debug("  预热模型: {}", modelName);
                }
            } catch (Exception e) {
                log.warn("  预热模型失败: {} - {}", modelName, e.getMessage());
            }
        }
        
        log.info("  模型预热完成");
    }
    
    /**
     * 清理模型缓存
     */
    public void clearModelCache() {
        log.info("🧹 清理模型缓存...");
        chatModelCache.clear();
        streamingChatModelCache.clear();
        log.info("  模型缓存清理完成");
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("聊天模型缓存: %d, 流式模型缓存: %d", 
            chatModelCache.size(), streamingChatModelCache.size());
    }
} 