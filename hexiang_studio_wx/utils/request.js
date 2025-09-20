/**
 * 网络请求工具
 * 封装微信小程序的网络请求API，统一处理请求、响应和错误
 */

// 导入统一配置
const { BASE_URL, FILE_URL, TIMEOUT } = require('../config/index');

// 获取全局App实例 - 修复递归调用问题
const getAppInstance = () => {
  return getApp();
};

// 请求方法
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 获取token，优先从本地存储获取
    const token = wx.getStorageSync('token');
    
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
        
        // 请求成功，但业务状态可能失败
        if (res.statusCode >= 200 && res.statusCode < 300) {
          // 后端接口统一格式：{ code: 200, message: 'success', data: {} }
          if (res.data.code === 200 || res.data.code === 0) {
            resolve(res.data);
          } else if (res.data.code === 401) {
            // token过期或未登录
            handleUnauthorized();
            reject(res.data);
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

// 处理401未授权错误
const handleUnauthorized = () => {
  // 清除登录状态
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  
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