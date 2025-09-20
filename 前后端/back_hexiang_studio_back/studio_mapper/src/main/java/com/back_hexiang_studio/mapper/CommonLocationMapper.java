package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.CommonLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 常用签到地点Mapper接口
 */
@Mapper
public interface CommonLocationMapper {
    /**
     * 获取所有常用签到地点
     * @return 常用地点列表
     */
    List<CommonLocation> getAllCommonLocations();
    
    /**
     * 根据ID获取常用签到地点
     * @param id 地点ID
     * @return 常用地点
     */
    CommonLocation getCommonLocationById(@Param("id") Integer id);
    
    /**
     * 新增常用签到地点
     * @param location 常用地点
     * @return 影响行数
     */
    int insertCommonLocation(CommonLocation location);
    
    /**
     * 更新常用签到地点
     * @param location 常用地点
     * @return 影响行数
     */
    int updateCommonLocation(CommonLocation location);
    
    /**
     * 删除常用签到地点
     * @param id 地点ID
     * @return 影响行数
     */
    int deleteCommonLocation(@Param("id") Integer id);

    /**
     * 根据ID获取常用签到地点信息
     * @param id
     * @return
     */
    Map<String,Double> getCommonLocationsById(int id);
}