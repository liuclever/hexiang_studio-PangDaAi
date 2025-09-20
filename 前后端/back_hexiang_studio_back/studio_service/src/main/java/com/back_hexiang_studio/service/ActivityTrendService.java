package com.back_hexiang_studio.service;

import java.util.Map;

/**
 * 工作室活跃度趋势服务接口
 */
public interface ActivityTrendService {
    
    /**
     * 获取工作室活跃度趋势（近5天）
     * @return 活跃度趋势数据
     */
    Map<String, Object> getActivityTrend();
} 