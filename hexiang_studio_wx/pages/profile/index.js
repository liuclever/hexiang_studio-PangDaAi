// pages/profile/index.js
const { http, BASE_URL } = require('../../utils/request');
const { checkLoginStatus, getUserInfo, logout: authLogout } = require('../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户详情
    userDetail: {},
    // 培训方向列表
    trainingDirections: [],
    // 加载状态
    loading: true,
    // 基础URL
    baseUrl: BASE_URL
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    // 加载用户信息
    this.loadUserDetail();
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 设置TabBar选中状态为我的（索引4）
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateSelected(4);
    }
    
    // 如果已经加载过数据，重新刷新
    if (!this.data.loading && this.data.userDetail.userId) {
      this.loadUserDetail();
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadUserDetail().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  },

  /**
   * 加载用户详情
   */
  async loadUserDetail() {
    try {
      this.setData({ loading: true });
      
      // 获取当前用户信息
      const currentUser = getUserInfo();
      if (!currentUser || !currentUser.userId) {
        wx.showToast({
          title: '用户信息异常',
          icon: 'none'
        });
        return;
      }

      // 请求用户详情 - 使用与成员页面相同的API
      const response = await http.get('/wx/user/detail', { 
        userId: currentUser.userId 
      });

      if (response.code === 200 && response.data) {
        let userDetail = response.data;
        
        // 处理头像路径
        if (userDetail.avatar && !userDetail.avatar.startsWith('http')) {
          userDetail.avatar = `${BASE_URL}/wx/file/view/${userDetail.avatar}`;
        }

        // 处理部门字段 - 将departmentName映射到department
        if (userDetail.departmentName) {
          userDetail.department = userDetail.departmentName;
        } else if (userDetail.roleId === 1) {
          // 学生角色但没有部门信息时显示"未分配"
          userDetail.department = '未分配';
        }

        // 处理培训方向 - 确保数组格式
        if (userDetail.directionIds && (userDetail.roleId === 1 || userDetail.roleId === 2)) {
          // 只对学员和讲师处理培训方向
          await this.mapDirectionIdsToNames(userDetail);
        }
        
        // 处理职位信息 - 根据positionId设置职位名称
        this.processPositionInfo(userDetail);
        
        // 处理用户状态 - 确保有status字段
        if (userDetail.status === undefined) {
          userDetail.status = 1; // 默认为启用状态
        }

        this.setData({
          userDetail: userDetail
        });
      } else {
        throw new Error(response.message || '获取用户详情失败');
      }
    } catch (error) {
      console.error('加载用户详情失败:', error);
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 处理职位信息
   */
  processPositionInfo(userData) {
    if (userData.positionId !== undefined && userData.positionId !== null) {
      // 根据positionId设置职位名称
      this.setDefaultPosition(userData);
    } else {
      // 没有positionId，使用默认职位
      this.setDefaultPosition(userData);
    }
    
    // 确保position属性也被设置，保持一致性
    userData.position = userData.positionName;
  },

  /**
   * 设置默认职位名称
   */
  setDefaultPosition(userData) {
    // 如果存在positionId，根据positionId设置职位名称
    if (userData.positionId !== undefined && userData.positionId !== null) {
      switch (parseInt(userData.positionId)) {
        case 0:
          userData.positionName = '访客';
          break;
        case 1:
          userData.positionName = '普通学员';
          break;
        case 3:
          userData.positionName = '部长';
          break;
        case 4:
          userData.positionName = '副部长';
          break;
        case 5:
          userData.positionName = '老师';
          break;
        case 6:
          userData.positionName = '主任';
          break;
        case 7:
          userData.positionName = '副主任';
          break;
        case 8:
          userData.positionName = '超级管理员';
          break;
        default:
          // 如果positionId不在预定义范围内，则根据roleId设置
          this.setPositionByRole(userData);
      }
    } else {
      // 没有positionId，根据roleId设置默认职位
      this.setPositionByRole(userData);
    }
  },

  /**
   * 根据角色ID设置默认职位名称
   */
  setPositionByRole(userData) {
    const roleId = parseInt(userData.roleId);
    
    switch(roleId) {
      case 0:
        userData.positionName = '访客';
        break;
      case 1:
        userData.positionName = '普通学员';
        break;
      case 2:
        userData.positionName = '老师';
        break;
      case 3:
        userData.positionName = '管理员';
        break;
      default:
        userData.positionName = '成员';
    }
  },

  /**
   * 将用户的培训方向ID映射为方向名称
   */
  async mapDirectionIdsToNames(userData) {
    // 如果是访客或管理员角色，直接跳过处理
    if (userData.roleId === 0 || userData.roleId === 3) {
      return;
    }
    
    try {
      // 确保培训方向数据已加载
      if (!this.data.trainingDirections || this.data.trainingDirections.length === 0) {
        // 如果培训方向未加载，先加载
        const res = await http.get('/wx/user/training-directions');
        if (res.code === 200 && Array.isArray(res.data)) {
          this.setData({
            trainingDirections: res.data
          });
        }
      }
      
      // 处理培训方向映射
      this.processDirectionMapping(userData);
    } catch (error) {
      console.error('获取培训方向列表失败:', error);
    }
  },

  /**
   * 处理培训方向映射
   */
  processDirectionMapping(userData) {
    const directions = this.data.trainingDirections;
    let directionIds = [];
    let directionNames = [];

    // 处理不同格式的培训方向数据
    if (typeof userData.directionIds === 'string') {
      // 如果是字符串，尝试解析为数组
      try {
        directionIds = JSON.parse(userData.directionIds);
      } catch (e) {
        // 如果不是JSON字符串，按逗号分割
        directionIds = userData.directionIds.split(',').map(id => parseInt(id.trim(), 10));
      }
    } else if (Array.isArray(userData.directionIds)) {
      directionIds = userData.directionIds;
    } else if (userData.directionId) {
      // 兼容单个方向ID的情况
      directionIds = [userData.directionId];
    }

    // 映射ID到名称
    directionIds.forEach(id => {
      const direction = directions.find(d => d.directionId === id);
      if (direction) {
        directionNames.push(direction.directionName);
      }
    });

    // 更新用户数据中的培训方向名称
    userData.directionIdNames = directionNames;
  },



  /**
   * 退出登录
   */
  async logout() {
    try {
      const result = await wx.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        confirmText: '退出',
        cancelText: '取消'
      });

      if (result.confirm) {
        // 显示加载提示
        wx.showLoading({
          title: '退出中...'
        });

        // 调用认证工具的登出函数
        await authLogout();

        wx.hideLoading();
        
        wx.showToast({
          title: '已退出登录',
          icon: 'success',
          duration: 1500,
          complete: () => {
            // 跳转到登录页面
            setTimeout(() => {
              wx.reLaunch({
                url: '/pages/login/index'
              });
            }, 1000);
          }
        });
      }
    } catch (error) {
      wx.hideLoading();
      console.error('退出登录失败:', error);
      wx.showToast({
        title: '退出失败，请重试',
        icon: 'none'
      });
    }
  }
});