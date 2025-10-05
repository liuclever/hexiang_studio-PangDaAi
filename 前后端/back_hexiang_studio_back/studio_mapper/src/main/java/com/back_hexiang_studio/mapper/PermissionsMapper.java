package com.back_hexiang_studio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionsMapper {

            /**
     * 根据用户ID查询权限码列表（基于职位）
     * 使用更简单的方法：先获取JSON，然后在Java中解析
     */
    @Select("SELECT p.permissions " +
            "FROM user u " +
            "JOIN position p ON u.position_id = p.position_id " +
            "WHERE u.user_id = #{userId}")
    String findPermissionsJsonByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查询权限码列表（备用方法）
     */
    List<String> findPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据职位ID查询权限JSON字符串
     */
    @Select("SELECT permissions FROM position WHERE position_id = #{positionId}")
    String findPermissionsJsonByPositionId(@Param("positionId") Long positionId);
    
    /**
     * 根据职位ID查询权限码列表（备用）
     */
    List<String> findPermissionsByRoleId(@Param("positionId") Long positionId);

    /**
     * 检查用户是否具有指定权限（基于职位）
     */
    @Select("SELECT JSON_CONTAINS(p.permissions, JSON_QUOTE(#{permissionCode})) " +
            "FROM user u " +
            "JOIN position p ON u.position_id = p.position_id " +
            "WHERE u.user_id = #{userId}")
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionCode") String permissionCode);

    /**
     * 获取所有权限
     */
    @Select("SELECT permission_code, permission_name, description FROM permission ORDER BY permission_id")
    List<String> getAllPermissions();
}
