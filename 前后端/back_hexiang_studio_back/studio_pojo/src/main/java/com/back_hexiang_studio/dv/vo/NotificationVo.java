package com.back_hexiang_studio.dv.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知VO
 */
@Data
public class NotificationVo {
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 通知类型：announcement(公告), task(任务), course(课程), system(系统通知)
     */
    private String type;
    
    /**
     * 通知类型名称
     */
    private String typeName;
    
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
     * 发送者名称
     */
    private String senderName;
    
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
     * 创建时间
     */
    private LocalDateTime createTime;
} 