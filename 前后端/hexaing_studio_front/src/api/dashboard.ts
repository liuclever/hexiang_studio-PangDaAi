import request from '@/utils/request';

/**
 * 获取当前用户的紧急待办任务列表
 * @returns 包含任务信息的列表
 */
export const getMyTasks = () => {
  return request({
    url: '/admin/dashboard/my-tasks',
    method: 'get'
  });
}; 