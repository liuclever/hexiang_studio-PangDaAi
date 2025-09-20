package com.back_hexiang_studio.dv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务提交DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionDto {
    
    /**
     * 提交说明
     */
    private String submissionNotice;
    
    /**
     * 原始文件名列表
     */
    private List<String> originalFileNames;
} 