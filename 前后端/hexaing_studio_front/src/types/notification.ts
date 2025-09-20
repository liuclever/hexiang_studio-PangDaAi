/**
 * 通知类型定义
 */

// 通知对象接口
export interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  sourceId?: number;
  senderId?: number;
  senderName?: string;
  isRead: number;
  readTime?: string;
  importance: number;
  createTime: string;
}

// 通知查询参数
export interface NotificationQuery {
  type?: string;
  page?: number;
  size?: number;
}

// 通知类型枚举
export const NotificationType = {
  ANNOUNCEMENT: 'announcement',
  TASK: 'task',
  COURSE: 'course',
  SYSTEM: 'system'
};

// 通知类型名称映射
export const NotificationTypeNames = {
  [NotificationType.ANNOUNCEMENT]: '公告',
  [NotificationType.TASK]: '任务',
  [NotificationType.COURSE]: '课程',
  [NotificationType.SYSTEM]: '系统'
}; 