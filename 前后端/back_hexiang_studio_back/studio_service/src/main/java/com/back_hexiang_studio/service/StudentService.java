package com.back_hexiang_studio.service;

import com.back_hexiang_studio.entity.Student;

import java.util.List;
import java.util.Map;

public interface StudentService {
    
    /**
     * 获取所有学生列表
     * @return 学生列表
     */
    List<Student> getAllStudents();
    
    /**
     * 获取学生列表（包含姓名）
     * 返回包含学生ID和姓名的列表，用于前端选择框
     * @return 学生列表（包含姓名）
     */
    List<Map<String, Object>> getStudentsWithNames();
    
    /**
     * 搜索学生
     * 根据关键词搜索学生（姓名、学号等）
     * @param query 搜索关键词
     * @return 符合条件的学生列表
     */
    List<Map<String, Object>> searchStudents(String query);
} 