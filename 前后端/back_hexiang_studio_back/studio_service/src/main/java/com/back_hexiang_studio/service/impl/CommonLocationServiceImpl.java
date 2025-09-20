package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.CommonLocationDto;
import com.back_hexiang_studio.entity.CommonLocation;
import com.back_hexiang_studio.mapper.CommonLocationMapper;
import com.back_hexiang_studio.service.CommonLocationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 常用签到地点
 */
@Service
public class CommonLocationServiceImpl implements CommonLocationService {
    
    @Autowired
    private CommonLocationMapper commonLocationMapper;

    /**
     * 获取所有常用签到地点
     * @return
     */
    @Override
    public List<CommonLocationDto> getAllCommonLocations() {
        List<CommonLocation> locations = commonLocationMapper.getAllCommonLocations();
        return locations.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * 根据ID获取常用签到地点
     * @param id
     * @return
     */
    @Override
    public CommonLocationDto getCommonLocationById(Integer id) {
        CommonLocation location = commonLocationMapper.getCommonLocationById(id);
        if (location == null) {
            throw new IllegalArgumentException("常用地点不存在");
        }
        return convertToDto(location);
    }

    /**
     * 创建常用签到地点
     * @param locationDto
     * @return
     */
    @Override
    @Transactional
    public Integer createCommonLocation(CommonLocationDto locationDto) {
        CommonLocation location = new CommonLocation();
        location.setName(locationDto.getName());
        location.setLatitude(locationDto.getLat());
        location.setLongitude(locationDto.getLng());
        location.setDescription(locationDto.getDescription());
        location.setCreateTime(LocalDateTime.now());
        
        commonLocationMapper.insertCommonLocation(location);
        return location.getId();
    }

    /**
     * 更新常用签到地点
     * @param id 地点ID
     * @param locationDto 常用地点DTO
     */
    @Override
    @Transactional
    public void updateCommonLocation(Integer id, CommonLocationDto locationDto) {
        CommonLocation existingLocation = commonLocationMapper.getCommonLocationById(id);
        if (existingLocation == null) {
            throw new IllegalArgumentException("常用地点不存在");
        }
        
        existingLocation.setName(locationDto.getName());
        existingLocation.setLatitude(locationDto.getLat());
        existingLocation.setLongitude(locationDto.getLng());
        existingLocation.setDescription(locationDto.getDescription());
        
        commonLocationMapper.updateCommonLocation(existingLocation);
    }

    /**
     * 删除常用签到地点
     * @param id 地点ID
     */
    @Override
    @Transactional
    public void deleteCommonLocation(Integer id) {
        CommonLocation existingLocation = commonLocationMapper.getCommonLocationById(id);
        if (existingLocation == null) {
            throw new IllegalArgumentException("常用地点不存在");
        }
        
        commonLocationMapper.deleteCommonLocation(id);
    }
    
    /**
     * 将实体转换为DTO
     * @param location 常用地点实体
     * @return 常用地点DTO
     */
    private CommonLocationDto convertToDto(CommonLocation location) {
        CommonLocationDto dto = new CommonLocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setLat(location.getLatitude());
        dto.setLng(location.getLongitude());
        dto.setDescription(location.getDescription());
        return dto;
    }
} 