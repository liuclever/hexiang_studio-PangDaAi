// pages/schedule/index.js
const { checkLoginStatus, getUserInfo } = require('../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    userInfo: null
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    // 获取用户信息
    this.setData({
      userInfo: getUserInfo()
    });
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 页面显示时可以刷新数据
    if (this.selectComponent('#dutyTable')) {
      this.selectComponent('#dutyTable').refreshData();
    }
  },

  /**
   * 处理值班详情事件
   */
  onDutyDetail(e) {
    const { dutyInfo } = e.detail;
    
    // 可以在这里处理值班详情显示
    // 例如弹出详情弹窗或跳转到详情页面
    console.log('值班详情:', dutyInfo);
    
    // 示例：显示详情信息
    const studentNames = dutyInfo.students.map(s => s.studentName).join('、');
    
    wx.showModal({
      title: '值班详情',
      content: `值班人员：${studentNames}\n地点：${dutyInfo.location}`,
      showCancel: false,
      confirmText: '知道了'
    });
  },

  /**
   * 页面下拉刷新
   */
  onPullDownRefresh() {
    // 刷新值班表数据
    const dutyTable = this.selectComponent('#dutyTable');
    if (dutyTable) {
      dutyTable.refreshData().finally(() => {
        wx.stopPullDownRefresh();
      });
    } else {
      wx.stopPullDownRefresh();
    }
  },

  /**
   * 页面分享
   */
  onShareAppMessage() {
    return {
      title: '工作室值班表',
      path: '/pages/schedule/index',
      imageUrl: '' // 可以设置分享图片
    };
  }
}); 