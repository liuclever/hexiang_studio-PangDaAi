// pages/attendance/activity/index.js
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
    
    // 活动列表
    activities: [],
    filteredActivities: [],
    
    // 页面状态
    loading: false,
    
    // 分页参数
    page: 1,
    pageSize: 20,
    hasMore: true,
    
    // 创建表单弹窗
    showCreateForm: false,
    submitting: false,
    createForm: {
      name: '',
      startTime: '',
      endTime: '',
      startTimeDisplay: '',
      endTimeDisplay: '',
      location: '',
      locationDisplay: '',
      locationIndex: 0,
      locationLat: '',
      locationLng: '',
      radius: '100',
      status: 1,
      statusIndex: 0,
      note: '',
      selectedStudents: [] // 已选择的学生列表
    },
    
    // 时间选择器数据
    dateTimeRange: [[], [], [], []], // 年、月、日、时分
    startTimeIndex: [0, 0, 0, 0],
    endTimeIndex: [0, 0, 0, 0],
    
    // 常用地点列表
    commonLocations: [],
    
    // 状态选项
    statusOptions: [
      { label: '有效', value: 1 },
      { label: '已取消', value: 0 }
    ],
    
    // 学生选择器相关
    showStudentSelector: false,
    allStudents: [], // 所有学生列表
    filteredStudents: [], // 筛选后的学生列表
    tempSelectedStudents: [], // 临时选择的学生（在弹窗中）
    studentSearchKeyword: '', // 搜索关键词
    studentsLoading: false,
    studentsPage: 1,
    studentsHasMore: true
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
    
    // 加载活动列表
    this.loadActivities();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 刷新活动列表
    if (this.data.userInfo) {
      this.loadActivities();
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.resetAndLoadActivities().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreActivities();
    }
  },

  /**
   * 重置并加载活动列表
   */
  async resetAndLoadActivities() {
    this.setData({
      page: 1,
      hasMore: true,
      activities: []
    });
    await this.loadActivities();
  },

  /**
   * 加载活动列表
   */
  async loadActivities() {
    if (this.data.loading) return;
    
    this.setData({ loading: true });
    
    try {
      const response = await http.get('/wx/attendance/plans', {
        type: 'activity',
        page: this.data.page,
        pageSize: this.data.pageSize
      });
      
      if (response.code === 200) {
        const newActivities = this.processActivities(response.data.records || []);
        
        this.setData({
          activities: this.data.page === 1 ? newActivities : [...this.data.activities, ...newActivities],
          hasMore: newActivities.length === this.data.pageSize
        });
        
        // 应用筛选
        this.applyFilter();
      }
    } catch (error) {
      console.error('加载活动列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 加载更多活动
   */
  async loadMoreActivities() {
    this.setData({
      page: this.data.page + 1
    });
    await this.loadActivities();
  },

  /**
   * 处理活动数据
   */
  processActivities(activities) {
    const now = new Date();
    
    return activities.map(activity => {
      // 字段名转换：后端下划线 -> 前端驼峰
      const convertedActivity = this.convertFieldNames(activity);
      
      const startTime = new Date(convertedActivity.startTime);
      const endTime = new Date(convertedActivity.endTime);
      
      // 判断活动状态（仅用于管理展示）
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
      
      return {
        ...convertedActivity,
        status,
        statusText
      };
    });
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
    let filteredActivities = this.data.activities;
    
    if (this.data.filter !== 'all') {
      filteredActivities = this.data.activities.filter(activity => 
        activity.status === this.data.filter
      );
    }
    
    this.setData({ filteredActivities });
  },

  /**
   * 获取筛选文本
   */
  getFilterText(filter) {
    switch(filter) {
      case 'ongoing': return '进行中的';
      case 'upcoming': return '即将开始的';
      case 'ended': return '已结束的';
      default: return '';
    }
  },

  /**
   * 显示创建表单弹窗
   */
  async showCreateForm() {
    if (this.data.userRole !== 'admin') {
      wx.showToast({
        title: '仅管理员可创建',
        icon: 'none'
      });
      return;
    }
    
    // 初始化时间选择器数据
    this.initDateTimeRange();
    
    // 获取常用地点列表
    await this.loadCommonLocations();
    
    // 初始化默认时间（当前时间和1小时后）
    this.initDefaultTimes();
    
    this.setData({
      showCreateForm: true
    });
  },

  /**
   * 隐藏创建表单弹窗
   */
  hideCreateForm() {
    this.setData({
      showCreateForm: false,
      createForm: {
        name: '',
        startTime: '',
        endTime: '',
        startTimeDisplay: '',
        endTimeDisplay: '',
        location: '',
        locationDisplay: '',
        locationIndex: 0,
        locationLat: '',
        locationLng: '',
        radius: '100',
        status: 1,
        statusIndex: 0,
        note: '',
        selectedStudents: [] // 重置选择的学生
      },
      // 重置学生选择器相关状态
      showStudentSelector: false,
      tempSelectedStudents: [],
      studentSearchKeyword: '',
      allStudents: [],
      filteredStudents: []
    });
  },

  /**
   * 阻止事件冒泡
   */
  preventClose() {
    // 空方法，用于阻止事件冒泡
  },

  /**
   * 初始化默认时间（当前时间和1小时后）
   */
  initDefaultTimes() {
    const now = new Date();
    const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
    
    const formatDateTime = (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hour}:${minute}`;
    };

    const startTimeDisplay = formatDateTime(now);
    const endTimeDisplay = formatDateTime(oneHourLater);
    
    // 计算时间选择器索引
    const getTimePickerIndex = (date) => {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = date.getHours();
      const minute = Math.floor(date.getMinutes() / 30) * 30;

      const yearIndex = this.data.dateTimeRange[0] ? this.data.dateTimeRange[0].findIndex(y => parseInt(y) === year) : 0;
      const monthIndex = month - 1;
      const dayIndex = day - 1;
      const timeIndex = hour * 2 + (minute === 30 ? 1 : 0);

      return [
        yearIndex >= 0 ? yearIndex : 0,
        Math.max(0, Math.min(monthIndex, 11)),
        Math.max(0, Math.min(dayIndex, 30)),
        Math.max(0, Math.min(timeIndex, 47))
      ];
    };

    setTimeout(() => {
      const startTimeIndex = getTimePickerIndex(now);
      const endTimeIndex = getTimePickerIndex(oneHourLater);
      
      this.setData({
        'createForm.startTime': startTimeDisplay,
        'createForm.endTime': endTimeDisplay,
        'createForm.startTimeDisplay': startTimeDisplay,
        'createForm.endTimeDisplay': endTimeDisplay,
        startTimeIndex: startTimeIndex,
        endTimeIndex: endTimeIndex
      });
    }, 100);
  },

  /**
   * 获取常用地点列表
   */
  async loadCommonLocations() {
    try {
      const response = await http.get('/admin/locations');
      
      if (response.code === 200 && response.data && response.data.length > 0) {
        const limitedLocations = response.data.slice(0, 8);
        // 初始化时也要对经纬度进行四舍五入
        const firstLocation = limitedLocations[0];
        const lat = firstLocation?.lat ? parseFloat(firstLocation.lat).toFixed(6) : '';
        const lng = firstLocation?.lng ? parseFloat(firstLocation.lng).toFixed(6) : '';
        
        this.setData({
          commonLocations: limitedLocations,
          'createForm.locationDisplay': firstLocation?.name || '',
          'createForm.location': firstLocation?.name || '',
          'createForm.locationLat': lat,
          'createForm.locationLng': lng
        });
      } else {
        wx.showToast({
          title: '请先配置常用地点',
          icon: 'none'
        });
        this.setData({
          commonLocations: []
        });
      }
    } catch (error) {
      console.error('[获取常用地点] 失败:', error);
      wx.showToast({
        title: '获取地点列表失败',
        icon: 'error'
      });
      this.setData({
        commonLocations: []
      });
    }
  },

  /**
   * 初始化时间选择器数据
   */
  initDateTimeRange() {
    const now = new Date();
    const currentYear = now.getFullYear();
    
    // 年份范围：当前年份前后2年
    const years = [];
    for (let i = currentYear - 2; i <= currentYear + 2; i++) {
      years.push(i.toString());
    }
    
    // 月份
    const months = [];
    for (let i = 1; i <= 12; i++) {
      months.push(i < 10 ? `0${i}` : i.toString());
    }
    
    // 日期
    const days = [];
    for (let i = 1; i <= 31; i++) {
      days.push(i < 10 ? `0${i}` : i.toString());
    }
    
    // 时间（每30分钟一个选项）
    const times = [];
    for (let h = 0; h < 24; h++) {
      for (let m = 0; m < 60; m += 30) {
        const hour = h < 10 ? `0${h}` : h.toString();
        const minute = m < 10 ? `0${m}` : m.toString();
        times.push(`${hour}:${minute}`);
      }
    }
    
    this.setData({
      dateTimeRange: [years, months, days, times]
    });
  },

  /**
   * 创建表单输入处理
   */
  onCreateNameInput(e) {
    this.setData({
      'createForm.name': e.detail.value
    });
  },

  onCreateRadiusInput(e) {
    this.setData({
      'createForm.radius': e.detail.value
    });
  },

  onCreateNoteInput(e) {
    this.setData({
      'createForm.note': e.detail.value
    });
  },

  onCreateLocationChange(e) {
    const index = e.detail.value;
    const selectedLocation = this.data.commonLocations[index];
    
    if (selectedLocation) {
      // 经纬度保留6位小数，四舍五入
      const lat = parseFloat(selectedLocation.lat).toFixed(6);
      const lng = parseFloat(selectedLocation.lng).toFixed(6);
      
      this.setData({
        'createForm.locationIndex': index,
        'createForm.location': selectedLocation.name,
        'createForm.locationDisplay': selectedLocation.name,
        'createForm.locationLat': lat,
        'createForm.locationLng': lng
      });
    }
  },

  onCreateStartTimeChange(e) {
    const values = e.detail.value;
    const ranges = this.data.dateTimeRange;
    const year = ranges[0][values[0]];
    const month = ranges[1][values[1]];
    const day = ranges[2][values[2]];
    const time = ranges[3][values[3]];
    
    const dateTimeStr = `${year}-${month}-${day} ${time}`;
    this.setData({
      startTimeIndex: values,
      'createForm.startTime': dateTimeStr,
      'createForm.startTimeDisplay': dateTimeStr
    });
  },

  onCreateEndTimeChange(e) {
    const values = e.detail.value;
    const ranges = this.data.dateTimeRange;
    const year = ranges[0][values[0]];
    const month = ranges[1][values[1]];
    const day = ranges[2][values[2]];
    const time = ranges[3][values[3]];
    
    const dateTimeStr = `${year}-${month}-${day} ${time}`;
    this.setData({
      endTimeIndex: values,
      'createForm.endTime': dateTimeStr,
      'createForm.endTimeDisplay': dateTimeStr
    });
  },

  onCreateStatusChange(e) {
    const index = e.detail.value;
    this.setData({
      'createForm.statusIndex': index,
      'createForm.status': this.data.statusOptions[index].value
    });
  },

  /**
   * 提交创建
   */
  async submitCreate() {
    const { createForm } = this.data;
    
    // 表单验证
    if (!createForm.name.trim()) {
      wx.showToast({
        title: '请输入考勤名称',
        icon: 'error'
      });
      return;
    }
    
    if (!createForm.startTime) {
      wx.showToast({
        title: '请选择开始时间',
        icon: 'error'
      });
      return;
    }
    
    if (!createForm.endTime) {
      wx.showToast({
        title: '请选择结束时间',
        icon: 'error'
      });
      return;
    }
    
    if (!createForm.location.trim()) {
      wx.showToast({
        title: '请选择活动地点',
        icon: 'error'
      });
      return;
    }

    if (!createForm.selectedStudents || createForm.selectedStudents.length === 0) {
      wx.showToast({
        title: '请选择参与学生',
        icon: 'error'
      });
      return;
    }

    // 验证时间
    const now = new Date();
    const startTime = new Date(createForm.startTime.replace(/-/g, '/'));
    const endTime = new Date(createForm.endTime.replace(/-/g, '/'));
    
    if (endTime <= startTime) {
      wx.showModal({
        title: '时间设置错误',
        content: '活动结束时间必须晚于开始时间，请重新设置。',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }
    
    // 验证开始时间不能是过去时间
    if (startTime <= now) {
      wx.showModal({
        title: '时间设置错误',
        content: '活动开始时间必须是未来时间，请重新选择。',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }
    
    // 验证开始时间不能太久远（比如超过1年）
    const oneYearLater = new Date();
    oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
    if (startTime > oneYearLater) {
      wx.showModal({
        title: '时间设置错误', 
        content: '活动开始时间不能超过一年，请重新选择合理的时间。',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }

    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: '创建中...' });
      
      // 格式化时间为后端期望的格式（兼容iOS）
      const formatTimeForBackend = (timeStr) => {
        if (!timeStr) return null;
        
        // 兼容iOS的时间格式处理
        let date;
        try {
          // 将 "YYYY-MM-DD HH:MM" 格式转换为 "YYYY/MM/DD HH:MM:SS" 格式（iOS兼容）
          if (typeof timeStr === 'string' && timeStr.includes('-')) {
            const isoStr = timeStr.replace(/(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})/, '$1/$2/$3 $4:$5:00');
            date = new Date(isoStr);
          } else {
            date = new Date(timeStr);
          }
          
          // 检查是否是有效日期
          if (isNaN(date.getTime())) {
            console.error('无效的时间格式:', timeStr);
            return null;
          }
          
          const year = date.getFullYear();
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          const hour = String(date.getHours()).padStart(2, '0');
          const minute = String(date.getMinutes()).padStart(2, '0');
          const second = String(date.getSeconds()).padStart(2, '0');
          return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
        } catch (error) {
          console.error('时间格式化失败:', timeStr, error);
          return null;
        }
      };
      
      // 准备提交数据
      const createData = {
        name: createForm.name.trim(),
        type: 'activity',
        startTime: formatTimeForBackend(createForm.startTime),
        endTime: formatTimeForBackend(createForm.endTime),
        location: createForm.location.trim(),
        locationLat: parseFloat(createForm.locationLat) || 0,
        locationLng: parseFloat(createForm.locationLng) || 0,
        radius: parseInt(createForm.radius) || 100,
        status: parseInt(createForm.status) || 1,
        note: createForm.note.trim()
      };
      
      // 调用创建接口
      const response = await http.post('/wx/attendance/plan', createData);
      
      if (response.code === 200) {
        const planResult = response.data;
        const planId = planResult.planId || planResult.plan_id;
        
        if (planId && createForm.selectedStudents.length > 0) {
          // 创建学生预约记录
          try {
            const reservationData = {
              planId: planId,
              studentIds: createForm.selectedStudents.map(s => s.id),
              remark: '创建活动时批量预约'
            };
            
            const reservationResponse = await http.post('/wx/attendance/reservation/batch', reservationData);
            
            if (reservationResponse.code === 200) {
              const result = reservationResponse.data;
              // 弹出成功提示
              wx.showToast({
                title: '创建成功',
                icon: 'success',
                duration: 2000
              });
            } else {
              // 即使预约创建失败，活动已创建成功，只需提示
              
              // 根据具体错误给出不同提示
              let errorTitle = '创建成功但预约失败';
              if (result && result.message) {
                if (result.message.includes('活动已开始')) {
                  errorTitle = '活动时间有误，请检查开始时间';
                } else if (result.message.includes('学生不存在')) {
                  errorTitle = '选择的学生信息有误';
                } else {
                  errorTitle += `：${result.message}`;
                }
              }
              
              wx.showModal({
                title: '温馨提示',
                content: `活动创建成功，但学生预约失败：${errorTitle}。\n\n您可以稍后在活动详情中手动添加学生。`,
                showCancel: false,
                confirmText: '我知道了'
              });
            }
          } catch (reservationError) {
            console.error('[创建预约] 失败:', reservationError);
            
            // 分析具体的错误原因
            let errorMsg = '网络错误或服务器异常';
            if (reservationError && reservationError.msg) {
              if (reservationError.msg.includes('活动已开始')) {
                errorMsg = '活动开始时间不能是过去时间';
              } else {
                errorMsg = reservationError.msg;
              }
            }
            
            wx.showModal({
              title: '创建结果',
              content: `活动考勤计划创建成功！\n\n但学生预约失败：${errorMsg}\n\n建议：请修改活动时间为未来时间，或稍后手动添加学生。`,
              showCancel: false,
              confirmText: '我知道了'
            });
          }
        } else {
          wx.showToast({
            title: '创建成功',
            icon: 'success'
          });
        }
        
        // 关闭弹窗并刷新列表
        this.hideCreateForm();
        setTimeout(() => {
          this.resetAndLoadActivities();
        }, 1500);
      } else {
        throw new Error(response.message || '创建失败');
      }
    } catch (error) {
      console.error('[创建活动考勤] 失败:', error);
      wx.showToast({
        title: error.message || '创建失败',
        icon: 'error'
      });
    } finally {
      this.setData({ submitting: false });
      wx.hideLoading();
    }
  },

  /**
   * 查看活动详情
   */
  viewActivityDetail(e) {
    const activity = e.currentTarget.dataset.activity;
    wx.navigateTo({
      url: `/pages/attendance/activity/detail?planId=${activity.planId}`
    });
  },

  /**
   * 查看签到记录
   */
  viewRecords(e) {
    e.stopPropagation(); // 阻止事件冒泡
    const plan = e.currentTarget.dataset.plan;
    
    wx.navigateTo({
      url: `/pages/attendance/records/index?type=activity&planId=${plan.planId}`
    });
  },

  // ==================== 学生选择器相关方法 ====================

  /**
   * 显示学生选择器
   */
  async showStudentSelector() {
    this.setData({
      showStudentSelector: true,
      tempSelectedStudents: [...this.data.createForm.selectedStudents],
      studentSearchKeyword: ''
    });
    
    // 加载学生列表
    await this.loadStudents();
  },

  /**
   * 隐藏学生选择器
   */
  hideStudentSelector() {
    this.setData({
      showStudentSelector: false
    });
  },

  /**
   * 加载学生列表
   */
  async loadStudents(keyword = '') {
    try {
      this.setData({ studentsLoading: true });
      
      // 构建请求参数，避免发送undefined字符串
      const params = {};
      if (keyword && keyword.trim()) {
        params.keyword = keyword.trim();
      }
      
      const response = await http.get('/wx/user/students', params);
      
      if (response.code === 200) {
        const students = response.data || [];
        this.setData({
          allStudents: students,
          filteredStudents: students
        });
      } else {
        wx.showToast({
          title: '获取学生列表失败',
          icon: 'error'
        });
      }
    } catch (error) {
      console.error('[加载学生列表] 失败:', error);
      wx.showToast({
        title: '网络错误',
        icon: 'error'
      });
    } finally {
      this.setData({ studentsLoading: false });
    }
  },

  /**
   * 搜索学生
   */
  onStudentSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ studentSearchKeyword: keyword });
    
    // 防抖搜索
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.searchStudents(keyword);
    }, 300);
  },

  /**
   * 执行搜索
   */
  async searchStudents(keyword) {
    if (!keyword.trim()) {
      this.setData({
        filteredStudents: this.data.allStudents
      });
      return;
    }
    
    try {
      const response = await http.get('/wx/attendance/students', {
        keyword: keyword.trim()
      });
      
      if (response.code === 200) {
        this.setData({
          filteredStudents: response.data || []
        });
      }
    } catch (error) {
      console.error('[搜索学生] 失败:', error);
    }
  },

  /**
   * 清空搜索
   */
  clearStudentSearch() {
    this.setData({
      studentSearchKeyword: '',
      filteredStudents: this.data.allStudents
    });
  },

  /**
   * 切换学生选择状态
   */
  toggleStudentSelection(e) {
    const student = e.currentTarget.dataset.student;
    if (!student) return;
    
    const { tempSelectedStudents } = this.data;
    const existingIndex = tempSelectedStudents.findIndex(s => s.id === student.id);
    
    if (existingIndex >= 0) {
      // 已选择，移除
      tempSelectedStudents.splice(existingIndex, 1);
    } else {
      // 未选择，添加
      tempSelectedStudents.push(student);
    }
    
    this.setData({ tempSelectedStudents });
  },

  /**
   * 从临时列表移除学生
   */
  removeStudentFromTemp(e) {
    const studentId = e.currentTarget.dataset.id;
    const tempSelectedStudents = this.data.tempSelectedStudents.filter(s => s.id !== studentId);
    this.setData({ tempSelectedStudents });
  },

  /**
   * 全选学生
   */
  selectAllStudents() {
    const { filteredStudents, tempSelectedStudents } = this.data;
    
    // 将当前筛选的学生中未选择的加入临时选择列表
    filteredStudents.forEach(student => {
      const exists = tempSelectedStudents.some(s => s.id === student.id);
      if (!exists) {
        tempSelectedStudents.push(student);
      }
    });
    
    this.setData({ tempSelectedStudents });
  },

  /**
   * 清空所有选择
   */
  clearAllStudents() {
    this.setData({ tempSelectedStudents: [] });
  },

  /**
   * 确认学生选择
   */
  confirmStudentSelection() {
    this.setData({
      'createForm.selectedStudents': this.data.tempSelectedStudents,
      showStudentSelector: false
    });
  },

  /**
   * 从主表单移除学生
   */
  removeStudent(e) {
    const index = e.currentTarget.dataset.index;
    const selectedStudents = [...this.data.createForm.selectedStudents];
    selectedStudents.splice(index, 1);
    
    this.setData({
      'createForm.selectedStudents': selectedStudents
    });
  },

  /**
   * 检查学生是否已选择
   */
  isStudentSelected(studentId) {
    return this.data.tempSelectedStudents.some(s => s.id === studentId);
  },

  /**
   * 防止事件冒泡（用于弹窗）
   */
  preventClose(e) {
    // 阻止事件冒泡，防止关闭弹窗
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation();
    }
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
      // 签到统计字段
      'present_count': 'attendanceCount',
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
  }
}); 