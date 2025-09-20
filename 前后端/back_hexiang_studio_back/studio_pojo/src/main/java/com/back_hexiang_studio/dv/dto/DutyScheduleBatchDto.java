package com.back_hexiang_studio.dv.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 值班安排批量操作DTO
 */
@Data
public class DutyScheduleBatchDto {
    
    /**
     * 新增的值班安排列表
     */
    private List<DutyScheduleDto> additions;
    
    /**
     * 要删除的值班安排ID列表
     * 前端可能传Long、Integer、String或混合类型，在getter中处理转换
     */
    private List<?> deletions;
    
    /**
     * 获取转换后的删除ID列表
     * @return 转换为Long类型的ID列表
     */
    public List<Long> getDeletions() {
        List<Long> ids = new ArrayList<>();
        
        if (deletions != null) {
            for (Object idObj : deletions) {
                if (idObj instanceof Integer) {
                    ids.add(((Integer) idObj).longValue());
                } else if (idObj instanceof Long) {
                    ids.add((Long) idObj);
                } else if (idObj instanceof String) {
                    try {
                        ids.add(Long.parseLong((String) idObj));
                    } catch (NumberFormatException e) {
                        // 忽略无法转换的值
                    }
                } else if (idObj instanceof Number) {
                    ids.add(((Number) idObj).longValue());
                }
            }
        }
        
        return ids;
    }
    
    /**
     * 设置删除ID列表
     * @param deletions 要删除的ID列表
     */
    public void setDeletions(List<?> deletions) {
        this.deletions = deletions;
    }
} 