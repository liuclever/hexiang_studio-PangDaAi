package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 公告状态更新DTO
 */
@Data
public class NoticeStatusUpdateDto {
    /**
     * 公告ID
     */
    private Long noticeId;
    
    /**
     * 公告状态：0-草稿，1-已发布
     */
    private Integer status;
} 