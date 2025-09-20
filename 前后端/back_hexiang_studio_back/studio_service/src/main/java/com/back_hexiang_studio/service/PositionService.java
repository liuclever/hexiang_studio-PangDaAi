package com.back_hexiang_studio.service;

import com.back_hexiang_studio.entity.Position;

import java.util.List;

public interface PositionService {
    /**
     * 获取所有职位列表
     * @return 职位列表
     */
    List<Position> getAll();

    /**
     * 根据角色获取职位列表
     * @param role 角色
     * @return 职位列表
     */
    List<Position> getByRole(String role);
} 