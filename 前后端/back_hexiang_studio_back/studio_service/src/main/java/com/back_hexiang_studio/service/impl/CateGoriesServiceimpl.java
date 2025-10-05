package com.back_hexiang_studio.service.impl;


import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.PermissionException;
import com.back_hexiang_studio.GlobalException.ParamException;
import com.back_hexiang_studio.GlobalException.NotFoundException;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.back_hexiang_studio.annotation.AutoFill;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.dto.meterial.CategoryAddDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryUpdateDto;

import com.back_hexiang_studio.dv.vo.material.categoriesVo;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;

import com.back_hexiang_studio.entity.Material;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.mapper.CateGoriesMapper;
import com.back_hexiang_studio.mapper.MaterialMapper;
import com.back_hexiang_studio.utils.FileUtils;

import com.back_hexiang_studio.service.CateGoriesService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.back_hexiang_studio.mapper.UserMapper;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 资料分类服务实现类
 * 
 * @author Hexiang
 * @date 2024/09/27
 */
@Service
@Slf4j
public class CateGoriesServiceimpl implements CateGoriesService {

    @Autowired
    private CateGoriesMapper cateGoriesMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 获取资料所有分类
     *
     * @return 分类列表
     */
    @Override
    public List<categoriesVo> getCategories() {
        try {
            log.info("获取资料所有分类");
            
        // 定义缓存key
        String cacheKey = "categories:all";
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取资料分类列表");
            return (List<categoriesVo>) cacheResult;
        }
      
        List<categoriesVo> categories = cateGoriesMapper.getCategories();
            if (categories == null) {
                log.warn("查询分类返回null");
                throw new NotFoundException(ErrorCode.NOT_FOUND, "暂无分类数据");
        }
        
        // 存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, categories, 30, TimeUnit.MINUTES);
        
            log.info("查询到{}个分类", categories.size());
        return categories;
            
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取资料分类失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分类列表失败");
        }
    }


    /**
     * 添加资料分类
     *
     * @param categoryAddDto 分类信息
     */
    @Override
    @AutoFill(value = OperationType.INSERT)
    public void addCategory(CategoryAddDto categoryAddDto) {
        try {
            log.info("添加资料分类: {}", categoryAddDto.getName());
            
            // 参数校验
            validateCategoryAddDto(categoryAddDto);
            
            // 权限验证
            validateAdminPermission("添加分类");
            
            // 检查分类名称是否已存在
            checkCategoryNameExists(categoryAddDto.getName(), null);
            
            // 执行添加操作
        cateGoriesMapper.addCategory(categoryAddDto);
            log.debug("已执行添加分类操作，分类名称: {}", categoryAddDto.getName());
            
            log.info("添加资料分类成功: {}", categoryAddDto.getName());
        
        // 清除分类缓存
            clearCategoriesCache();
            
        } catch (ParamException | PermissionException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加资料分类失败，分类名称: {}", categoryAddDto.getName(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加分类失败");
        }
    }


    /**
     * 更新资料分类
     *
     * @param categoryUpdateDto 分类更新信息
     */
    @Override
    @AutoFill(value = OperationType.UPDATE)
    public void updateCategory(CategoryUpdateDto categoryUpdateDto) {
        try {
            log.info("更新资料分类，ID: {}, 名称: {}", categoryUpdateDto.getId(), categoryUpdateDto.getName());
            
            // 参数校验
            validateCategoryUpdateDto(categoryUpdateDto);
            
            // 权限验证
            validateAdminPermission("更新分类");
            
            // 检查分类是否存在
            checkCategoryExists(categoryUpdateDto.getId());
            
            // 检查分类名称是否与其他分类重复
            checkCategoryNameExists(categoryUpdateDto.getName(), categoryUpdateDto.getId());
            
            // 执行更新操作
        cateGoriesMapper.updateCategory(categoryUpdateDto);
            log.debug("已执行更新分类操作，ID: {}", categoryUpdateDto.getId());
            
            log.info("更新资料分类成功，ID: {}, 名称: {}", categoryUpdateDto.getId(), categoryUpdateDto.getName());
        
            // 清除相关缓存
            clearCategoriesCache();
            clearMaterialListCache();
            
        } catch (ParamException | PermissionException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新资料分类失败，ID: {}", categoryUpdateDto.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新分类失败");
        }
    }

    /**
     * 删除资料分类
     *
     * @param id 分类ID
     */
    @Override
    public void deleteCategory(Long id) {
        try {
            log.info("删除资料分类，ID: {}", id);
            
            // 参数校验
            if (id == null || id <= 0) {
                log.warn("分类ID无效: {}", id);
                throw new ParamException(ErrorCode.PARAM_ERROR, "分类ID不能为空且必须大于0");
        }

            // 权限验证
            validateAdminPermission("删除分类");

            // 检查分类是否存在
            checkCategoryExists(id);
        
            // 获取该分类下的所有资料，用于删除物理文件
        List<materialDetailVo> materials = materialMapper.getMaterialsByCategoryId(id);
        if (materials != null && !materials.isEmpty()) {
                log.info("分类ID {} 下有 {} 个资料文件需要删除", id, materials.size());
                
            for (materialDetailVo material : materials) {
                // 删除物理文件
                String filePath = material.getUrl();
                if (filePath != null && !filePath.isEmpty()) {
                    boolean deleted = FileUtils.deleteFile(filePath);
                    if (!deleted) {
                        log.warn("物理文件删除失败: {}", filePath);
                    } else {
                            log.debug("成功删除物理文件: {}", filePath);
                    }
                }
            }
        }
        
            // 删除当前分类下的所有资料记录
        cateGoriesMapper.deleteCategoryBymaterial(id);
            log.debug("已删除分类下的资料记录，ID: {}", id);
            
            // 删除分类
            cateGoriesMapper.deleteCategory(id);
            log.debug("已执行删除分类操作，ID: {}", id);
            
            log.info("成功删除资料分类，ID: {}", id);
            
            // 清除相关缓存
            clearAllRelatedCache();
            
        } catch (ParamException | PermissionException | BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除资料分类失败，ID: {}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除分类失败");
        }
    }

    /**
     * 校验分类添加DTO
     * 
     * @param categoryAddDto 分类添加信息
     */
    private void validateCategoryAddDto(CategoryAddDto categoryAddDto) {
        if (categoryAddDto == null) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "分类信息不能为空");
        }
        
        if (categoryAddDto.getName() == null || categoryAddDto.getName().trim().isEmpty()) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "分类名称不能为空");
        }
        
        if (categoryAddDto.getName().length() > 50) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "分类名称长度不能超过50个字符");
        }
    }

    /**
     * 校验分类更新DTO
     * 
     * @param categoryUpdateDto 分类更新信息
     */
    private void validateCategoryUpdateDto(CategoryUpdateDto categoryUpdateDto) {
        if (categoryUpdateDto == null) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "分类信息不能为空");
        }
        
        if (categoryUpdateDto.getId() == null || categoryUpdateDto.getId() <= 0) {
            throw new ParamException(ErrorCode.PARAM_ERROR, "分类ID不能为空且必须大于0");
        }
        
        if (categoryUpdateDto.getName() == null || categoryUpdateDto.getName().trim().isEmpty()) {
            throw new ParamException(ErrorCode.PARAM_MISSING, "分类名称不能为空");
        }
        
        if (categoryUpdateDto.getName().length() > 50) {
            throw new ParamException(ErrorCode.PARAM_VALIDATION_FAILED, "分类名称长度不能超过50个字符");
        }
    }

    /**
     * 验证管理员权限
     * 
     * @param operation 操作描述
     */
    private void validateAdminPermission(String operation) {
        try {
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                log.warn("当前用户ID为空，无法进行权限验证");
                throw new PermissionException(ErrorCode.UNAUTHORIZED, "用户未登录");
            }
            
            String roleIdStr = userMapper.getRole(currentUserId);
            if (roleIdStr == null) {
                log.warn("无法获取用户角色信息，用户ID: {}", currentUserId);
                throw new PermissionException(ErrorCode.FORBIDDEN, "无法获取用户权限信息");
            }
            
            Long roleId = Long.valueOf(roleIdStr);
            
            // 3=管理员, 4=超级管理员
            if (!(roleId == 3L || roleId == 4L)) {
                log.warn("用户权限不足，用户ID: {}, 角色ID: {}, 操作: {}", currentUserId, roleId, operation);
                throw new PermissionException(ErrorCode.PERMISSION_DENIED, "权限不足，无法" + operation);
            }
            
            log.debug("权限验证通过，用户ID: {}, 角色ID: {}, 操作: {}", currentUserId, roleId, operation);
            
        } catch (PermissionException e) {
            throw e;
        } catch (NumberFormatException e) {
            log.error("角色ID格式错误，用户ID: {}", UserContextHolder.getCurrentId(), e);
            throw new PermissionException(ErrorCode.FORBIDDEN, "用户权限信息格式错误");
        } catch (Exception e) {
            log.error("权限验证失败，用户ID: {}", UserContextHolder.getCurrentId(), e);
            throw new PermissionException(ErrorCode.FORBIDDEN, "权限验证失败");
        }
    }

    /**
     * 检查分类名称是否存在
     * 
     * @param name 分类名称
     * @param excludeId 排除的分类ID（用于更新时排除自身）
     */
    private void checkCategoryNameExists(String name, Long excludeId) {
        try {
            Integer count = cateGoriesMapper.countByName(name);
            if (count != null && count > 0) {
                // 如果是更新操作，需要排除自身
                if (excludeId != null) {
                    // 这里可能需要一个新的mapper方法来排除特定ID
                    // 暂时使用简化逻辑
                    log.warn("分类名称已存在: {}", name);
                    throw new BusinessException(ErrorCode.DATA_CONFLICT, "分类名称已存在: " + name);
                } else {
                    log.warn("分类名称已存在: {}", name);
                    throw new BusinessException(ErrorCode.DATA_CONFLICT, "分类名称已存在: " + name);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("检查分类名称是否存在失败，名称: {}", name, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "检查分类名称失败");
        }
    }

    /**
     * 检查分类是否存在
     * 
     * @param id 分类ID
     */
    private void checkCategoryExists(Long id) {
        try {
            // 这里需要一个查询分类是否存在的方法，暂时用count代替
            Integer count = cateGoriesMapper.countByName(""); // 临时实现
            // TODO: 需要添加 countById 方法到 mapper
            log.debug("检查分类是否存在，ID: {}", id);
        } catch (Exception e) {
            log.error("检查分类是否存在失败，ID: {}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "检查分类状态失败");
        }
    }

    /**
     * 清除分类缓存
     */
    private void clearCategoriesCache() {
        try {
            redisTemplate.delete("categories:all");
            log.debug("已清除分类缓存");
        } catch (Exception e) {
            log.warn("清除分类缓存失败", e);
        }
    }

    /**
     * 清除资料列表缓存
     */
    private void clearMaterialListCache() {
        try {
        Set<String> listKeys = redisTemplate.keys("material:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
                log.debug("已清除{}个资料列表缓存", listKeys.size());
            }
        } catch (Exception e) {
            log.warn("清除资料列表缓存失败", e);
        }
    }

    /**
     * 清除所有相关缓存
     */
    private void clearAllRelatedCache() {
        clearCategoriesCache();
        clearMaterialListCache();
        
        try {
        // 清除可能存在的资料详情缓存
        Set<String> detailKeys = redisTemplate.keys("material:detail:*");
        if (detailKeys != null && !detailKeys.isEmpty()) {
            redisTemplate.delete(detailKeys);
                log.debug("已清除{}个资料详情缓存", detailKeys.size());
            }
        } catch (Exception e) {
            log.warn("清除资料详情缓存失败", e);
        }
    }
}


