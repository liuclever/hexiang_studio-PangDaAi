package com.back_hexiang_studio.dv.vo.material;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class materialDetailVo {
    private Integer id;

    private Long status;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String url;

    private String description;

    private Integer categoryId;

    private  String category;

    private String uploadTime;

    private Integer uploaderId;

    private String uploader;

    private Integer downloadCount;

    private Integer isPublic;
}
