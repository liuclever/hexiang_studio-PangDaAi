package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.dv.vo.approval.ApprovalCountVo;
import com.back_hexiang_studio.dv.vo.approval.ApprovalRecordVo;
import com.back_hexiang_studio.dv.vo.AdminStatsVo;
import com.back_hexiang_studio.service.TaskSubmissionService;
import com.back_hexiang_studio.service.LeaveApprovalService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 微信端审批控制器
 * 处理审批中心相关的统计和查询功能
 */
@Slf4j
@RestController
@RequestMapping("/wx/approval")
public class WxApprovalController {

    @Autowired
    private TaskSubmissionService taskSubmissionService;
    
    @Autowired
    private LeaveApprovalService leaveApprovalService;

    /**
     * 获取管理员工作台统计数据 (统一接口)
     * @return 管理员工作台统计数据
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    public Result<AdminStatsVo> getAdminStats() {
        log.info("获取管理员工作台统计数据");
        try {
            // 获取任务提交待审批数量
            int pendingTaskSubmissions = taskSubmissionService.getPendingSubmissionCount();
            
            // 获取请假申请待审批数量
            int pendingLeaveRequests = leaveApprovalService.getPendingCount();
            
            // 计算总的待审批数量
            int totalPendingApprovals = pendingTaskSubmissions + pendingLeaveRequests;
            
            // 获取今日已处理数量
            int taskProcessedToday = taskSubmissionService.getTodayProcessedCount();
            int leaveProcessedToday = leaveApprovalService.getTodayProcessedCount();
            int todayProcessedApprovals = taskProcessedToday + leaveProcessedToday;
            
            AdminStatsVo stats = AdminStatsVo.builder()
                    .totalPendingApprovals(totalPendingApprovals)
                    .pendingTaskSubmissions(pendingTaskSubmissions)
                    .pendingLeaveRequests(pendingLeaveRequests)
                    .todayProcessedApprovals(todayProcessedApprovals)
                    .build();
            
            log.info("管理员工作台统计 - 任务待审批: {}, 请假待审批: {}, 今日已处理: {}", 
                    pendingTaskSubmissions, pendingLeaveRequests, todayProcessedApprovals);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取管理员工作台统计数据失败", e);
            return Result.error("获取统计数据失败");
        }
    }

    /**
     * 获取待审批请假申请数量
     * @return 待审批请假申请数量
     */
    @GetMapping("/leave/pending/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TEACHER') or hasAuthority('ROLE_MANAGER')")
    public Result<ApprovalCountVo> getPendingLeaveCount() {
        // 🔧 优化：频繁查询的统计接口，降级为DEBUG
        log.debug("获取待审批请假申请数量");
        try {
            // 获取待审批请假申请数量
            int count = leaveApprovalService.getPendingCount();
            
            ApprovalCountVo result = ApprovalCountVo.builder()
                    .count(count)
                    .type("pending_leave_request")
                    .build();
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待审批请假申请数量失败", e);
            return Result.error("获取待审批请假申请数量失败");
        }
    }

    /**
     * 获取今日已处理审批数量
     * @return 今日已处理审批数量
     */
    @GetMapping("/today/processed/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TEACHER') or hasAuthority('ROLE_MANAGER')")
    public Result<ApprovalCountVo> getTodayProcessedCount() {
        // 🔧 优化：频繁查询的统计接口，降级为DEBUG
        log.debug("获取今日已处理审批数量");
        try {
            // 获取今日已处理的任务提交数量
            int taskCount = taskSubmissionService.getTodayProcessedCount();
            
            // 获取今日已处理的请假申请数量
            int leaveCount = leaveApprovalService.getTodayProcessedCount();
            
            int totalCount = taskCount + leaveCount;
            
            ApprovalCountVo result = ApprovalCountVo.builder()
                    .count(totalCount)
                    .type("today_processed_approval")
                    .build();
            
            // 🔧 优化：详细统计信息降级为DEBUG
            log.debug("今日已处理审批统计 - 任务提交: {}, 请假申请: {}, 总计: {}", taskCount, leaveCount, totalCount);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取今日已处理审批数量失败", e);
            return Result.error("获取今日已处理审批数量失败");
        }
    }

    /**
     * 获取已审批记录列表
     * @param days 查询天数 (1-今天, 3-三天内, 30-一个月内)
     * @return 已审批记录列表
     */
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TEACHER') or hasAuthority('ROLE_MANAGER')")
    public Result<List<ApprovalRecordVo>> getApprovalRecords(@RequestParam(defaultValue = "3") Integer days) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("获取已审批记录列表，查询天数: {}", days);
        try {
            // 获取任务提交已审批记录
            List<ApprovalRecordVo> taskRecords = taskSubmissionService.getProcessedTaskSubmissions(days);
            
            // 获取请假申请已审批记录
            List<ApprovalRecordVo> leaveRecords = leaveApprovalService.getProcessedLeaveRequests(days);
            
            // 合并并按审批时间排序
            List<ApprovalRecordVo> allRecords = new ArrayList<>();
            allRecords.addAll(taskRecords);
            allRecords.addAll(leaveRecords);
            
            // 按审批时间倒序排序
            allRecords.sort(Comparator.comparing(ApprovalRecordVo::getReviewTime).reversed());
            
            // 🔧 优化：统计信息降级为DEBUG
            log.debug("查询到已审批记录 - 任务提交: {}条, 请假申请: {}条, 总计: {}条", 
                    taskRecords.size(), leaveRecords.size(), allRecords.size());
            
            return Result.success(allRecords);
        } catch (Exception e) {
            log.error("获取已审批记录列表失败", e);
            return Result.error("获取已审批记录失败");
        }
    }
} 