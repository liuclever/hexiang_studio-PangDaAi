package com.back_hexiang_studio.dv.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员工作台统计数据VO
 */
@Data
@Builder
public class AdminStatsVo {
    
    /**
     * 待审批申请总数
     */
    private Integer totalPendingApprovals;
    
    /**
     * 任务提交待审批数量
     */
    private Integer pendingTaskSubmissions;
    
    /**
     * 请假申请待审批数量
     */
    private Integer pendingLeaveRequests;
    
    /**
     * 今日已处理审批数量
     */
    private Integer todayProcessedApprovals;
} 