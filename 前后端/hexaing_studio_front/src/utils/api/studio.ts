import request from '../request';

/**
 * 获取工作室信息
 * @returns 工作室信息
 */
export const getStudioInfo = () => {
  return request({
    url: '/admin/studio/info',
    method: 'get'
  });
};

/**
 * 更新工作室信息
 * @param data 工作室信息数据
 * @returns 更新结果
 */
export const updateStudioInfo = (data: any) => {
  return request({
    url: '/admin/studio/update',
    method: 'post',
    data
  });
}; 