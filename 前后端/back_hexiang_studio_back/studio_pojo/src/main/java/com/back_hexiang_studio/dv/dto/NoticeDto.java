package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告数据传输对象
 */
@Data
public class NoticeDto implements Serializable {
    /**
     * 公告ID，新增时为null
     */
    private Long id;
    
    /**
     * 公告ID，用于兼容实体类
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
     * 公告类型：1-系统公告，2-活动公告
     */
    private Integer type;
    
    /**
     * 公告状态：0-草稿，1-已发布
     */
    private Integer status;
    
    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 发布人
     */
    private String publisher;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    private Long createUser;
    
    /**
     * 更新人ID
     */
    private Long updateUser;
}
