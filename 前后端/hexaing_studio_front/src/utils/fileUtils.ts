/**
 * 文件工具类
 * 提供文件类型验证、文件大小格式化等通用功能
 */

// 当路径为空时，返回存放在 /public/images/ 目录下的默认头像
const defaultAvatar = '/images/default-avatar.svg';

const API_BASE_URL = '/api/admin/file/view/';

// 文件类型分类及其对应的扩展名
export const FILE_TYPES = {
  // 图片文件
  IMAGE: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'],
  
  // 文档文件
  DOCUMENT: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'],
  
  // 视频文件
  VIDEO: ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'],
  
  // 音频文件
  AUDIO: ['mp3', 'wav', 'ogg', 'flac', 'aac'],
  
  // 压缩文件
  ARCHIVE: ['zip', 'rar', '7z', 'tar', 'gz']
};

// 所有允许上传的文件类型扩展名
export const ALLOWED_EXTENSIONS = [
  ...FILE_TYPES.IMAGE,
  ...FILE_TYPES.DOCUMENT,
  ...FILE_TYPES.VIDEO,
  ...FILE_TYPES.AUDIO,
  ...FILE_TYPES.ARCHIVE
];

// 危险文件类型扩展名（可能包含可执行代码，存在安全风险）
export const DANGEROUS_EXTENSIONS = [
  'exe', 'bat', 'cmd', 'sh', 'ps1', 'vbs', 'js', 'py', 'php', 'asp', 'aspx', 'jsp', 
  'cgi', 'pl', 'dll', 'so', 'dylib', 'jar', 'war', 'msi', 'com', 'scr', 'gadget'
];

// 可以在浏览器中预览的文件类型
export const PREVIEWABLE_EXTENSIONS = [
  ...FILE_TYPES.IMAGE,
  'pdf',
  'txt'
];

/**
 * 解析资源的完整URL路径。
 * 
 * - 如果传入的路径为空、null或未定义，则返回默认头像。
 * - 如果路径已经是完整的HTTP/HTTPS URL，则直接返回。
 * - 如果路径已经包含了API前缀，也直接返回以避免重复拼接。
 * - 否则，将API前缀与相对路径拼接成完整URL。
 *
 * @param path - 文件的相对路径或完整URL。
 * @returns - 返回一个可用的、完整的图片URL。
 */
export function resolveAvatarUrl(path?: string | null): string {
  if (!path) {
    return defaultAvatar;
  }
  
  // 检查是否已经是完整的URL
  if (path.startsWith('http')) {
    return path;
  }
  
  // 检查是否已经包含了API前缀（避免重复添加）
  if (path.includes('/api/admin/file/view/')) {
    return path;
  }
  
  // 对于相对路径，添加API前缀
  return `/api/admin/file/view/${path}`;
} 

/**
 * 根据文件扩展名获取文件类型分类
 * @param extension 文件扩展名（不含点号）
 * @returns 文件类型分类名称，如 'IMAGE', 'DOCUMENT' 等，未知类型返回 'OTHER'
 */
export const getFileTypeCategory = (extension: string): string => {
  extension = extension.toLowerCase();
  
  for (const [category, extensions] of Object.entries(FILE_TYPES)) {
    if (extensions.includes(extension)) {
      return category;
    }
  }
  
  return 'OTHER';
};

/**
 * 获取文件扩展名
 * @param fileName 文件名
 * @returns 文件扩展名（不含点号），如果没有扩展名则返回空字符串
 */
export const getFileExtension = (fileName: string): string => {
  if (!fileName || !fileName.includes('.')) {
    return '';
  }
  return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
};

/**
 * 验证文件类型是否允许上传
 * @param file 文件对象
 * @returns 如果允许上传返回true，否则返回false
 */
export const validateFileType = (file: File): boolean => {
  const extension = getFileExtension(file.name);
  
  // 首先检查是否是危险文件类型
  if (DANGEROUS_EXTENSIONS.includes(extension)) {
    return false;
  }
  
  // 然后检查是否在允许的文件类型列表中
  return ALLOWED_EXTENSIONS.includes(extension);
};

/**
 * 检查文件是否可以在浏览器中预览
 * @param extension 文件扩展名（不含点号）
 * @returns 如果可以预览返回true，否则返回false
 */
export const isPreviewable = (extension: string): boolean => {
  return PREVIEWABLE_EXTENSIONS.includes(extension.toLowerCase());
};

/**
 * 格式化文件大小
 * @param bytes 文件大小（字节）
 * @param decimals 小数位数
 * @returns 格式化后的文件大小字符串，如 "1.5 MB"
 */
export const formatFileSize = (bytes: number, decimals: number = 2): string => {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i];
}; 