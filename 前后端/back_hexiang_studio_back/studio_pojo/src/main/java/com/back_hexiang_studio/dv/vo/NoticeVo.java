package com.back_hexiang_studio.dv.vo;

import lombok.Data;

import java.util.List;

@Data
public class NoticeVo {
    /** 公告ID */
    private Long noticeId;
    /** 公告标题 */
    private String title;
    /** 公告内容 */
    private String content;
    /** 发布者 */
    private String publisher;
    /** 发布时间 */
    private String publishTime;
    /** 状态：1-已发布，0-草稿 */
    private Integer status;
    /** 公告类型：0通知 1活动 2新闻 3其他 */
    private Integer type;
    private String createTime;
    private String updateTime;
    private List<NoticeImageVo> images;
    private List<NoticeAttachmentVo> attachments;
}
