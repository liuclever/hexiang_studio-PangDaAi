import request from '@/utils/request';

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return request({
    url: '/admin/notification/unread-count',
    method: 'get'
  });
}

/**
 * 获取通知列表
 * @param params 查询参数
 */
export function getNotificationList(params: {
  type?: string;
  page?: number;
  size?: number;
  readStatus?: number;
}) {
  return request({
    url: '/admin/notification/list',
    method: 'get',
    params: params // 直接传递所有参数
  });
}

/**
 * 标记通知为已读
 * @param id 通知ID
 */
export function markAsRead(id: number) {
  return request({
    url: `/admin/notification/read/${id}`,
    method: 'put'
  });
}

/**
 * 标记所有通知为已读
 * @param type 通知类型（可选）
 */
export function markAllAsRead(type?: string) {
  return request({
    url: '/admin/notification/read-all',
    method: 'put',
    params: { type }
  });
}

/**
 * 获取通知详情
 * @param id 通知ID
 */
export function getNotificationDetail(id: number) {
  return request({
    url: `/admin/notification/detail/${id}`,
    method: 'get'
  });
} 