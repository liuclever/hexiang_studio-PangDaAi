package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天历史记录Mapper接口
 */
@Mapper
public interface ChatHistoryMapper {
    
    /**
     * 插入聊天记录
     * @param chatHistory 聊天记录
     * @return 影响行数
     */
    int insert(ChatHistory chatHistory);
    
    /**
     * 批量插入聊天记录
     * @param chatHistories 聊天记录列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<ChatHistory> chatHistories);
    
    /**
     * 根据用户ID获取聊天历史（按时间倒序）
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 聊天历史列表
     */
    List<ChatHistory> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 根据会话ID获取聊天历史（按消息顺序）
     * @param sessionId 会话ID
     * @return 聊天历史列表
     */
    List<ChatHistory> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取用户的最近N次会话
     * @param userId 用户ID
     * @param sessionLimit 会话数限制
     * @return 聊天历史列表
     */
    List<ChatHistory> selectRecentSessions(@Param("userId") Long userId, @Param("sessionLimit") Integer sessionLimit);
    
    /**
     * 删除用户的所有聊天记录
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 删除指定会话的聊天记录
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定时间之前的聊天记录
     * @param beforeDays 多少天前
     * @return 影响行数
     */
    int deleteOldRecords(@Param("beforeDays") Integer beforeDays);
    
    /**
     * 统计用户的聊天记录数
     * @param userId 用户ID
     * @return 记录数
     */
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> selectSessionIdsByUserId(@Param("userId") Long userId);
} 