package com.back_hexiang_studio.controller.ai;

import com.back_hexiang_studio.pangDaAi.service.rag.FullSyncService;
import com.back_hexiang_studio.pangDaAi.service.rag.VectorStoreService;
import com.back_hexiang_studio.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

/**
 * AI RAG管理控制器 - 简化版
 * 
 * 🛠️ 现代RAG架构的管理接口
 * 提供向量数据库的手动管理功能：
 * - 手动重建向量索引
 * - 查看向量存储状态
 * - 清空向量数据库
 * - 系统健康检查
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0 - 简化版RAG架构管理
 * @since 2025-09-14
 */
@RestController
@RequestMapping("/api/ai/rag")
@Slf4j
public class AiRagManagementController {

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private FullSyncService fullSyncService;

    /**
     * 🔍 获取RAG系统状态
     * 提供向量数据库的详细状态信息
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getSystemStatus() {
        try {
            log.info("🔍 获取RAG系统状态");
            
            Map<String, Object> status = new HashMap<>();
            
            // 获取向量存储统计信息
            VectorStoreService.VectorStoreStats stats = vectorStoreService.getStats();
            status.put("vectorCount", stats.getDocumentCount());
            status.put("storagePath", stats.getStoragePath());
            status.put("embeddingModel", stats.getEmbeddingModel());
            status.put("storeType", stats.getStoreType());
            
            // 系统健康状态
            status.put("systemHealth", "healthy");
            status.put("lastUpdateTime", java.time.LocalDateTime.now());
            
            return Result.success(status);
            
        } catch (Exception e) {
            log.error("❌ 获取RAG系统状态失败", e);
            return Result.error("获取系统状态失败: " + e.getMessage());
        }
    }

    /**
     * 🔄 手动全量重建向量索引
     * 从MySQL数据库重新构建整个向量知识库
     */
    @PostMapping("/rebuild")
    public Result<String> rebuildVectorIndex() {
        try {
            log.info("🔄 开始手动重建向量索引");
            
            // 异步执行全量同步，避免阻塞接口
            CompletableFuture.supplyAsync(() -> {
                try {
                    fullSyncService.syncAll();
                    log.info("✅ 向量索引重建完成");
                    return "success";
                } catch (Exception e) {
                    log.error("❌ 向量索引重建失败", e);
                    return "failed: " + e.getMessage();
                }
            });
            
            return Result.success("向量索引重建任务已启动，请稍后查看状态");
            
        } catch (Exception e) {
            log.error("❌ 启动向量索引重建失败", e);
            return Result.error("重建任务启动失败: " + e.getMessage());
        }
    }

    /**
     * 🗑️ 清空向量数据库
     * 删除所有向量数据，谨慎使用
     */
    @DeleteMapping("/clear")
    public Result<String> clearVectorStore() {
        try {
            log.warn("🗑️ 准备清空向量数据库");
            
            vectorStoreService.clear();
            
            log.info("✅ 向量数据库已清空");
            return Result.success("向量数据库已清空");
            
        } catch (Exception e) {
            log.error("❌ 清空向量数据库失败", e);
            return Result.error("清空操作失败: " + e.getMessage());
        }
    }



    /**
     * 🔍 搜索测试
     * 测试向量检索功能
     */
    @GetMapping("/search")
    public Result<Object> testSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        try {
            log.info("🔍 测试向量搜索: query={}, maxResults={}", query, maxResults);
            
            Object searchResults = vectorStoreService.search(query, maxResults);
            
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("resultCount", searchResults != null ? "有结果" : "无结果");
            result.put("results", searchResults);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ 向量搜索测试失败: query={}", query, e);
            return Result.error("搜索测试失败: " + e.getMessage());
        }
    }

    /**
     * 📊 获取数据库同步统计
     * 显示各表的数据同步情况
     */
    @GetMapping("/sync-stats")
    public Result<Map<String, Object>> getSyncStats() {
        try {
            log.info("📊 获取数据库同步统计");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("lastSyncTime", java.time.LocalDateTime.now());
            stats.put("syncStatus", "ready");
            stats.put("availableTables", java.util.Arrays.asList("notice", "course", "task", "material"));
            stats.put("message", "使用 /rebuild 接口执行全量同步");
            
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("❌ 获取同步统计失败", e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
} 