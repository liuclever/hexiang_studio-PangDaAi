package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor // 显式添加公共的全参数构造函数
@NoArgsConstructor  // 如果需要无参构造函数
public class UserLoginVo {
    private Long userId;
    private String userName;
    private String password;
    private String token;
    private String avatar;
    private String name;
    private Long roleId;
    private Long positionId; // 添加职位ID字段
}

