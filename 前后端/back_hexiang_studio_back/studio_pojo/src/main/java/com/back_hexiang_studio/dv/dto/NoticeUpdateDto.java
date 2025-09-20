package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 更新公告的数据传输对象
 */
@Data
public class NoticeUpdateDto {
    /**
     * 公告ID
     */
    private Long id;
    
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
} 