package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.entity.SystemNotification;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.NotificationService;
import com.back_hexiang_studio.utils.NotificationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/admin/notification")
@Slf4j
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result getUnreadCount() {
        Long userId = UserContextHolder.getCurrentId();
        int count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
    
    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result getNotificationList(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer readStatus) { // 添加 readStatus 参数
        Long userId = UserContextHolder.getCurrentId();
        // 将 readStatus 传递给 service 层
        PageResult pageResult = notificationService.getNotificationList(userId, type, page, size, readStatus);
        return Result.success(pageResult);
    }
    
    /**
     * 标记通知为已读
     */
    @PutMapping("/read/{id}")
    public Result markAsRead(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentId();
        boolean success = notificationService.markAsRead(id, userId);
        if (success) {
            return Result.success();
        } else {
            return Result.error("标记已读失败");
        }
    }
    
    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public Result markAllAsRead(
            @RequestParam(required = false) String type) {
        Long userId = UserContextHolder.getCurrentId();
        boolean success = notificationService.markAllAsRead(userId, type);
        if (success) {
            return Result.success();
        } else {
            return Result.error("标记全部已读失败");
        }
    }
    
    /**
     * 获取通知详情
     */
    @GetMapping("/detail/{id}")
    public Result getNotificationDetail(@PathVariable Long id) {
        SystemNotification notification = notificationService.getNotificationById(id);
        if (notification != null) {
            // 自动标记为已读
            Long userId = UserContextHolder.getCurrentId();
            notificationService.markAsRead(id, userId);
            return Result.success(notification);
        } else {
            return Result.error("通知不存在");
        }
    }
    
    /**
     * 批量删除通知
     */
    @DeleteMapping("/delete")
    public Result deleteNotifications(@RequestBody List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return Result.error("请选择要删除的通知");
        }
        
        Long userId = UserContextHolder.getCurrentId();
        boolean success = notificationService.deleteNotifications(userId, notificationIds);
        if (success) {
            log.info("用户 {} 删除了 {} 个通知", userId, notificationIds.size());
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
} 