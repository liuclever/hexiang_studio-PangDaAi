package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class addStudentDto {
    private Long courseId;
    private Long studentId;
    private String creatUser;
    private LocalDateTime joinTime;
}
