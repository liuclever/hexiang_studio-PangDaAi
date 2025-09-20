package com.back_hexiang_studio.service.impl;



import com.back_hexiang_studio.GlobalException.BusinessException;

import com.back_hexiang_studio.GlobalException.PermissionException;
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
     * @return
     */
    @Override
    public List<categoriesVo> getCategories() {
        // 定义缓存key
        String cacheKey = "categories:all";
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取资料分类列表");
            return (List<categoriesVo>) cacheResult;
        }
        
      
        List<categoriesVo> categories = cateGoriesMapper.getCategories();
        if (categories==null){
            throw new BusinessException("分类不存在");
        }
        
        // 存入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, categories, 30, TimeUnit.MINUTES);
        
        return categories;
    }


    /**
     * 添加资料分类
     *
     * @param categoryAddDto
     */
    @Override
    @AutoFill(value = OperationType.INSERT)
    public void addCategory(CategoryAddDto categoryAddDto) {
        //权限验证
        //  判断当前用户是否为管理员或超级管理员
        Long currentUserId = UserContextHolder.getCurrentId(); // 获取当前登录用户ID
        Long roleId= Long.valueOf(userMapper.getRole(currentUserId));

        if (!(roleId == 3L || roleId == 4L)) { // 3=管理员, 4=超级管理员
            throw new PermissionException("无权限访问该资料");
        }

        // 1. 先查询是否存在同名分类
        Integer count = cateGoriesMapper.countByName(categoryAddDto.getName());
        if (count != null && count > 0) {
            throw new BusinessException("分类名称已存在：" + categoryAddDto.getName());
        }
        cateGoriesMapper.addCategory(categoryAddDto);
        log.info("添加资料分类成功");
        
        // 清除分类缓存
        redisTemplate.delete("categories:all");
    }


    /**
     * 更新课程分类
     *
     * @param categoryUpdateDto
     */
    @Override
    @AutoFill(value = OperationType.UPDATE)
    public void updateCategory(CategoryUpdateDto categoryUpdateDto) {
        // 1. 先查询是否存在同名分类
        Integer count = cateGoriesMapper.countByName(categoryUpdateDto.getName());
        if (count == null && count == 0) {
            throw new BusinessException("分类名称为空：" + categoryUpdateDto.getName());
        }
        // 2. 更新分类
        cateGoriesMapper.updateCategory(categoryUpdateDto);
        
        // 清除分类缓存
        redisTemplate.delete("categories:all");
        
        // 清除资料列表缓存
        Set<String> listKeys = redisTemplate.keys("material:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
    }

    /**
     * 删除资料分类
     *
     * @param id
     */
    @Override
    public void deleteCategory(Long id) {
        if (id == null) {
            throw new BusinessException("资料ID无效");
        }

        //权限验证
        //  判断当前用户是否为管理员或超级管理员
        Long currentUserId = UserContextHolder.getCurrentId(); // 获取当前登录用户ID
        Long roleId= Long.valueOf(userMapper.getRole(currentUserId));

        if (!(roleId == 3L || roleId == 4L)) { // 3=管理员, 4=超级管理员
                throw new PermissionException("无权限访问该资料");
        }
        
        // 1. 先获取该分类下的所有资料，以便删除物理文件
        List<materialDetailVo> materials = materialMapper.getMaterialsByCategoryId(id);
        if (materials != null && !materials.isEmpty()) {
            for (materialDetailVo material : materials) {
                // 删除物理文件
                String filePath = material.getUrl();
                if (filePath != null && !filePath.isEmpty()) {
                    boolean deleted = FileUtils.deleteFile(filePath);
                    if (!deleted) {
                        log.warn("物理文件删除失败: {}", filePath);
                    } else {
                        log.info("成功删除物理文件: {}", filePath);
                    }
                }
            }
        }
        
        // 2. 删除当前分类下的所有资料记录
        cateGoriesMapper.deleteCategoryBymaterial(id);
        
        // 3. 删除分类
        cateGoriesMapper.deleteCategory(id);
        
        // 清除分类缓存
        redisTemplate.delete("categories:all");
        
        // 清除资料列表缓存
        Set<String> listKeys = redisTemplate.keys("material:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
        
        // 清除可能存在的资料详情缓存
        Set<String> detailKeys = redisTemplate.keys("material:detail:*");
        if (detailKeys != null && !detailKeys.isEmpty()) {
            redisTemplate.delete(detailKeys);
        }
    }




}


