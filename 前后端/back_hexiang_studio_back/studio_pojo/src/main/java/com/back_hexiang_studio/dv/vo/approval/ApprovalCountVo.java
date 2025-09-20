package com.back_hexiang_studio.dv.vo.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批统计数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCountVo {
    
    /**
     * 数量
     */
    private Integer count;
    
    /**
     * 类型描述
     */
    private String type;
} 