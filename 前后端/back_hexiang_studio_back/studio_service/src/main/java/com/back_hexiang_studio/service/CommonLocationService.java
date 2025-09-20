package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.CommonLocationDto;
import com.back_hexiang_studio.entity.CommonLocation;

import java.util.List;

/**
 * 常用签到地点服务接口
 */
public interface CommonLocationService {
    /**
     * 获取所有常用签到地点
     * @return 常用地点列表
     */
    List<CommonLocationDto> getAllCommonLocations();
    
    /**
     * 根据ID获取常用签到地点
     * @param id 地点ID
     * @return 常用地点
     */
    CommonLocationDto getCommonLocationById(Integer id);
    
    /**
     * 新增常用签到地点
     * @param locationDto 常用地点DTO
     * @return 新增的地点ID
     */
    Integer createCommonLocation(CommonLocationDto locationDto);
    
    /**
     * 更新常用签到地点
     * @param id 地点ID
     * @param locationDto 常用地点DTO
     */
    void updateCommonLocation(Integer id, CommonLocationDto locationDto);
    
    /**
     * 删除常用签到地点
     * @param id 地点ID
     */
    void deleteCommonLocation(Integer id);
} 