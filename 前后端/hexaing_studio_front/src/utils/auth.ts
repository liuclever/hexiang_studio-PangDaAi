// 检查用户是否已登录
export const isLoggedIn = (): boolean => {
  const token = localStorage.getItem('token');
  console.log('检查登录状态:', { 
    token: token ? `${token.substring(0, 20)}...` : '不存在',
    tokenLength: token ? token.length : 0
  });
  
  // 如果没有token，肯定未登录
  if (!token) {
    return false;
  }
  
  try {
    // 可以在这里添加token有效性验证
    // 例如：检查token是否过期（如果token是JWT）
    // 或者添加与后端的验证请求
    
    // 简单的验证示例（如果使用JWT）
    // const tokenData = JSON.parse(atob(token.split('.')[1]));
    // const expiry = tokenData.exp * 1000;
    // if (Date.now() > expiry) {
    //   console.log('Token已过期');
    //   clearSession();
    //   return false;
    // }
    
    return true;
  } catch (error) {
    console.error('Token验证出错:', error);
    clearSession();
    return false;
  }
};

// 获取用户ID
export const getUserId = (): number | null => {
  const userId = localStorage.getItem('user_id');
  if (!userId) return null;
  
  const userIdNum = parseInt(userId, 10);
  return isNaN(userIdNum) ? null : userIdNum;
};

// 获取用户名(username)，而不是名字(name)
export const getUserName = (): string | null => {
  return localStorage.getItem('user_name');
};

// 获取用户头像路径
export const getUserAvatar = (): string | null => {
  return localStorage.getItem('user_avatar');
};

// 获取token
export const getToken = (): string | null => {
  const token = localStorage.getItem('token');
  if (token) {
    console.log('获取到token:', {
      length: token.length,
      preview: `${token.substring(0, 20)}...`
    });
  } else {
    console.log('token不存在');
  }
  return token;
};

// 导入AI会话清理函数
import { clearAiSessionCache } from '@/api/ai-bear';

// 清除用户会话
export const clearSession = (): void => {
  console.log('清除用户会話');
  
  // 获取当前用户ID，用于清理对应的AI会话缓存
  const currentUserId = getUserId();
  
  // 🔧 保存记住密码信息（在清理前）
  const remember = localStorage.getItem('remember');
  const savedUsername = localStorage.getItem('saved_username');
  
  // 清理localStorage中的用户信息
  localStorage.removeItem('token');
  localStorage.removeItem('user_id');
  localStorage.removeItem('user_name');
  localStorage.removeItem('user_avatar');
  
  // 🔧 恢复记住密码信息（如果之前有的话）
  if (remember && savedUsername) {
    localStorage.setItem('remember', remember);
    localStorage.setItem('saved_username', savedUsername);
    console.log('💾 保留记住密码信息:', { remember, savedUsername });
  }
  
  // 🔧 清理AI会话缓存
  if (currentUserId) {
    clearAiSessionCache(currentUserId.toString());
  } else {
    // 如果无法获取用户ID，清理所有AI会话缓存
    clearAiSessionCache();
  }
};

// 导入请求工具
import request from './request';

// 登出
export const logout = async (): Promise<void> => {
  console.log('执行登出操作');
  
  try {
    // 调用后端登出API，清除Redis中的token
    await request.post('/admin/user/logout');
    console.log('后端登出成功，Redis token已清除');
  } catch (error) {
    console.error('后端登出失败:', error);
    // 即使后端登出失败，也要清除本地存储
  } finally {
    // 无论后端是否成功，都清除本地存储（clearSession已处理记住密码保留）
    clearSession();
  }
}; 