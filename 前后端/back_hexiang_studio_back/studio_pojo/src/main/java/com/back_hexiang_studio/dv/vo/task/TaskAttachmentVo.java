package com.back_hexiang_studio.dv.vo.task;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TaskAttachmentVo implements Serializable {
    private Long attachmentId;
    private Long taskId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Long uploaderId;
    private String uploaderName; 
    private String uploadTime;
} 