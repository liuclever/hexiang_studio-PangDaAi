package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.PageDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryAddDto;
import com.back_hexiang_studio.dv.dto.meterial.CategoryUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.categoriesVo;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.dv.vo.material.materialVo;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CateGoriesMapper {
    //获取资料分类
    List<categoriesVo> getCategories();

    //添加分类
    void addCategory(CategoryAddDto categoryAddDto);

    //判断分类名称是否存在
    Integer countByName(String name);

    //获取分类列表
    void updateCategory(CategoryUpdateDto categoryUpdateDto);

    //删除分类
    void deleteCategory(Long id);

    //删除分类中的资料
    void deleteCategoryBymaterial(Long id);
    //根据id获取分类名称
    String getCategoryNameById(Long categoryId);
}
