import request from '../request';
import { processApiResponse } from '../urlProcessor';

// 定义接口类型
export interface PageNoticeParams {
  page?: number;
  pageSize?: number;
  title?: string;
  type?: any;
  status?: string;
  createTime?: string;
  beginTime?: string;
  endTime?: string;
}

export interface NoticeData {
  id?: number;
  title: string;
  content: string;
  type: number;
  status: string | number;
  isTop?: number;
  publishTime?: string | Date;
  createTime?: string | Date;
  updateTime?: string | Date;
  createUser?: number;
  updateUser?: number;
  // 前端额外字段
  publisher?: string;
  images?: Array<{
    id?: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
  }>;
  attachments?: Array<{
    id?: number;
    name: string;
    url: string;
    size: number;
    raw?: File;
  }>;
}

/**
 * 获取公告列表
 * @param params 查询参数
 * @returns 
 */
export function getNoticeList(params: PageNoticeParams) {
  return request({
    url: '/admin/notice/list',
    method: 'get',
    params
  }).then(processApiResponse);
}

/**
 * 获取公告详情
 * @param id 公告ID
 * @returns 
 */
export function getNoticeDetail(id: number) {
  return request({
    url: '/admin/notice/detail',
    method: 'get',
    params: { id }
  }).then(processApiResponse);
}

/**
 * 添加公告
 * @param data 公告数据
 * @returns 
 */
export function addNotice(data: NoticeData) {
  // 创建 FormData 对象用于文件上传
  const formData = new FormData();
  
  // 创建公告DTO对象
  const noticeDto = {
    title: data.title,
    content: data.content,
    type: data.type,
    status: Number(data.status), // 直接将 '1' 或 '0' 转换为数字
    isTop: data.isTop || 0
  };
  
  // 将公告DTO转换为JSON字符串，并作为一个part添加到FormData
  formData.append('noticeDto', JSON.stringify(noticeDto));
  
  // 添加图片文件
  if (data.images && data.images.length > 0) {
    data.images.forEach(image => {
      if (image.raw) {
        formData.append('imageFiles', image.raw);
      }
    });
  }
  
  // 添加附件文件
  if (data.attachments && data.attachments.length > 0) {
    data.attachments.forEach(attachment => {
      if (attachment.raw) {
        formData.append('attachmentFiles', attachment.raw);
      }
    });
  }
  
  return request({
    url: '/admin/notice/add',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 更新公告
 * @param data 公告数据
 * @returns 
 */
export function updateNotice(data: NoticeData) {
  // 创建 FormData 对象用于文件上传
  const formData = new FormData();
  
  // 创建公告DTO对象
  const noticeDto = {
    id: data.id,
    noticeId: data.id, // 确保同时设置noticeId字段
    title: data.title,
    content: data.content,
    type: data.type,
    status: Number(data.status), // 直接将 '1' 或 '0' 转换为数字
    isTop: data.isTop || 0
  };
  
  console.log('提交更新的公告数据:', noticeDto);
  
  // 将公告DTO转换为JSON字符串，并作为一个part添加到FormData
  formData.append('noticeDto', JSON.stringify(noticeDto));
  
  // 收集要保留的图片ID
  const keepImageIds: number[] = [];
  
  // 添加图片文件
  if (data.images && data.images.length > 0) {
    data.images.forEach(image => {
      if (image.raw) {
        formData.append('imageFiles', image.raw);
      } else if (image.id) {
        // 对于已有图片，保存ID
        keepImageIds.push(image.id);
      }
    });
  }
  
  // 收集要保留的附件ID
  const keepAttachmentIds: number[] = [];
  
  // 添加附件文件
  if (data.attachments && data.attachments.length > 0) {
    data.attachments.forEach(attachment => {
      if (attachment.raw) {
        formData.append('attachmentFiles', attachment.raw);
      } else if (attachment.id) {
        // 对于已有附件，保存ID
        keepAttachmentIds.push(attachment.id);
      }
    });
  }
  
  // 添加要保留的图片ID和附件ID
  if (keepImageIds.length > 0) {
    formData.append('keepImageIds', JSON.stringify(keepImageIds));
  }
  
  if (keepAttachmentIds.length > 0) {
    formData.append('keepAttachmentIds', JSON.stringify(keepAttachmentIds));
  }
  
  return request({
    url: '/admin/notice/update',
    method: 'put',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 删除公告
 * @param ids 公告ID数组
 * @returns 
 */
export function deleteNotice(ids: number[]) {
  return request({
    url: '/admin/notice/delete',
    method: 'delete',
    data: ids
  });
}

/**
 * 获取近一个月的系统公告（默认10条）
 * @param params 可选参数
 * @returns 近一个月的最新系统公告
 */
export function getRecentNotices(params?: any) {
  return request({
    url: '/admin/notice/recent',
    method: 'get',
    params
  });
}

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