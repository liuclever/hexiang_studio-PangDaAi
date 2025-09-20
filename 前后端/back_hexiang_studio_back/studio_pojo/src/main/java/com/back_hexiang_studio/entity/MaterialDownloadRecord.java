package com.back_hexiang_studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料下载记录实体类
 */
@Data
@TableName("material_download_record")
public class MaterialDownloadRecord {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 资料ID
     */
    @TableField("material_id")
    private Long materialId;
    
    /**
     * 下载用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 下载时间
     */
    @TableField("download_time")
    private LocalDateTime downloadTime;
    
    /**
     * 下载IP地址
     */
    @TableField("ip_address")
    private String ipAddress;
    
    /**
     * 设备信息
     */
    @TableField("device_info")
    private String deviceInfo;
} 