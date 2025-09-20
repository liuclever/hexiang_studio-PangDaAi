package com.back_hexiang_studio.controller.test;

import com.back_hexiang_studio.pangDaAi.test.SimpleToolTest;
import com.back_hexiang_studio.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import com.back_hexiang_studio.pangDaAi.service.assistant.AssistantAgent;

/**
 * 🧪 简单工具调用测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/public/test/tool")  // 修改为公开API，不需要身份验证
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class SimpleToolTestController {

    @Autowired
    private SimpleToolTest simpleToolTest;

    @Autowired
    private AssistantAgent assistantAgent;

    /**
     * �� 测试基础工具调用功能（公开API）
     */
    @PostMapping("/simple")
    public Result<String> testSimpleToolCall(@RequestBody Map<String, String> request) {
        log.info("🧪 收到简单工具调用测试请求（公开API）");
        
        try {
            String message = request.getOrDefault("message", "现在几点了？");
            log.info("🧪 测试消息: {}", message);
            
            String response = simpleToolTest.testSimpleToolCall(message);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("❌ 简单工具调用测试控制器异常", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 🧪 预设测试用例（公开API）
     */
    @GetMapping("/preset/{testCase}")
    public Result<String> testPresetCase(@PathVariable String testCase) {
        log.info("🧪 运行预设测试用例（公开API）: {}", testCase);
        
        String message;
        switch (testCase) {
            case "time":
                message = "现在几点了？";
                break;
            case "math":
                message = "请帮我计算 15 + 27 等于多少";
                break;
            case "both":
                message = "请告诉我现在几点，然后计算 8 + 12";
                break;
            default:
                return Result.error("未知的测试用例: " + testCase);
        }
        
        try {
            String response = simpleToolTest.testSimpleToolCall(message);
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("❌ 预设测试用例执行失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 🧪 测试流式工具调用功能（模拟前端调用方式）
     */
    @PostMapping("/test-streaming")
    public ResponseEntity<String> testStreamingToolCall(@RequestBody Map<String, String> request) {
        log.info("🌊 开始测试流式工具调用功能");
        
        try {
            String testMessage = request.getOrDefault("message", "查询工作室成员统计信息");
            String sessionId = "test_streaming_" + System.currentTimeMillis();
            Long userId = 1L; // 测试用户ID
            
            log.info("🌊 测试参数 - 用户: {}, 消息: {}, 会话: {}", userId, testMessage, sessionId);
            
            // 使用AssistantAgent进行流式测试
            StringBuilder fullResponse = new StringBuilder();
            assistantAgent.streamChat(testMessage, sessionId, userId,
                chunk -> {
                    // 收集流式片段
                    fullResponse.append(chunk);
                    log.debug("🌊 收到流式片段: {}", chunk);
                },
                () -> {
                    log.info("✅ 流式处理完成");
                },
                error -> {
                    log.error("❌ 流式处理失败: {}", error.getMessage(), error);
                }
            );
            
            // 等待流式处理完成（简单的同步等待）
            Thread.sleep(10000); // 等待10秒让流式处理完成
            
            String result = fullResponse.toString();
            log.info("🌊 流式测试结果: {}", result);
            
            return ResponseEntity.ok(result.isEmpty() ? "流式处理超时或无响应" : result);
            
        } catch (Exception e) {
            log.error("🌊 测试流式工具调用失败: {}", e.getMessage(), e);
            return ResponseEntity.ok("流式测试失败: " + e.getMessage());
        }
    }

    /**
     * 🧪 对比同步和流式工具调用的差异
     */
    @PostMapping("/compare-sync-stream")
    public Result<Map<String, String>> compareToolCalls(@RequestBody Map<String, String> request) {
        log.info("🔍 开始对比同步和流式工具调用");
        
        try {
            String testMessage = request.getOrDefault("message", "生成工作室数据仪表盘");
            String sessionId = "compare_test_" + System.currentTimeMillis();
            Long userId = 1L;
            
            Map<String, String> results = new HashMap<>();
            
            // 1. 测试同步调用
            log.info("🔄 测试同步工具调用...");
            String syncResult = assistantAgent.chat(testMessage, sessionId + "_sync");
            results.put("syncResult", syncResult);
            log.info("✅ 同步调用完成: {}", syncResult);
            
            // 2. 测试流式调用
            log.info("🌊 测试流式工具调用...");
            StringBuilder streamResult = new StringBuilder();
            assistantAgent.streamChat(testMessage, sessionId + "_stream", userId,
                chunk -> streamResult.append(chunk),
                () -> log.info("✅ 流式调用完成"),
                error -> log.error("❌ 流式调用失败: {}", error.getMessage())
            );
            
            // 等待流式处理完成
            Thread.sleep(8000);
            results.put("streamResult", streamResult.toString());
            
            // 3. 测试简单工具调用
            log.info("⚡ 测试简单工具调用...");
            String simpleResult = simpleToolTest.testSimpleToolCall("请告诉我现在几点，然后计算 8 + 12");
            results.put("simpleResult", simpleResult);
            log.info("✅ 简单调用完成: {}", simpleResult);
            
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("🔍 对比测试失败: {}", e.getMessage(), e);
            return Result.error("对比测试失败: " + e.getMessage());
        }
    }

    /**
     * �� 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return Result.success("测试API正常运行");
    }
} 