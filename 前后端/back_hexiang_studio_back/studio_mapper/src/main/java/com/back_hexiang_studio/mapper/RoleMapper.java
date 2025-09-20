package com.back_hexiang_studio.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper {

    /**
     * 根据角色名称获取角色ID
     * @param roleName
     * @return
     */

    String getRoleIdByName(String roleName);
}
