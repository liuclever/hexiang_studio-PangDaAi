package com.back_hexiang_studio.pangDaAi.tool.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 通用表格转换工具服务
 * 将文本数据转换为标准化表格JSON格式
 */
@Service
@Slf4j
public class TableToolService {
    
    @Autowired
    private ObjectMapper objectMapper;

    @Tool("将文本数据转换为表格JSON格式，前端可直接渲染为表格")
    public String convertToTable(@P("需要转换的文本数据") String textData, 
                                @P("表格标题") String title) {
        log.info(" AI Table Tool: 转换文本为表格 - 标题: {}", title);
        
        try {
            // 简单解析：按行分割，提取关键信息
            String[] lines = textData.split("\n");
            List<String> columns = Arrays.asList("序号", "内容");
            List<List<Object>> rows = new ArrayList<>();
            
            int index = 1;
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    List<Object> row = Arrays.asList(index++, trimmedLine);
                    rows.add(row);
                }
            }
            
            // 创建表格数据
            TableData.TableMetadata metadata = new TableData.TableMetadata();
            metadata.setTotalCount(rows.size());
            metadata.setGenerateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            metadata.setDataSource("文本数据转换");
            metadata.setSortable(true);
            metadata.setExportable(true);
            
            TableData tableData = TableData.full(
                title != null ? title : "数据表格", 
                columns, 
                rows, 
                metadata
            );
            
            return objectMapper.writeValueAsString(tableData);
            
        } catch (Exception e) {
            log.error("转换文本为表格失败: {}", e.getMessage(), e);
            return "转换文本为表格时出现错误: " + e.getMessage();
        }
    }
} 