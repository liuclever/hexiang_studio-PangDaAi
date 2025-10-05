package com.back_hexiang_studio.controller.ai;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.entity.ChatHistory;

import com.back_hexiang_studio.pangDaAi.service.assistant.AssistantAgent;
import com.back_hexiang_studio.pangDaAi.util.Result;
import com.back_hexiang_studio.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI熊助手控制器
 * 处理AI熊的聊天消息、历史记录等功能
 * 权限：需要AI_CHAT_BASIC权限，所有认证用户可使用基础AI功能
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-15
 */
@RestController
@RequestMapping("/ai-bear")
@Slf4j
@PreAuthorize("hasAuthority('AI_CHAT_BASIC') or hasPermission(null, 'AI_CHAT_BASIC') or hasAuthority('ROLE_ADMIN')")
public class AiBearController {

    @Autowired
    private ChatHistoryService chatHistoryService;
    
    @Autowired
    private AssistantAgent assistantAgent;

    /**
     * 获取聊天历史记录
     * 返回当前用户的完整聊天历史（用户消息+AI回复）
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getChatHistory() {
        try {
            // 获取当前登录用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                log.warn("用户未登录，无法获取聊天历史");
                return Result.error("用户未登录");
            }

            // 获取用户最近的聊天记录（限制100条）
            List<ChatHistory> chatHistories = chatHistoryService.getUserChatHistory(currentUserId, 100);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> formattedHistory = chatHistories.stream()
                .map(this::formatChatMessage)
                .collect(Collectors.toList());

            log.info("获取聊天历史成功 - 用户ID: {}, 记录数: {}", currentUserId, formattedHistory.size());
            return Result.success(formattedHistory);

        } catch (Exception e) {
            log.error("获取聊天历史失败: {}", e.getMessage(), e);
            return Result.error("获取聊天历史失败");
        }
    }

    /**
     * 清除聊天历史记录
     */
    @DeleteMapping("/history")
    public Result<String> clearChatHistory() {
        try {
            // 获取当前登录用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                log.warn("用户未登录，无法清除聊天历史");
                return Result.error("用户未登录");
            }

            log.info(" 开始清理用户会话 - 用户ID: {}", currentUserId);
            
            // 🔧 Step 1: 强制清空AssistantAgent中的ChatMemory（优先清理，避免状态不一致）
            try {
                assistantAgent.clearUserAllChatMemories(currentUserId);
                log.info(" 已清空AssistantAgent内存缓存 - 用户ID: {}", currentUserId);
            } catch (Exception e) {
                log.error(" 清空AssistantAgent内存缓存失败 - 用户ID: {}, 错误: {}", currentUserId, e.getMessage());
                // 出现异常时使用紧急清理
                try {
                    assistantAgent.clearAllChatMemories();
                    log.warn("️ 已执行紧急清理所有ChatMemory");
                } catch (Exception ex) {
                    log.error(" 紧急清理也失败: {}", ex.getMessage());
                }
            }

            //   2: 清除会话缓存，重置对话上下文
            chatHistoryService.clearUserSessionCache(currentUserId);
            
            //  3: 清除用户的聊天记录（数据库层面）
            int deletedCount = chatHistoryService.clearUserHistory(currentUserId);
            
            //  4: 强制生成新的会话ID（确保下次对话是全新会话）
            String newSessionId = chatHistoryService.generateSessionId(currentUserId);
            
            log.info(" 清除聊天历史成功 - 用户ID: {}, 删除记录数: {}, 新会话ID: {}",
                    currentUserId, deletedCount, newSessionId);
            return Result.success("聊天记录已清除，共删除 " + deletedCount + " 条记录。已开启全新会话。");

        } catch (Exception e) {
            log.error(" 清除聊天历史失败: {}", e.getMessage(), e);
            
            //  异常情况下的紧急处理：强制清理所有缓存
            try {
                Long currentUserId = UserContextHolder.getCurrentId();
                if (currentUserId != null) {
                    assistantAgent.clearAllChatMemories();
                    chatHistoryService.clearUserSessionCache(currentUserId);
                    log.warn("️ 执行紧急会话清理");
                }
            } catch (Exception ex) {
                log.error(" 紧急清理失败: {}", ex.getMessage());
            }
            
            return Result.error("清除聊天历史失败，但已尝试重置会话状态");
        }
    }

    /**
     * 发送聊天消息（与现有AI助手集成）
     */
    @PostMapping("/chat")
    public Result<String> sendMessage(@RequestBody ChatRequest request) {
        try {
            // 获取当前登录用户ID
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                log.warn("用户未登录，无法发送消息");
                return Result.error("用户未登录");
            }

            // 生成会话ID
            String sessionId = chatHistoryService.generateSessionId(currentUserId);

            // 调用AI助手获取回复
            String aiResponse = assistantAgent.chat(request.getMessage(), sessionId);

            // 保存完整对话到历史记录
            chatHistoryService.saveConversation(currentUserId, sessionId, request.getMessage(), aiResponse);

            log.info("AI熊聊天成功 - 用户ID: {}", currentUserId);
            return Result.success(aiResponse);

        } catch (Exception e) {
            log.error("AI熊聊天失败: {}", e.getMessage(), e);
            return Result.error("AI助手暂时无法回复，请稍后再试");
        }
    }

    /**
     * 流式聊天接口（支持实时输出）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> sendMessageStream(@RequestBody ChatRequest request) {
        // 获取当前登录用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        if (currentUserId == null) {
            return Flux.just("用户未登录");
        }

        // 生成会话ID
        String sessionId = chatHistoryService.generateSessionId(currentUserId);
        
        log.info("AI熊流式聊天开始 - 用户ID: {}, 消息: {}", currentUserId, request.getMessage());

        return Flux.<String>create(sink -> {
            try {
                // 先保存用户消息
                chatHistoryService.saveUserMessage(currentUserId, sessionId, request.getMessage(), 1);
                
                StringBuilder aiResponseBuilder = new StringBuilder();

                // 调用AI助手进行流式处理
                assistantAgent.streamChat(request.getMessage(), sessionId, currentUserId,
                    chunk -> {
                        // 实时推送每个文字片段
                        aiResponseBuilder.append(chunk);
                        sink.next(chunk);
                    },
                    () -> {
                        // 流式处理完成，保存AI回复
                        String fullAiResponse = aiResponseBuilder.toString();
                        chatHistoryService.saveAiMessage(currentUserId, sessionId, fullAiResponse, 2);
                        
                        log.info("AI熊流式聊天完成 - 用户ID: {}", currentUserId);
                        sink.complete();
                    },
                    error -> {
                        // 处理错误
                        log.error("AI熊流式聊天失败 - 用户ID: {}, 错误: {}", currentUserId, error.getMessage(), error);
                        sink.error(error);
                    }
                );

            } catch (Exception e) {
                log.error("AI熊流式聊天启动失败 - 用户ID: {}, 错误: {}", currentUserId, e.getMessage(), e);
                sink.error(e);
            }
        });
    }

    /**
     * 获取小熊要说的话（欢迎消息）
     */
    @GetMapping("/message")
    public Result<Map<String, Object>> getBearMessage() {
        try {
            Map<String, Object> bearMessage = new HashMap<>();
            bearMessage.put("message", "你好！我是你的AI助理胖达，我不仅知道工作室的事情还知道，生活中的很多事情，有什么可以帮助你的吗？");
            bearMessage.put("type", "welcome");
            bearMessage.put("timestamp", System.currentTimeMillis());
            
            return Result.success(bearMessage);
            
        } catch (Exception e) {
            log.error("获取小熊消息失败: {}", e.getMessage(), e);
            return Result.error("获取消息失败");
        }
    }

    /**
     * 获取聊天统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getChatStats() {
        try {
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMessages", chatHistoryService.countUserMessages(currentUserId));
            stats.put("totalSessions", chatHistoryService.getUserSessionIds(currentUserId).size());
            
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("获取聊天统计失败: {}", e.getMessage(), e);
            return Result.error("获取统计信息失败");
        }
    }

    // ===================================================================
    // 工具方法
    // ===================================================================

    /**
     * 格式化聊天消息为前端需要的格式
     */
    private Map<String, Object> formatChatMessage(ChatHistory chatHistory) {
        Map<String, Object> message = new HashMap<>();
        
        message.put("id", chatHistory.getChatId());
        message.put("content", chatHistory.getContent());
        message.put("type", chatHistory.getMessageType()); // "user" 或 "ai"
        message.put("timestamp", chatHistory.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        message.put("sessionId", chatHistory.getSessionId());
        message.put("messageOrder", chatHistory.getMessageOrder());
        
        // 如果是用户消息，添加用户信息
        if ("user".equals(chatHistory.getMessageType())) {
            message.put("userName", chatHistory.getUserName());
            message.put("realName", chatHistory.getRealName());
        }
        
        return message;
    }

    /**
     * 聊天请求数据结构
     */
    public static class ChatRequest {
        private String message;     // 用户消息
        private String sessionId;   // 会话ID（可选）
        
        // Getters and Setters
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
} 