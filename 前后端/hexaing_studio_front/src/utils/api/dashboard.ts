import request from '../request';

/**
 * 获取首页概览数据
 * @returns 包含学生总数、资料总数、课程总数等信息
 */
export function getDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  });
}

/**
 * 获取活动数据概览
 * @param type 数据类型：activity(活动考勤)、course(课程考勤)、duty(值班考勤)
 * @returns 对应类型的考勤统计数据
 */
export function getActivityData(type = 'activity') {
  return request({
    url: '/admin/dashboard/activity-data',
    method: 'get',
    params: { type }
  });
}

/**
 * 获取当前考勤周期数据
 * @returns 当前考勤周期的统计数据
 */
export function getCurrentPeriodData() {
  return request({
    url: '/admin/dashboard/current-period',
    method: 'get'
  });
}

/**
 * 获取工作室信息
 */
export function getStudioInfo() {
  return request({
    url: '/admin/studio/info',
    method: 'get'
  });
} 