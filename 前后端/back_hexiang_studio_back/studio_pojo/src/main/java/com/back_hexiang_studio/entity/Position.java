package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 职位实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 职位ID
     */
    private Integer positionId;

    /**
     * 角色（对应 user.role）
     */
    private String role;

    /**
     * 职位名称
     */
    private String positionName;

    /**
     * 权限列表
     */
    private String permissions;
} 