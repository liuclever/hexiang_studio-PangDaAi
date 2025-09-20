package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 部门实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    
    /**
     * 部门ID
     */
    private Long id;
    
    /**
     * 部门ID（兼容旧字段名）
     */
    private Long departmentId;
    
    /**
     * 部门名称
     */
    private String departmentName;
    
    /**
     * 部门描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 