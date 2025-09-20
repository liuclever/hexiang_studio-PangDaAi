package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料更新DTO
 */
@Data
public class MaterialUpdateDto {
    /**
     * 资料ID
     */
    private Long id;
    
    /**
     * 资料描述
     */
    private String description;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 是否公开：1-公开，0-不公开
     */
    private Boolean isPublic;
    
    /**
     * 资料访问路径（仅在分类变更时需要更新）
     */
    private String url;
    
    /**
     * 更新时间（自动填充）
     */
    private LocalDateTime updateTime;
    
    /**
     * 更新人ID（自动填充）
     */
    private Long updateUser;
}
