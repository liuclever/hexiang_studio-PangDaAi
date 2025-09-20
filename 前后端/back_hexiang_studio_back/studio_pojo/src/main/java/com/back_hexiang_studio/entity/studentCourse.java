package com.back_hexiang_studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data

public class studentCourse {

    // 对应表中的 id 字段，课程参与记录的唯一标识
    private Long id;
    // 学生 ID，关联学生表
    private Long studentId;
    // 课程 ID，关联课程表
    private Long courseId;
    // 加入课程的时间
    private LocalDateTime joinTime;
}


