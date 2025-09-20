package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * AI助手选择项数据结构
 * 用于歧义消解时展示给用户的选择选项
 * 
 * @author 胖达AI助手开发团队
 * @version 1.0
 * @since 2025-09-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceOption {
    
    /**
     * 选择项唯一标识符
     */
    private String id;
    
    /**
     * 显示给用户的文本
     */
    private String displayText;
    
    /**
     * 选择项的详细描述（可选）
     */
    private String description;
    
    /**
     * 对应的查询意图或关键词
     * 用户点击后会发送此查询给AI
     */
    private String queryIntent;
    
    /**
     * 选择项的图标（可选）
     * 前端可以根据此字段显示相应图标
     */
    private String icon;
    
    /**
     * 选择项分类（可选）
     * 用于前端分组显示
     */
    private String category;
    
    // ===================================================================
    // 静态工厂方法
    // ===================================================================
    
    /**
     * 创建基本选择项
     */
    public static ChoiceOption create(String id, String displayText, String queryIntent) {
        return new ChoiceOption(id, displayText, null, queryIntent, null, null);
    }
    
    /**
     * 创建带描述的选择项
     */
    public static ChoiceOption create(String id, String displayText, String description, String queryIntent) {
        return new ChoiceOption(id, displayText, description, queryIntent, null, null);
    }
    
    /**
     * 创建完整的选择项
     */
    public static ChoiceOption create(String id, String displayText, String description, 
                                     String queryIntent, String icon, String category) {
        return new ChoiceOption(id, displayText, description, queryIntent, icon, category);
    }
} 