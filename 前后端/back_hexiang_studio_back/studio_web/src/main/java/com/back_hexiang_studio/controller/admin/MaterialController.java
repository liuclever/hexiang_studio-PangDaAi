package com.back_hexiang_studio.controller.admin;


import com.back_hexiang_studio.GlobalException.BusinessException;
import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryAddDto;

import com.back_hexiang_studio.dv.dto.meterial.CategoryUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.categoriesVo;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.CateGoriesService;
import com.back_hexiang_studio.service.MaterialService;
import com.back_hexiang_studio.utils.PathUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/admin/material")
@Data
@Slf4j
public class MaterialController {

    @Autowired
    private CateGoriesService cateGoriesService;

    @Autowired
    private MaterialService materialService;

    /**
     * 获取资料分类
     * 所有认证用户都可以查看分类
     */
    @GetMapping("/categories")
    public Result<List<categoriesVo>> getCategories() {
        log.info("获取资料分类");
        try{
           List<categoriesVo> cateGories=cateGoriesService.getCategories();
            return Result.success(cateGories);
        }catch (Exception e){
            log.error("获取资料分类失败：{}",e.getMessage());
            throw new RuntimeException("获取资料分类失败");
        }
    }

    /**
     * 添加资料分类
     * 需要管理权限：部长、副部长、主任、副主任、超级管理员
     * @param categoryAddDto
     * @return
     */
    @PostMapping("/category/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    public Result<String> addCategory(@RequestBody CategoryAddDto categoryAddDto) {
        log.info("添加资料分类：{}", categoryAddDto);
        try{
            cateGoriesService.addCategory(categoryAddDto);
            return Result.success("添加资料分类成功");
        }catch (Exception e){
            log.error("添加资料分类失败：{}",e.getMessage());
            return Result.error("权限不足，只有管理员可以添加资料分类");
        }
    }



    /**
     *  更新资料分类
     *  需要管理权限：部长、副部长、主任、副主任、超级管理员
     */
@PostMapping("/category/update")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    public Result updateCategory(@RequestBody CategoryUpdateDto categoryUpdateDto) {
        log.info("更新资料分类：{}",  categoryUpdateDto);
        try{
            cateGoriesService.updateCategory(categoryUpdateDto);
            return Result.success("更新资料分类成功");
        } catch (RuntimeException e) {
            log.error("更新资料分类失败：{}",e.getMessage());
            return Result.error("权限不足，只有管理员可以修改资料分类");
        }
    }



    /**
     *  删除资料分类
     *  需要管理权限：部长、副部长、主任、副主任、超级管理员
     */
    @PostMapping("/category/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    public Result deleteCategory(@RequestBody Map<String, Long> param) {
        Long id = param.get("id");
        log.info("删除资料分类id：{}",  id);
        try {
            cateGoriesService.deleteCategory(id);
            return Result.success("删除资料分类成功");
        } catch (Exception e) {
            log.error("删除资料分类失败：{}",e.getMessage());
            return Result.error("权限不足，只有管理员可以删除资料分类");
        }
    }









    /**
     * 获取资料列表
     * 所有认证用户都可以查看资料列表
     * @param pageMaterialDto
     * @return
     */
    @PostMapping("/list")
    public Result<PageResult> getList(@RequestBody PageMaterialDto pageMaterialDto) {
        log.info("获取资料列表：{}", pageMaterialDto);
        PageResult list =materialService.getList(pageMaterialDto);
        return Result.success(list);
    }



    /**
     * 获取资料详情
     * 所有认证用户都可以查看资料详情
     * @param id
     * @return
     */
    @GetMapping("/detail")
    public Result<materialDetailVo> detal(Long id) {
        log.info("获取资料详情：{}",id);
        try{
            materialDetailVo material =materialService.getDetail(id);
            return Result.success(material);
        }catch (Exception e){
            throw new RuntimeException("获取资料失败");
        }

    }

    /**
     * 上传资料
     * 需要管理权限：部长、副部长、主任、副主任、超级管理员
     * @param files 上传的文件列表
     * @param categoryId 分类ID
     * @param isPublic 是否公开
     * @param description 描述信息
     * @return 上传结果
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    @Transactional
    public Result uploadMaterial(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("isPublic") Integer isPublic,
            @RequestParam(value = "description", required = false) String description) {
        
        if (files == null || files.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        
        try {
            // 先验证所有文件类型
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    return Result.error("文件名不能为空");
                }
                
                // 使用PathUtils验证文件安全性
                if (!PathUtils.isFilenameExtensionSafe(originalFilename)) {
                    String extension = PathUtils.getFileExtension(originalFilename);
                    log.warn("尝试上传不安全的文件类型: {}", extension);
                    return Result.error("不允许上传" + extension + "类型的文件，该类型可能存在安全风险");
                }
            }
            
            materialService.uploadMaterial(files, categoryId, isPublic, description);
            return Result.success("上传资料成功");
        } catch (BusinessException e) {
            log.warn("上传资料业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传资料失败", e);
            return Result.error("权限不足，只有管理员可以上传资料");
        }
    }




    /**
     *  删除资料
     *  需要管理权限：部长、副部长、主任、副主任、超级管理员
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    public Result delete(@RequestBody Map<String, Long> param) {
        Long id = param.get("id");
        log.info("删除资料id：{}",  id);
        try{
            materialService.delete(id);
            return Result.success("删除资料成功");
        } catch (RuntimeException e) {
            log.error("删除资料失败：{}",e.getMessage());
            return Result.error("权限不足，只有管理员可以删除资料");
        }
    }


    /**
     * 更新资料
     * 需要管理权限：部长、副部长、主任、副主任、超级管理员
     * @param materialUpdateDto
     * @return
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('MATERIAL_MANAGE')")
    public Result update(@RequestBody MaterialUpdateDto materialUpdateDto) {
        log.info("更新资料：{}", materialUpdateDto);
        try {
        materialService.update(materialUpdateDto);
        return Result.success("更新资料成功");
        } catch (Exception e) {
            log.error("更新资料失败：{}",e.getMessage());
            return Result.error("权限不足，只有管理员可以修改资料");
        }
    }

}
