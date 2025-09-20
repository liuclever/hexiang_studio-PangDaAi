package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 通知查询DTO
 */
@Data
public class NotificationQueryDto {
    /**
     * 通知类型
     */
    private String type;
    
    /**
     * 是否已读：0未读，1已读，null表示全部
     */
    private Integer isRead;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
} 