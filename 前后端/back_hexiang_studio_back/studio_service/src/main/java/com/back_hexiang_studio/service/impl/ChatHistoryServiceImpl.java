package com.back_hexiang_studio.service.impl;


import com.back_hexiang_studio.entity.ChatHistory;
import com.back_hexiang_studio.mapper.ChatHistoryMapper;
import com.back_hexiang_studio.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 聊天历史记录服务实现类
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    //  添加会话缓存机制 - 30分钟内保持相同会话ID
    private static final Map<Long, SessionInfo> userSessionCache = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30分钟

    //  会话信息数据类
    private static class SessionInfo {
        private final String sessionId;
        private final long timestamp;

        public SessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    @Override
    @Transactional
    public void saveUserMessage(Long userId, String sessionId, String message, Integer messageOrder) {
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageType("user")
                .content(message)
                .messageOrder(messageOrder)
                .createTime(LocalDateTime.now())
                .build();
        
        chatHistoryMapper.insert(chatHistory);
        log.debug("保存用户消息成功 - 用户ID: {}, 会话ID: {}", userId, sessionId);
    }

    @Override
    @Transactional
    public void saveAiMessage(Long userId, String sessionId, String aiResponse, Integer messageOrder) {
        ChatHistory chatHistory = ChatHistory.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageType("ai")
                .content(aiResponse)
                .messageOrder(messageOrder)
                .createTime(LocalDateTime.now())
                .build();
        
        chatHistoryMapper.insert(chatHistory);
        log.debug("保存AI回复成功 - 用户ID: {}, 会话ID: {}", userId, sessionId);
    }

    @Override
    @Transactional
    public void saveConversation(Long userId, String sessionId, String userMessage, String aiResponse) {
        log.debug("保存对话开始 - 用户ID: {}, 会话ID: {}, 用户消息长度: {}, AI回复长度: {}",
                userId, sessionId, 
                (userMessage != null ? userMessage.length() : "null"),
                (aiResponse != null ? aiResponse.length() : "null"));
        
        // 获取当前会话的最大消息序号
        List<ChatHistory> existingMessages = chatHistoryMapper.selectBySessionId(sessionId);
        AtomicInteger maxOrder = new AtomicInteger(0);
        existingMessages.forEach(msg -> {
            if (msg.getMessageOrder() > maxOrder.get()) {
                maxOrder.set(msg.getMessageOrder());
            }
        });

        int nextOrder = maxOrder.get() + 1;

        // 保存用户消息
        saveUserMessage(userId, sessionId, userMessage, nextOrder);
        
        // 保存AI回复
        saveAiMessage(userId, sessionId, aiResponse, nextOrder + 1);
        
        log.info("保存完整对话成功 - 用户ID: {}, 会话ID: {}", userId, sessionId);
    }

    @Override
    public List<ChatHistory> getUserChatHistory(Long userId, Integer limit) {
        return chatHistoryMapper.selectByUserId(userId, limit);
    }

    @Override
    public List<ChatHistory> getSessionHistory(String sessionId) {
        return chatHistoryMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<ChatHistory> getRecentSessions(Long userId, Integer sessionLimit) {
        return chatHistoryMapper.selectRecentSessions(userId, sessionLimit);
    }

    @Override
    @Transactional
    public int clearUserHistory(Long userId) {
        int deletedCount = chatHistoryMapper.deleteByUserId(userId);
        log.info("清除用户聊天记录成功 - 用户ID: {}, 删除记录数: {}", userId, deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public int deleteSession(String sessionId) {
        int deletedCount = chatHistoryMapper.deleteBySessionId(sessionId);
        log.info("删除会话记录成功 - 会话ID: {}, 删除记录数: {}", sessionId, deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public int cleanOldRecords(Integer beforeDays) {
        int deletedCount = chatHistoryMapper.deleteOldRecords(beforeDays);
        log.info("清理过期聊天记录成功 - {}天前的记录, 删除记录数: {}", beforeDays, deletedCount);
        return deletedCount;
    }

    @Override
    public long countUserMessages(Long userId) {
        return chatHistoryMapper.countByUserId(userId);
    }

    @Override
    public List<String> getUserSessionIds(Long userId) {
        return chatHistoryMapper.selectSessionIdsByUserId(userId);
    }

    @Override
    public String generateSessionId(Long userId) {
        // 检查缓存中是否存在当前用户的会话
        SessionInfo cachedSession = userSessionCache.get(userId);
        if (cachedSession != null && System.currentTimeMillis() - cachedSession.getTimestamp() < SESSION_TIMEOUT) {
            log.debug("从缓存加载会话ID - 用户ID: {}, 缓存会话ID: {}", userId, cachedSession.getSessionId());
            return cachedSession.getSessionId();
        }

        String newSessionId = "session_" + userId + "_" + System.currentTimeMillis();
        userSessionCache.put(userId, new SessionInfo(newSessionId));
        log.debug("生成新会话ID - 用户ID: {}, 新会话ID: {}", userId, newSessionId);
        return newSessionId;
    }

    /**
     * 清除用户的会话缓存（重置对话上下文）
     */
    public void clearUserSessionCache(Long userId) {
        // 清除本地会话缓存
        userSessionCache.remove(userId);
        log.info("清除用户会话缓存 - 用户ID: {}", userId);
    }

    /**
     * 清除所有过期的会话缓存
     */
    public void cleanExpiredSessionCache() {
        long now = System.currentTimeMillis();
        userSessionCache.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue().getTimestamp() >= SESSION_TIMEOUT;
            if (expired) {
                log.debug("清除过期会话缓存 - 用户ID: {}", entry.getKey());
            }
            return expired;
        });
    }


} 
