/**
 * 认证工具函数
 * 用于处理登录、注销、角色判断等认证相关功能
 */

const { http } = require('./request');

/**
 * 检查是否已登录
 * @returns {boolean} 是否已登录
 */
const isLoggedIn = () => {
  const token = wx.getStorageSync('token');
  return !!token;
};

/**
 * 获取当前用户信息
 * @returns {Object|null} 用户信息，未登录时返回null
 */
const getUserInfo = () => {
  return wx.getStorageSync('userInfo') || null;
};

/**
 * 获取当前用户角色
 * @returns {string} 角色类型，默认为'student'
 */
const getUserRole = () => {
  return wx.getStorageSync('role') || 'student';
};

/**
 * 判断是否为管理员
 * @returns {boolean} 是否为管理员
 */
const isAdmin = () => {
  const role = getUserRole();
  return role === 'admin';
};

/**
 * 判断是否为老师
 * @returns {boolean} 是否为老师
 */
const isTeacher = () => {
  const role = getUserRole();
  return role === 'teacher';
};

/**
 * 登录
 * @param {string} username 用户名
 * @param {string} password 密码
 * @returns {Promise} 登录结果
 */
const login = (username, password) => {
  return new Promise((resolve, reject) => {
    // 使用微信小程序的登录API
          http.post('/wx/user/login', { 
      userName: username, 
      password: password 
    })
      .then(res => {
        if (res.code === 200 && res.data) {
          const userData = res.data;
          // 保存登录信息到本地，匹配后端返回格式
          wx.setStorageSync('token', userData.token);
          
          // 根据positionId判断角色
          let userRole = 'student'; // 默认学生
          const positionId = userData.positionId || userData.position_id || userData.position?.positionId;
          
          console.log('登录用户数据:', userData);
          console.log('获取到的positionId:', positionId);
          
          if (positionId) {
            switch (parseInt(positionId)) {
              case 5: // 老师
                userRole = 'teacher';
                break;
              case 6: // 主任
              case 7: // 副主任
              case 8: // 超级管理员
                userRole = 'admin';
                break;
              default: // 学生相关角色 (1, 3, 4等)
                userRole = 'student';
                break;
            }
          }
          
          console.log('最终判定角色:', userRole);
          
          wx.setStorageSync('userInfo', {
            userId: userData.userId,  // 修改为 userId
            name: userData.name,
            avatar: userData.avatar,
            role: userRole,
            positionId: positionId
          });
          wx.setStorageSync('role', userRole);
        
          resolve(userData);
        } else {
          reject(new Error(res.msg || '登录失败'));
        }
      })
      .catch(err => {
        // 如果错误对象包含业务错误信息，则显示具体错误
        if (err && err.msg) {
          reject(new Error(err.msg));
        } else {
          reject(new Error(err.message || '网络请求失败'));
        }
      });
  });
};

/**
 * 退出登录
 * @param {Function} callback 退出登录后的回调函数
 */
const logout = (callback) => {
  // 清除登录信息
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  wx.removeStorageSync('role');
  
  // 可选：向服务器发送登出请求
      http.post('/wx/user/logout', {}, { showError: false, showLoading: false })
    .finally(() => {
      callback && callback();
    });
};



/**
 * 检查当前页面是否需要登录权限，如需要则跳转到登录页
 * @param {string} pageUrl 当前页面URL，可选，默认使用当前页
 */
const checkLoginStatus = (pageUrl) => {
  if (!isLoggedIn()) {
    wx.showToast({
      title: '请先登录',
      icon: 'none',
      duration: 1500,
      complete: () => {
        // 将当前页面作为登录成功后的跳转目标
        const currentPage = pageUrl || getCurrentPageUrl();
        if (currentPage && currentPage !== '/pages/login/index') {
          setTimeout(() => {
            wx.redirectTo({
              url: `/pages/login/index?redirect=${encodeURIComponent(currentPage)}`
            });
          }, 500);
        } else {
          setTimeout(() => {
            wx.redirectTo({
              url: '/pages/login/index'
            });
          }, 500);
        }
      }
    });
    return false;
  }
  return true;
};

/**
 * 获取当前页面URL
 * @returns {string} 当前页面URL
 */
const getCurrentPageUrl = () => {
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  let url = `/${currentPage.route}`;
  
  const options = currentPage.options;
  if (options && Object.keys(options).length) {
    url += '?';
    for (let key in options) {
      url += `${key}=${options[key]}&`;
    }
    url = url.substring(0, url.length - 1); // 移除末尾的&
  }
  
  return url;
};

/**
 * 检查权限，如果没有权限则提示并返回
 * @param {string} requiredRole 需要的角色，默认为'admin'
 * @returns {boolean} 是否有权限
 */
const checkPermission = (requiredRole = 'admin') => {
  const role = getUserRole();
  if (requiredRole === 'admin' && role !== 'admin') {
    wx.showToast({
      title: '权限不足',
      icon: 'none',
      duration: 2000
    });
    return false;
  }
  return true;
};

module.exports = {
  isLoggedIn,
  getUserInfo,
  getUserRole,
  isAdmin,
  isTeacher,
  login,
  logout,
  checkLoginStatus,
  checkPermission
}; 