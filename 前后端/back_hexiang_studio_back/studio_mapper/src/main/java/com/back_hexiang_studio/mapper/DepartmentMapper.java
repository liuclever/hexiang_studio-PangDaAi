package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.Department;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 部门数据访问层
 */
@Mapper
public interface DepartmentMapper {
    
    /**
     * 获取所有部门列表
     * @return 部门列表
     */
    @Select("SELECT department_id, department_name, create_time FROM department ORDER BY department_id")
    List<Department> getAllDepartments();
    
    /**
     * 根据部门ID获取部门信息
     * @param departmentId 部门ID
     * @return 部门信息
     */
    @Select("SELECT department_id, department_name, create_time FROM department WHERE department_id = #{departmentId}")
    Department getDepartmentById(Long departmentId);
    
    /**
     * 根据部门ID获取部门名称
     * @param departmentId 部门ID
     * @return 部门名称
     */
    @Select("SELECT department_name FROM department WHERE department_id = #{departmentId}")
    String getDepartmentNameById(Long departmentId);
    
    /**
     * 获取部门总数
     * @return 部门总数
     */
    @Select("SELECT COUNT(*) FROM department")
    Long countDepartments();
    
    /**
     * 新增部门
     * @param department 部门信息
     */
    @Insert("INSERT INTO department (department_name, create_time) " +
            "VALUES (#{departmentName}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "departmentId")
    void addDepartment(Department department);
    
    /**
     * 更新部门信息
     * @param department 部门信息
     */
    @Update("UPDATE department SET department_name = #{departmentName} WHERE department_id = #{departmentId}")
    void updateDepartment(Department department);
    
    /**
     * 批量删除部门
     * @param ids 部门ID列表
     */
    @Delete("<script>" +
            "DELETE FROM department WHERE department_id IN " +
            "<foreach collection='list' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void deleteDepartments(List<Long> ids);
    
    /**
     * 根据部门名称统计数量
     * @param departmentName 部门名称
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM department WHERE department_name = #{departmentName}")
    int countByDepartmentName(String departmentName);
    
    /**
     * 根据部门名称统计数量（排除指定ID）
     * @param departmentName 部门名称
     * @param excludeId 排除的部门ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM department WHERE department_name = #{departmentName} AND department_id != #{excludeId}")
    int countByDepartmentNameExcludeId(@Param("departmentName") String departmentName, @Param("excludeId") Long excludeId);
    
    /**
     * 根据部门ID统计学生数量
     * 只有学生才分部门，所以查询student表
     * @param departmentId 部门ID
     * @return 该部门下的学生数量
     */
    @Select("SELECT COUNT(*) FROM student WHERE department_id = #{departmentId}")
    int countUsersByDepartmentId(Long departmentId);
} 