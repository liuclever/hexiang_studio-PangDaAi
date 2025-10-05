package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.dto.CommonLocationDto;
import com.back_hexiang_studio.entity.CommonLocation;
import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.NotFoundException;
import com.back_hexiang_studio.GlobalException.ParamException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.back_hexiang_studio.mapper.CommonLocationMapper;
import com.back_hexiang_studio.service.CommonLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 常用签到地点服务实现类
 * 
 * @author Hexiang
 * @date 2024/09/27
 */
@Slf4j
@Service
public class CommonLocationServiceImpl implements CommonLocationService {
    
    @Autowired
    private CommonLocationMapper commonLocationMapper;

    /**
     * 获取所有常用签到地点
     * 
     * @return 常用地点列表
     */
    @Override
    public List<CommonLocationDto> getAllCommonLocations() {
        try {
            log.info("获取所有常用签到地点");
            List<CommonLocation> locations = commonLocationMapper.getAllCommonLocations();
            
            if (locations == null) {
                log.warn("查询常用地点返回null");
                throw new NotFoundException(ErrorCode.NOT_FOUND, "暂无常用地点数据");
            }
            
            log.info("查询到{}个常用地点", locations.size());
            return locations.stream().map(this::convertToDto).collect(Collectors.toList());
            
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取常用签到地点列表失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取常用地点列表失败");
        }
    }

    /**
     * 根据ID获取常用签到地点
     * 
     * @param id 地点ID
     * @return 常用地点DTO
     */
    @Override
    public CommonLocationDto getCommonLocationById(Integer id) {
        try {
            log.info("根据ID获取常用签到地点: {}", id);
            
            // 参数校验
            if (id == null || id <= 0) {
                log.warn("常用地点ID无效: {}", id);
                throw new ParamException(ErrorCode.PARAM_ERROR, "常用地点ID不能为空且必须大于0");
            }
            
            CommonLocation location = commonLocationMapper.getCommonLocationById(id);
            if (location == null) {
                log.warn("常用地点不存在，ID: {}", id);
                throw new NotFoundException(ErrorCode.NOT_FOUND, "常用地点不存在");
            }
            
            log.info("查询到常用地点: {}", location.getName());
            return convertToDto(location);
            
        } catch (ParamException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据ID获取常用地点失败，ID: {}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取常用地点失败");
        }
    }

    /**
     * 创建常用签到地点
     * 
     * @param locationDto 地点信息
     * @return 创建的地点ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createCommonLocation(CommonLocationDto locationDto) {
        try {
            log.info("创建常用签到地点: {}", locationDto.getName());
            
            // 参数校验
            validateLocationDto(locationDto, false);
            
            CommonLocation location = new CommonLocation();
            location.setName(locationDto.getName());
            location.setLatitude(locationDto.getLat());
            location.setLongitude(locationDto.getLng());
            location.setDescription(locationDto.getDescription());
            location.setCreateTime(LocalDateTime.now());
            
            commonLocationMapper.insertCommonLocation(location);
            
            log.info("成功创建常用地点，ID: {}, 名称: {}", location.getId(), location.getName());
            return location.getId();
            
        } catch (ParamException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建常用签到地点失败，名称: {}", locationDto.getName(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建常用地点失败");
        }
    }

    /**
     * 更新常用签到地点
     * 
     * @param id 地点ID
     * @param locationDto 常用地点DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCommonLocation(Integer id, CommonLocationDto locationDto) {
        try {
            log.info("更新常用签到地点，ID: {}, 名称: {}", id, locationDto.getName());
            
            // 参数校验
            if (id == null || id <= 0) {
                log.warn("常用地点ID无效: {}", id);
                throw new ParamException(ErrorCode.PARAM_ERROR, "常用地点ID不能为空且必须大于0");
            }
            
            validateLocationDto(locationDto, true);
            
            // 检查地点是否存在
            CommonLocation existingLocation = commonLocationMapper.getCommonLocationById(id);
            if (existingLocation == null) {
                log.warn("要更新的常用地点不存在，ID: {}", id);
                throw new NotFoundException(ErrorCode.NOT_FOUND, "要更新的常用地点不存在");
            }
            
            existingLocation.setName(locationDto.getName());
            existingLocation.setLatitude(locationDto.getLat());
            existingLocation.setLongitude(locationDto.getLng());
            existingLocation.setDescription(locationDto.getDescription());
            
            int updateCount = commonLocationMapper.updateCommonLocation(existingLocation);
            if (updateCount <= 0) {
                log.error("更新常用地点失败，影响行数为0，ID: {}", id);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新常用地点失败");
            }
            
            log.info("成功更新常用地点，ID: {}, 名称: {}", id, locationDto.getName());
            
        } catch (ParamException | NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新常用签到地点失败，ID: {}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新常用地点失败");
        }
    }

    /**
     * 删除常用签到地点
     * 
     * @param id 地点ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommonLocation(Integer id) {
        try {
            log.info("删除常用签到地点，ID: {}", id);
            
            // 参数校验
            if (id == null || id <= 0) {
                log.warn("常用地点ID无效: {}", id);
                throw new ParamException(ErrorCode.PARAM_ERROR, "常用地点ID不能为空且必须大于0");
            }
            
            // 检查地点是否存在
            CommonLocation existingLocation = commonLocationMapper.getCommonLocationById(id);
            if (existingLocation == null) {
                log.warn("要删除的常用地点不存在，ID: {}", id);
                throw new NotFoundException(ErrorCode.NOT_FOUND, "要删除的常用地点不存在");
            }
            
            int deleteCount = commonLocationMapper.deleteCommonLocation(id);
            if (deleteCount <= 0) {
                log.error("删除常用地点失败，影响行数为0，ID: {}", id);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除常用地点失败");
            }
            
            log.info("成功删除常用地点，ID: {}, 名称: {}", id, existingLocation.getName());
            
        } catch (ParamException | NotFoundException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除常用签到地点失败，ID: {}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除常用地点失败");
        }
    }
    
    /**
     * 校验地点DTO参数
     * 
     * @param locationDto 地点信息
     * @param isUpdate 是否为更新操作
     */
    private void validateLocationDto(CommonLocationDto locationDto, boolean isUpdate) {
        if (locationDto == null) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "地点信息不能为空");
        }
        
        if (locationDto.getName() == null || locationDto.getName().trim().isEmpty()) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "地点名称不能为空");
        }
        
        if (locationDto.getName().length() > 50) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "地点名称长度不能超过50个字符");
        }
        
        if (locationDto.getLat() == null) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "纬度不能为空");
        }
        
        if (locationDto.getLng() == null) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "经度不能为空");
        }
        
        // 校验坐标范围
        if (locationDto.getLat() < -90 || locationDto.getLat() > 90) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "纬度必须在-90到90之间");
        }
        
        if (locationDto.getLng() < -180 || locationDto.getLng() > 180) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "经度必须在-180到180之间");
        }
        
        // 描述长度校验
        if (locationDto.getDescription() != null && locationDto.getDescription().length() > 200) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "地点描述长度不能超过200个字符");
        }
    }
    
    /**
     * 将实体转换为DTO
     * 
     * @param location 常用地点实体
     * @return 常用地点DTO
     */
    private CommonLocationDto convertToDto(CommonLocation location) {
        if (location == null) {
            return null;
        }
        
        CommonLocationDto dto = new CommonLocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setLat(location.getLatitude());
        dto.setLng(location.getLongitude());
        dto.setDescription(location.getDescription());
        return dto;
    }
} 