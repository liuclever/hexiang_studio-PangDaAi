package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.dto.NotificationQueryDto;
import com.back_hexiang_studio.entity.SystemNotification;
import com.back_hexiang_studio.result.PageResult;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    /**
     * 获取用户通知列表
     * @param userId 用户ID
     * @param type 通知类型，可为null
     * @param page 页码
     * @param size 每页大小
     * @param readStatus 阅读状态，可为null
     * @return 分页结果
     */
    PageResult getNotificationList(Long userId, String type, int page, int size, Integer readStatus);
    
    /**
     * 获取用户未读通知数
     * @param userId 用户ID
     * @return 未读通知数
     */
    int getUnreadCount(Long userId);
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long notificationId, Long userId);
    
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @param type 通知类型，可为null
     * @return 是否成功
     */
    boolean markAllAsRead(Long userId, String type);
    
    /**
     * 创建通知
     * @param notification 通知对象
     * @return 是否成功
     */
    boolean createNotification(SystemNotification notification);
    
    /**
     * 获取通知详情
     * @param id 通知ID
     * @return 通知对象
     */
    SystemNotification getNotificationById(Long id);
} 