import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';

/**
 * 获取指定用户的荣誉列表
 * @param userId 用户ID
 */
export const getHonorsByUserId = (userId: number): Promise<ApiResponse> => {
    return request.get(`/admin/achievement/honors?userId=${userId}`);
};

/**
 * 新增荣誉
 * @param data 包含荣誉数据和可选文件的 FormData
 */
export const addHonor = (data: FormData): Promise<ApiResponse> => {
    console.log('API调用: addHonor', data);
    // 打印FormData内容，便于调试
    console.log('FormData内容:');
    for (const pair of data.entries()) {
        console.log(pair[0], pair[1]);
    }
    return request.post('/admin/achievement/honor/add', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

/**
 * 更新荣誉
 * @param data 包含荣誉数据和可选文件的 FormData
 */
export const updateHonor = (data: FormData): Promise<ApiResponse> => {
    console.log('API调用: updateHonor', data);
    // 打印FormData内容，便于调试
    console.log('FormData内容:');
    for (const pair of data.entries()) {
        console.log(pair[0], pair[1]);
    }
    return request.post('/admin/achievement/honor/update', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

/**
 * 删除荣誉
 * @param honorId 荣誉ID
 */
export const deleteHonor = (honorId: number): Promise<ApiResponse> => {
    return request.delete(`/admin/achievement/honor/delete/${honorId}`);
};


/**
 * 获取指定用户的证书列表
 * @param userId 用户ID
 */
export const getCertificatesByUserId = (userId: number): Promise<ApiResponse> => {
    return request.get(`/admin/achievement/certificates?userId=${userId}`);
};

/**
 * 新增证书
 * @param data 包含证书数据和可选文件的 FormData
 */
export const addCertificate = (data: FormData): Promise<ApiResponse> => {
    return request.post('/admin/achievement/certificate/add', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

/**
 * 更新证书
 * @param data 包含证书数据和可选文件的 FormData
 */
export const updateCertificate = (data: FormData): Promise<ApiResponse> => {
    return request.post('/admin/achievement/certificate/update', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

/**
 * 删除证书
 * @param certificateId 证书ID
 */
export const deleteCertificate = (certificateId: number): Promise<ApiResponse> => {
    return request.delete(`/admin/achievement/certificate/delete/${certificateId}`);
}; 