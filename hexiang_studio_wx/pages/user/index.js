// pages/user/index.js
const { http, BASE_URL } = require('../../utils/request');
const { checkLoginStatus, getUserInfo } = require('../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户列表
    users: [],
    // 当前页
    currentPage: 1,
    // 每页数据量
    pageSize: 15,
    // 总数据量
    total: 0,
    // 搜索关键词
    searchKeyword: '',
    // 角色筛选
    selectedRole: '',
    // 方向筛选
    selectedDirection: '',
    // 培训方向列表
    trainingDirections: [],
    // 部门列表
    departments: [],
    // 选中的部门
    selectedDepartment: '',
    // 加载状态
    loading: false,
    // 是否有更多数据
    hasMore: true,
    // 显示用户详情
    showUserDetail: false,
    // 当前选中的用户ID
    currentUserId: null,
    // 当前用户详情
    userDetail: null,
    // 加载用户详情状态
    loadingDetail: false,
    // 底部加载提示
    loadingMoreText: '上拉加载更多',
    // 动画是否启用
    animationsEnabled: true,
    // 基础URL
    baseUrl: BASE_URL
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态，未登录则跳转到登录页
    if (!checkLoginStatus()) {
      return;
    }
    
    // 默认禁用动画以提高性能
    this.setData({
      animationsEnabled: false
    });
    
    // 设置默认选中"全部"标签，并在回调中确保加载用户列表
    this.setData({
      activeTag: 'all'
    }, () => {
      // 确保在设置完activeTag后立即加载用户列表
      this.loadUsers(true);
    });
    
    // 加载用户统计数据
    this.loadUserStats();
    
    // 加载部门列表
    this.loadDepartments();
  },
  
  // 已移除动画初始化函数，减少性能开销

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 检查用户列表是否为空，如果为空则重新加载
    if (this.data.users.length === 0) {
      this.loadUsers(true);
    } else {
      // 每次显示页面时刷新用户列表
      this.loadUsers(true);
    }
    
    // 设置TabBar选中状态为成员（索引3）
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateSelected(3);
    }
  },

  // 已移除设备性能检查，默认禁用动画以提高性能

  /**
   * 加载用户数据
   * @param {boolean} refresh 是否刷新列表
   */
  loadUsers(refresh = false) {
    const { currentPage, pageSize, searchKeyword, loading, activeTag } = this.data;
    
    if (loading) {
      return;
    }
    
    // 设置加载状态
    this.setData({
      loading: true,
      loadingMoreText: '加载中...'
    });
    
    // 如果是刷新列表，重置页码
    if (refresh) {
      this.setData({
        currentPage: 1,
        users: [],
        hasMore: true
      });
    }
    
    // 构建请求参数
    const params = {
      name: searchKeyword,
      page: refresh ? 1 : currentPage,
      pageSize
    };
    
    // 如果选择了特定角色，添加到请求参数
    if (activeTag && activeTag !== 'all') {
      params.roleId = this.getRoleIdByTag(activeTag);
    }
    
    // 如果选择了特定部门，添加到请求参数
    if (this.data.selectedDepartment) {
      params.departmentId = this.data.selectedDepartment;
    }
    
    // 请求用户列表 - 使用微信端API
          http.get('/wx/user/list', params)
      .then(res => {
        console.log('获取用户列表成功:', res);
        console.log('原始用户数据示例:', res.data.records && res.data.records[0]);
        const newUsers = this.formatUserData(res.data.records || []);
        console.log('格式化后的用户数据示例:', newUsers[0]);
        const total = res.data.total || 0;
        
        // 更新用户列表
        this.setData({
          users: refresh ? newUsers : [...this.data.users, ...newUsers],
          hasMore: newUsers.length === pageSize && (this.data.users.length + newUsers.length < total),
          currentPage: refresh ? 2 : currentPage + 1,
          total: total,
          loadingMoreText: newUsers.length < pageSize ? '没有更多数据了' : '上拉加载更多'
        }, () => {
          console.log('更新后的用户列表长度:', this.data.users.length);
          console.log('更新后的用户列表:', this.data.users);
          // 添加列表加载动画
          if (refresh && this.data.animationsEnabled) {
            console.log('使用动画加载列表项');
            this.animateListItems();
          } else {
            console.log('不使用动画，直接设置列表项可见');
            this.makeAllItemsVisible();
          }
        });
      })
      .catch(err => {
        console.error('获取用户列表失败:', err);
        
        // 显示错误提示
        wx.showToast({
          title: '加载用户失败，请重试',
          icon: 'none'
        });
        
        this.setData({
          users: [], // 确保清空用户列表
          loading: false,
          hasMore: false,
          loadingMoreText: '加载失败，点击重试'
        });
      })
      .finally(() => {
        this.setData({
          loading: false
        });
        
        if (refresh) {
          wx.stopPullDownRefresh();
        }
      });
  },
  
  /**
   * 直接设置所有列表项可见（不使用动画）
   */
  makeAllItemsVisible() {
    // 如果没有用户数据，直接返回
    if (!this.data.users || this.data.users.length === 0) {
      console.log('用户列表为空，跳过动画设置');
      return;
    }
    
    // 创建一个动画数据对象
    const animationData = {};
    
    // 为每个用户项设置初始可见状态
    this.data.users.forEach((_, index) => {
      animationData[index] = null;
    });
    
    // 更新界面，使所有用户卡片可见
    this.setData({
      animationData
    });
  },

  /**
   * 将后端返回的用户数据格式化为前端需要的格式
   */
  formatUserData(users) {
    console.log('开始格式化用户数据，原始数据:', users);
    
    return users.filter(user => {
      // 过滤掉无效的用户数据
      return user && user.userId && (user.name || user.userName);
    }).map(user => {
      console.log('处理用户数据:', user);
      
      // 获取用户角色ID和名称
      const roleId = parseInt(user.roleId);
      let roleName;
      
      switch(roleId) {
        case 0:
          roleName = '访客';
          break;
        case 1:
          roleName = '学员';
          break;
        case 2:
          roleName = '讲师';
          break;
        case 3:
          roleName = '管理员';
          break;
        default:
          roleName = user.roleName || '其他';
      }
      
      // 处理头像路径，添加BASE_URL前缀（如果需要）
      let avatar = user.avatar;
      if (avatar && !avatar.startsWith('http')) {
        avatar = `${BASE_URL}/wx/file/view/${avatar}`;
      }
      
      // 处理部门信息
      const department = user.departmentName || user.department || (user.roleId === 1 ? '未分配' : null);
      
      // 处理宿舍信息（仅学生显示）
      const dormitory = user.dormitory || null;
      
      // 处理职位信息
      let position = user.position || user.positionName;
      
      // 如果没有职位信息，但有positionId，根据positionId设置职位
      if (!position && (user.positionId !== undefined && user.positionId !== null)) {
        switch (parseInt(user.positionId)) {
          case 0:
            position = '访客';
            break;
          case 1:
            position = '普通学员';
            break;
          case 3:
            position = '部长';
            break;
          case 4:
            position = '副部长';
            break;
          case 5:
            position = '老师';
            break;
          case 6:
            position = '主任';
            break;
          case 7:
            position = '副主任';
            break;
          case 8:
            position = '超级管理员';
            break;
        }
      }
      
      // 如果仍然没有职位信息，根据角色设置默认职位
      if (!position) {
        switch(roleId) {
          case 0: // 访客
            position = '访客';
            break;
          case 1: // 学员
            position = '普通学员';
            break;
          case 2: // 教师
            position = '老师';
            break;
          case 3: // 管理员
            position = '管理员';
            break;
          default:
            position = '成员';
        }
      }
      
      const formattedUser = {
        id: user.userId,
        name: user.name || user.userName || '未命名用户',
        roleId: roleId,
        roleName: roleName,
        department: department,
        position: position,
        positionName: position, // 兼容两种属性名
        positionId: user.positionId, // 保存职位ID以便后续使用
        avatar: avatar,
        phone: user.phone,
        email: user.email,
        dormitory: dormitory, // 添加宿舍信息
        status: user.status !== undefined ? user.status : 1, // 用户状态：1启用，0禁用
        isOnline: user.isOnline || false, // 🔥 添加在线状态字段
        // 保存原始数据，以便在详情页展示
        originalData: user
      };
      
      console.log('格式化后的用户数据:', formattedUser);
      return formattedUser;
    });
  },

  /**
   * 根据标签获取对应的roleId
   */
  getRoleIdByTag(tag) {
    switch (tag) {
      case 'visitor': return 0;
      case 'student': return 1;
      case 'teacher': return 2;
      case 'manager': return 3;
      default: return null;
    }
  },

  /**
   * 加载用户统计数据
   */
  loadUserStats() {
    // 请求用户统计 - 使用微信端API
          http.get('/wx/user/stats')
      .then(res => {
        const stats = res.data || {
          total: 0,
          active: 0,
          departments: 0
        };
        
        // 如果启用动画，则使用动画效果，否则直接设置数值
        if (this.data.animationsEnabled) {
          this.setData({ userStats: stats }, () => {
            this.animateNumbers();
          });
        } else {
          this.setData({ userStats: stats });
        }
      })
      .catch(err => {
        console.error('获取用户统计失败:', err);
        
        // 显示错误提示
        wx.showToast({
          title: '获取统计数据失败',
          icon: 'none'
        });
        
        // 设置空统计数据
        const stats = {
          total: 0,
          active: 0,
          departments: 0
        };
        
          this.setData({ userStats: stats });
      });
      
    // 加载培训方向列表
    this.loadTrainingDirections();
  },

  /**
   * 加载部门列表
   */
  loadDepartments() {
    http.get('/wx/user/departments')
      .then(res => {
        console.log('获取部门列表成功:', res);
        const departments = res.data || [];
        this.setData({ departments });
      })
      .catch(err => {
        console.error('获取部门列表失败:', err);
        this.setData({ departments: [] });
      });
  },

  /**
   * 加载培训方向列表
   */
  loadTrainingDirections() {
            http.get('/wx/user/training-directions')
      .then(res => {
        if (res.code === 200 && Array.isArray(res.data)) {
          this.setData({
            trainingDirections: res.data
          });
        }
      })
      .catch(err => {
        console.error('获取培训方向列表失败:', err);
      });
  },

  /**
   * 搜索框获取焦点
   */
  onSearchFocus() {
    this.setData({
      searchFocused: true
    });
  },

  /**
   * 搜索框失去焦点
   */
  onSearchBlur() {
    this.setData({
      searchFocused: false
    });
  },

  /**
   * 搜索用户
   */
  onSearch(e) {
    this.setData({
      searchKeyword: e.detail.value,
      users: [],
      currentPage: 1,
      hasMore: true
    });
    
    // 延迟搜索，避免频繁请求
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    
    this.searchTimer = setTimeout(() => {
      this.loadUsers(true);
    }, 500);
  },

  /**
   * 清空搜索
   */
  clearSearch() {
    this.setData({
      searchKeyword: '',
      users: [],
      currentPage: 1,
      hasMore: true
    });
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    this.loadUsers(true);
  },

  /**
   * 按标签筛选
   */
  filterByTag(e) {
    const tag = e.currentTarget.dataset.tag;
    
    // 如果选择的是非学生角色，清除部门筛选（因为只有学生有部门）
    let updateData = {
      activeTag: tag,
      users: [],
      currentPage: 1,
      hasMore: true
    };
    
    if (tag !== 'all' && tag !== 'student') {
      updateData.selectedDepartment = ''; // 清除部门筛选
    }
    
    this.setData(updateData);
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    this.loadUsers(true);
  },

  /**
   * 按部门筛选
   */
  filterByDepartment(e) {
    const departmentId = e.currentTarget.dataset.department;
    
    this.setData({
      selectedDepartment: departmentId,
      users: [],
      currentPage: 1,
      hasMore: true
    });
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    this.loadUsers(true);
  },

  /**
   * 查看用户详情
   */
  viewUserDetail(e) {
    const id = e.currentTarget.dataset.id;
    const user = e.currentTarget.dataset.user || this.data.users.find(u => u.id === id);
    
    if (!user) {
      wx.showToast({
        title: '用户信息不存在',
        icon: 'none'
      });
      return;
    }
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    // 获取用户详细信息
    this.loadUserDetail(id, user);
  },

  /**
   * 加载用户详细信息
   */
  loadUserDetail(userId, basicUser) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });
    
    // 请求用户详情 - 使用微信端API
          http.get('/wx/user/detail', { userId })
      .then(res => {
        if (res.code === 200 && res.data) {
          // 处理用户详情中的头像
          if (res.data.avatar && !res.data.avatar.startsWith('http')) {
            res.data.avatar = `${BASE_URL}/wx/file/view/${res.data.avatar}`;
          }

          // 处理部门字段 - 将departmentName映射到department
          if (res.data.departmentName) {
            res.data.department = res.data.departmentName;
          } else if (res.data.roleId === 1) {
            // 学生角色但没有部门信息时显示"未分配"
            res.data.department = '未分配';
          }

          // 处理培训方向 - 确保数组格式
          if (res.data.directionIds && (res.data.roleId === 1 || res.data.roleId === 2)) {
            // 只对学员和讲师处理培训方向
            this.mapDirectionIdsToNames(res.data);
          }
          
          // 处理职位信息 - 根据positionId设置职位名称
          this.processPositionInfo(res.data);
          
          // 处理用户状态 - 确保有status字段
          if (res.data.status === undefined) {
            res.data.status = 1; // 默认为启用状态
          }
          
          // 打开弹窗显示用户详情
          this.setData({
            currentUser: res.data
          }, () => {
            this.showDetailModal();
          });
        } else {
          // 如果API请求成功但没有数据，使用列表中的基本信息
          const userData = basicUser.originalData || basicUser;
          
          // 确保基本信息中的头像也有正确的路径
          if (userData.avatar && !userData.avatar.startsWith('http')) {
            userData.avatar = `${BASE_URL}/wx/file/view/${userData.avatar}`;
          }
          
          // 处理部门字段 - 将departmentName映射到department（如果存在）
          if (userData.departmentName) {
            userData.department = userData.departmentName;
          } else if (!userData.department && userData.roleId === 1) {
            // 学生角色但没有部门信息时显示"未分配"
            userData.department = '未分配';
          }
          
          // 处理基本信息中的培训方向
          if (userData.directionIds && (userData.roleId === 1 || userData.roleId === 2)) {
            // 只对学员和讲师处理培训方向
            this.mapDirectionIdsToNames(userData);
          }
          
          // 处理职位信息 - 根据positionId设置职位名称
          this.processPositionInfo(userData);
          
          // 处理用户状态 - 确保有status字段
          if (userData.status === undefined) {
            userData.status = 1; // 默认为启用状态
          }
          
          this.setData({
            currentUser: userData
          }, () => {
            this.showDetailModal();
          });
        }
      })
      .catch(err => {
        console.error('获取用户详情失败:', err);
        
        // 显示错误提示
        wx.showToast({
          title: '获取用户详情失败',
          icon: 'none'
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  /**
   * 处理职位信息 - 根据positionId设置职位名称
   * @param {Object} userData 用户数据
   */
  processPositionInfo(userData) {
    // 如果已经有职位名称，不做处理
    if (userData.positionName) {
      return;
    }
    
    // 根据positionId设置职位名称
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
          // 如果positionId不在预定义范围内，则使用默认职位
          this.setDefaultPosition(userData);
      }
    } else {
      // 没有positionId，使用默认职位
      this.setDefaultPosition(userData);
    }
    
    // 确保position属性也被设置，保持一致性
    userData.position = userData.positionName;
  },

  /**
   * 将用户的培训方向ID映射为方向名称
   * @param {Object} userData 用户数据
   */
  mapDirectionIdsToNames(userData) {
    // 如果是访客或管理员角色，直接跳过处理
    if (userData.roleId === 0 || userData.roleId === 3) {
      return;
    }
    
    // 确保培训方向数据已加载
    if (!this.data.trainingDirections || this.data.trainingDirections.length === 0) {
      // 如果培训方向未加载，先加载
      http.get('/wx/user/training-directions')
        .then(res => {
          if (res.code === 200 && Array.isArray(res.data)) {
            this.setData({
              trainingDirections: res.data
            });
            // 加载完成后再映射
            this.processDirectionMapping(userData);
          }
        })
        .catch(err => {
          console.error('获取培训方向列表失败:', err);
        });
    } else {
      // 培训方向已加载，直接映射
      this.processDirectionMapping(userData);
    }
  },

  /**
   * 处理培训方向映射
   * @param {Object} userData 用户数据
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
    
    // 如果已经在显示用户详情，更新界面
    if (this.data.currentUser && this.data.currentUser.userId === userData.userId) {
      this.setData({
        'currentUser.directionIdNames': directionNames
      });
    }
  },

  /**
   * 设置默认职位名称
   * @param {Object} userData 用户数据
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
   * @param {Object} userData 用户数据
   */
  setPositionByRole(userData) {
    const roleId = parseInt(userData.roleId);
    
    switch(roleId) {
      case 0: // 访客
        userData.positionName = '访客';
        break;
      case 1: // 学员
        userData.positionName = '普通学员';
        break;
      case 2: // 教师
        userData.positionName = '老师';
        break;
      case 3: // 管理员
        userData.positionName = '管理员';
        break;
      default:
        userData.positionName = '成员';
    }
  },

  /**
   * 显示详情弹窗，不使用动画
   */
  showDetailModal() {
    // 设置弹窗可见
    this.setData({
      showDetailModal: true
    });
  },

  /**
   * 关闭详情弹窗，不使用动画
   */
  closeDetailModal() {
    // 直接关闭弹窗，无动画
    this.setData({
      showDetailModal: false
    });
  },

  /**
   * 阻止弹窗内容滑动穿透
   */
  preventTouchMove() {
    // 不再阻止滚动，允许内容滚动
    // 只有在点击模态框外部时才会关闭
    return;
  },

  /**
   * 点击底部加载更多
   */
  loadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadUsers();
    }
  },

  /**
   * 查看用户荣誉证书（适用于所有角色）
   */
  viewUserAchievements(e) {
    const userId = this.data.currentUser.userId;
    const userName = this.data.currentUser.name;
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    wx.navigateTo({
      url: `/subpackages/user-related/pages/achievements/index?userId=${userId}&userName=${userName}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 切换用户状态（启用/禁用）
   */
  toggleUserStatus(e) {
    const currentUser = this.data.currentUser;
    
    // 添加调试信息
    console.log('当前用户信息:', currentUser);
    console.log('当前用户状态:', currentUser.status);
    console.log('状态类型:', typeof currentUser.status);
    
    // 修复：将status转换为数字进行比较
    const currentStatus = parseInt(currentUser.status);
    const newStatus = currentStatus === 1 ? 0 : 1;
    const statusText = currentStatus === 1 ? '禁用' : '启用';
    
    console.log('转换后的当前状态:', currentStatus);
    console.log('新状态:', newStatus);
    console.log('操作文本:', statusText);
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    wx.showModal({
      title: '确认操作',
      content: `确定要${statusText}用户"${currentUser.name}"吗？`,
      confirmText: '确定',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.updateUserStatus(currentUser.userId, newStatus);
        }
      }
    });
  },

  /**
   * 更新用户状态
   */
  updateUserStatus(userId, status) {
    wx.showLoading({
      title: '更新中...',
      mask: true
    });

    // 调用微信端API更新用户状态
    http.post('/wx/user/updateStatus', {
      userId: userId,
      status: status
    })
    .then(res => {
      if (res.code === 200) {
        // 更新当前用户详情中的状态
        this.setData({
          'currentUser.status': status
        });
        
        // 更新用户列表中对应用户的状态
        const users = this.data.users;
        const userIndex = users.findIndex(user => user.id === userId);
        if (userIndex !== -1) {
          this.setData({
            [`users[${userIndex}].status`]: status
          });
        }
        
        wx.showToast({
          title: status === 1 ? '用户已启用' : '用户已禁用',
          icon: 'success'
        });
      } else {
        throw new Error(res.message || '操作失败');
      }
    })
    .catch(err => {
      console.error('更新用户状态失败:', err);
      wx.showToast({
        title: '操作失败，请重试',
        icon: 'none'
      });
    })
    .finally(() => {
      wx.hideLoading();
    });
  },

  /**
   * 查看学生课程
   */
  viewStudentCourses(e) {
    const userId = this.data.currentUser.userId;
    const userName = this.data.currentUser.name;
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    wx.navigateTo({
      url: `/subpackages/course-related/pages/student/index?userId=${userId}&userName=${userName}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 查看老师授课安排
   */
  viewTeachingCourses(e) {
    const userId = this.data.currentUser.userId;
    const userName = this.data.currentUser.name;
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    wx.navigateTo({
      url: `/subpackages/course-related/pages/teaching/index?userId=${userId}&userName=${userName}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 查看管理员部门成员
   */
  viewDepartmentMembers(e) {
    const department = this.data.currentUser.department || this.data.currentUser.positionName;
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    if (!department) {
      wx.showToast({
        title: '未找到部门信息',
        icon: 'none'
      });
      return;
    }
    
    wx.navigateTo({
      url: `/pages/user/index?department=${encodeURIComponent(department)}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 查看用户考勤记录
   */
  viewUserAttendance(e) {
    const userId = this.data.currentUser.userId;
    const userName = this.data.currentUser.name;
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    wx.navigateTo({
      url: `/pages/attendance/records/index?userId=${userId}&userName=${userName}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 编辑用户（管理员）
   */
  editUser(e) {
    // 添加波纹效果
    this.addRippleEffect(e);
    
    if (!this.data.isAdmin) {
      wx.showToast({
        title: '权限不足',
        icon: 'none'
      });
      return;
    }
    
    const userId = this.data.currentUser.userId;
    wx.navigateTo({
      url: `/pages/user/edit?id=${userId}`
    });
    
    this.closeDetailModal();
  },

  /**
   * 新增用户（管理员）
   */
  addUser(e) {
    // 添加波纹效果
    this.addRippleEffect(e);
    
    if (!this.data.isAdmin) {
      wx.showToast({
        title: '权限不足',
        icon: 'none'
      });
      return;
    }
    
    wx.navigateTo({
      url: '/pages/user/edit'
    });
  },

  /**
   * 发送消息给用户
   */
  sendMessage(e) {
    // 阻止事件冒泡，避免触发列表项点击事件
    e.stopPropagation();
    
    const userId = e.currentTarget.dataset.id;
    const userInfo = this.data.users.find(user => user.id === userId);
    
    if (!userInfo) {
      wx.showToast({
        title: '用户信息不存在',
        icon: 'none'
      });
      return;
    }
    
    // 添加波纹效果
    this.addRippleEffect(e);
    
    // 跳转到消息发送页面或打开消息对话框
    wx.navigateTo({
      url: `/pages/message/index?targetId=${userId}&targetName=${userInfo.name}`
    });
  },

  /**
   * 波纹效果已禁用，提高性能
   */
  addRippleEffect(e) {
    // 已禁用波纹效果，提高性能
    return;
  },

  /**
   * 数字增长动画 - 简化版本
   */
  animateNumbers() {
    // 直接设置最终值，不做动画
    const { total, active, departments } = this.data.userStats;
    
    this.setData({
      'userStats.total': total,
      'userStats.active': active,
      'userStats.departments': departments
    });
  },

  /**
   * 列表项加载 - 不使用动画
   */
  animateListItems() {
    // 直接设置所有项可见，不使用动画
    const animationData = {};
    
    // 为每个用户项设置初始可见状态
    if (this.data.users && this.data.users.length) {
      this.data.users.forEach((_, index) => {
        animationData[index] = null;
      });
    }
    
    // 更新界面，使所有用户卡片可见
    this.setData({
      animationData
    });
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadUsers(true);
    this.loadUserStats();
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadUsers();
    }
  },

  /**
   * 分享页面
   */
  onShareAppMessage() {
    return {
      title: '何湘技能大师工作室 - 成员管理',
      path: '/pages/user/index'
    };
  }
});