import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';

/**
 * 上传用户头像
 * @param userId 用户ID
 * @param file 头像文件
 * @returns Promise
 */
export function uploadAvatar(userId: number, file: File) {
  const formData = new FormData();
  formData.append('userId', userId.toString());
  formData.append('file', file);
  
  return request({
    url: '/admin/user/avatar/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 获取文件URL
 * @param filePath 文件相对路径
 * @returns 完整的文件URL
 */
export function getFileUrl(filePath: string | undefined) {
  if (!filePath) return '';
  if (filePath.startsWith('http')) return filePath;
  return `/api/file/${filePath}`;
}

/**
 * 更新用户头像
 * @param userId 用户ID
 * @param avatarPath 新头像的相对路径
 * @returns Promise
 */
export function updateAvatar(userId: number, avatarPath: string) {
  return request({
    url: '/admin/user/avatar',
    method: 'post',
    data: {
      userId,
      avatarPath
    }
  });
}

/**
 * 新增用户
 * @param data 包含用户数据和可选头像文件的 FormData
 * @returns Promise<ApiResponse>
 */
export const addUser = (data: FormData): Promise<ApiResponse> => {
    return request.post('/admin/user/add', data, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

/**
 * 更新用户
 * @param data 包含用户数据和可选头像文件的 FormData
 * @returns Promise<ApiResponse>
 */
export const updateUser = (data: FormData): Promise<ApiResponse> => {
    return request.post('/admin/user/update', data, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

// 你可以把其他的用户相关API也放在这里，例如：
// export function getUserList(params) { ... }
// export function deleteUser(userId) { ... } 