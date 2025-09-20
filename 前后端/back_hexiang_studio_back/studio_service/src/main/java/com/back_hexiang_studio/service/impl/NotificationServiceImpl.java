package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.SystemNotification;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.mapper.NotificationMapper;
import com.back_hexiang_studio.mapper.UserMapper;
import com.back_hexiang_studio.result.PageResult;
import com.back_hexiang_studio.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

/**
 * 通知服务实现类
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取通知类型名称
     */
    private static final Map<String, String> TYPE_NAMES = new HashMap<String, String>() {{
        put("announcement", "公告");
        put("task", "任务");
        put("course", "课程");
        put("system", "系统");
    }};
    
    @Override
    public PageResult getNotificationList(Long userId, String type, int page, int size, Integer readStatus) {
        PageHelper.startPage(page, size);
        List<SystemNotification> notifications = notificationMapper.selectUserNotifications(userId, type, readStatus);
        Page<SystemNotification> p = (Page<SystemNotification>) notifications;
        return new PageResult(p.getTotal(), p.getResult());
    }
    
    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadNotifications(userId);
    }
    
    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        return notificationMapper.markAsRead(notificationId, userId) > 0;
    }
    
    @Override
    @Transactional
    public boolean markAllAsRead(Long userId, String type) {
        return notificationMapper.markAllAsRead(userId, type) > 0;
    }
    
    @Override
    @Transactional
    public boolean createNotification(SystemNotification notification) {
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        if (notification.getStatus() == null) {
            notification.setStatus(1);
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        if (notification.getImportance() == null) {
            notification.setImportance(0);
        }
        
        return notificationMapper.insert(notification) > 0;
    }
    
    @Override
    public SystemNotification getNotificationById(Long id) {
        return notificationMapper.selectById(id);
    }
} 