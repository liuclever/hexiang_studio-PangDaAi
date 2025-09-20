package com.back_hexiang_studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料实体类
 */
@Data
@TableName("material")
public class Material {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;
    
    /**
     * 文件类型/扩展名
     */
    @TableField("file_type")
    private String fileType;
    
    /**
     * 文件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;
    
    /**
     * 资料访问路径
     */
    @TableField("url")
    private String url;
    
    /**
     * 资料描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;
    
    /**
     * 上传时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;
    
    /**
     * 上传者ID
     */
    @TableField("uploader_id")
    private Long uploaderId;
    
    /**
     * 上传者姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String uploaderName;
    
    /**
     * 下载次数
     */
    @TableField("download_count")
    private Long downloadCount;
    
    /**
     * 是否公开 0不公开 1公开
     */
    @TableField("is_public")
    private Integer isPublic;
    
    /**
     * 状态：1-正常，0-已删除
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    /**
     * 更新者ID
     */
    @TableField("update_id")
    private Long updateUser;
}
