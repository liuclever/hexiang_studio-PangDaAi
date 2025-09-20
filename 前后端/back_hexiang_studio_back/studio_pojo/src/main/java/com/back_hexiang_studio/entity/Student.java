package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 用户ID (关联用户表)
     */
    private Long userId;
    
    /**
     * 年级
     */
    private String gradeYear;
    
    /**
     * 专业班级
     */
    private String majorClass;
    
    /**
     * 学号
     */
    private String studentNumber;
    
    /**
     * 培训方向ID
     */
    private Long directionId;
    
    /**
     * 部门ID
     */
    private Long departmentId;
    
    /**
     * 辅导员
     */
    private String counselor;
    
    /**
     * 宿舍
     */
    private String dormitory;
    
    /**
     * 学分/成绩
     */
    private String score;
    
    /**
     * 状态：1-正常，0-已删除
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 关联用户信息
     */
    private String userName;
    private String name;
    private String sex;
    private String phone;
    private String email;
    
    /**
     * 关联培训方向
     */
    private String directionName;
    
    /**
     * 兼容方法 - 为保持向后兼容
     * @return 用户ID
     */
    public Long getUser_id() {
        return userId;
    }
    
    /**
     * 兼容方法 - 为保持向后兼容
     * @return 学生ID
     */
    public Long getStudent_id() {
        return studentId;
    }

    private String createUser;
    private String updateUser;
}
