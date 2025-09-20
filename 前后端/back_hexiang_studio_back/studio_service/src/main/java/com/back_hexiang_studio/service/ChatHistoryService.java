package com.back_hexiang_studio.service;


import com.back_hexiang_studio.entity.ChatHistory;

import java.util.List;

/**
 * 聊天历史记录服务接口
 */
public interface ChatHistoryService {

    /**
     * 保存用户消息
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param messageOrder 消息序号
     */
    void saveUserMessage(Long userId, String sessionId, String message, Integer messageOrder);

    /**
     * 保存AI回复
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param aiResponse AI回复内容
     * @param messageOrder 消息序号
     */
    void saveAiMessage(Long userId, String sessionId, String aiResponse, Integer messageOrder);

    /**
     * 保存完整对话（用户消息 + AI回复）
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param aiResponse AI回复
     */
    void saveConversation(Long userId, String sessionId, String userMessage, String aiResponse);

    /**
     * 获取用户的聊天历史（最近N条）
     * @param userId 用户ID
     * @param limit 限制条数，null表示不限制
     * @return 聊天历史列表
     */
    List<ChatHistory> getUserChatHistory(Long userId, Integer limit);

    /**
     * 获取指定会话的完整对话
     * @param sessionId 会话ID
     * @return 聊天历史列表（按消息顺序排列）
     */
    List<ChatHistory> getSessionHistory(String sessionId);

    /**
     * 获取用户最近的会话（最近N次对话）
     * @param userId 用户ID
     * @param sessionLimit 会话数限制
     * @return 聊天历史列表
     */
    List<ChatHistory> getRecentSessions(Long userId, Integer sessionLimit);

    /**
     * 清除用户的所有聊天记录
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int clearUserHistory(Long userId);

    /**
     * 删除指定会话
     * @param sessionId 会话ID
     * @return 删除的记录数
     */
    int deleteSession(String sessionId);

    /**
     * 清理过期的聊天记录
     * @param beforeDays 多少天前的记录
     * @return 删除的记录数
     */
    int cleanOldRecords(Integer beforeDays);

    /**
     * 统计用户的聊天记录数
     * @param userId 用户ID
     * @return 记录数
     */
    long countUserMessages(Long userId);

    /**
     * 获取用户的所有会话ID
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> getUserSessionIds(Long userId);

    /**
     * 生成新的会话ID
     * @param userId 用户ID
     * @return 会话ID
     */
    String generateSessionId(Long userId);

    /**
     * 清除用户的会话缓存（重置对话上下文）
     * @param userId 用户ID
     */
    void clearUserSessionCache(Long userId);

    /**
     * 清除所有过期的会话缓存
     */
    void cleanExpiredSessionCache();
} 