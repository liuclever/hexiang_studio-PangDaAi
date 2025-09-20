package com.back_hexiang_studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料分类实体类
 */
@Data
@TableName("material_category")
public class MaterialCategory {
    
    /**
     * 分类ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类名称
     */
    @TableField("name")
    private String name;
    
    /**
     * 排序权重
     */
    @TableField("order_id")
    private Long orderId;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    /**
     * 更新人ID
     */
    @TableField("update_user")
    private Long updateUser;
    
    /**
     * 创建人ID
     */
    @TableField("create_user")
    private Long createUser;
} 