package com.back_hexiang_studio.dv.dto;

import lombok.Data;

/**
 * 常用签到地点DTO
 */
@Data
public class CommonLocationDto {
    /**
     * 地点ID
     */
    private Integer id;
    
    /**
     * 地点名称
     */
    private String name;
    
    /**
     * 纬度
     */
    private Double lat;
    
    /**
     * 经度
     */
    private Double lng;
    
    /**
     * 地点描述
     */
    private String description;
} 