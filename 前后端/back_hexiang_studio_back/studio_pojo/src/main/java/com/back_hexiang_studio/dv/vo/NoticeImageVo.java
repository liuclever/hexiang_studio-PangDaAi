package com.back_hexiang_studio.dv.vo;

import lombok.Data;

/**
 * 公告图片信息VO
 */
@Data
public class NoticeImageVo {
    /** 图片ID */
    private Long imageId;
    /** 图片名称 */
    private String fileName;
    /** 图片路径 */
    private String filePath;
    /** 图片大小 */
    private Long fileSize;
    /** 图片类型 */
    private String fileType;
    /** 完整访问URL */
    private String url;
} 