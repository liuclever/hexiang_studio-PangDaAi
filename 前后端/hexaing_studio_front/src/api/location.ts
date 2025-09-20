import request from '@/utils/request';

export interface CommonLocation {
  id?: number;
  name: string;
  lat: number;
  lng: number;
  description?: string;
}

/**
 * 获取所有常用地点
 */
export function getAllLocations() {
  return request.get('/admin/locations');
}

/**
 * 根据ID获取常用地点
 */
export function getLocationById(id: number) {
  return request.get(`/admin/locations/${id}`);
}

/**
 * 创建常用地点
 */
export function createLocation(data: CommonLocation) {
  return request.post('/admin/locations', data);
}

/**
 * 更新常用地点
 */
export function updateLocation(id: number, data: CommonLocation) {
  return request.put(`/admin/locations/${id}`, data);
}

/**
 * 删除常用地点
 */
export function deleteLocation(id: number) {
  return request.delete(`/admin/locations/${id}`);
} 