package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.Position;
import com.back_hexiang_studio.mapper.PositionMapper;
import com.back_hexiang_studio.service.PositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PositionServiceImpl implements PositionService {

    @Autowired
    private PositionMapper positionMapper;

    @Override
    public List<Position> getAll() {
        log.info("获取所有职位列表");
        return positionMapper.getAll();
    }

    @Override
    public List<Position> getByRole(String role) {
        log.info("根据角色获取职位列表: {}", role);
        return positionMapper.getByRole(role);
    }
} 