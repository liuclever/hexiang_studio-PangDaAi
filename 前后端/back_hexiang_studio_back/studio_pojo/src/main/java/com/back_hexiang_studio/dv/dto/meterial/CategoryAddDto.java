package com.back_hexiang_studio.dv.dto.meterial;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料分类添加DTO
 */
@Data
public class CategoryAddDto {
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 排序权重
     */
    private Long orderId;
    
    /**
     * 更新时间（自动填充）
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建时间（自动填充）
     */
    private LocalDateTime createTime;
    
    /**
     * 更新人ID（自动填充）
     */
    private Long updateUser;
    
    /**
     * 创建人ID（自动填充）
     */
    private Long createUser;
}
