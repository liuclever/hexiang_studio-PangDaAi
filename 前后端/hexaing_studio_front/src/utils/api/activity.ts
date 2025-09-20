import request from '../request';

/**
 * 获取近一个月的活动类型公告
 * @returns 近一个月的活动类型公告
 */
export function getRecentActivityNotices() {
  return request({
    url: '/admin/notice/recent-activities',
    method: 'get'
  });
} 