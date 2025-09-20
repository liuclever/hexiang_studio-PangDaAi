package com.back_hexiang_studio.controller.wx;

import com.back_hexiang_studio.dv.dto.NoticeStatusUpdateDto;
import com.back_hexiang_studio.dv.dto.PageNoticeDto;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.NoticeService;
import com.back_hexiang_studio.context.UserContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 微信端公告控制器
 */
@Slf4j
@RestController
@RequestMapping("/wx/notice")
public class WxNoticeController {
    
    @Autowired
    private NoticeService noticeService;
    
    /**
     * 获取公告列表（分页）
     * @param pageNoticeDto 分页查询参数
     * @return 公告列表
     */
    @GetMapping("/list")
    public Result<PageResult> list(PageNoticeDto pageNoticeDto) {
        // 🔧 优化：频繁查询，降级为DEBUG，减少参数详情泄露
        log.debug("微信端获取公告列表开始");
        
        try {
            PageResult notices = noticeService.list(pageNoticeDto);
            
            // 🔧 优化：查询结果统计降级为DEBUG
            log.debug("微信端获取公告列表成功，总数：{}，当前页数据量：{}", 
                    notices.getTotal(), 
                    notices.getRecords() != null ? notices.getRecords().size() : 0);
            
            // 🔧 删除：调试用的数据示例打印，生产环境不需要
            
            return Result.success(notices);
        } catch (Exception e) {
            log.error("微信端获取公告列表失败", e);
            return Result.error("获取公告列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取公告详情
     * @param id 公告ID
     * @return 公告详情
     */
    @GetMapping("/detail")
    public Result getNoticeDetail(@RequestParam Long id) {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("微信端获取公告详情，ID：{}", id);
        return Result.success(noticeService.getNoticeDetail(id));
    }
    
    /**
     * 获取近一个月的系统公告（限制3条）
     * @return 近一个月的最新3条系统公告
     */
    @GetMapping("/recent")
    public Result getRecentNotices() {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("微信端获取近期公告");
        List<?> notices = noticeService.getRecentNotices();
        return Result.success(notices);
    }
    
    /**
     * 获取近一个月的活动类型公告
     * @return 近一个月的活动类型公告
     */
    @GetMapping("/recent-activities")
    public Result getRecentActivityNotices() {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("微信端获取近期活动公告");
        List<?> activities = noticeService.getRecentActivityNotices();
        return Result.success(activities);
    }
    
    /**
     * 获取近一个月的所有系统公告
     * @return 近一个月的所有系统公告
     */  
    @GetMapping("/all-recent")
    public Result getAllRecentNotices() {
        // 🔧 优化：频繁查询，降级为DEBUG
        log.debug("微信端获取所有近期公告");
        List<?> notices = noticeService.getAllRecentNotices();
        return Result.success(notices);
    }
    
    /**
     * 更新公告状态（公告管理功能）
     * @param noticeStatusUpdateDto 状态更新信息
     * @return 更新结果
     */
    @PostMapping("/update-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER') ")
    public Result updateNoticeStatus(@RequestBody NoticeStatusUpdateDto noticeStatusUpdateDto) {
        log.info("微信端更新公告状态，公告ID: {}, 新状态: {}", 
                noticeStatusUpdateDto.getNoticeId(), noticeStatusUpdateDto.getStatus());
        
        try {
            noticeService.updateStatus(noticeStatusUpdateDto);
            log.info("公告状态更新成功，公告ID: {}", noticeStatusUpdateDto.getNoticeId());
            return Result.success("公告状态更新成功");
        } catch (IllegalArgumentException e) {
            log.warn("公告状态更新参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("微信端更新公告状态失败", e);
            return Result.error("更新公告状态失败：" + e.getMessage());
        }
    }
}