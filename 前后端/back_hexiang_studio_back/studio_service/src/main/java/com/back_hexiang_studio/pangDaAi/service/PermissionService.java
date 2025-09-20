package com.back_hexiang_studio.pangDaAi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI工具权限验证服务
 * 负责验证用户在AI工具调用中的权限
 */
@Service
@Slf4j
public class PermissionService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 检查用户是否有考勤统计查看权限
     */
    public boolean canViewAttendanceStatistics(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT u.role_id, p.permissions FROM user u " +
                        "LEFT JOIN position p ON u.position_id = p.position_id " +
                        "WHERE u.user_id = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            
            // 管理员(3)和超级管理员(4)有权限查看所有考勤统计
            return roleId != null && (roleId == 3L || roleId == 4L);
            
        } catch (Exception e) {
            log.error("检查考勤统计权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以查看指定用户的考勤记录
     */
    public boolean canViewUserAttendance(Long currentUserId, String targetUserName) {
        if (currentUserId == null || targetUserName == null) {
            return false;
        }
        
        try {
            // 获取当前用户角色
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> currentUser = jdbcTemplate.queryForList(sql, currentUserId);
            if (currentUser.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) currentUser.get(0).get("role_id");
            
            // 管理员(3)和超级管理员(4)可以查看所有人的考勤
            if (roleId == 3L || roleId == 4L) {
                return true;
            }
            
            // 获取当前用户姓名
            String currentUserSql = "SELECT name FROM user WHERE user_id = ?";
            List<Map<String, Object>> userInfo = jdbcTemplate.queryForList(currentUserSql, currentUserId);
            if (userInfo.isEmpty()) {
                return false;
            }
            
            String currentUserName = (String) userInfo.get(0).get("name");
            
            // 学生(1)只能查看自己的考勤记录
            if (roleId == 1L) {
                return currentUserName.equals(targetUserName);
            }
            
            // 老师(2)可以查看自己负责课程中学生的考勤记录
            if (roleId == 2L) {
                // 如果查看的是自己，允许
                if (currentUserName.equals(targetUserName)) {
                    return true;
                }
                
                // 检查是否是自己负责课程的学生
                String teacherSql = "SELECT COUNT(*) as count FROM course c " +
                                   "JOIN teacher t ON c.teacher_id = t.teacher_id " +
                                   "JOIN user tu ON t.user_id = tu.user_id " +
                                   "JOIN student_course sc ON c.course_id = sc.course_id " +
                                   "JOIN student s ON sc.student_id = s.student_id " +
                                   "JOIN user su ON s.user_id = su.user_id " +
                                   "WHERE tu.user_id = ? AND su.name = ?";
                
                List<Map<String, Object>> teacherCheck = jdbcTemplate.queryForList(teacherSql, currentUserId, targetUserName);
                Long count = (Long) teacherCheck.get(0).get("count");
                return count > 0;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("检查用户考勤查看权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以查看缺勤学生列表
     */
    public boolean canViewAbsentStudents(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            
            // 老师(2)、管理员(3)和超级管理员(4)可以查看缺勤学生列表
            return roleId != null && (roleId == 2L || roleId == 3L || roleId == 4L);
            
        } catch (Exception e) {
            log.error("检查缺勤学生查看权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            String sql = "SELECT u.user_id, u.name, u.role_id, r.role_name, u.position_id, p.position_name " +
                        "FROM user u " +
                        "LEFT JOIN role r ON u.role_id = r.role_id " +
                        "LEFT JOIN position p ON u.position_id = p.position_id " +
                        "WHERE u.user_id = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            return results.isEmpty() ? null : results.get(0);
            
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取角色名称
     */
    public String getRoleName(Long roleId) {
        switch (roleId.intValue()) {
            case 0: return "访客";
            case 1: return "学员";
            case 2: return "老师"; 
            case 3: return "管理员";
            case 4: return "超级管理员";
            default: return "未知角色";
        }
    }
    
    /**
     * 检查用户是否有管理用户的权限 - 创建、修改、删除用户
     */
    public boolean canManageUsers(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            
            // 管理员(3)和超级管理员(4)可以管理用户
            return roleId != null && (roleId == 3L || roleId == 4L);
            
        } catch (Exception e) {
            log.error("检查用户管理权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取用户管理权限描述信息
     */
    public String getUserManagementPermissionInfo(Long userId) {
        if (userId == null) {
            return "用户未登录，无法获取权限信息";
        }
        
        try {
            Map<String, Object> userInfo = getUserInfo(userId);
            if (userInfo == null) {
                return "用户信息不存在，无法验证权限";
            }
            
            Long roleId = (Long) userInfo.get("role_id");
            String userName = (String) userInfo.get("name");
            String roleName = (String) userInfo.get("role_name");
            
            if (roleId == 3L || roleId == 4L) {
                return String.format("✅ %s（%s）有用户管理权限", userName, roleName);
            } else {
                return String.format("❌ %s（%s）没有用户管理权限。只有管理员和超级管理员才能进行用户管理操作", userName, roleName);
            }
            
        } catch (Exception e) {
            log.error("获取用户管理权限信息失败: {}", e.getMessage());
            return "检查权限时发生系统错误，请联系管理员";
        }
    }
    
    /**
     * 检查用户是否有管理资料的权限
     */
    public boolean canManageMaterials(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            // 管理员(3)和超级管理员(4)可以管理资料
            return roleId != null && (roleId == 3 || roleId == 4);
        } catch (Exception e) {
            log.error("检查资料管理权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否有管理公告的权限
     */
    public boolean canManageNotices(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            // 管理员(3)和超级管理员(4)可以管理公告
            return roleId != null && (roleId == 3 || roleId == 4);
        } catch (Exception e) {
            log.error("检查公告管理权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否有管理课程的权限
     */
    public boolean canManageCourses(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            String sql = "SELECT role_id FROM user WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (results.isEmpty()) {
                return false;
            }
            
            Long roleId = (Long) results.get(0).get("role_id");
            // 老师(2)、管理员(3)和超级管理员(4)可以管理课程
            return roleId != null && (roleId == 2 || roleId == 3 || roleId == 4);
        } catch (Exception e) {
            log.error("检查课程管理权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取资料管理权限信息
     */
    public String getMaterialManagementPermissionInfo(Long userId) {
        if (!canManageMaterials(userId)) {
            return "❌ 权限不足：只有管理员和超级管理员可以管理资料";
        }
        
        return "✅ 权限验证通过：您可以进行资料的增加、修改、删除操作";
    }
    
    /**
     * 获取公告管理权限信息
     */
    public String getNoticeManagementPermissionInfo(Long userId) {
        if (!canManageNotices(userId)) {
            return "❌ 权限不足：只有管理员和超级管理员可以管理公告";
        }
        
        return "✅ 权限验证通过：您可以进行公告的增加、修改、删除操作";
    }
    
    /**
     * 获取课程管理权限信息
     */
    public String getCourseManagementPermissionInfo(Long userId) {
        if (!canManageCourses(userId)) {
            return "❌ 权限不足：只有老师、管理员和超级管理员可以管理课程";
        }
        
        return "✅ 权限验证通过：您可以进行课程的增加、修改、删除操作";
    }
} 