package com.back_hexiang_studio.entity;

import lombok.Data;

/**
 * 角色表
 */
@Data
public class Role {
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String description;
}