// src/utils/urlProcessor.ts

const API_BASE_URL = 'http://localhost:8044';

/**
 * 将相对URL转换为完整的绝对URL
 * @param relativePath 资源的相对路径
 * @returns 完整的URL
 */
function getFullUrl(relativePath: string | undefined): string {
  if (!relativePath) return '';
  if (relativePath.startsWith('http')) return relativePath; 
  
  const path = relativePath.startsWith('/') ? relativePath : `/${relativePath}`;
  return `${API_BASE_URL}${path}`;
}

/**
 * 定义资源项的通用接口
 */
interface ResourceItem {
    url?: string;
    filePath?: string;
    [key: string]: any;
}

/**
 * 处理单个资源项，确保其拥有完整的URL
 * @param item 资源对象
 * @returns 处理后的资源对象
 */
function processItem(item: ResourceItem): ResourceItem {
    if (!item) return item;
    const newItem = { ...item };
    const relativeUrl = newItem.url || newItem.filePath;
    newItem.url = getFullUrl(relativeUrl);
    return newItem;
}

/**
 * 递归处理API返回的数据，转换所有资源URL
 * @param data 需要处理的数据
 * @returns 处理完成的数据
 */
function processData(data: any): any {
    if (!data) return data;

    if (Array.isArray(data)) {
        return data.map(item => processData(item));
    }

    if (typeof data === 'object' && data !== null) {
        const newData = { ...data };

        // 统一处理图片和附件数组
        if (newData.images && Array.isArray(newData.images)) {
            newData.images = newData.images.map(processItem);
        }
        if (newData.attachments && Array.isArray(newData.attachments)) {
            newData.attachments = newData.attachments.map(processItem);
        }

        // 递归处理嵌套的对象
        for (const key in newData) {
            if (key !== 'images' && key !== 'attachments' && typeof newData[key] === 'object') {
                newData[key] = processData(newData[key]);
            }
        }
        return newData;
    }
    
    return data;
}


/**
 * 处理整个API响应对象
 * @param response API响应
 * @returns 处理后的API响应
 */
export function processApiResponse<T extends { data?: any }>(response: T): T {
    if (response && response.data) {
        // 处理分页列表数据，常见的字段为 list 或 records
        if (response.data.list && Array.isArray(response.data.list)) {
            response.data.list = processData(response.data.list);
        } else if (response.data.records && Array.isArray(response.data.records)) {
            response.data.records = processData(response.data.records);
        } else {
             // 处理非分页的详情数据
            response.data = processData(response.data);
        }
    }
    return response;
} 