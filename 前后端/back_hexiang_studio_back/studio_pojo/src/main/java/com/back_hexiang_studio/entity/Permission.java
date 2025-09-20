package com.back_hexiang_studio.entity;

import lombok.Data;
import java.util.Date;

/**
 * 权限表
 */
@Data
public class Permission {
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    private String description;
    private Date createdAt;
    private Date updatedAt;
}