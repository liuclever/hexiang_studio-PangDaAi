package com.back_hexiang_studio.dv.vo;

import lombok.Data;

/**
 * 公告附件信息VO
 */
@Data
public class NoticeAttachmentVo {
    /** 附件ID */
    private Long attachmentId;
    /** 附件名称 */
    private String fileName;
    /** 附件路径 */
    private String filePath;
    /** 附件大小 */
    private Long fileSize;
    /** 附件类型 */
    private String fileType;
    /** 下载次数 */
    private Integer downloadCount;
    /** 完整访问URL */
    private String url;
} 