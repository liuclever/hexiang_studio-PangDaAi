package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.GlobalException.PermissionException;
import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.dv.vo.material.categoriesVo;
import com.back_hexiang_studio.mapper.CateGoriesMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;
import java.lang.reflect.Method;

/**
 * 微信端资料库控制器
 * 
 * @author System
 * @since 2024-12-19
 */
@Slf4j
@RestController
@RequestMapping("/wx/material")
public class WxMaterialController {

    @Autowired
    private MaterialService materialService;
    
    @Autowired
    private CateGoriesMapper cateGoriesMapper;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 获取资料分类列表
     * @return 分类列表
     */
    @GetMapping("/categories")
    public Result<List<categoriesVo>> getCategories() {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("微信端获取资料分类列表");
        try {
            List<categoriesVo> categories = cateGoriesMapper.getCategories();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("获取资料分类列表失败", e);
            return Result.error("获取分类列表失败");
        }
    }

    /**
     * 分页获取资料列表
     * @param pageMaterialDto 分页查询参数
     * @return 分页资料列表
     */
    @PostMapping("/list")
    public Result<PageResult> getMaterialList(@RequestBody PageMaterialDto pageMaterialDto) {
        // 🔧 优化：频繁查询，降级为DEBUG，减少参数详情泄露
        log.debug("微信端分页获取资料列表，页码: {}, 页面大小: {}", 
                pageMaterialDto.getPage(), pageMaterialDto.getPageSize());
        // 🔧 删除：参数详情日志，减少日志噪音
        
        try {
            // 设置默认分页参数
            if (pageMaterialDto.getPage() == null || pageMaterialDto.getPage() <= 0) {
                pageMaterialDto.setPage(1);
            }
            if (pageMaterialDto.getPageSize() == null || pageMaterialDto.getPageSize() <= 0) {
                pageMaterialDto.setPageSize(20);
            }
            
            // 微信端只显示公开资料，除非是管理员
            Long currentUserId = UserContextHolder.getCurrentId();
            Long roleId = Long.valueOf(userMapper.getRole(currentUserId));
            if (!roleId.equals(3L)) { // 非管理员只能看公开资料
                pageMaterialDto.setIsPublic(1);
            }
            
            PageResult pageResult = materialService.getList(pageMaterialDto);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取资料列表失败", e);
            return Result.error("获取资料列表失败");
        }
    }

    /**
     * 获取资料详情
     * @param id 资料ID
     * @return 资料详情
     */
    @GetMapping("/detail/{id}")
    public Result<materialDetailVo> getMaterialDetail(@PathVariable Long id) {
        // 🔧 优化：频繁访问，降级为DEBUG
        log.debug("微信端获取资料详情，ID：{}", id);
        try {
            materialDetailVo detail = materialService.getDetail(id);
            return Result.success(detail);
        } catch (PermissionException e) {
            log.warn("无权限访问资料，ID：{}，用户：{}", id, UserContextHolder.getCurrentId());
            return Result.error("无权限访问该资料");
        } catch (BusinessException e) {
            log.warn("获取资料详情失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取资料详情失败，ID：{}", id, e);
            return Result.error("获取资料详情失败");
        }
    }

    /**
     * 记录资料下载
     * @param id 资料ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/download/{id}")
    public Result<Void> recordDownload(@PathVariable Long id, HttpServletRequest request) {
        // 🔧 优化：操作记录，降级为DEBUG
        log.debug("记录资料下载，ID：{}", id);
        try {
            Long userId = UserContextHolder.getCurrentId();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            materialService.recordDownload(id, userId, ipAddress, userAgent);
            return Result.success();
        } catch (Exception e) {
            log.error("记录资料下载失败，ID：{}", id, e);
            return Result.error("记录下载失败");
        }
    }

    /**
     * 记录资料查看（可选功能）
     * @param id 资料ID
     * @return 操作结果
     */
    @PostMapping("/view/{id}")
    public Result<Void> recordView(@PathVariable Long id) {
        // 🔧 优化：频繁访问，降级为DEBUG
        log.debug("记录资料查看，ID：{}", id);
        try {
            // 这里可以添加查看记录逻辑，目前只做日志记录
            Long userId = UserContextHolder.getCurrentId();
            // 🔧 优化：减少用户ID泄露，降级为DEBUG
            log.debug("用户查看了资料 {}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("记录资料查看失败，ID：{}", id, e);
            return Result.success(); // 查看记录失败不影响功能
        }
    }

    /**
     * 获取相关资料推荐
     * @param categoryId 分类ID
     * @param fileType 文件类型
     * @param excludeId 排除的资料ID
     * @param page 页码
     * @param pageSize 页面大小
     * @return 相关资料列表
     */
    @GetMapping("/related")
    public Result<PageResult> getRelatedMaterials(
            @RequestParam Long categoryId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Long excludeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 🔧 优化：降级为DEBUG，减少参数详情泄露
        log.debug("获取相关资料，分类：{}, 类型：{}, 排除：{}", categoryId, fileType, excludeId);
        
        try {
            // 构建查询参数
            PageMaterialDto queryDto = new PageMaterialDto();
            queryDto.setCategoryId(categoryId);
            queryDto.setFileType(fileType);
            queryDto.setPage(page);
            queryDto.setPageSize(pageSize);
            queryDto.setIsPublic(1); // 只显示公开资料
            
            PageResult pageResult = materialService.getList(queryDto);
            
            // 从结果中排除指定ID的资料
            if (excludeId != null && pageResult.getRecords() != null) {
                List<?> originalRecords = pageResult.getRecords();
                List<Object> filteredRecords = originalRecords.stream()
                    .filter(record -> {
                        // 假设记录有getId方法，需要根据实际情况调整
                        try {
                            Method getIdMethod = record.getClass().getMethod("getId");
                            Object id = getIdMethod.invoke(record);
                            return !excludeId.equals(id);
                        } catch (Exception e) {
                            return true; // 出错时保留记录
                        }
                    })
                    .collect(Collectors.toList());
                
                // 🔧 优化：简化日志，降级为DEBUG
                log.debug("相关资料排除结果：原始{}个，排除后{}个", 
                        originalRecords.size(), filteredRecords.size());
                
                pageResult = new PageResult(pageResult.getTotal(), filteredRecords);
            }
            
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取相关资料失败", e);
            return Result.error("获取相关资料失败");
        }
    }

    /**
     * 删除资料（仅管理员或上传者）
     * @param id 资料ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        // 🔧 优化：管理操作，保持INFO级别但简化
        log.info("删除资料，ID：{}", id);
        try {
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                throw new PermissionException("用户未登录");
            }
            
            String roleIdStr = userMapper.getRole(currentUserId);
            if (roleIdStr == null) {
                throw new PermissionException("用户角色信息异常");
            }
            Long roleId = Long.valueOf(roleIdStr);
            
            // 获取资料详情检查权限
            materialDetailVo material = materialService.getDetail(id);
            
            // 检查权限：管理员或上传者
            Integer uploaderId = material.getUploaderId();
            boolean isAdmin = roleId.equals(3L);
            boolean isUploader = uploaderId != null && Long.valueOf(uploaderId).equals(currentUserId);
            
            // 🔧 优化：权限校验日志降级为DEBUG，减少用户信息泄露
            log.debug("删除权限校验 - 角色ID: {}, 是否管理员: {}, 是否上传者: {}", 
                     roleId, isAdmin, isUploader);
            
            if (!isAdmin && !isUploader) {
                throw new PermissionException("无权限删除该资料");
            }
            
            materialService.delete(id);
            return Result.success();
        } catch (PermissionException e) {
            log.warn("删除资料权限不足，ID：{}，用户：{}", id, UserContextHolder.getCurrentId());
            return Result.error("无权限删除该资料");
        } catch (BusinessException e) {
            log.warn("删除资料失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除资料失败，ID：{}", id, e);
            return Result.error("删除资料失败");
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 