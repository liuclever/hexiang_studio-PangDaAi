package com.back_hexiang_studio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    /**
     * 删除用户角色关联
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
    
    /**
     * 批量删除用户角色关联
     * @param userIds 用户ID列表
     */
    void deleteByUserIds(@Param("userIds") List<String> userIds);
} 