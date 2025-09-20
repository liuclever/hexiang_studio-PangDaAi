import request from '../request';
import type { ApiResponse } from '../types';

/**
 * 资料分类接口
 */
export interface MaterialCategory {
  id: number;
  name: string;
  orderId: number;
}

/**
 * 资料列表项接口
 */
export interface Material {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  url: string;
  description: string;
  categoryId: number;
  name: string; // 分类名称
  uploadTime: string;
  uploaderId: number;
  uploader: string;
  downloadCount: number;
  isPublic: number;
  status: number;
}

/**
 * 资料详情接口
 */
export interface MaterialDetail {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  url: string;
  description: string;
  categoryId: number;
  category: string;
  uploadTime: string;
  uploaderId: number;
  uploader: string;
  downloadCount: number;
  isPublic: number;
  status: number;
}

/**
 * 分页查询参数接口
 */
export interface MaterialQueryParams {
  page: number;
  pageSize: number;
  name?: string;
  categoryId?: number;
  fileType?: string;
  fileTypes?: string[];
  startDate?: string;
  endDate?: string;
  isPublic?: number;
}

/**
 * 获取资料分类列表
 */
export function getMaterialCategories(): Promise<ApiResponse<MaterialCategory[]>> {
  return request({
    url: '/admin/material/categories',
    method: 'get'
  });
}

/**
 * 获取资料列表
 * @param params 查询参数
 */
export function getMaterialList(params: MaterialQueryParams): Promise<ApiResponse<{
  total: number;
  records: Material[];
  pages: number;
}>> {
  return request({
    url: '/admin/material/list',
    method: 'post',
    data: params
  });
}

/**
 * 获取资料详情
 * @param id 资料ID
 */
export function getMaterialDetail(id: number): Promise<ApiResponse<MaterialDetail>> {
  return request({
    url: '/admin/material/detail',
    method: 'get',
    params: { id }
  });
}

/**
 * 上传资料
 * @param data 表单数据
 */
export function uploadMaterial(data: FormData): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/upload',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data
  });
}

/**
 * 更新资料
 * @param data 资料数据
 */
export function updateMaterial(data: {
  id: number;
  description: string;
  categoryId: number;
  isPublic: number;
}): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/update',
    method: 'post',
    data
  });
}

/**
 * 删除资料
 * @param id 资料ID
 */
export function deleteMaterial(id: number): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/delete',
    method: 'post',
    data: { id }
  });
}

/**
 * 记录资料下载
 * @param id 资料ID
 */
export function recordMaterialDownload(id: number): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/download/record',
    method: 'post',
    data: { id }
  });
}

/**
 * 添加资料分类
 * @param data 分类数据
 */
export function addMaterialCategory(data: {
  name: string;
  orderId: number;
}): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/category/add',
    method: 'post',
    data
  });
}

/**
 * 更新资料分类
 * @param data 分类数据
 */
export function updateMaterialCategory(data: {
  id: number;
  name: string;
  orderId: number;
}): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/category/update',
    method: 'post',
    data
  });
}

/**
 * 删除资料分类
 * @param id 分类ID
 */
export function deleteMaterialCategory(id: number): Promise<ApiResponse<object>> {
  return request({
    url: '/admin/material/category/delete',
    method: 'post',
    data: { id }
  });
} 