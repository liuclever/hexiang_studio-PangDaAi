package com.back_hexiang_studio.dv.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * AI助手聊天响应数据结构
 * 支持普通文本回复和歧义消解选择项两种模式
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    
    /**
     * 响应类型
     * "text" - 普通文本回复
     * "choices" - 选择项回复（歧义消解）
     */
    private String type;
    
    /**
     * 普通文本回复内容
     * 当 type = "text" 时使用
     */
    private String content;
    
    /**
     * 歧义消解提示语
     * 当 type = "choices" 时显示给用户的说明文字
     */
    private String prompt;
    
    /**
     * 选择项列表
     * 当 type = "choices" 时使用
     */
    private List<ChoiceOption> choices;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 是否是流式响应的一部分
     */
    private Boolean isStreaming;
    
    // ===================================================================
    // 静态工厂方法
    // ===================================================================
    
    /**
     * 创建普通文本响应
     */
    public static ChatResponse text(String content) {
        return new ChatResponse("text", content, null, null, null, false);
    }
    
    /**
     * 创建普通文本响应（带会话ID）
     */
    public static ChatResponse text(String content, String sessionId) {
        return new ChatResponse("text", content, null, null, sessionId, false);
    }
    
    /**
     * 创建选择项响应
     */
    public static ChatResponse choices(String prompt, List<ChoiceOption> choices) {
        return new ChatResponse("choices", null, prompt, choices, null, false);
    }
    
    /**
     * 创建选择项响应（带会话ID）
     */
    public static ChatResponse choices(String prompt, List<ChoiceOption> choices, String sessionId) {
        return new ChatResponse("choices", null, prompt, choices, sessionId, false);
    }
    
    /**
     * 创建流式文本响应
     */
    public static ChatResponse streamingText(String content, String sessionId) {
        return new ChatResponse("text", content, null, null, sessionId, true);
    }
} 