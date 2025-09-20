package com.back_hexiang_studio.utils;

import com.back_hexiang_studio.entity.SystemNotification;
import com.back_hexiang_studio.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通知工具类，用于在各个业务流程中自动创建通知
 */
@Component
public class NotificationUtils {

    private static NotificationService notificationService;

    @Autowired
    public void setNotificationService(NotificationService notificationService) {
        NotificationUtils.notificationService = notificationService;
    }

    /**
     * 创建公告通知
     * @param title 通知标题
     * @param content 通知内容
     * @param sourceId 公告ID
     * @param senderId 发送者ID
     * @param importance 重要程度：0普通，1重要，2紧急
     * @return 是否成功
     */
    public static boolean createAnnouncementNotification(String title, String content, Long sourceId, Long senderId, Integer importance) {
        SystemNotification notification = new SystemNotification();
        notification.setType("announcement");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setSourceId(sourceId);
        notification.setSenderId(senderId);
        notification.setTargetUserId(null); // 全局通知
        notification.setIsRead(0);
        notification.setImportance(importance);
        notification.setStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        return notificationService.createNotification(notification);
    }

    /**
     * 创建任务通知
     * @param title 通知标题
     * @param content 通知内容
     * @param sourceId 任务ID
     * @param senderId 发送者ID
     * @param targetUserId 目标用户ID
     * @param importance 重要程度：0普通，1重要，2紧急
     * @return 是否成功
     */
    public static boolean createTaskNotification(String title, String content, Long sourceId, Long senderId, Long targetUserId, Integer importance) {
        SystemNotification notification = new SystemNotification();
        notification.setType("task");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setSourceId(sourceId);
        notification.setSenderId(senderId);
        notification.setTargetUserId(targetUserId);
        notification.setIsRead(0);
        notification.setImportance(importance);
        notification.setStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        return notificationService.createNotification(notification);
    }

    /**
     * 创建课程通知
     * @param title 通知标题
     * @param content 通知内容
     * @param sourceId 课程ID
     * @param senderId 发送者ID
     * @param targetUserId 目标用户ID，null表示发送给所有选课学生
     * @param importance 重要程度：0普通，1重要，2紧急
     * @return 是否成功
     */
    public static boolean createCourseNotification(String title, String content, Long sourceId, Long senderId, Long targetUserId, Integer importance) {
        SystemNotification notification = new SystemNotification();
        notification.setType("course");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setSourceId(sourceId);
        notification.setSenderId(senderId);
        notification.setTargetUserId(targetUserId);
        notification.setIsRead(0);
        notification.setImportance(importance);
        notification.setStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        return notificationService.createNotification(notification);
    }

    /**
     * 创建系统通知
     * @param title 通知标题
     * @param content 通知内容
     * @param targetUserId 目标用户ID，null表示全局通知
     * @param importance 重要程度：0普通，1重要，2紧急
     * @return 是否成功
     */
    public static boolean createSystemNotification(String title, String content, Long targetUserId, Integer importance) {
        SystemNotification notification = new SystemNotification();
        notification.setType("system");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setSourceId(null);
        notification.setSenderId(null); // 系统发送
        notification.setTargetUserId(targetUserId);
        notification.setIsRead(0);
        notification.setImportance(importance);
        notification.setStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        return notificationService.createNotification(notification);
    }
} 