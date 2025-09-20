package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告展示图片实体类
 */
@Data
public class NoticeImage {
    /** 图片ID */
    private Long imageId;
    
    /** 公告ID */
    private Long noticeId;
    
    /** 图片名称 */
    private String imageName;
    
    /** 图片路径 */
    private String imagePath;
    
    /** 图片大小(字节) */
    private Long imageSize;
    
    /** 图片类型 */
    private String imageType;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 创建人 */
    private String createUser;
} 