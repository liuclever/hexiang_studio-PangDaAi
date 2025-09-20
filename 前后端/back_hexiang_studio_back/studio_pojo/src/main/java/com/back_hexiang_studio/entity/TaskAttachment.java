package com.back_hexiang_studio.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TaskAttachment implements Serializable {
    private Long attachmentId;
    private Long taskId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Long uploaderId;
    private LocalDateTime uploadTime;
} 