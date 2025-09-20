package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PositionMapper {
    /**
     * 获取所有职位列表
     * @return 职位列表
     */
    @Select("SELECT position_id, role, position_name, permissions FROM position")
    List<Position> getAll();

    /**
     * 根据角色获取职位列表
     * @param role 角色
     * @return 职位列表
     */
    @Select("SELECT position_id, role, position_name, permissions FROM position WHERE role = #{role}")
    List<Position> getByRole(@Param("role") String role);
} 