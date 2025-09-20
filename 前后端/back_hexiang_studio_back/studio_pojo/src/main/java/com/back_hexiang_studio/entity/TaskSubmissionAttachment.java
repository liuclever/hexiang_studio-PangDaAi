package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务提交附件实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionAttachment {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 