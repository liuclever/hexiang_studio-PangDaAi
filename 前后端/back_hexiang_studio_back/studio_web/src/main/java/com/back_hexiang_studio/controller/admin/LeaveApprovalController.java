package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.LeaveApprovalService;
import com.back_hexiang_studio.dv.vo.LeaveRequestVO;
import com.back_hexiang_studio.dv.dto.LeaveRequestQueryDTO;
import com.back_hexiang_studio.dv.dto.RejectDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 请假审批控制器
 * 权限：超级管理员或只有副主任、主任可以访问（属于考勤管理）
 */
@RestController
@RequestMapping("/admin/approval/leave")
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TEACHER') or hasAuthority('ATTENDANCE_MANAGE')")
public class LeaveApprovalController {

    @Autowired
    private LeaveApprovalService leaveApprovalService;

    /**
     * 获取请假申请列表 (分页+条件查询)
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<PageResult> getLeaveRequests(LeaveRequestQueryDTO queryDTO) {
        // 🔧 优化：删除冗余的调试信息，仅保留必要的业务日志
        log.debug("查询请假申请列表，状态过滤: {}", queryDTO.getStatus());
        
        PageResult pageResult = leaveApprovalService.getLeaveRequests(queryDTO);
        
        // 🔧 优化：简化返回结果日志，删除详细数据内容
        log.debug("查询完成，总数: {}", pageResult.getTotal());
        
        return Result.success(pageResult);
    }

    /**
     * 批准请假申请
     * @param requestId 申请ID
     * @return 操作结果
     */
    @PostMapping("/{requestId}/approve")
    public Result approveRequest(@PathVariable Long requestId) {
        log.info("批准请假申请，ID: {}", requestId);
        // 假设当前用户ID从UserContextHolder获取
        Long currentUserId = com.back_hexiang_studio.context.UserContextHolder.getCurrentId();
        leaveApprovalService.approveRequest(requestId, currentUserId);
        return Result.success("批准成功");
    }

    /**
     * 驳回请假申请
     * @param requestId 申请ID
     * @param rejectDTO 包含驳回理由的数据
     * @return 操作结果
     */
    @PostMapping("/{requestId}/reject")
    public Result rejectRequest(@PathVariable Long requestId, @RequestBody RejectDTO rejectDTO) {
        log.info("驳回请假申请，ID: {}, 理由: {}", requestId, rejectDTO.getRemark());
        // 假设当前用户ID从UserContextHolder获取
        Long currentUserId = com.back_hexiang_studio.context.UserContextHolder.getCurrentId();
        leaveApprovalService.rejectRequest(requestId, rejectDTO.getRemark(), currentUserId);
        return Result.success("驳回成功");
    }

    /**
     * 获取请假申请详情
     * @param requestId 申请ID
     * @return 详情数据
     */
    @GetMapping("/{requestId}")
    public Result<LeaveRequestVO> getLeaveRequestDetail(@PathVariable Long requestId) {
        log.info("查询请假申请详情, ID: {}", requestId);
        LeaveRequestVO detail = leaveApprovalService.getLeaveRequestDetail(requestId);
        return Result.success(detail);
    }
}