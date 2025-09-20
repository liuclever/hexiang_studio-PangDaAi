package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Course {
        // 课程 ID
        private Long courseId;
        // 课程名称
        private String name;
        // 课程描述
        private String description;
        // 教师 ID
        private Long teacherId;


        // 课程状态（tinyint 类型，可根据实际业务用枚举等优化）
        private Integer status;
        // 课程时长
        private String duration;
        // 课程封面图片 URL
        private String coverImage;
        // 课程分类 ID
        private Long categoryId;

        // 上课地点
        private String location;
        // 上课时间安排
        private String schedule;

        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Long createUser;
        private Long updateUser;
}

