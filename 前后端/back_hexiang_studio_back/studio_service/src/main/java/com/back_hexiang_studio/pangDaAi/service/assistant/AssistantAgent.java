package com.back_hexiang_studio.pangDaAi.service.assistant;

import com.back_hexiang_studio.pangDaAi.tool.api.WeatherToolService;
import com.back_hexiang_studio.service.ChatHistoryService;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.pangDaAi.tool.workflow.CourseManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.MaterialManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.NoticeManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.UserManagementTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// RAG相关导入
import com.back_hexiang_studio.pangDaAi.service.rag.RagRetriever;
import com.back_hexiang_studio.pangDaAi.service.rag.VectorStoreService;

/**
 * AI助手智能体 - 集成工具调用 + RAG向量检索
 * @version 9.0 - 重构为使用SmartToolSelector动态加载工作流工具
 */
@Service
@Slf4j
public class AssistantAgent {

    //  统一的系统提示词
    private static final String SYSTEM_PROMPT = "你是何湘工作室的智能助手胖达AI。\n\n" +
        " RAG背景信息的作用：\n" +
        "- 主要包含工具使用指南、组织架构等静态知识\n" +
        "- 帮助你理解何时使用哪个工具\n" +
        "-不包含人员信息和实时数据\n\n" +
        " 核心工作原则：\n" +
        "1. 工具调用绝对优先：所有实时数据（人员、公告、任务、考勤等）必须通过工具获取\n" +
        "2. 身份识别与授权：对于需要知道当前用户身份的操作（如查询“我的”信息、管理操作等），你必须在调用工具时，将用户问题中提供的 `currentUserId` 作为参数传递进去。\n" +
        "3. RAG背景信息主要用于：理解工具使用方法、工作室基本架构、联系方式\n" +
        "4. 管理操作会自动进行权限检查：所有管理类工具（用户、资料、公告、课程管理）内部已集成权限验证，无需额外权限检查\n" +
        "5. 身份查询→getCurrentUserProfile(currentUserId)，成员统计→getStudioMemberStatistics\n" +
        "6. 天气查询→getTodayWeather/getWeatherForecast，给出贴心建议\n" +
        "7. 新闻查询→getTodayNews/getNewsByDate（每天60秒读懂世界）\n" +
        "8. 数据展示→先获取数据，再用convertToTable转换为表格格式\n\n" +
        " 重要提醒：\n" +
        "- 绝不编造信息，只根据工具调用结果回答\n" +
        "- 工具调用失败时才参考RAG背景信息\n" +
        "- 不要描述工具调用过程，直接给出结果\n" +
        "- 不要提及用户ID，用姓名称呼用户\n" +
        "- RAG中的组织架构信息可能滞后，涉及人员统计时仍需调用工具验证\n\n" +
        " 错误处理安全原则：\n" +
        "- 遇到任何系统错误、异常或技术故障时，统一回复：'抱歉，当前网络不佳，请稍后重试'\n" +
        "- 绝不暴露任何技术细节：方法名、类名、数据库错误、服务异常等\n" +
        "- 不要提及具体的系统组件或内部逻辑\n" +
        "- 保持用户友好的语调，避免技术术语";

    @Autowired
    private StreamingChatModel streamingChatModel;
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private ChatHistoryService chatHistoryService;
    
    @Autowired
    private com.back_hexiang_studio.pangDaAi.service.DynamicAIService dynamicAIService;

    // RAG相关服务注入
    @Autowired
    private RagRetriever ragRetriever;

    
    // 智能工具选择器
    @Autowired
    private SmartToolSelector smartToolSelector;

    // 对话记忆存储
    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    
    // Assistant实例 - 统一接口
    private Assistant assistant;
    
    @PostConstruct
    private void initAssistant() {
        log.info("  初始化Assistant...");
        
        try {
            // 自动加载所有工具
            List<Object> allTools = smartToolSelector.getAllWorkflowTools();
            
            // 构建统一Assistant - 支持流式和同步
            this.assistant = AiServices.builder(Assistant.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemories.computeIfAbsent(
                        (String) memoryId, 
                        id -> MessageWindowChatMemory.withMaxMessages(20)
                    ))
                    .tools(allTools) // 传入所有扫描到的工具
                    .build();
                    
            log.info(" 统一Assistant初始化成功，共加载 {} 个工具。", allTools.size());
            
        } catch (Exception e) {
            log.error(" Assistant初始化失败: {}", e.getMessage(), e);
        }
    }




    /**
     * 统一的Assistant接口 - 支持流式和同步
     */
    private interface Assistant {
        @dev.langchain4j.service.SystemMessage(SYSTEM_PROMPT)
        TokenStream chatStreaming(@dev.langchain4j.service.MemoryId String sessionId,
                                @dev.langchain4j.service.UserMessage String userMessage);
        
        @dev.langchain4j.service.SystemMessage(SYSTEM_PROMPT)  
        String chatSync(@dev.langchain4j.service.MemoryId String sessionId,
                       @dev.langchain4j.service.UserMessage String userMessage);
    }

    /**
     * 流式聊天处理 - 集成RAG检索和工具调用
     */
    public void streamChat(String userMessage, String sessionId, Long userId,
                          Consumer<String> onChunk,
                          Runnable onComplete,
                          Consumer<Throwable> onError) {
        log.info(" 开始流式处理 [会话: {}]: {}", sessionId, userMessage);
        
        // 设置用户上下文
        final boolean needClearContext;
        if (userId != null) {
            UserContextHolder.setCurrentId(userId);
            needClearContext = true;
            log.debug(" 设置用户上下文: {}", userId);
        } else {
            needClearContext = false;
        }
        
        try {
            // 步骤1: RAG向量检索获取相关背景信息
            String ragContext = "";
            try {
                log.info(" 开始RAG向量检索...");
                Object retrievalResult = ragRetriever.retrieve(userMessage, 3, userId);
                
                if (retrievalResult != null) {
                    ragContext = retrievalResult.toString();
                    log.info(" RAG检索成功，获得背景信息: {}", ragContext.substring(0, Math.min(ragContext.length(), 100)) + "...");
                } else {
                    log.info(" RAG未找到相关背景信息");
                }
            } catch (Exception e) {
                log.warn(" RAG检索失败，继续使用工具调用: {}", e.getMessage());
            }
            
            // 步骤2: 构建精准的用户消息（只传递最相关的背景信息）
            String enhancedMessage;
            String userInfo = userId != null ? String.format("\\n\\n currentUserId: %d (这是当前用户的ID，你在调用需要身份验证的工具时必须把它作为参数传入)", userId) : "";
            
            //  精准背景信息筛选 - 避免信息过载
            String focusedContext = filterRelevantContext(ragContext, userMessage);
            
            if (!focusedContext.isEmpty()) {
                enhancedMessage = String.format(
                    "相关提示：%s%s\\n\\n用户问题：%s\\n\\n重要提醒：对于任何管理操作，必须先调用相应的权限检查工具验证用户权限。",
                    focusedContext, userInfo, userMessage);
                log.info(" 构建精准增强消息，筛选后背景信息长度: {} (原长度: {})", focusedContext.length(), ragContext.length());
            } else {
                enhancedMessage = userMessage + userInfo + "\\n\\n重要提醒：对于任何管理操作，必须先调用相应的权限检查工具验证用户权限。";
            }
            
            if (assistant != null) {
                //  智能工具选择 - 根据用户查询动态选择相关工具
                List<Object> relevantTools = smartToolSelector.selectRelevantTools(userMessage);
                log.info(" 为查询选择了 {} 个工具实例", relevantTools.size());
                
                log.info(" 使用智能Assistant处理增强消息...");
                
                TokenStream tokenStream;
                
                //  预防性会话健康检查
                ensureSessionHealth(sessionId, userId);
                
                //  使用智能路由选择最佳模型
                try {
                    dev.langchain4j.model.chat.StreamingChatModel dynamicModel = 
                        dynamicAIService.getStreamingChatModel(userMessage, userId, sessionId);
                    
                    // 重新构建使用动态模型和智能工具选择的Assistant
                    Assistant dynamicAssistant = AiServices.builder(Assistant.class)
                            .chatModel(chatModel)
                            .streamingChatModel(dynamicModel)
                            .chatMemoryProvider(memoryId -> chatMemories.computeIfAbsent(
                                (String) memoryId, 
                                id -> MessageWindowChatMemory.withMaxMessages(20)
                            ))
                            .tools(relevantTools) // 传入选择的工具
                            .build();
                    
                    tokenStream = dynamicAssistant.chatStreaming(sessionId, enhancedMessage);
                    log.info("  使用动态模型+智能工具选择进行对话");
                    
                } catch (Exception e) {
                    log.warn("  动态选择失败，使用原有方案: {}", e.getMessage());
                    tokenStream = assistant.chatStreaming(sessionId, enhancedMessage);
                }
                
                StringBuilder fullResponse = new StringBuilder();
                
                tokenStream.onPartialResponse(token -> {
                    fullResponse.append(token);
                    onChunk.accept(token);
                });
                
                tokenStream.onCompleteResponse(response -> {
                    try {
                        String finalResult = fullResponse.toString();
                        log.info(" 完整响应: {}", finalResult);
                        
                    
                        if (userId != null && chatHistoryService != null) {
                            log.info(" 保存聊天记录...");
                            chatHistoryService.saveConversation(userId, sessionId, userMessage, finalResult);
                            log.info("  聊天记录保存成功");
                        }
                        
                        onComplete.run();
                        
                       
                        if (needClearContext) {
                            UserContextHolder.clear();
                            log.debug("  延迟清理用户上下文（在完成回调后）");
                        }
                    } catch (Exception ex) {
                        log.error("  完成回调失败: {}", ex.getMessage(), ex);
                        onError.accept(ex);
                    }
                });
                
                tokenStream.onError(error -> {
                    log.error("  流式处理失败: {}", error.getMessage(), error);
                    
                    //   特殊处理LangChain4j工具调用错误
                    if (error.getMessage() != null && error.getMessage().contains("tool_calls") && 
                        error.getMessage().contains("tool messages responding")) {
                        log.warn("  检测到工具调用状态不一致错误，尝试强制清理ChatMemory");
                        
                        try {
                            // 强制清理当前会话的ChatMemory
                            if (sessionId != null && chatMemories.containsKey(sessionId)) {
                                ChatMemory memory = chatMemories.get(sessionId);
                                if (memory != null) {
                                    memory.clear();
                                    log.info("  已强制清理会话 {} 的ChatMemory", sessionId);
                                }
                                chatMemories.remove(sessionId);
                            }
                            
                            
                            if (userId != null) {
                                String userPrefix = userId + "_";
                                chatMemories.entrySet().removeIf(entry -> {
                                    if (entry.getKey().startsWith(userPrefix)) {
                                        try {
                                            if (entry.getValue() != null) {
                                                entry.getValue().clear();
                                            }
                                        } catch (Exception e) {
                                            log.warn("清理ChatMemory时出错: {}", e.getMessage());
                                        }
                                        return true;
                                    }
                                    return false;
                                });
                                log.info("  已清理用户 {} 的所有ChatMemory", userId);
                            }
                            
                        } catch (Exception cleanupError) {
                            log.error("  清理ChatMemory时发生错误: {}", cleanupError.getMessage(), cleanupError);
                        }
                    }
                    
                    onError.accept(error);
                    
                    //   在错误时也要清理上下文
                    if (needClearContext) {
                        UserContextHolder.clear();
                        log.debug("  错误时清理用户上下文");
                    }
                });
                
                tokenStream.start();
                
            } else {
                log.error(" StreamingAssistant未初始化");
                onError.accept(new RuntimeException("StreamingAssistant未初始化"));
            }
            
        } catch (Exception e) {
            log.error(" 流式处理启动失败: {}", e.getMessage(), e);
            onError.accept(e);
            //   启动失败时立即清理上下文
            if (needClearContext) {
                UserContextHolder.clear();
                log.debug("  启动失败时清理用户上下文");
            }
        }
        //   移除finally中的上下文清理，改为在回调中清理
    }

    /**
     *  精准背景信息过滤 - 避免AI信息过载
     * 根据用户查询筛选最相关的背景信息，而不是传递所有信息
     */
    private String filterRelevantContext(String ragContext, String userMessage) {
        if (ragContext == null || ragContext.trim().isEmpty()) {
            return "";
        }
        
        try {
            String lowerQuery = userMessage.toLowerCase();
            StringBuilder focusedContext = new StringBuilder();
            
            //  根据用户查询关键词精准匹配相关信息
            String[] contextLines = ragContext.split("\\n");
            int relevantLinesCount = 0;
            int maxLines = 8; // 最多返回8行相关信息
            
            // 高优先级关键词 - 直接返回相关工具指导
            if (containsAny(lowerQuery, "用户", "成员", "人员", "档案", "添加", "删除", "修改")) {
                focusedContext.append("用户管理工具：查询档案用getCurrentUserProfile，管理操作已集成权限验证\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "部门", "统计", "组织", "工作室")) {
                focusedContext.append("工作室信息工具：成员统计用getStudioMemberStatistics，部门详情用getDepartmentDetails\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "公告", "通知", "消息")) {
                focusedContext.append("公告管理工具：查询用getLatestNotices，管理操作已集成权限验证\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "考勤", "签到", "出勤")) {
                focusedContext.append("考勤管理工具：统计用getAttendanceStatistics\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "任务", "作业", "项目")) {
                focusedContext.append("任务管理工具：用户任务用getUserTasks，我的任务用getCurrentUserUncompletedTasks\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "课程", "培训", "上课")) {
                focusedContext.append("课程管理工具：课程列表用getCourseList，管理操作需先检查权限\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "资料", "材料", "文档", "文件")) {
                focusedContext.append("资料管理工具：分类查询用getAllMaterialCategories，管理操作需先检查权限\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "天气", "预报", "气温")) {
                focusedContext.append("天气查询工具：今日天气用getTodayWeather，预报用getWeatherForecast\\n");
                relevantLinesCount++;
            }
            
            if (containsAny(lowerQuery, "新闻", "资讯", "头条")) {
                focusedContext.append("新闻查询工具：今日新闻用getTodayNews，指定日期用getNewsByDate\\n");
                relevantLinesCount++;
            }
            
            // 如果没有匹配到具体工具，返回通用指导（限制长度）
            if (relevantLinesCount == 0) {
                for (String line : contextLines) {
                    if (relevantLinesCount >= 3) break; // 最多3行通用信息
                    
                    if (line.contains("工具") || line.contains("权限") || line.contains("联系")) {
                        focusedContext.append(line.trim()).append("\\n");
                        relevantLinesCount++;
                    }
                }
            }
            
            String result = focusedContext.toString().trim();
            
            // 限制总长度，避免信息过载
            if (result.length() > 300) {
                result = result.substring(0, 300) + "...";
            }
            
            log.debug(" 精准过滤结果：匹配{}行，原长度{}→筛选后{}", relevantLinesCount, ragContext.length(), result.length());
            return result;
            
        } catch (Exception e) {
            log.warn("  背景信息过滤失败，返回空: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * 检查字符串是否包含任意一个关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }



    /**
     * 同步聊天处理 
     */
    public String chat(String userMessage, String sessionId) {
        log.info(" 开始同步处理 [会话: {}]: {}", sessionId, userMessage);
        
        // 获取用户ID
        Long userId = null;
        try {
            userId = UserContextHolder.getCurrentId();
        } catch (Exception e) {
            log.warn("  无法获取用户ID: {}", e.getMessage());
        }
        
        try {
            // 使用统一Assistant的同步方法
            String response = assistant.chatSync(sessionId, userMessage);
            
            // 保存聊天记录
            if (userId != null && chatHistoryService != null) {
                chatHistoryService.saveConversation(userId, sessionId, userMessage, response);
                log.debug(" 同步聊天记录已保存");
            }
            
            log.info("  同步处理完成 [会话: {}]", sessionId);
            return response;
            
        } catch (Exception e) {
            log.error("  同步处理失败: {}", e.getMessage(), e);
            return "抱歉，处理您的请求时出现了问题：" + e.getMessage();
        }
    }

  

    /**
     *   清空指定用户的ChatMemory缓存
     */
    public void clearUserChatMemory(String sessionId) {
        if (sessionId != null && chatMemories.containsKey(sessionId)) {
            chatMemories.remove(sessionId);
            log.info("  已清空会话内存缓存: {}", sessionId);
        } else {
            log.warn("  会话内存缓存不存在或sessionId为空: {}", sessionId);
        }
    }
    
    /**
     *   清空指定用户所有相关的ChatMemory缓存
     */
    public void clearUserAllChatMemories(Long userId) {
        if (userId == null) {
            log.warn("  用户ID为空，无法清空内存缓存");
            return;
        }
        
        // 清空该用户相关的所有会话缓存
        String userPrefix = userId + "_";
        int removedCount = 0;
        
        //   先统计要删除的数量
        for (String sessionId : chatMemories.keySet()) {
            if (sessionId.startsWith(userPrefix)) {
                removedCount++;
            }
        }
        
        //   使用迭代器安全删除，并强制清理内存
        chatMemories.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(userPrefix)) {
                // 强制清理ChatMemory内部状态
                try {
                    ChatMemory memory = entry.getValue();
                    if (memory != null) {
                        memory.clear(); // 清空内存中的对话历史
                    }
                } catch (Exception e) {
                    log.warn("  清理ChatMemory内部状态失败: {}", e.getMessage());
                }
                return true;
            }
            return false;
        });
        
        log.info("  已清空用户 {} 的所有内存缓存，清空数量: {}", userId, removedCount);
    }
    
    /**
     *   强制清空所有ChatMemory缓存（紧急情况使用）
     */
    public void clearAllChatMemories() {
        int totalSize = chatMemories.size();
        
        // 强制清理所有ChatMemory内部状态
        chatMemories.values().forEach(memory -> {
            try {
                if (memory != null) {
                    memory.clear();
                }
            } catch (Exception e) {
                log.warn("  清理ChatMemory状态失败: {}", e.getMessage());
            }
        });
        
        chatMemories.clear();
        log.warn("  紧急清空所有ChatMemory缓存，总数: {}", totalSize);
    }
    
    /**
     *   
     * 只在真正必要时清理ChatMemory，避免破坏正常对话上下文
     */
    private void ensureSessionHealth(String sessionId, Long userId) {
        try {
            if (sessionId == null) {
                return;
            }
            
            ChatMemory memory = chatMemories.get(sessionId);
            if (memory != null) {
                // 检查ChatMemory的消息数量，只有在极端情况下才清理
                int messageCount = memory.messages().size();
                
                //   调整阈值：只有超过100条消息才认为异常
                if (messageCount > 100) {
                    log.warn("  会话 {} 消息数量过多 ({}), 清理重建", sessionId, messageCount);
                    memory.clear();
                    chatMemories.remove(sessionId);
                    // 重新创建
                    chatMemories.put(sessionId, MessageWindowChatMemory.withMaxMessages(30));
                } else {
                    log.debug("  会话 {} 健康状态良好，消息数量: {}", sessionId, messageCount);
                }
                
                //   移除过于激进的工具调用检查，避免误判
                // 正常的工具调用完成后不应该清理会话
            }
            
            log.debug("  会话健康检查完成 - sessionId: {}", sessionId);
            
        } catch (Exception e) {
            log.warn("  会话健康检查时出错: {}, 但保留现有会话", e.getMessage());
    
            if (e.getClass().equals(OutOfMemoryError.class)) {
                log.error(" 内存不足，强制清理会话: {}", sessionId);
                if (sessionId != null && chatMemories.containsKey(sessionId)) {
                    chatMemories.remove(sessionId);
                    chatMemories.put(sessionId, MessageWindowChatMemory.withMaxMessages(30));
                }
            }
        }
    }

    /**
     * 获取服务状态
     */
    public String getServiceStatus() {
        return " 胖达AI智能体服务状态：\n" +
               "  StreamingChatModel: 已注入\n" +
               "  ChatModel: 已注入\n" +
               "  RAG向量检索: 已集成\n" +
               "  Assistant: " + (assistant != null ? "已初始化" : "未初始化") + "\n" +
               " 当前ChatMemory缓存数: " + chatMemories.size();
    }
} 


