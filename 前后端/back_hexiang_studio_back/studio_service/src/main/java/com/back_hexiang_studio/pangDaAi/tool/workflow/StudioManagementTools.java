package com.back_hexiang_studio.pangDaAi.tool.workflow;

import com.back_hexiang_studio.pangDaAi.service.PermissionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 【工作流工具】工作室管理工具
 * 将工作室基础信息查询、成员管理、组织架构等操作封装成完整的工作流工具。
 * 提供工作室详情、部门信息、成员统计、联系方式等功能。
 */
@Service
@Slf4j
public class StudioManagementTools {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    // ====================================================================================
    // 1. 工作室基础信息查询工具 (Read-Only)
    // ====================================================================================

    @Tool("获取何湘技能大师工作室的基本详细信息，包括成立时间、负责人、成员数量等")
    public String getStudioBasicInfo() {
        log.info("  AI Workflow Tool: 查询工作室基本信息");
        
        try {
            String sql = "SELECT name, establish_time, director, member_count, project_count, " +
                        "awards, phone, email, address, room, create_time, update_time " +
                        "FROM studio_info WHERE id = 1";
            
            Map<String, Object> studioInfo = jdbcTemplate.queryForMap(sql);
            
            StringBuilder info = new StringBuilder();
            info.append(" **何湘技能大师工作室** 基本信息：\n\n");
            info.append(" **工作室名称**：").append(studioInfo.get("name")).append("\n");
            info.append(" **成立时间**：").append(studioInfo.get("establish_time")).append("\n");
            info.append(" **负责人**：").append(studioInfo.get("director")).append("\n");
            info.append(" **成员数量**：").append(studioInfo.get("member_count")).append(" 人\n");
            info.append(" **项目数量**：").append(studioInfo.get("project_count")).append(" 个\n");
            info.append(" **获奖情况**：").append(studioInfo.get("awards")).append("\n");
            info.append(" **联系电话**：").append(studioInfo.get("phone")).append("\n");
            info.append(" **邮箱**：").append(studioInfo.get("email")).append("\n");
            info.append(" **地址**：").append(studioInfo.get("address")).append("\n");
            info.append(" **房间**：").append(studioInfo.get("room"));
            
            return info.toString();
            
        } catch (EmptyResultDataAccessException e) {
            return "  未找到工作室基本信息，请联系管理员完善信息。";
        } catch (Exception e) {
            log.error("  查询工作室基本信息失败: {}", e.getMessage(), e);
            return "  查询工作室基本信息时出现系统错误，请稍后重试。";
        }
    }

    @Tool("获取工作室的联系方式和地址信息，用于对外联系和来访指引")
    public String getStudioContactInfo() {
        log.info("  AI Workflow Tool: 查询工作室联系方式和地址信息");
        
        try {
            String sql = "SELECT name, director, phone, email, address, room FROM studio_info WHERE id = 1";
            Map<String, Object> contactInfo = jdbcTemplate.queryForMap(sql);
            
            StringBuilder result = new StringBuilder();
            result.append(" **工作室联系方式**：\n\n");
            result.append(" **负责人**：").append(contactInfo.get("director")).append("\n");
            result.append(" **电话**：").append(contactInfo.get("phone")).append("\n");
            result.append(" **邮箱**：").append(contactInfo.get("email")).append("\n");
            result.append(" **地址**：").append(contactInfo.get("address")).append("\n");
            result.append(" **房间**：").append(contactInfo.get("room")).append("\n");
            result.append(" **办公时间**：工作日 8:00-18:00\n\n");
            result.append(" **温馨提示**：建议来访前先电话预约，确保能够得到更好的接待服务！");
            
            return result.toString();
            
        } catch (EmptyResultDataAccessException e) {
            return "  未找到工作室联系信息，请联系管理员完善信息。";
        } catch (Exception e) {
            log.error("  查询工作室联系信息失败: {}", e.getMessage(), e);
            return "  查询工作室联系信息时出现系统错误，请稍后重试。";
        }
    }

    // ====================================================================================
    // 2. 组织架构与部门管理工具
    // ====================================================================================

    @Tool("获取工作室的所有部门信息和组织架构")
    public String getAllDepartments() {
        log.info("  AI Workflow Tool: 查询工作室所有部门信息");
        
        try {
            String sql = "SELECT department_id, department_name, create_time FROM department ORDER BY department_id ASC";
            
            List<Map<String, Object>> departments = jdbcTemplate.queryForList(sql);
            
            if (departments.isEmpty()) {
                return " 工作室暂未设置部门信息。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("  **工作室部门架构**（共 ").append(departments.size()).append(" 个部门）：\n\n");
            
            for (int i = 0; i < departments.size(); i++) {
                Map<String, Object> dept = departments.get(i);
                result.append(String.format("%d. **%s**\n", i + 1, dept.get("department_name")));
                result.append(String.format("    部门ID：%s\n", dept.get("department_id")));
                
                if (dept.get("create_time") != null) {
                    result.append(String.format("    创建时间：%s\n", dept.get("create_time")));
                }
                
                result.append("\n");
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询部门信息失败: {}", e.getMessage(), e);
            return "  查询部门信息时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询指定部门的详细信息，包括部门成员和人员详情")
    public String getDepartmentDetails(@P("部门名称，可以是部分名称") String departmentName) {
        log.info("  AI Workflow Tool: 查询部门详细信息 - {}", departmentName);
        
        if (!StringUtils.hasText(departmentName)) {
            return "  部门名称不能为空。";
        }
        
        try {
            // 查询部门基本信息
            String deptSql = "SELECT department_id, department_name, create_time FROM department WHERE department_name LIKE ?";
            Map<String, Object> deptInfo = jdbcTemplate.queryForMap(deptSql, "%" + departmentName + "%");
            
            // 查询部门成员
            String memberSql = "SELECT u.name, u.phone, u.email, s.student_number, s.grade_year, s.majorClass, s.counselor " +
                             "FROM student s " +
                             "JOIN user u ON s.user_id = u.user_id " +
                             "WHERE s.department_id = ? AND u.status = '1' " +
                             "ORDER BY s.grade_year DESC, u.name";
            
            Object deptId = deptInfo.get("department_id");
            Long departmentId = deptId instanceof BigInteger ? 
                ((BigInteger) deptId).longValue() : ((Number) deptId).longValue();
            
            List<Map<String, Object>> members = jdbcTemplate.queryForList(memberSql, departmentId);
            
            StringBuilder result = new StringBuilder();
            result.append("  **").append(deptInfo.get("department_name")).append("** 部门详情：\n\n");
            result.append(" **成员数量**：").append(members.size()).append(" 人\n");
            
            if (deptInfo.get("create_time") != null) {
                result.append(" **创建时间**：").append(deptInfo.get("create_time")).append("\n");
            }
            
            result.append("\n");
            
            if (!members.isEmpty()) {
                result.append("###  部门成员名单\n\n");
                
                // 按年级分组显示
                String currentGrade = "";
                for (int i = 0; i < members.size(); i++) {
                    Map<String, Object> member = members.get(i);
                    String gradeYear = member.get("grade_year") != null ? member.get("grade_year").toString() : "";
                    
                    if (!gradeYear.equals(currentGrade)) {
                        if (!currentGrade.isEmpty()) {
                            result.append("\n");
                        }
                        currentGrade = gradeYear;
                        if (!gradeYear.isEmpty()) {
                            result.append("**【").append(gradeYear).append("级】**\n");
                        }
                    }
                    
                    result.append(String.format("%d. **%s**\n", i + 1, member.get("name")));
                    result.append(String.format("    学号：%s\n", member.get("student_number")));
                    result.append(String.format("    班级：%s\n", member.get("majorClass")));
                    
                    if (member.get("phone") != null) {
                        result.append(String.format("    电话：%s\n", member.get("phone")));
                    }
                    
                    if (member.get("counselor") != null) {
                        result.append(String.format("    辅导员：%s\n", member.get("counselor")));
                    }
                    
                    result.append("\n");
                }
            } else {
                result.append(" 该部门暂无成员。\n");
            }
            
            return result.toString().trim();
            
        } catch (EmptyResultDataAccessException e) {
            return "  未找到名为 '" + departmentName + "' 的部门，请检查部门名称是否正确。";
        } catch (Exception e) {
            log.error("  查询部门详情失败: {}", e.getMessage(), e);
            return "  查询部门详情时出现系统错误，请稍后重试。";
        }
    }

    // ====================================================================================
    // 3. 成员统计与查询工具
    // ====================================================================================

    @Tool("获取工作室成员的统计信息，包括角色分布、部门分布、培训方向分布等")
    public String getStudioMemberStatistics() {
        log.info("  AI Workflow Tool: 查询工作室成员统计信息");
        
        try {
            // 查询各角色成员数量
            String roleSql = "SELECT r.role_name, COUNT(u.user_id) as count " +
                           "FROM role r " +
                           "LEFT JOIN user u ON r.role_id = u.role_id AND u.status = '1' " +
                           "GROUP BY r.role_id, r.role_name " +
                           "ORDER BY r.role_id";
            
            List<Map<String, Object>> roleStats = jdbcTemplate.queryForList(roleSql);
            
            // 查询部门成员分布
            String deptSql = "SELECT d.department_name, " +
                           "COALESCE(COUNT(CASE WHEN u.status = '1' THEN s.student_id END), 0) as student_count " +
                           "FROM department d " +
                           "LEFT JOIN student s ON d.department_id = s.department_id " +
                           "LEFT JOIN user u ON s.user_id = u.user_id " +
                           "GROUP BY d.department_id, d.department_name " +
                           "ORDER BY student_count DESC, d.department_name";
            
            List<Map<String, Object>> deptStats = jdbcTemplate.queryForList(deptSql);
            
            // 查询培训方向分布
            String directionSql = "SELECT td.direction_name, COUNT(s.student_id) as student_count " +
                                "FROM training_direction td " +
                                "LEFT JOIN student s ON td.direction_id = s.direction_id " +
                                "LEFT JOIN user u ON s.user_id = u.user_id AND u.status = '1' " +
                                "GROUP BY td.direction_id, td.direction_name " +
                                "ORDER BY student_count DESC";
            
            List<Map<String, Object>> directionStats = jdbcTemplate.queryForList(directionSql);
            
            StringBuilder result = new StringBuilder();
            result.append("  **工作室成员统计报告**：\n\n");
            
            // 角色分布统计
            result.append("###  角色分布\n");
            int totalMembers = 0;
            for (Map<String, Object> role : roleStats) {
                Object countObj = role.get("count");
                int count = countObj instanceof BigInteger ? 
                    ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                totalMembers += count;
                result.append(String.format("- **%s**：%d 人\n", role.get("role_name"), count));
            }
            result.append(String.format("\n **总计**：%d 人\n\n", totalMembers));
            
            // 部门分布统计
            if (!deptStats.isEmpty()) {
                result.append("###   部门分布\n");
                for (Map<String, Object> dept : deptStats) {
                    Object countObj = dept.get("student_count");
                    int count = countObj instanceof BigInteger ? 
                        ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                    result.append(String.format("- **%s**：%d 人\n", dept.get("department_name"), count));
                }
                result.append("\n");
            }
            
            // 培训方向分布统计
            if (!directionStats.isEmpty()) {
                result.append("###  培训方向分布\n");
                for (Map<String, Object> direction : directionStats) {
                    Object countObj = direction.get("student_count");
                    int count = countObj instanceof BigInteger ? 
                        ((BigInteger) countObj).intValue() : ((Number) countObj).intValue();
                    if (count > 0) {
                        result.append(String.format("- **%s**：%d 人\n", direction.get("direction_name"), count));
                    }
                }
            }
            
            return result.toString().trim();
            
        } catch (Exception e) {
            log.error("  查询成员统计失败: {}", e.getMessage(), e);
            return "  查询成员统计时出现系统错误，请稍后重试。";
        }
    }

    @Tool("查询工作室所有成员名单，包括师资团队和学生团队的详细信息")
    public String getAllStudioMembers() {
        log.info("  AI Workflow Tool: 查询工作室所有成员名单");
        
        try {
            // 查询教师信息
            String teacherSql = "SELECT DISTINCT u.name, p.position_name, t.title, t.office_location " +
                               "FROM user u " +
                               "LEFT JOIN position p ON u.position_id = p.position_id " +
                               "LEFT JOIN teacher t ON u.user_id = t.user_id " +
                               "WHERE u.status = '1' " +
                               "AND (p.position_name LIKE '%导师%' OR p.position_name LIKE '%老师%' OR p.position_name LIKE '%讲师%' OR t.teacher_id IS NOT NULL) " +
                               "AND u.role_id != 1 " +  // 排除超级管理员
                               "ORDER BY u.name";
            
            List<Map<String, Object>> teachers = jdbcTemplate.queryForList(teacherSql);
            
            // 查询学生信息  
            String studentSql = "SELECT u.name, s.grade_year, s.majorClass, d.department_name, td.direction_name " +
                               "FROM user u " +
                               "INNER JOIN student s ON u.user_id = s.user_id " +
                               "LEFT JOIN department d ON s.department_id = d.department_id " +
                               "LEFT JOIN training_direction td ON s.direction_id = td.direction_id " +
                               "WHERE u.status = '1' " +
                               "ORDER BY s.grade_year DESC, d.department_name, u.name";
            
            List<Map<String, Object>> students = jdbcTemplate.queryForList(studentSql);
            
            StringBuilder result = new StringBuilder();
            result.append(" **何湘技能大师工作室成员名单**：\n\n");
            
            // 显示师资团队
            if (!teachers.isEmpty()) {
                result.append("###  师资团队\n\n");
                for (int i = 0; i < teachers.size(); i++) {
                    Map<String, Object> teacher = teachers.get(i);
                    String name = (String) teacher.get("name");
                    String position = (String) teacher.get("position_name");
                    String title = (String) teacher.get("title");
                    String office = (String) teacher.get("office_location");
                    
                    result.append(String.format("%d. **%s**", i + 1, name));
                    
                    if (StringUtils.hasText(title)) {
                        result.append(" - ").append(title);
                    } else if (StringUtils.hasText(position)) {
                        result.append(" - ").append(position);
                    }
                    
                    if (StringUtils.hasText(office)) {
                        result.append("（ ").append(office).append("）");
                    }
                    
                    result.append("\n");
                }
                result.append("\n");
            }
            
            // 显示学生团队
            if (!students.isEmpty()) {
                result.append("###  学生团队\n\n");
                
                // 按部门分组显示
                String currentDept = "";
                int studentIndex = 1;
                
                for (Map<String, Object> student : students) {
                    String name = (String) student.get("name");
                    String gradeYear = student.get("grade_year") != null ? student.get("grade_year").toString() : "";
                    String majorClass = (String) student.get("majorClass");
                    String department = (String) student.get("department_name");
                    String direction = (String) student.get("direction_name");
                    
                    // 如果部门改变，显示部门标题
                    if (department != null && !department.equals(currentDept)) {
                        if (!currentDept.isEmpty()) {
                            result.append("\n");
                        }
                        currentDept = department;
                        result.append("**【").append(department).append("】**\n");
                    }
                    
                    result.append(String.format("%d. **%s**", studentIndex++, name));
                    
                    // 添加年级和班级信息
                    if (StringUtils.hasText(gradeYear) || StringUtils.hasText(majorClass)) {
                        result.append("（");
                        if (StringUtils.hasText(gradeYear)) {
                            result.append(gradeYear).append("级");
                            if (StringUtils.hasText(majorClass)) {
                                result.append(" ").append(majorClass);
                            }
                        } else if (StringUtils.hasText(majorClass)) {
                            result.append(majorClass);
                        }
                        result.append("）");
                    }
                    
                    // 添加培训方向
                    if (StringUtils.hasText(direction)) {
                        result.append(" -  ").append(direction);
                    }
                    
                    result.append("\n");
                }
            }
            
            // 统计信息
            int totalMembers = teachers.size() + students.size();
            result.append("\n---\n");
            result.append("  **团队概况**：");
            if (!teachers.isEmpty()) {
                result.append("师资 ").append(teachers.size()).append(" 人");
            }
            if (!students.isEmpty()) {
                if (!teachers.isEmpty()) result.append("，");
                result.append("学生 ").append(students.size()).append(" 人");
            }
            result.append("，共计 **").append(totalMembers).append("** 人");
            
            if (totalMembers == 0) {
                return " 工作室暂时没有公开的成员信息。";
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("  查询工作室成员失败: {}", e.getMessage(), e);
            return "  查询工作室成员信息时出现系统错误，请稍后重试。";
        }
    }
} 