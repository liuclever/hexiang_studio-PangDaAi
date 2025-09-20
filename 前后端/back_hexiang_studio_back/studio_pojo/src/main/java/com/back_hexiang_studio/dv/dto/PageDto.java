package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PageDto {
    // 搜索条件：姓名
    private String name;

    // 当前页码
    private Integer page = 1; // 默认为第1页

    // 每页记录数
    private Integer pageSize = 10; // 默认每页10条

    // 分页起始位置
    private Integer start;

    // 角色ID
    private String roleId;

    // 状态
    private String status;
    
    // 部门ID
    private String departmentId;

    // 为了兼容前端可能传递的snake_case命名
    public void setRole_id(String roleId) {
        this.roleId = roleId;
    }
    
    public void setDepartment_id(String departmentId) {
        this.departmentId = departmentId;
    }
}
