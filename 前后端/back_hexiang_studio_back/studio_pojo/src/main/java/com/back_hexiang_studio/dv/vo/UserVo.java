package com.back_hexiang_studio.dv.vo;

import com.back_hexiang_studio.entity.TrainingDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {
    private Long userId;
    private String name;

    private String sex;
    private Long roleId;
    private String phone;
    private Long positionId;
    private String avatar;
    private String status;
    private String email;

    private String createTime;
    private String updateTime;

    // 针对 teacher 表可能存在的额外字段
    //老师学生都有培训列表
    private List<String> directionIdNames = new ArrayList<>();

    // 教师特有字段
    private String officeLocation;
    private String title;

    // 学生特有字段
    private String studentNumber;
    private String gradeYear;
    private String major; // 专业
    private String counselor; // 辅导员
    private String dormitory; // 宿舍楼号
    private String score; // 分数
    private Long departmentId; // 部门ID
    private String departmentName; // 部门名称


}
