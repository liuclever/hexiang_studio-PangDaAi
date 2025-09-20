import service from '@/utils/request';

// 定义常用地点的数据结构
export interface CommonLocation {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  description: string;
}

/**
 * 获取所有常用签到地点
 * @param category 可选的分类参数
 * @returns 常用地点列表
 */
export function getAllCommonLocations(category?: string) {
  return service({
    url: '/admin/locations',
    method: 'get',
    params: category ? { category } : {}
  })
}

/**
 * 根据ID获取常用签到地点
 * @param id 地点ID
 * @returns 常用地点详情
 */
export function getCommonLocationById(id: number) {
  return service({
    url: `/admin/locations/${id}`,
    method: 'get'
  })
}

/**
 * 创建常用签到地点
 * @param data 地点数据
 * @returns 创建结果
 */
export function createCommonLocation(data: any) {
  return service({
    url: '/admin/locations',
    method: 'post',
    data
  })
}

/**
 * 更新常用签到地点
 * @param id 地点ID
 * @param data 地点数据
 * @returns 更新结果
 */
export function updateCommonLocation(id: number, data: any) {
  return service({
    url: `/admin/locations/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除常用签到地点
 * @param id 地点ID
 * @returns 删除结果
 */
export function deleteCommonLocation(id: number) {
  return service({
    url: `/admin/locations/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除常用签到地点
 * @param ids 地点ID数组
 * @returns 删除结果
 */
export function batchDeleteCommonLocations(ids: number[]) {
  return service({
    url: '/admin/locations/batch',
    method: 'delete',
    data: { ids }
  })
} 