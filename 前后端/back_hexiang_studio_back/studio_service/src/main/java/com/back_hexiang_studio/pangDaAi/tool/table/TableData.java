package com.back_hexiang_studio.pangDaAi.tool.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标准化表格数据格式
 * 用于AI工具返回结构化表格数据，前端可直接消费
 * 
 * @author 胖达AI开发团队
 * @since 2025-09-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    
    /**
     * 表格标题
     */
    @JsonProperty("title")
    private String title;
    
    /**
     * 表格列名
     */
    @JsonProperty("columns") 
    private List<String> columns;
    
    /**
     * 表格行数据
     */
    @JsonProperty("rows")
    private List<List<Object>> rows;
    
    /**
     * 额外的表格元数据（可选）
     */
    @JsonProperty("metadata")
    private TableMetadata metadata;
    
    /**
     * 表格元数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableMetadata {
        
        /**
         * 数据总条数
         */
        @JsonProperty("totalCount")
        private Integer totalCount;
        
        /**
         * 数据生成时间
         */
        @JsonProperty("generateTime")
        private String generateTime;
        
        /**
         * 数据来源说明
         */
        @JsonProperty("dataSource")
        private String dataSource;
        
        /**
         * 是否支持排序
         */
        @JsonProperty("sortable")
        private Boolean sortable = false;
        
        /**
         * 是否支持导出
         */
        @JsonProperty("exportable") 
        private Boolean exportable = true;
    }
    
    /**
     * 创建简单表格数据
     */
    public static TableData simple(String title, List<String> columns, List<List<Object>> rows) {
        return new TableData(title, columns, rows, null);
    }
    
    /**
     * 创建完整表格数据
     */
    public static TableData full(String title, List<String> columns, List<List<Object>> rows, 
                                TableMetadata metadata) {
        return new TableData(title, columns, rows, metadata);
    }
} 