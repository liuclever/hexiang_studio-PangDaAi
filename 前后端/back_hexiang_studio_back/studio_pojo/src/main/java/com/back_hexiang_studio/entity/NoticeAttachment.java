package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告附件实体类
 */
@Data
public class NoticeAttachment {
    /** 附件ID */
    private Long attachmentId;
    
    /** 公告ID */
    private Long noticeId;
    
    /** 文件名称 */
    private String fileName;
    
    /** 文件路径 */
    private String filePath;
    
    /** 文件大小(字节) */
    private Long fileSize;
    
    /** 文件类型 */
    private String fileType;
    
    /** 下载次数 */
    private Integer downloadCount;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 创建人 */
    private String createUser;
} 