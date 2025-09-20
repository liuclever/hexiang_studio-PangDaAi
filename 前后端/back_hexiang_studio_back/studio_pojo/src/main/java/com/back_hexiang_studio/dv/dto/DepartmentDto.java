package com.back_hexiang_studio.dv.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DepartmentDto implements Serializable {
    /** 部门ID */
    private Long id;
    
    /** 部门名称 */
    private String departmentName;
    
    /** 部门描述 */
    private String description;
} 