package com.back_hexiang_studio.controller.ai;

import com.back_hexiang_studio.context.UserContextHolder;

import com.back_hexiang_studio.pangDaAi.service.assistant.AssistantAgent;
import com.back_hexiang_studio.pangDaAi.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.TimeUnit;

/**
 * AI助手流式输出控制器
 * 支持 Flux<> 流式输出，实现实时AI对话
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-13
 */
@RestController
@RequestMapping("/ai-assistant/stream")
@Slf4j
@CrossOrigin
public class StreamingAssistantController {

    @Autowired
    private AssistantAgent assistantAgent;

    // 🔧 Redis缓存支持（可选，如果Redis不可用则使用内存缓存）
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    // 🔧 内存缓存作为备用 - 30分钟内保持相同会话ID
    private static final Map<String, SessionInfo> userSessionCache = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30分钟
    private static final String REDIS_SESSION_PREFIX = "ai_session:";

    /**
     * 流式聊天接口
     * 使用 Flux<String> 实现实时流式输出
     * 
     * @param request 聊天请求
     * @return 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        // 获取当前登录用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        String actualUserId = currentUserId != null ? currentUserId.toString() : request.getUserId();
        
        log.info("🌊 流式AI助手接收聊天请求 - 登录用户: {}, 请求用户: {}, 消息: {}", 
                currentUserId, request.getUserId(), request.getMessage());

        // 🔧 优先使用前端传入的会话ID，否则生成持久化会话ID
        String sessionId = generateOrGetSessionId(actualUserId, request.getSessionId());
        
        // 保存当前的Security上下文，防止异步处理时丢失权限信息
        SecurityContext securityContext = SecurityContextHolder.getContext();
        
        return Flux.<String>create(sink -> {
            try {
                // 在异步线程中恢复Security上下文
                SecurityContextHolder.setContext(securityContext);
                
                // 构建包含用户上下文的消息
                String contextualMessage = buildUserContextualMessage(request.getMessage(), currentUserId);
                
                // 调用AI助手进行流式处理（传递userId解决异步ThreadLocal丢失问题）
                assistantAgent.streamChat(contextualMessage, sessionId, currentUserId,
                    chunk -> {
                        // 实时推送每个文字片段
                        sink.next(chunk);
                    },
                    () -> {
                        // 处理完成
                        log.info("🌊 流式对话完成 - 用户: {}", request.getUserId());
                        sink.complete();
                        
                        //  清理Security上下文
                        SecurityContextHolder.clearContext();
                    },
                    error -> {
                        // 处理错误
                        log.error(" 流式对话失败 - 用户: {}, 错误: {}",
                                 request.getUserId(), error.getMessage(), error);
                        sink.error(error);
                        
                        // 清理Security上下文
                        SecurityContextHolder.clearContext();
                    }
                );
                
            } catch (Exception e) {
                log.error(" 流式处理启动失败 - 用户: {}, 错误: {}",
                         request.getUserId(), e.getMessage(), e);
                sink.error(e);
                
                //  清理Security上下文
                SecurityContextHolder.clearContext();
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // 🔒 使用支持阻塞操作的调度器
        .doOnNext(chunk -> {
            // 调试日志：记录流式输出片段
            log.debug(" 推送流式片段 - 用户: {}, 长度: {}", request.getUserId(), chunk.length());
        });
    }

    /**
     * 流式聊天接口（带进度信息）
     * 返回 JSON 格式的流式数据，包含类型和内容
     * 
     * @param request 聊天请求
     * @return 结构化流式响应
     */
    @PostMapping(value = "/chat-with-progress", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<StreamResponse> streamChatWithProgress(@RequestBody ChatRequest request) {
        // 获取当前登录用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        String actualUserId = currentUserId != null ? currentUserId.toString() : request.getUserId();
        
        log.info("🌊 带进度的流式AI助手接收请求 - 登录用户: {}, 请求用户: {}, 消息: {}", 
                currentUserId, request.getUserId(), request.getMessage());
        
        String sessionId = generateOrGetSessionId(actualUserId, request.getSessionId());
        
        // 🔒 保存当前的Security上下文
        SecurityContext securityContext = SecurityContextHolder.getContext();
        
        return Flux.<StreamResponse>create(sink -> {
            try {
                // 🔒 在异步线程中恢复Security上下文
                SecurityContextHolder.setContext(securityContext);
                
                // 构建包含用户上下文的消息
                String contextualMessage = buildUserContextualMessage(request.getMessage(), currentUserId);
                
                // 发送开始信息
                sink.next(StreamResponse.progress("🔍 正在分析您的问题..."));
                
                // 调用简化后的AI助手进行处理
                assistantAgent.streamChat(contextualMessage, sessionId, currentUserId,
                    // 流式内容回调
                    chunk -> sink.next(StreamResponse.content(chunk)),
                    // 完成回调
                    () -> {
                        sink.next(StreamResponse.complete("✅ 对话完成"));
                        sink.complete();
                        
                        // 🔒 清理Security上下文
                        SecurityContextHolder.clearContext();
                    },
                    // 错误回调
                    error -> {
                        sink.next(StreamResponse.error("❌ 处理失败: " + error.getMessage()));
                        sink.error(error);
                        
                        // 🔒 清理Security上下文
                        SecurityContextHolder.clearContext();
                    }
                );
                
            } catch (Exception e) {
                log.error("🌊 带进度流式处理启动失败: {}", e.getMessage(), e);
                sink.next(StreamResponse.error("❌ 启动失败: " + e.getMessage()));
                sink.error(e);
                
                // 🔒 清理Security上下文
                SecurityContextHolder.clearContext();
            }
        })
        .subscribeOn(Schedulers.boundedElastic()); // 🔒 使用支持阻塞操作的调度器
    }

    /**
     * Server-Sent Events 流式聊天接口
     * 兼容前端 EventSource API
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @return SSE 流式响应
     */
    @GetMapping(value = "/chat-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<String> streamChatSSE(@RequestParam String userId, @RequestParam String message) {
        // 获取当前登录用户ID
        Long currentUserId = UserContextHolder.getCurrentId();
        String actualUserId = currentUserId != null ? currentUserId.toString() : userId;
        
        log.info("📡 SSE流式AI助手接收请求 - 登录用户: {}, 请求用户: {}, 消息: {}", 
                currentUserId, userId, message);
        
        String sessionId = generateOrGetSessionId(actualUserId, null); // SSE没有sessionId参数
        
        // 🔒 保存当前的Security上下文
        SecurityContext securityContext = SecurityContextHolder.getContext();
        
        return Flux.<String>create(sink -> {
            try {
                // 🔒 在异步线程中恢复Security上下文
                SecurityContextHolder.setContext(securityContext);
                
                // 构建包含用户上下文的消息
                String contextualMessage = buildUserContextualMessage(message, currentUserId);
                
                // 发送开始事件
                sink.next("event: start\ndata: 🌊 开始流式对话\n\n");
                
                assistantAgent.streamChat(contextualMessage, sessionId, currentUserId,
                    chunk -> {
                        // SSE 格式推送
                        sink.next("event: message\ndata: " + chunk + "\n\n");
                    },
                    () -> {
                        sink.next("event: complete\ndata: ✅ 对话完成\n\n");
                        sink.complete();
                        
                        // 🔒 清理Security上下文
                        SecurityContextHolder.clearContext();
                    },
                    error -> {
                        sink.next("event: error\ndata: ❌ 错误: " + error.getMessage() + "\n\n");
                        sink.error(error);
                        
                        // 🔒 清理Security上下文
                        SecurityContextHolder.clearContext();
                    }
                );
                
            } catch (Exception e) {
                sink.next("event: error\ndata: ❌ 启动失败: " + e.getMessage() + "\n\n");
                sink.error(e);
                
                // 🔒 清理Security上下文
                SecurityContextHolder.clearContext();
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // 🔒 使用支持阻塞操作的调度器
        .delayElements(Duration.ofMillis(50)); // 控制推送频率
    }

    /**
     * 🧪 测试工具调用功能
     */
    @PostMapping("/test-tool-call")
    public ResponseEntity<Result<String>> testToolCall(@RequestBody Map<String, String> request) {
        log.info("🧪 开始测试工具调用功能");
        
        try {
            String testMessage = request.getOrDefault("message", "现在几点了？");
            String sessionId = generateOrGetSessionId("test_user", "test_tool_call_session");
            
            // 获取当前用户ID
            Long userId = UserContextHolder.getCurrentId();
            if (userId == null) {
                userId = 1L; // 测试用户ID
            }
            
            log.info("🧪 测试参数 - 用户: {}, 消息: {}, 会话: {}", userId, testMessage, sessionId);
            
            // 使用AssistantAgent进行同步测试
            String response = assistantAgent.chat(testMessage, sessionId);
            
            log.info("🧪 测试结果: {}", response);
            
            return ResponseEntity.ok(Result.success("工具调用测试完成", response));
            
        } catch (Exception e) {
            log.error("🧪 测试工具调用失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error("测试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取流式助手状态
     */
    @GetMapping("/status")
    public Result<String> getStreamStatus() {
        try {
            String status = "🌊 流式AI助手服务正常运行！\n\n" +
                           "📡 支持的流式接口：\n" +
                           "• POST /stream/chat - Flux<String> 流式文本\n" +
                           "• POST /stream/chat-with-progress - 带进度的结构化流式\n" +
                           "• GET /stream/chat-sse - Server-Sent Events\n\n" +
                           "🎯 特性：\n" +
                           "• 实时逐字输出\n" +
                           "• 进度状态推送\n" +
                           "• 持久化对话记忆\n" +
                           "• Qwen 模型支持\n" +
                           "• 错误处理和恢复\n\n" +
                           "💡 推荐使用 chat-with-progress 接口获得最佳体验！";
                           
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取流式状态失败: {}", e.getMessage(), e);
            return Result.error("无法获取流式服务状态");
        }
    }

    // ===================================================================
    // 工具方法和数据类
    // ===================================================================

    /**
     * 构建用户上下文消息
     * 🔒 保护隐私：不在消息中暴露用户ID，但保持上下文语义
     */
    private String buildUserContextualMessage(String originalMessage, Long currentUserId) {
        if (currentUserId == null) {
            return originalMessage;
        }
        
        String lowerMessage = originalMessage.toLowerCase();
        
        // 检测是否是个人相关的问题
        String[] personalKeywords = {
            "我的", "我有", "我参加", "我的任务", "我的考勤", "我的课程", 
            "我需要", "我要", "给我", "帮我查", "查一下我", "我今天", "我这个月",
            "我是谁", "我是", "谁是我", "我的信息", "我的档案", "我的资料"
        };
        
        boolean isPersonalQuery = false;
        for (String keyword : personalKeywords) {
            if (lowerMessage.contains(keyword)) {
                isPersonalQuery = true;
                break;
            }
        }
        
        if (isPersonalQuery) {
            // 🔒 不暴露用户ID，但添加个人查询的上下文标记
            return String.format("当前用户询问：%s", originalMessage);
        }
        
        return originalMessage;
    }

    /**
     * 生成或获取会话ID（支持Redis持久化）
     */
    private String generateOrGetSessionId(String userId, String providedSessionId) {
        if (providedSessionId != null && !providedSessionId.isEmpty()) {
            log.debug("🔧 使用前端提供的会话ID: {}", providedSessionId);
            return providedSessionId;
        }

        String sessionKey = userId + "_stream_session";
        
        // 🔧 优先从Redis获取（如果可用）
        SessionInfo sessionInfo = getSessionFromRedis(sessionKey);
        
        // 🔧 Redis不可用时从内存缓存获取
        if (sessionInfo == null) {
            sessionInfo = userSessionCache.get(sessionKey);
        }

        if (sessionInfo != null && System.currentTimeMillis() - sessionInfo.getTimestamp() < SESSION_TIMEOUT) {
            log.debug("🔧 从缓存获取会话ID (有效期): {}", sessionInfo.getSessionId());
            return sessionInfo.getSessionId();
        }

        // 🔧 生成新会话ID
        String newSessionId = userId + "_stream_" + System.currentTimeMillis();
        SessionInfo newSession = new SessionInfo(newSessionId);
        
        // 🔧 同时保存到Redis和内存
        saveSessionToRedis(sessionKey, newSession);
        userSessionCache.put(sessionKey, newSession);
        
        log.debug("🔧 生成新的会话ID (持久化): {}", newSessionId);
        return newSessionId;
    }
    
    /**
     * 从Redis获取会话信息
     */
    private SessionInfo getSessionFromRedis(String sessionKey) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            String redisKey = REDIS_SESSION_PREFIX + sessionKey;
            String sessionData = redisTemplate.opsForValue().get(redisKey);
            
            if (sessionData != null) {
                String[] parts = sessionData.split(":");
                if (parts.length == 2) {
                    String sessionId = parts[0];
                    long timestamp = Long.parseLong(parts[1]);
                    return new SessionInfo(sessionId, timestamp);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 从Redis获取会话失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 保存会话信息到Redis
     */
    private void saveSessionToRedis(String sessionKey, SessionInfo sessionInfo) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            String redisKey = REDIS_SESSION_PREFIX + sessionKey;
            String sessionData = sessionInfo.getSessionId() + ":" + sessionInfo.getTimestamp();
            
            // 设置过期时间为会话超时时间
            redisTemplate.opsForValue().set(redisKey, sessionData, SESSION_TIMEOUT, TimeUnit.MILLISECONDS);
            log.debug("💾 会话信息已保存到Redis: {}", sessionInfo.getSessionId());
        } catch (Exception e) {
            log.warn("⚠️ 保存会话到Redis失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清除指定用户的会话信息
     */
    public void clearUserSession(String userId) {
        String sessionKey = userId + "_stream_session";
        
        // 清除Redis中的会话
        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_SESSION_PREFIX + sessionKey;
                redisTemplate.delete(redisKey);
                log.debug("🗑️ 已从Redis清除用户会话: {}", userId);
            } catch (Exception e) {
                log.warn("⚠️ 从Redis清除会话失败: {}", e.getMessage());
            }
        }
        
        // 清除内存缓存中的会话
        userSessionCache.remove(sessionKey);
        log.debug("🗑️ 已从内存清除用户会话: {}", userId);
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

    /**
     * 流式响应数据结构
     */
    public static class StreamResponse {
        private String type;        // 响应类型：progress, content, complete, error
        private String data;        // 响应内容
        private long timestamp;     // 时间戳
        
        public StreamResponse(String type, String data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        // 静态工厂方法
        public static StreamResponse progress(String message) {
            return new StreamResponse("progress", message);
        }
        
        public static StreamResponse content(String content) {
            return new StreamResponse("content", content);
        }
        
        public static StreamResponse complete(String message) {
            return new StreamResponse("complete", message);
        }
        
        public static StreamResponse error(String message) {
            return new StreamResponse("error", message);
        }
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            this.data = data;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    // 🔧 添加会话信息数据类
    private static class SessionInfo {
        private final String sessionId;
        private final long timestamp;

        public SessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public SessionInfo(String sessionId, long timestamp) {
            this.sessionId = sessionId;
            this.timestamp = timestamp;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
} 