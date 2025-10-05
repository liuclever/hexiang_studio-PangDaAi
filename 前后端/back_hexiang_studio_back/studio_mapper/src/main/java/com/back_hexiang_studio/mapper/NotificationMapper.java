package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.SystemNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    
    /**
     * 分页获取用户通知列表 (配合 PageHelper 使用)
     * @param userId 用户ID
     * @param type 通知类型，可为null
     * @param isRead 是否已读 (0: 未读, 1: 已读)，可为null
     * @return 通知列表
     */
    List<SystemNotification> selectUserNotifications(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Integer isRead);
    
    /**
     * 获取用户通知总数
     * @param userId 用户ID
     * @param type 通知类型，可为null
     * @param isRead 是否已读，可为null
     * @return 通知总数
     */
    int countUserNotifications(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Integer isRead);
    
    /**
     * 获取用户未读通知数
     * @param userId 用户ID
     * @return 未读通知数
     */
    int countUnreadNotifications(@Param("userId") Long userId);
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);
    
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @param type 通知类型，可为null
     * @return 影响行数
     */
    int markAllAsRead(@Param("userId") Long userId, @Param("type") String type);
    
    /**
     * 添加通知
     * @param notification 通知对象
     * @return 影响行数
     */
    int insert(SystemNotification notification);
    
    /**
     * 获取通知详情
     * @param id 通知ID
     * @return 通知对象
     */
    SystemNotification selectById(@Param("id") Long id);
    
    /**
     * 批量删除用户的通知
     * @param userId 用户ID
     * @param notificationIds 通知ID列表
     * @return 影响行数
     */
    int deleteUserNotifications(@Param("userId") Long userId, @Param("notificationIds") List<Long> notificationIds);
} 