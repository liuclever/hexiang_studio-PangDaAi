package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PageNoticeDto implements Serializable {
    private Integer page;
    private Integer pageSize;
    private String title;
    private String content;
    private String type;
    private String status;
    private String createTime;
    private String beginTime;
    private String endTime;
    private String Keyword;
}
