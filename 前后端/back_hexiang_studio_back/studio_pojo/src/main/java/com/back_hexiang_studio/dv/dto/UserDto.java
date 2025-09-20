package com.back_hexiang_studio.dv.dto;

import com.back_hexiang_studio.entity.TrainingDirection;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDto {

    private Long userId;
    private String userName;
    private String name;
    private String password;
    private String sex;
    private String phone;
    private String email;
    private Integer roleId;
    private Long teacherId;

    private Integer type;
    private String position;
    private Integer positionId;

    /**
     * 主研究方向ID
     */
    private Long directionId;

    /**
     * 培训方向ID列表
     */
    private List<Integer> training;

    private String avatar;
    private String status;
    private LocalDateTime createTime;
    private Long createUser;
    private LocalDateTime updateTime;
    private Long updateUser;

    private Long studentId;

    // 学生特有字段
    private String gradeYear;
    private String major;
    private String studentNumber;
    private String counselor;
    private String dormitory;
    private String score;
    private Long departmentId;

    // 教师特有字段
    private String officeLocation;
    private String title;
}
