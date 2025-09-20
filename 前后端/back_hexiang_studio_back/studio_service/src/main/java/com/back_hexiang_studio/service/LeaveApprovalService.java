package com.back_hexiang_studio.service;

import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.dv.dto.LeaveRequestQueryDTO;
import com.back_hexiang_studio.dv.dto.LeaveRequestCreateDTO;
import com.back_hexiang_studio.dv.vo.LeaveRequestVO;

import java.util.List;

/**
 * 请假审批服务接口
 */
public interface LeaveApprovalService {

    /**
     * 分页查询请假申请列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult getLeaveRequests(LeaveRequestQueryDTO queryDTO);

    /**
     * 批准请假申请
     * @param requestId 请假申请ID
     * @param approverId 审批人ID
     */
    void approveRequest(Long requestId, Long approverId);

    /**
     * 驳回请假申请
     * @param requestId 请假申请ID
     * @param remark 驳回理由
     * @param approverId 审批人ID
     */
    void rejectRequest(Long requestId, String remark, Long approverId);

    /**
     * 获取单个请假申请详情
     * @param requestId 申请ID
     * @return 详情视图对象
     */
    LeaveRequestVO getLeaveRequestDetail(Long requestId);

    /**
     * 获取待审批的请假申请数量
     * @return 待审批数量
     */
    int getPendingCount();

    /**
     * 获取今日已处理的请假申请数量
     * @return 今日已处理数量
     */
    int getTodayProcessedCount();

    /**
     * 获取已审批的请假申请记录
     * @param days 查询天数 (1-今天, 3-三天内, 30-一个月内)
     * @return 已审批记录列表
     */
    List<com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo> getProcessedLeaveRequests(Integer days);



    // ===================== 学生端功能 =====================
    
    /**
     * 创建请假申请
     * @param createDTO 请假申请数据
     * @return 请假申请ID
     */
    Long createLeaveRequest(LeaveRequestCreateDTO createDTO);

    /**
     * 撤销请假申请（只能撤销待审批状态的申请）
     * @param requestId 请假申请ID
     * @param userId 用户ID
     * @return 撤销是否成功
     */
    boolean cancelLeaveRequest(Long requestId, Long userId);

    /**
     * 根据用户ID获取学生ID
     * @param userId 用户ID
     * @return 学生ID
     */
    Long getStudentIdByUserId(Long userId);
}
