package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class basicUserVo {

    private Long userId;
    private String name;
    private String sex;
    private String roleId;

    private String phone;

    private Long positionId;
    private String avatar;
    private String status;
    private String email;
    
    // 部门相关字段（仅学生有）
    private Long departmentId;
    private String departmentName;
    
    // 宿舍字段（仅学生有）
    private String dormitory;

    // 在线状态
    private Boolean isOnline;

}
