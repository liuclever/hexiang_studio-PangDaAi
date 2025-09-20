// pages/login/index.js
const { login } = require('../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    username: '',
    password: '',
    isLoading: false,
    // 重定向地址，登录成功后跳转
    redirect: '/pages/index/index'
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 获取重定向地址
    if (options.redirect) {
      this.setData({
        redirect: decodeURIComponent(options.redirect)
      });
    }
  },

  /**
   * 输入用户名
   */
  inputUsername(e) {
    this.setData({
      username: e.detail.value
    });
  },

  /**
   * 输入密码
   */
  inputPassword(e) {
    this.setData({
      password: e.detail.value
    });
  },



  /**
   * 提交登录
   */
  submitLogin() {
    const { username, password } = this.data;
    
    // 表单验证
    if (!username.trim()) {
      wx.showToast({
        title: '请输入用户名',
        icon: 'none'
      });
      return;
    }
    
    if (!password.trim()) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      });
      return;
    }
    
    // 设置登录中状态
    this.setData({ isLoading: true });
    
    // 调用登录接口
    login(username, password)
      .then(res => {
        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500
        });
        
        // 登录成功，跳转到指定页面
        setTimeout(() => {
          wx.reLaunch({
            url: this.data.redirect
          });
        }, 1500);
      })
      .catch(err => {
        wx.showToast({
          title: err.message || '登录失败',
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ isLoading: false });
      });
  },

  /**
   * 微信一键登录
   */
  wechatLogin() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  
  /**
   * 跳转到注册页
   */
  goToRegister() {
    wx.showToast({
      title: '注册功能开发中',
      icon: 'none'
    });
  },

  /**
   * 忘记密码处理
   */
  goToForgotPassword() {
    wx.showModal({
      title: '忘记密码',
      content: '请联系管理员重置密码哦',
      showCancel: false,
      confirmText: '知道了'
    });
  }
}); 