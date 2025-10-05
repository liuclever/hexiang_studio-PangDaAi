// app.js - 何湘技能大师工作室小程序入口文件
const { BASE_URL } = require('./config/index.js');
const storage = require('./utils/storage.js');

App({
  // 全局数据
  globalData: {
    userInfo: null,
    role: 'student', // 默认角色: 'student' 或 'admin'
    token: '',
    baseUrl: BASE_URL, // 使用配置文件中的API基础URL
    version: '1.0.0'
  },

  onLaunch() {
    // 小程序启动时执行
    this.checkLogin();
    this.checkUpdate();
  },

  // 检查登录状态
  checkLogin() {
    const token = storage.getToken();
    const userInfo = storage.getUserInfo();
    const role = storage.getRole() || 'student';
    
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.role = role;
    }
  },
  


  // 登录方法
  login(username, password, callback) {
    const that = this;
    wx.request({
      url: `${this.globalData.baseUrl}/wx/user/login`,
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
          
          // 本地存储（统一storage）
          storage.setToken(token);
          storage.setUserInfo(userInfo);
          storage.setRole(that.globalData.role);
          
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
    storage.removeToken();
    storage.removeUserInfo();
    
    // 跳转到登录页
    wx.reLaunch({
      url: '/pages/login/index'
    });
  },

  // 切换角色
  switchRole(role) {
    if (role === 'student' || role === 'admin') {
      this.globalData.role = role;
      storage.setRole(role);
      
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