// app.js - 何湘技能大师工作室小程序入口文件
App({
  // 全局数据
  globalData: {
    userInfo: null,
    role: 'student', // 默认角色: 'student' 或 'admin'
    token: '',
    baseUrl: 'http://172.20.10.2:8044', // 修改API基础URL为本地开发地址
    version: '1.0.0'
  },

  onLaunch() {
    // 小程序启动时执行
    this.checkLogin();
    this.checkUpdate();
  },

  // 检查登录状态
  checkLogin() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    const role = wx.getStorageSync('role') || 'student';
    
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.role = role;
      
      // 验证token有效性
      this.validateToken();
    }
  },
  
  // 验证Token有效性
  validateToken() {
    const that = this;
    wx.request({
      url: `${this.globalData.baseUrl}/api/auth/validate`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${this.globalData.token}`
      },
      success(res) {
        if (res.data.code !== 200) {
          that.logout();
        }
      },
      fail() {
        console.error('Token验证失败');
      }
    });
  },

  // 登录方法
  login(username, password, callback) {
    const that = this;
    wx.request({
      url: `${this.globalData.baseUrl}/api/auth/login`,
      method: 'POST',
      data: {
        username,
        password
      },
      success(res) {
        if (res.data.code === 200) {
          const { token, userInfo, role } = res.data.data;
          
          // 保存登录信息
          that.globalData.token = token;
          that.globalData.userInfo = userInfo;
          that.globalData.role = role || 'student';
          
          // 本地存储
          wx.setStorageSync('token', token);
          wx.setStorageSync('userInfo', userInfo);
          wx.setStorageSync('role', that.globalData.role);
          
          callback && callback(true, '登录成功');
        } else {
          callback && callback(false, res.data.message || '登录失败');
        }
      },
      fail(err) {
        console.error('登录请求失败', err);
        callback && callback(false, '网络请求失败，请检查网络连接');
      }
    });
  },

  // 退出登录
  logout() {
    // 清除全局数据
    this.globalData.token = '';
    this.globalData.userInfo = null;
    
    // 清除本地存储
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    
    // 跳转到登录页
    wx.reLaunch({
      url: '/pages/login/index'
    });
  },

  // 切换角色
  switchRole(role) {
    if (role === 'student' || role === 'admin') {
      this.globalData.role = role;
      wx.setStorageSync('role', role);
      
      // 重定向到首页
      wx.reLaunch({
        url: '/pages/index/index'
      });
      
      return true;
    }
    return false;
  },

  // 检查更新
  checkUpdate() {
    if (wx.canIUse('getUpdateManager')) {
      const updateManager = wx.getUpdateManager();
      updateManager.onCheckForUpdate((res) => {
        if (res.hasUpdate) {
          updateManager.onUpdateReady(() => {
            wx.showModal({
              title: '更新提示',
              content: '新版本已经准备好，是否重启应用？',
              success(res) {
                if (res.confirm) {
                  updateManager.applyUpdate();
                }
              }
            });
          });
          
          updateManager.onUpdateFailed(() => {
            wx.showModal({
              title: '更新提示',
              content: '新版本下载失败，请检查网络设置后重试',
              showCancel: false
            });
          });
        }
      });
    }
  }
}); 