import request from '@/utils/request';

/**
 * 获取工作室信息
 */
export function getStudioInfo() {
  return request({
    url: '/admin/studio/info',
    method: 'get'
  });
}

/**
 * 更新工作室信息
 * @param data 工作室信息
 */
export function updateStudioInfo(data: {
  name: string;
  logo?: string;
  description?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  website?: string;
  foundedYear?: number;
  memberCount?: number;
  achievementCount?: number;
  projectCount?: number;
  [key: string]: any; // 允许其他字段
}) {
  return request({
    url: '/admin/studio/update',
    method: 'post',
    data
  });
} 