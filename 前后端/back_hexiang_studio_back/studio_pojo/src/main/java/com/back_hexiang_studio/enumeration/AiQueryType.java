package com.back_hexiang_studio.enumeration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI查询类型枚举
 */
public enum AiQueryType {
    // 枚举常量：每个枚举值通过字符串数组传递关键词，构造时自动转为List
    USER_INFO("用户信息", new String[]{"张三", "李四", "王五", "用户", "同事", "学生", "老师"}),
    COURSE_INFO("课程信息", new String[]{"课程", "培训", "班级", "教学", "学习", "考试"}),
    TASK_INFO("任务信息", new String[]{"任务", "工作", "项目", "作业", "计划"}),
    STATISTICS("统计数据", new String[]{"统计", "数据", "报表", "人数", "数量", "总数"}),
    DEPARTMENT("组织架构", new String[]{"部门", "工作室", "组织", "职位", "角色"}),
    GENERAL("通用问题", new String[]{}); // 空关键词数组，对应空List

    private final String description;

    private final List<String> keywords;

    AiQueryType(String description, String[] keywords) {
        this.description = description;
        this.keywords = Arrays.asList(keywords);
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}