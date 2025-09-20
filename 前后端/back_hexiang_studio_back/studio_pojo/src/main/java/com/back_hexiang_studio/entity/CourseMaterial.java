package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterial {
    private Long materialId;
    private Long courseId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Integer downloadCount;
    private LocalDateTime uploadTime;
    private Long uploaderId;
} 