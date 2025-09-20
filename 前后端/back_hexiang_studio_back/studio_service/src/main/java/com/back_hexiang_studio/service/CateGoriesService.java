package com.back_hexiang_studio.service;


import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryAddDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.MaterialUploadDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.categoriesVo;

import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.result.PageResult;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CateGoriesService {

    /**
          * 获取资料所有分类
     * @return
     */
    List<categoriesVo> getCategories();

    /**
     * 添加分类
     * @param categoryAddDto
     */
    void addCategory(CategoryAddDto categoryAddDto);



    /**
     * 更新资料分类
     * @param categoryUpdateDto
     */
    void updateCategory(CategoryUpdateDto categoryUpdateDto);

    /**
     * 删除资料分类
     * @param id
     */
    void deleteCategory(Long id);


}
