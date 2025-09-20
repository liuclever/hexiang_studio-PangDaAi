package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.DepartmentDto;
import com.back_hexiang_studio.entity.Department;
import com.back_hexiang_studio.mapper.DepartmentMapper;
import com.back_hexiang_studio.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门服务实现类
 */
@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Override
    public List<Department> getAllDepartments() {
        log.info("获取所有部门列表");
        return departmentMapper.getAllDepartments();
    }
    
    @Override
    public Department getDepartmentById(Long departmentId) {
        log.info("根据ID获取部门信息，departmentId: {}", departmentId);
        if (departmentId == null) {
            return null;
        }
        return departmentMapper.getDepartmentById(departmentId);
    }
    
    @Override
    public String getDepartmentNameById(Long departmentId) {
        log.info("根据ID获取部门名称，departmentId: {}", departmentId);
        if (departmentId == null) {
            return null;
        }
        return departmentMapper.getDepartmentNameById(departmentId);
    }
    
    @Override
    public Long countDepartments() {
        log.info("获取部门总数");
        return departmentMapper.countDepartments();
    }
    
    @Override
    @Transactional
    public void addDepartment(DepartmentDto departmentDto) {
        log.info("新增部门: {}", departmentDto.getDepartmentName());
        Department department = new Department();
        BeanUtils.copyProperties(departmentDto, department);
        departmentMapper.addDepartment(department);
    }
    
    @Override
    @Transactional
    public void updateDepartment(DepartmentDto departmentDto) {
        log.info("更新部门: {}", departmentDto.getDepartmentName());
        Department department = new Department();
        BeanUtils.copyProperties(departmentDto, department);
        // 确保departmentId字段正确设置
        department.setDepartmentId(departmentDto.getId());
        departmentMapper.updateDepartment(department);
    }
    
    @Override
    @Transactional
    public void deleteDepartments(List<Long> ids) {
        log.info("批量删除部门: {}", ids);
        departmentMapper.deleteDepartments(ids);
    }
    
    @Override
    public boolean isDepartmentNameExists(String departmentName) {
        log.info("检查部门名称是否存在: {}", departmentName);
        return departmentMapper.countByDepartmentName(departmentName) > 0;
    }
    
    @Override
    public boolean isDepartmentNameExistsExcludeId(String departmentName, Long excludeId) {
        log.info("检查部门名称是否存在（排除ID: {}）: {}", excludeId, departmentName);
        return departmentMapper.countByDepartmentNameExcludeId(departmentName, excludeId) > 0;
    }
    
    @Override
    public boolean hasDepartmentUsers(Long departmentId) {
        log.info("检查部门是否有关联学生: {}", departmentId);
        return departmentMapper.countUsersByDepartmentId(departmentId) > 0;
    }
} 