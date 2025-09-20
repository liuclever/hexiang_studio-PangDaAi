package com.back_hexiang_studio.dv.dto;

import lombok.Data;

@Data

public class PageCourseDto {
    private Integer page;
    private Integer pageSize;
    private String name;
    private String teacher;
    private String status;
    private String directionId;
    private String studentNumber;
}
