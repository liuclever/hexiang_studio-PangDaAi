/**
 * 网络请求工具
 * 封装微信小程序的网络请求API，统一处理请求、响应和错误
 */

// 导入统一配置
const { BASE_URL, FILE_URL, TIMEOUT } = require('../config/index');
const storage = require('./storage.js');

// 获取全局App实例 - 修复递归调用问题
const getAppInstance = () => {
  return getApp();
};

// 添加Token刷新功能

let isRefreshing = false; // 防止多次同时刷新
let failedQueue = []; // 失败请求队列

// 处理队列中的请求
const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

// 刷新Token
const refreshToken = () => {
  return new Promise((resolve, reject) => {
    const refreshToken = storage.getRefreshToken();
    
    if (!refreshToken) {
      reject(new Error('没有Refresh Token'));
      return;
    }
    
    wx.request({
      url: `${BASE_URL}/wx/user/refresh-by-token`,
      method: 'POST',
      data: { refreshToken },
      header: { 'content-type': 'application/json' },
      success(res) {
        if (res.statusCode === 200 && res.data.code === 200) {
          const newAccessToken = res.data.data.accessToken;
          storage.setAccessToken(newAccessToken);
          resolve(newAccessToken);
        } else {
          reject(new Error('刷新失败'));
        }
      },
      fail(err) {
        reject(err);
      }
    });
  });
};

// 请求方法
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 使用Access Token
    const token = storage.getAccessToken() || storage.getToken();
    
    // 合并请求头
    const header = {
      'content-type': 'application/json',
      ...options.header
    };
    
    // 如果有token，添加到请求头
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }
    
    // 显示加载提示
    if (options.showLoading !== false) {
      wx.showLoading({
        title: options.loadingText || '加载中',
        mask: true
      });
    }
    
    // 发起请求
    wx.request({
      url: /^(http|https):\/\//.test(options.url) ? options.url : `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      timeout: options.timeout || TIMEOUT,
      success(res) {
        // 隐藏加载提示
        if (options.showLoading !== false) {
          wx.hideLoading();
        }
        
        // 🚀 检查无感刷新Token
        if (res.header['x-new-access-token'] || res.header['X-New-Access-Token']) {
          const newToken = res.header['x-new-access-token'] || res.header['X-New-Access-Token'];
          storage.setAccessToken(newToken);
          console.log('小程序Token自动更新成功');
        }
        
        // 请求成功，但业务状态可能失败
        if (res.statusCode >= 200 && res.statusCode < 300) {
          // 后端接口统一格式：{ code: 200, message: 'success', data: {} }
          if (res.data.code === 200 || res.data.code === 0) {
            resolve(res.data);
          } else if (res.data.code === 401) {
            // 🚀 尝试自动刷新Token
            handleTokenRefresh(options, resolve, reject);
          } else {
            // 其他业务错误
            if (options.showError !== false) {
              wx.showToast({
                title: res.data.message || '请求失败',
                icon: 'none'
              });
            }
            reject(res.data);
          }
        } else if (res.statusCode === 401) {
          // 🚀 HTTP 401，尝试刷新
          handleTokenRefresh(options, resolve, reject);
        } else {
          // HTTP错误
          handleHttpError(res.statusCode);
          reject({ code: res.statusCode, message: '请求失败' });
        }
      },
      fail(err) {
        // 隐藏加载提示
        if (options.showLoading !== false) {
          wx.hideLoading();
        }
        
        // 网络错误
        if (options.showError !== false) {
          wx.showToast({
            title: '网络异常，请检查网络设置',
            icon: 'none'
          });
        }
        
        reject({ code: -1, message: '网络异常', error: err });
      }
    });
  });
};

// 🚀 处理Token刷新
const handleTokenRefresh = (originalOptions, resolve, reject) => {
  if (isRefreshing) {
    // 如果正在刷新，将请求加入队列
    return new Promise((resolve, reject) => {
      failedQueue.push({ resolve, reject });
    }).then(token => {
      originalOptions.header = originalOptions.header || {};
      originalOptions.header['Authorization'] = `Bearer ${token}`;
      return request(originalOptions);
    }).then(resolve).catch(reject);
  }
  
  isRefreshing = true;
  
  refreshToken().then(newToken => {
    processQueue(null, newToken);
    
    // 重试原始请求
    originalOptions.header = originalOptions.header || {};
    originalOptions.header['Authorization'] = `Bearer ${newToken}`;
    return request(originalOptions);
  }).then(resolve).catch(err => {
    processQueue(err, null);
    // 刷新失败，跳转登录页
    handleUnauthorized();
    reject(err);
  }).finally(() => {
    isRefreshing = false;
  });
};

// 处理401未授权错误
const handleUnauthorized = () => {
  // 清除登录状态
  storage.clearTokens(); // 使用新的清除方法
  storage.removeUserInfo();
  storage.removeRole();
  
  // 跳转到登录页面
  wx.showToast({
    title: '登录已过期，请重新登录',
    icon: 'none',
    duration: 2000,
    complete: () => {
      setTimeout(() => {
        wx.reLaunch({
          url: '/pages/login/index'
        });
      }, 100);
    }
  });
};

// 处理HTTP错误
const handleHttpError = (statusCode) => {
  let message = '请求失败';
  
  switch (statusCode) {
    case 400:
      message = '请求参数错误';
      break;
    case 401:
      message = '未授权，请登录';
      handleUnauthorized();
      return;
    case 403:
      message = '拒绝访问';
      break;
    case 404:
      message = '请求的资源不存在';
      break;
    case 500:
      message = '服务器错误';
      break;
    case 502:
      message = '网关错误';
      break;
    case 503:
      message = '服务不可用';
      break;
    case 504:
      message = '网关超时';
      break;
    default:
      message = `未知错误(${statusCode})`;
  }
  
  wx.showToast({
    title: message,
    icon: 'none'
  });
};

// HTTP请求方法简化
const http = {
  get: (url, data, options = {}) => {
    return request({
      url,
      method: 'GET',
      data,
      ...options
    });
  },
  post: (url, data, options = {}) => {
    return request({
      url,
      method: 'POST',
      data,
      ...options
    });
  },
  put: (url, data, options = {}) => {
    return request({
      url,
      method: 'PUT',
      data,
      ...options
    });
  },
  delete: (url, data, options = {}) => {
    return request({
      url,
      method: 'DELETE',
      data,
      ...options
    });
  }
};

module.exports = {
  request,
  http,
  BASE_URL,  // API基础地址
  FILE_URL   // 文件访问地址
}; 