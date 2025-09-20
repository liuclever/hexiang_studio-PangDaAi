package com.back_hexiang_studio.entity;

import lombok.Data;

/**
 * 角色权限表
 */
@Data
public class RolePermission {
    private Long roleId;
    private Long permissionId;
}