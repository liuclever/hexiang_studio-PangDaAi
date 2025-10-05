package com.back_hexiang_studio.pangDaAi.service.memory;

import dev.langchain4j.data.message.ChatMessage;
// 注释暂时不兼容的ChatMemoryStore接口，0.29.1版本中此接口可能不存在
// import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 持久化对话记忆存储
 * 兼容LangChain4j 0.29.1版本的独立聊天记忆管理类
 * 
 * @author 胖达AI助手开发团队
 * @version 2.1 - 兼容0.29.1版本
 * @since 2025-09-13
 */
@Service
@Slf4j
public class PersistentChatMemoryStore {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 如果没有 Redis，使用内存存储作为降级
    private final java.util.Map<Object, List<ChatMessage>> memoryFallback = 
            new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Redis key 前缀
     */
    private static final String REDIS_KEY_PREFIX = "chat_memory:";
    
    /**
     * 默认过期时间：7天
     */
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    /**
     * 获取指定记忆ID的聊天消息列表
     * 
     * @param memoryId 记忆ID（通常是用户ID或会话ID）
     * @return 聊天消息列表
     */
    public List<ChatMessage> getMessages(Object memoryId) {
        log.debug("  获取对话记忆 - memoryId: {}", memoryId);
        
        try {
            if (redisTemplate != null) {
                return getMessagesFromRedis(memoryId);
            } else {
                return getMessagesFromMemory(memoryId);
            }
        } catch (Exception e) {
            log.error("  获取对话记忆失败 - memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新指定记忆ID的聊天消息列表
     * 
     * @param memoryId 记忆ID
     * @param messages 聊天消息列表
     */
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        log.debug("  更新对话记忆 - memoryId: {}, 消息数量: {}", memoryId, messages.size());
        
        try {
            if (redisTemplate != null) {
                updateMessagesInRedis(memoryId, messages);
            } else {
                updateMessagesInMemory(memoryId, messages);
            }
            
            log.info("  对话记忆更新成功 - memoryId: {}", memoryId);
            
        } catch (Exception e) {
            log.error("  更新对话记忆失败 - memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
        }
    }

    /**
     * 删除指定记忆ID的所有聊天消息
     * 
     * @param memoryId 记忆ID
     */
    public void deleteMessages(Object memoryId) {
        log.info("  删除对话记忆 - memoryId: {}", memoryId);
        
        try {
            if (redisTemplate != null) {
                deleteMessagesFromRedis(memoryId);
            } else {
                deleteMessagesFromMemory(memoryId);
            }
            
            log.info("  对话记忆删除成功 - memoryId: {}", memoryId);
            
        } catch (Exception e) {
            log.error("  删除对话记忆失败 - memoryId: {}, 错误: {}", memoryId, e.getMessage(), e);
        }
    }

    // ===================================================================
    // Redis 存储实现
    // ===================================================================

    /**
     * 从 Redis 获取消息
     */
    @SuppressWarnings("unchecked")
    private List<ChatMessage> getMessagesFromRedis(Object memoryId) {
        String key = REDIS_KEY_PREFIX + memoryId;
        Object messagesObj = redisTemplate.opsForValue().get(key);
        
        if (messagesObj instanceof List) {
            return (List<ChatMessage>) messagesObj;
        }
        
        return new ArrayList<>();
    }

    /**
     * 向 Redis 更新消息
     */
    private void updateMessagesInRedis(Object memoryId, List<ChatMessage> messages) {
        String key = REDIS_KEY_PREFIX + memoryId;
        redisTemplate.opsForValue().set(key, messages, DEFAULT_TTL.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 从 Redis 删除消息
     */
    private void deleteMessagesFromRedis(Object memoryId) {
        String key = REDIS_KEY_PREFIX + memoryId;
        redisTemplate.delete(key);
    }

    // ===================================================================
    // 内存存储实现（降级方案）
    // ===================================================================

    /**
     * 从内存获取消息
     */
    private List<ChatMessage> getMessagesFromMemory(Object memoryId) {
        return memoryFallback.getOrDefault(memoryId, new ArrayList<>());
    }

    /**
     * 向内存更新消息
     */
    private void updateMessagesInMemory(Object memoryId, List<ChatMessage> messages) {
        memoryFallback.put(memoryId, new ArrayList<>(messages));
    }

    /**
     * 从内存删除消息
     */
    private void deleteMessagesFromMemory(Object memoryId) {
        memoryFallback.remove(memoryId);
    }

    // ===================================================================
    // 管理方法
    // ===================================================================

    /**
     * 获取所有记忆ID（仅内存模式支持）
     * 
     * @return 记忆ID集合
     */
    public java.util.Set<Object> getAllMemoryIds() {
        if (redisTemplate != null) {
            // Redis 模式：扫描所有 key
            try {
                java.util.Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
                return keys.stream()
                    .map(key -> key.replace(REDIS_KEY_PREFIX, ""))
                    .collect(java.util.stream.Collectors.toSet());
            } catch (Exception e) {
                log.warn("获取所有记忆ID失败: {}", e.getMessage());
                return java.util.Collections.emptySet();
            }
        } else {
            // 内存模式
            return memoryFallback.keySet();
        }
    }

    /**
     * 清空所有对话记忆
     */
    public void clearAllMemories() {
        log.warn("🗑 清空所有对话记忆");
        
        if (redisTemplate != null) {
            try {
                java.util.Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("️ Redis 中的对话记忆已清空，共删除 {} 个记忆", keys.size());
                }
            } catch (Exception e) {
                log.error("清空 Redis 对话记忆失败: {}", e.getMessage(), e);
            }
        } else {
            memoryFallback.clear();
            log.info("🗑 内存中的对话记忆已清空");
        }
    }

    /**
     * 获取对话记忆统计信息
     * 
     * @return 统计信息字符串
     */
    public String getMemoryStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("  对话记忆存储统计：\n\n");
        
        if (redisTemplate != null) {
            stats.append(" 存储模式：Redis 持久化存储\n");
            try {
                java.util.Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
                int keyCount = keys != null ? keys.size() : 0;
                stats.append(" 总记忆数量：").append(keyCount).append(" 个\n");
                stats.append(" 默认过期时间：").append(DEFAULT_TTL.toDays()).append(" 天\n");
            } catch (Exception e) {
                stats.append(" 获取统计信息失败：").append(e.getMessage()).append("\n");
            }
        } else {
            stats.append(" 存储模式：内存存储（降级模式）\n");
            stats.append(" 总记忆数量：").append(memoryFallback.size()).append(" 个\n");
            stats.append(" 注意：重启后数据将丢失\n");
        }
        
        return stats.toString();
    }
} 