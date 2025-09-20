package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDto {
    private Long courseId;
    private String name;
    private String description;
    private Long teacherId;
    private String teacherName;
    private Integer status;
    private String duration;
    private String coverImage;
    private String materialUrl;
    private Long categoryId;
    private String location;
    private String schedule;
}
