package com.back_hexiang_studio.controller.admin;


import com.back_hexiang_studio.entity.Student;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生管理控制器
 * 权限：超级管理员或具有用户管理权限的用户可以访问
 * 使用动态权限验证系统，基于数据库权限配置
 */
@RestController
@RequestMapping("/admin/students")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('USER_MANAGE')")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 获取学生列表
     * @return 学生列表
     */
    @GetMapping("/list")
    public Result<List<Student>> getStudentList() {
        List<Student> students = studentService.getAllStudents();
        return Result.success(students);
    }
    
    /**
     * 获取学生列表（包含姓名）用于值班安排
     * @return 学生列表（包含姓名）
     */
    @GetMapping("/list-with-names")
    public Result<List<Map<String, Object>>> getStudentListWithNames() {
        List<Map<String, Object>> students = studentService.getStudentsWithNames();
        return Result.success(students);
    }
    
    /**
     * 搜索学生
     * @param keyword 搜索关键词（姓名、学号等）
     * @return 符合条件的学生列表
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchStudents(@RequestParam String keyword) {
        List<Map<String, Object>> students = studentService.searchStudents(keyword);
        return Result.success(students);
    }
} 