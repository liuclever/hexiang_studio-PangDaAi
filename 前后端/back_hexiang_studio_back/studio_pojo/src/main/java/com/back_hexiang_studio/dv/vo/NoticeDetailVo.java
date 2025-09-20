package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 公告详情VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDetailVo {
    /**
     * 公告ID
     */
    private Long noticeId;
    
    /**
     * 公告标题
     */
    private String title;
    
    /**
     * 公告内容
     */
    private String content;
    
    /**
     * 公告类型：0通知 1活动 2新闻 3其他
     */
    private Integer type;
    
    /**
     * 状态：1-已发布，0-草稿
     */
    private Integer status;
    
    /**
     * 发布人
     */
    private String publisher;
    
    /**
     * 发布时间
     */
    private String publishTime; // 根据建议，将类型改为String以优化缓存和兼容性
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 图片列表
     */
    private List<NoticeImageVo> images;
    
    /**
     * 附件列表
     */
    private List<NoticeAttachmentVo> attachments;
} 