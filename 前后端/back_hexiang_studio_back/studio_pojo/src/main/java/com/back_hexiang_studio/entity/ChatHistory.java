package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI聊天历史记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    
    /**
     * 聊天记录ID
     */
    private Long chatId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会话ID (同一次连续对话共享相同session_id)
     */
    private String sessionId;
    
    /**
     * 消息类型：user-用户消息，ai-AI回复
     */
    private String messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 消息序号（在同一会话中的顺序）
     */
    private Integer messageOrder;
    
    /**
     * 用户名（关联查询字段）
     */
    private String userName;
    
    /**
     * 用户真实姓名（关联查询字段）
     */
    private String realName;
} 