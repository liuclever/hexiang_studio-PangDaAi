package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.PermissionException;
import com.back_hexiang_studio.annotation.AutoFill;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.dv.vo.material.materialVo;
import com.back_hexiang_studio.entity.Material;
import com.back_hexiang_studio.entity.MaterialDownloadRecord;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.enumeration.OperationType;
import com.back_hexiang_studio.mapper.CateGoriesMapper;
import com.back_hexiang_studio.mapper.MaterialMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.service.MaterialService;
import com.back_hexiang_studio.utils.FileUtils;
import com.back_hexiang_studio.utils.PathUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 资料服务实现类
 */
@Service
@Slf4j
public class MaterialServiceImpl implements MaterialService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MaterialMapper materialMapper;
    
    @Autowired
    private CateGoriesMapper cateGoriesMapper;

    /**
     * 资料分页查询
     *
     * @param pageMaterialDto 分页查询DTO
     * @return 分页结果
     */
    @Override
    public PageResult getList(PageMaterialDto pageMaterialDto) {
        // 构建缓存key
        StringBuilder cacheKeyBuilder = new StringBuilder("material:list:");
        cacheKeyBuilder.append(pageMaterialDto.getPage()).append(":")
                .append(pageMaterialDto.getPageSize());
                
        if (pageMaterialDto.getName() != null && !pageMaterialDto.getName().isEmpty()) {
            cacheKeyBuilder.append(":name:").append(pageMaterialDto.getName());
        }
        if (pageMaterialDto.getFileType() != null && !pageMaterialDto.getFileType().isEmpty()) {
            cacheKeyBuilder.append(":fileType:").append(pageMaterialDto.getFileType());
        }
        if (pageMaterialDto.getFileTypes() != null && !pageMaterialDto.getFileTypes().isEmpty()) {
            cacheKeyBuilder.append(":fileTypes:").append(String.join(",", pageMaterialDto.getFileTypes()));
        }
        if (pageMaterialDto.getCategoryId() != null) {
            cacheKeyBuilder.append(":categoryId:").append(pageMaterialDto.getCategoryId());
        }
        if (pageMaterialDto.getStartDate() != null && !pageMaterialDto.getStartDate().isEmpty()) {
            cacheKeyBuilder.append(":startDate:").append(pageMaterialDto.getStartDate());
        }
        if (pageMaterialDto.getEndDate() != null && !pageMaterialDto.getEndDate().isEmpty()) {
            cacheKeyBuilder.append(":endDate:").append(pageMaterialDto.getEndDate());
        }
        if (pageMaterialDto.getIsPublic() != null) {
            cacheKeyBuilder.append(":isPublic:").append(pageMaterialDto.getIsPublic());
        }
        
        String cacheKey = cacheKeyBuilder.toString();
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取资料列表");
            return (PageResult) cacheResult;
        }
        
        // 执行分页查询
        PageHelper.startPage(pageMaterialDto.getPage(), pageMaterialDto.getPageSize());
        Page<materialVo> materialList = materialMapper.getList(pageMaterialDto);
        
        // 构建分页结果
        PageInfo<materialVo> pageInfo = new PageInfo<>(materialList);
        PageResult pageResult = new PageResult(
            pageInfo.getTotal(),
            materialList,
            pageInfo.getPageNum(),
            pageInfo.getSize(),
            pageInfo.getPages()
        );
        
        // 将结果存入缓存，分钟过期
        redisTemplate.opsForValue().set(cacheKey, pageResult, 5, TimeUnit.MINUTES);
        
        return pageResult;
    }

    /**
     * 获取资料详情
     * @param id 资料ID
     * @return 资料详情
     */
    @Override
    public materialDetailVo getDetail(Long id) {
        // 定义缓存key
        String cacheKey = "material:detail:" + id;
        
        // 从缓存获取
        Object cacheResult = redisTemplate.opsForValue().get(cacheKey);
        if (cacheResult != null) {
            log.info("从缓存获取资料详情, id: {}", id);
            return (materialDetailVo) cacheResult;
        }
  
        // 参数校验
        if (id == null || id <= 0) {
            log.error("获取资料详情失败，ID为空或无效: {}", id);
            throw new BusinessException("资料ID无效");
        }

        // 查询资料详情
        materialDetailVo materialDetailVo = materialMapper.getDetail(id);

        // 校验查询结果
        if (materialDetailVo == null) {
            log.warn("资料不存在，ID: {}", id);
            throw new BusinessException("资料不存在");
        }

        // 权限校验 私有资料仅限上传者或管理员查看
        Integer isPublic = materialDetailVo.getIsPublic();
        if (isPublic != null && isPublic == 0) {
            Long currentUserId = UserContextHolder.getCurrentId(); // 获取当前登录用户ID
            log.info("权限校验 - 当前用户ID: {}, 资料上传者ID: {}", currentUserId, materialDetailVo.getUploaderId());
            
            if (currentUserId == null) {
                throw new PermissionException("用户未登录");
            }
            
            // 获取用户角色，添加null检查
            String roleIdStr = userMapper.getRole(currentUserId);
            if (roleIdStr == null) {
                log.warn("用户角色信息不存在，用户ID: {}", currentUserId);
                throw new PermissionException("用户角色信息异常");
            }
            Long roleId = Long.valueOf(roleIdStr);
            log.info("权限校验 - 用户角色ID: {}", roleId);
            
            // 检查是否为上传者或管理员
            Integer uploaderIdInt = materialDetailVo.getUploaderId();
            Long uploaderId = uploaderIdInt != null ? uploaderIdInt.longValue() : null;
            boolean isUploader = uploaderId != null && uploaderId.equals(currentUserId);
            boolean isAdmin = roleId.equals(3L) || roleId.equals(4L); // 3=管理员, 4=超级管理员
            
            log.info("权限校验结果 - 是否上传者: {}, 是否管理员: {}", isUploader, isAdmin);
            
            if (!isUploader && !isAdmin) {
                throw new PermissionException("无权限访问该资料");
            }
        }
       
        // 将结果存入缓存，设10分钟过期
        redisTemplate.opsForValue().set(cacheKey, materialDetailVo, 10, TimeUnit.MINUTES);

        return materialDetailVo;
    }

    /**
     * 上传资料
     * @param files 上传的文件列表
     * @param categoryId 分类ID
     * @param isPublic 是否公开
     * @param description 描述
     */
    @Override

    public void uploadMaterial(List<MultipartFile> files, Long categoryId, Integer isPublic, String description) {
        // 获取当前登录用户ID
        Long currentId = UserContextHolder.getCurrentId();

        User user = userMapper.getUserById(currentId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 判断是否为管理员
        Long role = user.getRoleId();
        if (role != 3) {
            throw new PermissionException("无权限上传资料");
        }

        List<Material> materialList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue; // 跳过空文件
            }
            
            try {
                // 根据分类id查询分类名字
                String categoryName = cateGoriesMapper.getCategoryNameById(categoryId);
                if (categoryName == null) {
                    throw new BusinessException("分类不存在");
                }
                
                // 注意：文件验证已在控制层完成，这里不再重复验证
                
                // 保存文件
                String relativePath = FileUtils.saveFile(file, categoryName);

                // 创建Material实体
                Material material = new Material();
                material.setFileName(file.getOriginalFilename());
                
                // 获取文件扩展名并映射为文件类型
                String extension = "";
                int i = file.getOriginalFilename().lastIndexOf('.');
                if (i > 0) {
                    extension = file.getOriginalFilename().substring(i + 1);
                }
                material.setFileType(getFileTypeFromExtension(extension));
                material.setFileSize(file.getSize());
                material.setUrl(relativePath);
                material.setDescription(description);
                material.setCategoryId(categoryId);
                material.setIsPublic(isPublic);
                material.setDownloadCount(0L);
                material.setStatus(1);

                // 设置公共字段
                material.setUploaderId(currentId);
                material.setUploadTime(now);
                material.setUpdateTime(now);
                material.setUpdateUser(currentId);

                materialList.add(material);
            } catch (IOException e) {
                log.error("处理文件上传时发生错误: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("文件上传失败: " + file.getOriginalFilename(), e);
            }
        }

        // 批量插入数据库
        if (!materialList.isEmpty()) {
            insertMaterialBatch(materialList);
        }
        
        // 清理缓存
        clearMaterialCache();
    }

    /**
     * 将文件扩展名映射为文件类型（直接返回小写扩展名）
     * @param extension 文件扩展名
     * @return 文件类型（扩展名）
     */
    private String getFileTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "other";
        }
        return extension.toLowerCase();
    }

    /**
     * 记录下载次数并添加下载记录
     * @param id 资料ID
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param deviceInfo 设备信息
     */
    @Override
    @Transactional
    public void recordDownload(Long id, Long userId, String ipAddress, String deviceInfo) {
        log.info("记录资料下载，ID: {}, 用户ID: {}", id, userId);
        if (id == null) {
            return;
        }
        
        // 增加下载次数
        materialMapper.incrementDownloadCount(id);
        
        // 添加下载记录
        MaterialDownloadRecord record = new MaterialDownloadRecord();
        record.setMaterialId(id);
        record.setUserId(userId);
        record.setDownloadTime(LocalDateTime.now());
        record.setIpAddress(ipAddress);
        record.setDeviceInfo(deviceInfo);
        
        materialMapper.addDownloadRecord(record);
        
        // 清除资料详情缓存
        String detailCacheKey = "material:detail:" + id;
        redisTemplate.delete(detailCacheKey);
    }

    /**
     * 批量插入资料
     * @param materialList 资料列表
     */
    @AutoFill(value = OperationType.INSERT)
    public void insertMaterialBatch(List<Material> materialList) {
        materialMapper.insertBatch(materialList);
    }

    /**
     * 删除资料
     * @param id 资料ID
     */
    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            log.error("获取资料id失败: {}", id);
            throw new BusinessException("资料ID无效");
        }
        
        // 先获取资料详情，以便获取文件路径
        materialDetailVo material = materialMapper.getDetail(id);
        if (material == null) {
            throw new BusinessException("资料不存在");
        }
        
        // 删除物理文件
        String filePath = material.getUrl();
        if (filePath != null && !filePath.isEmpty()) {
            boolean deleted = FileUtils.deleteFile(filePath);
            if (!deleted) {
                log.warn("物理文件删除失败: {}", filePath);
                // 可以选择继续执行或抛出异常
                // throw new BusinessException("物理文件删除失败");
            } else {
                log.info("成功删除物理文件: {}", filePath);
            }
        }
        
        // 删除数据库记录
        materialMapper.deleteMaterial(id);
        
        // 清除资料详情缓存
        String detailCacheKey = "material:detail:" + id;
        redisTemplate.delete(detailCacheKey);
        
        // 清除资料列表缓存
        clearMaterialCache();
    }

    /**
     * 更新资料
     * @param materialUpdateDto 资料更新DTO
     */
    @Override
    @AutoFill(value = OperationType.UPDATE)
    @Transactional
    public void update(MaterialUpdateDto materialUpdateDto) {
        // 参数校验
        if (materialUpdateDto.getId() == null || materialUpdateDto.getId() <= 0) {
            log.error("获取资料id失败: {}", materialUpdateDto.getId());
            throw new BusinessException("资料ID无效");
        }
        if (materialUpdateDto.getDescription() == null) {
            throw new BusinessException("描述不能为空");
        }
        if (materialUpdateDto.getCategoryId() == null) {
            throw new BusinessException("分类不能为空");
        }
        if (materialUpdateDto.getIsPublic() == null) {
            throw new BusinessException("是否公开不能为空");
        }

        // 获取旧的资料信息
        materialDetailVo oldMaterial = materialMapper.getDetail(materialUpdateDto.getId());
        if (oldMaterial == null) {
            throw new BusinessException("资料不存在");
        }

        // 检查分类是否变更
        boolean categoryChanged = !oldMaterial.getCategoryId().equals(materialUpdateDto.getCategoryId());

        if (categoryChanged) {
            try {
                String newCategoryName = cateGoriesMapper.getCategoryNameById(materialUpdateDto.getCategoryId());
                if (newCategoryName == null) {
                    throw new BusinessException("新的资料分类不存在");
                }

                // 调用FileUtils移动文件并获取新路径
                String newUrl = FileUtils.moveMaterialFile(oldMaterial.getUrl(), newCategoryName);

                // 更新DTO中的URL为新路径
                materialUpdateDto.setUrl(newUrl);

            } catch (IOException e) {
                log.error("移动资料文件时发生IO异常", e);
                throw new BusinessException("移动文件失败: " + e.getMessage());
            }
        }

        // 更新数据库
        materialMapper.update(materialUpdateDto);

        // 清除缓存
        String detailCacheKey = "material:detail:" + materialUpdateDto.getId();
        redisTemplate.delete(detailCacheKey);
        
        clearMaterialCache();
    }

    /**
     * 清除资料相关缓存
     */
    private void clearMaterialCache() {
        // 清除资料列表缓存
        Set<String> listKeys = redisTemplate.keys("material:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
    }
}


