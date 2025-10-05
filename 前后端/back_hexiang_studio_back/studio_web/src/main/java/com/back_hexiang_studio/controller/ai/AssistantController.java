package com.back_hexiang_studio.controller.ai;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.pangDaAi.service.assistant.AssistantAgent;
import com.back_hexiang_studio.pangDaAi.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI助手统一入口控制器
 * 接收用户自然语言输入，调用AI助手服务处理
 * 权限：需要AI_CHAT_BASIC权限，所有认证用户可使用基础AI功能
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-13
 */
@RestController
@RequestMapping("/ai-assistant")
@Slf4j
@PreAuthorize("hasAuthority('AI_CHAT_BASIC')")
public class AssistantController {

    @Autowired
    private AssistantAgent assistantAgent;

    /**
     * AI助手聊天接口
     * 用户发送自然语言，AI智能回复
     * 
     * @param request 聊天请求
     * @return AI助手回复
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        // 获取当前登录用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        String actualUserId = currentUserId != null ? currentUserId.toString() : request.getUserId();
        
        log.info(" AI助手接收聊天请求 - 登录用户: {}, 请求用户: {}, 消息: {}",
                currentUserId, request.getUserId(), request.getMessage());
        
        try {
            // 确保用户上下文正确设置（如果当前没有，则使用请求中的用户ID）
            if (currentUserId == null && request.getUserId() != null) {
                try {
                    Long requestUserId = Long.valueOf(request.getUserId());
                    UserContextHolder.setCurrentId(requestUserId);
                    log.debug("设置用户上下文: {}", requestUserId);
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID格式: {}", request.getUserId());
                }
            }
            
            // 生成会话ID
            String sessionId = generateSessionId(actualUserId);
            
            // 调用AI智能体（现在会自动保存聊天记录）
            String response = assistantAgent.chat(request.getMessage(), sessionId);
            
            log.info(" AI助手回复成功 - 用户: {}", actualUserId);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error(" AI助手处理失败 - 用户: {}, 错误: {}",
                     actualUserId, e.getMessage(), e);
            
            return Result.error("AI助手暂时无法处理您的请求，请稍后再试");
        } finally {
            // 清理用户上下文（如果是临时设置的）
            if (currentUserId == null && request.getUserId() != null) {
                UserContextHolder.clear();
            }
        }
    }

    /**
     * 获取AI助手服务状态
     */
    @GetMapping("/status")
    public Result<String> getStatus() {
        try {
            String status = assistantAgent.getServiceStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取AI助手状态失败: {}", e.getMessage(), e);
            return Result.error("无法获取服务状态");
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI助手服务运行正常");
    }

    /**
     * 生成会话ID
     * 
     * @param userId 用户ID
     * @return 会话ID
     */
    private String generateSessionId(String userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    /**
     * 聊天请求数据结构
     */
    public static class ChatRequest {
        private String userId;      // 用户ID
        private String message;     // 用户消息
        private String sessionId;   // 会话ID（可选）
        
        // Getters and Setters
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
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