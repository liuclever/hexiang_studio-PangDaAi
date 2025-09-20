package com.back_hexiang_studio.dv.dto.meterial;

import lombok.Data;
import java.util.List;

/**
 * 资料分页查询DTO
 */
@Data
public class PageMaterialDto {
    /**
     * 页码，默认第1页
     */
    private Integer page = 1;
    
    /**
     * 每页记录数，默认10条
     */
    private Integer pageSize = 10;
    
    /**
     * 关键词搜索（文件名或描述）
     */
    private String name;
    
    /**
     * 搜索关键词（别名，与name字段功能相同）
     */
    private String keyword;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件类型列表（用于多选筛选）
     */
    private List<String> fileTypes;
    
    /**
     * 状态：1-正常，0-已删除
     */
    private String status;
    
    /**
     * 开始日期
     */
    private String startDate;
    
    /**
     * 结束日期
     */
    private String endDate;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 是否公开：1-公开，0-不公开
     */
    private Integer isPublic;

    /**
     * 排序字段（如：upload_time, file_size, file_name等）
     */
    private String orderBy;
    
    /**
     * 排序方向：ASC-升序，DESC-降序，默认DESC
     */
    private String orderDirection = "DESC";

}
