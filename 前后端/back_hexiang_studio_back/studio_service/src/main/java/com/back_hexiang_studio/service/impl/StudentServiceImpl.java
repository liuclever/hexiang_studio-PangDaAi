package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.Student;
import com.back_hexiang_studio.mapper.StudentMapper;
import com.back_hexiang_studio.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;
    
    @Override
    public List<Student> getAllStudents() {
        return studentMapper.selectAllStudents();
    }
    
    @Override
    public List<Map<String, Object>> getStudentsWithNames() {
        return studentMapper.selectStudentsWithNames();
    }
    
    @Override
    public List<Map<String, Object>> searchStudents(String query) {
        // 调用Mapper中的搜索方法
        return studentMapper.searchStudents(query);
    }
} 