package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.MaterialUpdateDto;
import com.back_hexiang_studio.dv.dto.meterial.PageMaterialDto;
import com.back_hexiang_studio.dv.vo.material.materialDetailVo;
import com.back_hexiang_studio.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料服务接口
 */
public interface MaterialService {
    /**
     * 获取资料列表
     * @param pageMaterialDto 分页查询DTO
     * @return 分页结果
     */
    PageResult getList(PageMaterialDto pageMaterialDto);

    /**
     * 获取资料详情
     * @param id 资料ID
     * @return 资料详情
     */
    materialDetailVo getDetail(Long id);

    /**
     * 上传资料
     * @param files 上传的文件列表
     * @param categoryId 分类ID
     * @param isPublic 是否公开
     * @param description 描述
     */
    void uploadMaterial(List<MultipartFile> files, Long categoryId, Integer isPublic, String description);

    /**
     * 记录下载次数
     * @param id 资料ID
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param deviceInfo 设备信息
     */
    void recordDownload(Long id, Long userId, String ipAddress, String deviceInfo);

    /**
     * 删除资料
     * @param id 资料ID
     */
    void delete(Long id);

    /**
     * 更新资料
     * @param materialUpdateDto 资料更新DTO
     */
    void update(MaterialUpdateDto materialUpdateDto);
}
