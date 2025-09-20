// pages/attendance/duty/index.js
const { checkLoginStatus, getUserInfo, getUserRole } = require('../../../utils/auth');
const { http } = require('../../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户信息
    userInfo: null,
    userRole: 'student',
    
    // 筛选条件
    filter: 'all',
    
    // 值班列表
    duties: [],
    filteredDuties: [],
    
    // 当前值班
    currentDuty: null,
    
    // 页面状态
    loading: false,
    
    // 分页参数
    page: 1,
    pageSize: 20,
    hasMore: true
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
      userInfo: getUserInfo(),
      userRole: getUserRole()
    });
    
    // 加载值班数据
    this.loadDuties();
    this.checkCurrentDuty();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 刷新值班数据
    if (this.data.userInfo) {
      this.loadDuties();
      this.checkCurrentDuty();
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    Promise.all([
      this.resetAndLoadDuties(),
      this.checkCurrentDuty()
    ]).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreDuties();
    }
  },

  /**
   * 重置并加载值班列表
   */
  async resetAndLoadDuties() {
    this.setData({
      page: 1,
      hasMore: true,
      duties: []
    });
    await this.loadDuties();
  },

  /**
   * 加载值班列表
   */
  async loadDuties() {
    if (this.data.loading) return;
    
    this.setData({ loading: true });
    
    try {
      const response = await http.get('/wx/attendance/plans', {
        type: 'duty',
        page: this.data.page,
        pageSize: this.data.pageSize
      });
      
      if (response.code === 200) {
        const newDuties = this.processDuties(response.data.records || []);
        
        this.setData({
          duties: this.data.page === 1 ? newDuties : [...this.data.duties, ...newDuties],
          hasMore: newDuties.length === this.data.pageSize
        });
        
        // 应用筛选
        this.applyFilter();
      }
    } catch (error) {
      console.error('加载值班列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 加载更多值班
   */
  async loadMoreDuties() {
    this.setData({
      page: this.data.page + 1
    });
    await this.loadDuties();
  },

  /**
   * 检查当前值班
   */
  async checkCurrentDuty() {
    try {
      const response = await http.get('/wx/attendance/current-duty');
      if (response.code === 200 && response.data) {
        const currentDuty = this.processCurrentDuty(response.data);
        this.setData({ currentDuty });
      } else {
        this.setData({ currentDuty: null });
      }
    } catch (error) {
      console.error('检查当前值班失败:', error);
      this.setData({ currentDuty: null });
    }
  },

  /**
   * 处理当前值班数据
   */
  processCurrentDuty(duty) {
    // 字段名转换：后端下划线 -> 前端驼峰
    const convertedDuty = this.convertFieldNames(duty);
    
    const now = new Date();
    const endTime = new Date(convertedDuty.endTime);
    const remainingMs = endTime.getTime() - now.getTime();
    
    let remainingTime = '';
    if (remainingMs > 0) {
      const hours = Math.floor(remainingMs / (1000 * 60 * 60));
      const minutes = Math.floor((remainingMs % (1000 * 60 * 60)) / (1000 * 60));
      
      if (hours > 0) {
        remainingTime = `${hours}小时${minutes}分钟`;
      } else {
        remainingTime = `${minutes}分钟`;
      }
    } else {
      remainingTime = '已结束';
    }
    
    return {
      ...convertedDuty,
      remainingTime
    };
  },

  /**
   * 处理值班数据
   */
  processDuties(duties) {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    return duties.map(duty => {
      // 字段名转换：后端下划线 -> 前端驼峰
      const convertedDuty = this.convertFieldNames(duty);
      
      const startTime = new Date(convertedDuty.startTime);
      const endTime = new Date(convertedDuty.endTime);
      const dutyDate = new Date(startTime.getFullYear(), startTime.getMonth(), startTime.getDate());
      
      // 判断值班状态（仅用于管理展示）
      let status, statusText;
      if (now < startTime) {
        status = 'upcoming';
        statusText = '即将开始';
      } else if (now >= startTime && now <= endTime) {
        status = 'ongoing';
        statusText = '进行中';
      } else {
        status = 'ended';
        statusText = '已结束';
      }
      
      // 判断是否为今日值班
      const isToday = dutyDate.getTime() === today.getTime();
      
      // 计算持续时间
      const durationMs = endTime.getTime() - startTime.getTime();
      const hours = Math.floor(durationMs / (1000 * 60 * 60));
      const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60));
      const duration = `${hours}小时${minutes > 0 ? minutes + '分钟' : ''}`;
      
      return {
        ...convertedDuty,
        status,
        statusText,
        isToday,
        duration
      };
    });
  },



  /**
   * 字段名转换：后端下划线命名 -> 前端驼峰命名
   */
  convertFieldNames(obj) {
    if (!obj) return obj;
    
    const converted = { ...obj };
    
    // 转换常用的下划线字段为驼峰命名
    const fieldMapping = {
      'plan_id': 'planId',
      'start_time': 'startTime',
      'end_time': 'endTime',
      'location_lat': 'locationLat',
      'location_lng': 'locationLng',
      'course_id': 'courseId',
      'create_user': 'createUser',
      'create_time': 'createTime',
      'update_time': 'updateTime',
      'schedule_id': 'scheduleId',
      'update_user': 'updateUser',
      'create_user_name': 'createUserName',
      'attendance_status': 'attendanceStatus',
      'attendance_status_text': 'attendanceStatusText',
      'other_students': 'otherStudents',
      // 签到统计字段
      'present_count': 'attendanceCount',
      'attendance_count': 'attendanceCount',
      'total_students': 'totalStudents',
      'late_count': 'lateCount',
      'absent_count': 'absentCount',
      'leave_count': 'leaveCount',
      'pending_count': 'pendingCount'
    };
    
    // 执行字段映射
    for (const [snakeCase, camelCase] of Object.entries(fieldMapping)) {
      if (converted.hasOwnProperty(snakeCase)) {
        converted[camelCase] = converted[snakeCase];
      }
    }
    
    // 特殊处理：如果有presentCount但没有attendanceCount，直接使用presentCount
    if (converted.presentCount !== undefined && converted.attendanceCount === undefined) {
      converted.attendanceCount = converted.presentCount;
    }
    
    return converted;
  },

  /**
   * 设置筛选条件
   */
  setFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    this.setData({ filter });
    this.applyFilter();
  },

  /**
   * 应用筛选
   */
  applyFilter() {
    let filteredDuties = this.data.duties;
    
    if (this.data.filter !== 'all') {
      filteredDuties = this.data.duties.filter(duty => 
        duty.status === this.data.filter
      );
    }
    
    this.setData({ filteredDuties });
  },



  /**
   * 查看值班详情
   */
  viewDutyDetail(e) {
    const duty = e.currentTarget.dataset.duty;
    wx.navigateTo({
      url: `/pages/attendance/duty/detail?planId=${duty.planId}`
    });
  },

  /**
   * 查看签到记录
   */
  viewRecords(e) {
    // catchtap 已经阻止了事件冒泡，不需要手动调用 stopPropagation
    const plan = e.currentTarget.dataset.plan;
    
    if (!plan.planId) {
      wx.showToast({
        title: '数据异常，请重试',
        icon: 'error'
      });
      return;
    }
    
    // 跳转到值班详情页面
    wx.navigateTo({
      url: `/pages/attendance/duty/detail?planId=${plan.planId}`
    });
  }
}); 