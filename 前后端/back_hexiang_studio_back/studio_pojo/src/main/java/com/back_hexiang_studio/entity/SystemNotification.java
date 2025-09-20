package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
public class SystemNotification {
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 通知类型：announcement(公告), task(任务), course(课程), system(系统通知)
     */
    private String type;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 来源ID(如任务ID、公告ID、课程ID等)
     */
    private Long sourceId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 目标用户ID，NULL表示全局通知
     */
    private Long targetUserId;
    
    /**
     * 是否已读：0未读，1已读
     */
    private Integer isRead;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 重要程度：0普通，1重要，2紧急
     */
    private Integer importance;
    
    /**
     * 状态：1有效，0已删除
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 