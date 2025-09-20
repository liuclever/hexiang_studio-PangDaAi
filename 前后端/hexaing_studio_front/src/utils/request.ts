import axios, { AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { getToken } from './auth';
import router from '../router';

/**
 * 将下划线命名转换为驼峰命名
 * @param str 下划线命名的字符串
 */
function snakeToCamel(str: string): string {
  return str.replace(/_([a-z])/g, (match, letter) => letter.toUpperCase());
}

/**
 * 将驼峰命名转换为下划线命名
 * @param str 驼峰命名的字符串
 */
function camelToSnake(str: string): string {
  return str.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`);
}

/**
 * 递归转换对象中的所有键为驼峰命名
 * @param obj 包含下划线命名键的对象
 */
function convertToCamelCase(obj: any): any {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(item => convertToCamelCase(item));
  }

  const result: Record<string, any> = {};
  Object.keys(obj).forEach(key => {
    const camelKey = snakeToCamel(key);
    result[camelKey] = convertToCamelCase(obj[key]);
  });

  return result;
}

/**
 * 递归转换对象中的所有键为下划线命名
 * @param obj 包含驼峰命名键的对象
 */
function convertToSnakeCase(obj: any): any {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(item => convertToSnakeCase(item));
  }

  const result: Record<string, any> = {};
  Object.keys(obj).forEach(key => {
    const snakeKey = camelToSnake(key);
    result[snakeKey] = convertToSnakeCase(obj[key]);
  });

  return result;
}

// 定义统一响应结构
export interface ApiResponse<T = any> {
  code: number;
  msg?: string;
  data: T;
  timestamp?: number;
}

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 设置基础URL，所有请求都会带上/api前缀
  timeout: 200000 // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加调试日志
    console.log('原始请求配置:', JSON.stringify(config));
    
    // 移除将 camelCase 转换为 snake_case 的逻辑

    // 添加token到请求头
    const token = getToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
      
      // 从localStorage获取用户ID，添加到请求中
      const userId = localStorage.getItem('userId');
      // 对于POST和PUT请求，添加createUser字段，但同样要排除FormData
      if (userId && (config.method === 'post' || config.method === 'put') && config.data && !(config.data instanceof FormData)) {
        if (typeof config.data === 'string') {
          // 如果数据是字符串（已经是JSON字符串），解析后添加字段再转回字符串
          try {
            const data = JSON.parse(config.data);
            data.createUser = userId; // 使用 camelCase
            config.data = JSON.stringify(data);
          } catch (e) {
            console.error('解析请求数据失败:', e);
          }
        } else {
          // 如果数据是对象，直接添加字段
          config.data.createUser = userId; // 使用 camelCase
        }
      }
    }
    
    // 请求前的处理
    console.log('最终请求配置:', JSON.stringify(config));
    return config;
  },
  error => {
    // 请求错误的处理
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 递归将响应数据中的 snake_case 转换为 camelCase
    const res = convertToCamelCase(response.data);
    
    console.log('API Response (converted):', res); // 添加详细日志
    console.log('API Response Headers:', response.headers);
    console.log('Response Code:', res.code, 'Type:', typeof res.code);
    
    // 将code转换为字符串进行比较，确保兼容数字和字符串类型的状态码
    const codeStr = String(res.code);

    // 首先处理需要特殊操作的错误码，如401
    if (codeStr === '401') {
      console.error('认证失败或Token过期(code=401)，执行登出...');
      ElMessage.error(res.msg || '登录已过期，请重新登录');
      
      // 清除会话并跳转到登录页
      // 建议将清除逻辑统一到auth.ts中的logout或clearSession函数
      localStorage.removeItem('token');
      localStorage.removeItem('user_id');
      localStorage.removeItem('user_name');
      router.push('/login');
      
      return Promise.reject(new Error('认证失败'));
    }

    // 修改成功状态码的判断，接受code=1和code=200作为成功状态码
    if (codeStr !== '1' && codeStr !== '200' && res.code !== undefined) { 
      console.error('请求失败，错误码:', res.code, '错误信息:', res.msg);
      ElMessage({
        message: res.msg || '请求失败',
        type: 'error',
        duration: 5 * 1000
      });
      
      // 处理特定的错误码
      if (codeStr === '401') {
        // 这段逻辑实际上不会被执行，因为上面的if块已经处理了401
        // 但为保持代码完整性，暂时保留
        localStorage.removeItem('token');
        router.push('/login');
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'));
    } else {
      // 请求成功，返回转换后的数据
      return res;
    }
  },
  error => {
    console.error('响应错误:', error);
    console.error('响应详情:', error.response); // 添加更多错误详情
    
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      router.push('/login');
    }
    
    ElMessage({
      message: error.message || '网络错误，请稍后重试',
      type: 'error',
      duration: 5 * 1000
    });
    return Promise.reject(error);
  }
);

export default service; 