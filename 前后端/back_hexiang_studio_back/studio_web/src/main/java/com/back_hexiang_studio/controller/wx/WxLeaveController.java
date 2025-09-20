package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.LeaveApprovalService;
import com.back_hexiang_studio.dv.vo.LeaveRequestVO;
import com.back_hexiang_studio.dv.dto.LeaveRequestQueryDTO;
import com.back_hexiang_studio.dv.dto.LeaveRequestCreateDTO;
import com.back_hexiang_studio.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;

/**
 * 微信端请假控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/leave")
public class WxLeaveController {

    @Autowired
    private LeaveApprovalService leaveApprovalService;

    /**
     * 学生提交请假申请
     * @param createDTO 请假申请数据
     * @return 操作结果
     */
    @PostMapping("/apply")
    public Result applyLeave(@RequestBody LeaveRequestCreateDTO createDTO) {
        log.info("学生提交请假申请: {}", createDTO);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 设置申请人
            createDTO.setApplicantId(currentUserId);
            
            // 提交请假申请
            Long requestId = leaveApprovalService.createLeaveRequest(createDTO);
            return Result.success(requestId);
        } catch (Exception e) {
            log.error("提交请假申请失败: {}", e.getMessage());
            return Result.error("提交请假申请失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的请假申请列表
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    @GetMapping("/my-requests")
    public Result<PageResult> getMyLeaveRequests(LeaveRequestQueryDTO queryDTO) {
        log.info("获取我的请假申请列表: {}", queryDTO);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 只查询当前用户的申请
            queryDTO.setApplicantId(currentUserId);
            
            PageResult pageResult = leaveApprovalService.getLeaveRequests(queryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取请假申请列表失败: {}", e.getMessage());
            return Result.error("获取请假申请列表失败");
        }
    }

    /**
     * 获取请假申请详情
     * @param requestId 申请ID
     * @return 详情数据
     */
    @GetMapping("/{requestId}")
    public Result<LeaveRequestVO> getLeaveRequestDetail(@PathVariable Long requestId) {
        log.info("查询请假申请详情, ID: {}", requestId);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            LeaveRequestVO detail = leaveApprovalService.getLeaveRequestDetail(requestId);
            
            // 权限检查：只能查看自己的申请
            Long currentStudentId = leaveApprovalService.getStudentIdByUserId(currentUserId);
            if (currentStudentId == null || !currentStudentId.equals(detail.getStudentId())) {
                // TODO: 添加管理员权限检查
                return Result.error("无权限查看此申请");
            }
            
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取请假申请详情失败: {}", e.getMessage());
            return Result.error("获取请假申请详情失败");
        }
    }

    /**
     * 撤销请假申请（只能撤销未审批的申请）
     * @param requestId 申请ID
     * @return 操作结果
     */
    @PostMapping("/{requestId}/cancel")
    public Result cancelLeaveRequest(@PathVariable Long requestId) {
        log.info("撤销请假申请: {}", requestId);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 撤销申请
            boolean success = leaveApprovalService.cancelLeaveRequest(requestId, currentUserId);
            if (success) {
                return Result.success("撤销申请成功");
            } else {
                return Result.error("撤销申请失败");
            }
        } catch (Exception e) {
            log.error("撤销请假申请失败: {}", e.getMessage());
            return Result.error("撤销申请失败: " + e.getMessage());
        }
    }

    /**
     * 上传请假相关文件
     * @param files 文件列表
     * @return 文件URL列表
     */
    @PostMapping("/upload-files")
    public Result<List<String>> uploadLeaveFiles(@RequestParam("files") MultipartFile[] files) {
        log.info("上传请假文件，文件数量: {}", files.length);
        try {
            // 获取当前登录用户
            Long currentUserId = UserContextHolder.getCurrentId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            if (files == null || files.length == 0) {
                return Result.error("没有文件可上传");
            }

            List<String> fileUrls = new ArrayList<>();
            
            // 处理每个上传的文件
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("跳过空文件: {}", file.getOriginalFilename());
                    continue;
                }
                
                try {
                    // TODO: 实现具体的文件上传逻辑
                    // 这里应该将文件保存到服务器或云存储，并返回访问URL
                    String fileName = file.getOriginalFilename();
                    String fileUrl = "temp://uploaded/" + fileName; // 临时URL格式
                    
                    fileUrls.add(fileUrl);
                    log.info("文件上传成功: {} -> {}", fileName, fileUrl);
                    
                } catch (Exception e) {
                    log.error("文件上传失败: {}, 错误: {}", file.getOriginalFilename(), e.getMessage());
                    // 继续处理其他文件
                }
            }
            
            return Result.success(fileUrls);
        } catch (Exception e) {
            log.error("上传请假文件失败: {}", e.getMessage());
            return Result.error("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取请假类型列表
     * @return 请假类型列表
     */
    @GetMapping("/leave-types")
    public Result getLeaveTypes() {
        log.info("获取请假类型列表");
        try {
            // TODO: 从数据库或配置中获取请假类型
            // 临时返回固定类型
            String[] leaveTypes = {"病假", "事假", "公假", "年假", "其他"};
            return Result.success(leaveTypes);
        } catch (Exception e) {
            log.error("获取请假类型失败: {}", e.getMessage());
            return Result.error("获取请假类型失败");
        }
    }
} 