package com.back_hexiang_studio.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 常用签到地点实体类
 */
@Data
public class CommonLocation {
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
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
    
    /**
     * 地点描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;
} 