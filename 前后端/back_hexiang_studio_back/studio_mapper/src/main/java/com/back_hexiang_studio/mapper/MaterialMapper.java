package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.dv.vo.material.materialVo;
import com.back_hexiang_studio.entity.Material;
import com.back_hexiang_studio.entity.MaterialDownloadRecord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资料数据访问接口
 */
@Mapper
public interface MaterialMapper {
    /**
     * 批量插入资料
     * @param materialList 资料列表
     */
    void insertBatch(List<Material> materialList);
    
    /**
     * 增加下载次数
     * @param id 资料ID
     */
    void incrementDownloadCount(Long id);
    
    /**
     * 更新资料信息
     * @param materialUpdateDto 资料更新DTO
     */
    void update(MaterialUpdateDto materialUpdateDto);
    
    /**
     * 删除资料
     * @param id 资料ID
     */
    void deleteMaterial(Long id);
    
    /**
     * 获取资料列表
     * @param pageMaterialDto 分页查询DTO
     * @return 资料列表
     */
    Page<materialVo> getList(PageMaterialDto pageMaterialDto);
    
    /**
     * 获取资料详情
     * @param id 资料ID
     * @return 资料详情
     */
    materialDetailVo getDetail(Long id);
    
    /**
     * 添加资料下载记录
     * @param record 下载记录
     */
    void addDownloadRecord(MaterialDownloadRecord record);

    /**
     * 获取资料总数
     * @return
     */
    @Select("SELECT COUNT(*) FROM material")
    int countMaterials();
    
    /**
     * 根据分类ID获取资料列表
     * @param categoryId 分类ID
     * @return 资料列表
     */
    List<materialDetailVo> getMaterialsByCategoryId(Long categoryId);
}