package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.DepartmentDto;
import com.back_hexiang_studio.entity.Department;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DepartmentService {
    
    /**
     * 获取所有部门列表
     * @return 部门列表
     */
    List<Department> getAllDepartments();
    
    /**
     * 根据部门ID获取部门信息
     * @param departmentId 部门ID
     * @return 部门信息
     */
    Department getDepartmentById(Long departmentId);
    
    /**
     * 根据部门ID获取部门名称
     * @param departmentId 部门ID
     * @return 部门名称
     */
    String getDepartmentNameById(Long departmentId);
    
    /**
     * 获取部门总数
     * @return 部门总数
     */
    Long countDepartments();
    
    /**
     * 新增部门
     * @param departmentDto 部门信息
     */
    void addDepartment(DepartmentDto departmentDto);
    
    /**
     * 更新部门信息
     * @param departmentDto 部门信息
     */
    void updateDepartment(DepartmentDto departmentDto);
    
    /**
     * 删除部门（批量）
     * @param ids 部门ID列表
     */
    void deleteDepartments(List<Long> ids);
    
    /**
     * 检查部门名称是否已存在
     * @param departmentName 部门名称
     * @return 是否存在
     */
    boolean isDepartmentNameExists(String departmentName);
    
    /**
     * 检查部门名称是否已存在（排除指定ID）
     * @param departmentName 部门名称
     * @param excludeId 排除的部门ID
     * @return 是否存在
     */
    boolean isDepartmentNameExistsExcludeId(String departmentName, Long excludeId);
    
    /**
     * 检查部门是否有关联用户
     * @param departmentId 部门ID
     * @return 是否有关联用户
     */
    boolean hasDepartmentUsers(Long departmentId);
} 