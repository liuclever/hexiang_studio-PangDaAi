package com.back_hexiang_studio.dv.vo;

import com.back_hexiang_studio.entity.CourseMaterial;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
public class CourseVo {
    // 课程 ID
    private Long courseId;
    // 课程名称
    private String name;
    // 课程描述
    private String description;
    // 教师 ID
    private Long teacherId;
    //老师名字
    private String teacherName;
    // 创建时间
    private String createTime;
    // 课程状态（tinyint 类型，可根据实际业务用枚举等优化）
    private Integer status;
    // 课程时长
    private String duration;
    // 课程封面图片 URL
    private String coverImage;
    // 课程资料链接
    private String materialUrl;
    // 课程分类 ID
    private Long categoryId;
    // 上课地点
    private String location;
    // 上课时间安排
    private String schedule;
    //上课学生数量
    private Integer studentCount;
    // 课程类别
    private String categoryName;
    private List<CourseMaterial> materials; // 修改这里
}
