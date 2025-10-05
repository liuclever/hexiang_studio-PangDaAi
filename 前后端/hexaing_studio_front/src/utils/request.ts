import axios from 'axios';
import type { AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { getToken } from '@/utils/auth';

// 递归转换对象的键名从 snake_case 到 camelCase
function toCamelCase(str: string): string {
  return str.replace(/_([a-z])/g, (match, letter) => letter.toUpperCase());
}

function convertToCamelCase(obj: any): any {
  if (Array.isArray(obj)) {
    return obj.map(convertToCamelCase);
  } else if (obj !== null && typeof obj === 'object') {
    return Object.keys(obj).reduce((result, key) => {
      const camelKey = toCamelCase(key);
      result[camelKey] = convertToCamelCase(obj[key]);
      return result;
    }, {} as any);
  }
  return obj;
}

/**
 * 处理Token刷新
 * @returns Promise<void>
 */
async function handleTokenRefresh(): Promise<void> {
  try {
    // 调用刷新接口（使用Cookie中的Refresh Token）
    const response = await axios.post('/api/admin/user/refresh', {}, {
      timeout: 10000, // 刷新请求10秒超时
      withCredentials: true // 确保携带Cookie
    });
    
    const result = convertToCamelCase(response.data);
    if (result.code === 1 || result.code === 200) {
      const newAccessToken = result.data.accessToken;
      if (newAccessToken) {
        localStorage.setItem('token', newAccessToken);
        console.log('Token刷新成功');
        return Promise.resolve();
      }
    }
    
    throw new Error('刷新响应无效');
  } catch (error) {
    console.error('Token刷新失败:', error);
    throw error;
  }
}

export interface ApiResponse<T = any> {
  code: number | string;
  msg?: string;
  data: T;
  timestamp?: number;
}

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 200000,
  withCredentials: true // 🚀 新增：确保携带Cookie
});

// 请求拦截器（保持不变）
service.interceptors.request.use(
  config => {
    console.log('原始请求配置:', JSON.stringify(config));
    
    const token = getToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
      
      const userId = localStorage.getItem('userId');
      if (userId && (config.method === 'post' || config.method === 'put') && config.data && !(config.data instanceof FormData)) {
        if (typeof config.data === 'string') {
          try {
            const data = JSON.parse(config.data);
            data.createUser = userId;
            config.data = JSON.stringify(data);
          } catch (e) {
            console.error('解析请求数据失败:', e);
          }
        } else {
          config.data.createUser = userId;
        }
      }
    }
    
    console.log('最终请求配置:', JSON.stringify(config));
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 🚀 升级后的响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = convertToCamelCase(response.data);
    
    console.log('API Response (converted):', res);
    console.log('API Response Headers:', response.headers);
    
    // 🚀 检查无感刷新Token
    const newAccessToken = response.headers['x-new-access-token'];
    if (newAccessToken) {
      console.log('检测到新的Access Token，自动更新...');
      localStorage.setItem('token', newAccessToken);
      console.log('Token自动更新成功');
    }
    
    const codeStr = String(res.code);

    // 处理401错误码
    if (codeStr === '401') {
      console.error('认证失败或Token过期(code=401)，尝试自动刷新...');
      
      // 🚀 尝试自动刷新Token
      return handleTokenRefresh().then(() => {
        // 刷新成功后重试原始请求
        const originalRequest = response.config;
        const newToken = localStorage.getItem('token');
        if (newToken && originalRequest) {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return service.request(originalRequest);
        }
        throw new Error('Token刷新失败');
      }).catch(() => {
        ElMessage.error('登录已过期，请重新登录');
        
        // 🔧 保存记住密码信息
        const remember = localStorage.getItem('remember');
        const savedUsername = localStorage.getItem('saved_username');
        
        localStorage.removeItem('token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('user_name');
        
        // 🔧 恢复记住密码信息
        if (remember && savedUsername) {
          localStorage.setItem('remember', remember);
          localStorage.setItem('saved_username', savedUsername);
        }
        
        router.push('/login');
        return Promise.reject(new Error('认证失败'));
      });
    }

    if (codeStr !== '1' && codeStr !== '200' && res.code !== undefined) { 
      console.error('请求失败，错误码:', res.code, '错误信息:', res.msg);
      // 🚨 移除拦截器中的消息显示，避免重复显示，由业务组件处理
      return Promise.reject(new Error(res.msg || '请求失败'));
    } else {
      return res;
    }
  },
  error => {
    console.error('响应错误:', error);
    
    // 处理网络层面的401错误
    if (error.response && error.response.status === 401) {
      console.error('HTTP 401错误，尝试自动刷新...');
      
      // 🚀 尝试自动刷新Token
      return handleTokenRefresh().then(() => {
        const originalRequest = error.config;
        const newToken = localStorage.getItem('token');
        if (newToken && originalRequest) {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return service.request(originalRequest);
        }
        throw new Error('Token刷新失败');
      }).catch(() => {
        // 🔧 保存记住密码信息
        const remember = localStorage.getItem('remember');
        const savedUsername = localStorage.getItem('saved_username');
        
        localStorage.removeItem('token');
        
        // 🔧 恢复记住密码信息
        if (remember && savedUsername) {
          localStorage.setItem('remember', remember);
          localStorage.setItem('saved_username', savedUsername);
        }
        
        router.push('/login');
        return Promise.reject(error);
      });
    }
    
    // 🚨 移除网络错误的通用提示，避免重复显示，由业务组件处理
    return Promise.reject(error);
  }
);

export default service; 