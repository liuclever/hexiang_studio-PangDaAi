package com.back_hexiang_studio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionsMapper {


        /**
         * 根据角色ID查询权限
         */
        List<String> findPermissionsByUserId(@Param("roleId") Long roleId);

}
