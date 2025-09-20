package com.back_hexiang_studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notice {
        /** 公告ID */
        private Long noticeId;
        /** 公告标题 */
        private String title;
        /** 公告内容 */
        private String content;
        /** 发布人 */
        private String publisher;
        /** 发布时间 */
        private LocalDateTime publishTime;
        /** 状态：1-已发布，0-草稿 */
        private String status;
        /** 公告类型：0通知 1活动 2新闻 3其他*/
        private Integer type;
        /** 创建时间 */
        private LocalDateTime createTime;
        /** 更新时间 */
        private LocalDateTime updateTime;
        /** 创建人 */
        private String createUser;
        /** 更新人 */
        private String updateUser;

}
