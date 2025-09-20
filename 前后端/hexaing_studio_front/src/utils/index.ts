
const API_BASE_URL = 'http://localhost:8044';

/**
 * 将相对URL转换为完整的绝对URL，用于模板中获取文件
 * @param relativePath 资源的相对路径
 * @returns 完整的URL，如果输入为空则返回空字符串
 */
export function getFileUrl(relativePath: string | undefined): string {
  if (!relativePath) {
    return '';
  }
  // 如果已经是完整URL，直接返回
  if (relativePath.startsWith('http')) {
    return relativePath;
  }
  // 确保路径以'/'开头
  const path = relativePath.startsWith('/') ? relativePath : `/${relativePath}`;
  
  return `${API_BASE_URL}${path}`;
} 