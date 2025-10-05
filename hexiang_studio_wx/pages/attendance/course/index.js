// pages/attendance/course/index.js
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
    
    // 课程列表
    courses: [],
    filteredCourses: [],
    
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
      courseId: '',
      courseIndex: 0,
      courseDisplay: '',
      startTime: '',
      endTime: '',
      startTimeDisplay: '',
      endTimeDisplay: '',
      location: '',
      locationDisplay: '',
      locationIndex: 0,
      locationLat: '',
      locationLng: '',
      radius: '10',
      status: 1,
      statusIndex: 0,
      note: ''
    },
    
    // 时间选择器数据
    dateTimeRange: [[], [], [], []], // 年、月、日、时分
    startTimeIndex: [0, 0, 0, 0],
    endTimeIndex: [0, 0, 0, 0],
    
    // 课程列表（用于创建时选择）
    allCourses: [],
    
    // 常用地点列表
    commonLocations: [],
    
    // 状态选项
    statusOptions: [
      { label: '有效', value: 1 },
      { label: '已取消', value: 0 }
    ]
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
    
    // 加载课程列表
    this.loadCourses();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 刷新课程列表
    if (this.data.userInfo) {
      this.loadCourses();
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.resetAndLoadCourses().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreCourses();
    }
  },

  /**
   * 重置并加载课程列表
   */
  async resetAndLoadCourses() {
    this.setData({
      page: 1,
      hasMore: true,
      courses: []
    });
    await this.loadCourses();
  },

  /**
   * 加载课程列表
   */
  async loadCourses() {
    if (this.data.loading) return;
    
    this.setData({ loading: true });
    
    try {
      const response = await http.get('/wx/attendance/plans', {
        type: 'course',
        page: this.data.page,
        pageSize: this.data.pageSize
      });
      
      if (response.code === 200) {
        const newCourses = this.processCourses(response.data.records || []);
        
        this.setData({
          courses: this.data.page === 1 ? newCourses : [...this.data.courses, ...newCourses],
          hasMore: newCourses.length === this.data.pageSize
        });
        
        // 应用筛选
        this.applyFilter();
      }
    } catch (error) {
      console.error('加载课程列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 加载更多课程
   */
  async loadMoreCourses() {
    this.setData({
      page: this.data.page + 1
    });
    await this.loadCourses();
  },

  /**
   * 处理课程数据
   */
  processCourses(courses) {
    const now = new Date();
    
    return courses.map(course => {
      // 字段名转换：后端下划线 -> 前端驼峰
      const convertedCourse = this.convertFieldNames(course);
      
      const startTime = new Date(convertedCourse.startTime);
      const endTime = new Date(convertedCourse.endTime);
      
      // 判断课程状态（仅用于管理展示）
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
      
      // 计算出勤率
      const attendanceRate = convertedCourse.expectedCount > 0 
        ? Math.round((convertedCourse.attendanceCount || 0) / convertedCourse.expectedCount * 100)
        : 0;
      
      return {
        ...convertedCourse,
        status,
        statusText,
        attendanceRate
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
    let filteredCourses = this.data.courses;
    
    if (this.data.filter !== 'all') {
      filteredCourses = this.data.courses.filter(course => 
        course.status === this.data.filter
      );
    }
    
    this.setData({ filteredCourses });
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
    
    // 获取所有课程列表
    await this.loadAllCourses();
    
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
        courseId: '',
        courseIndex: 0,
        courseDisplay: '',
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
        note: ''
      }
    });
  },

  /**
   * 阻止事件冒泡
   */
  preventClose() {
    // 空方法，用于阻止事件冒泡
  },

  /**
   * 获取所有课程列表（用于创建时选择）
   */
  async loadAllCourses() {
    try {
      const response = await http.get('/wx/course/all-courses', {
        page: 1,
        pageSize: 100
      });
      
      if (response.code === 200 && response.data && response.data.records) {
        // 过滤有效的课程（status为1的课程）
        const validCourses = response.data.records.filter(course => course.status === 1);
        
        this.setData({
          allCourses: validCourses,
          'createForm.courseDisplay': validCourses[0]?.name || '',
          'createForm.courseId': validCourses[0]?.courseId || ''
        });
      } else {
        wx.showToast({
          title: '请先添加课程信息',
          icon: 'none'
        });
        this.setData({
          allCourses: []
        });
      }
    } catch (error) {
      console.error('[获取课程列表] 失败:', error);
      wx.showToast({
        title: '获取课程列表失败',
        icon: 'error'
      });
      this.setData({
        allCourses: []
      });
    }
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
        this.setData({
          commonLocations: limitedLocations,
          'createForm.locationDisplay': limitedLocations[0]?.name || '',
          'createForm.location': limitedLocations[0]?.name || '',
          'createForm.locationLat': limitedLocations[0]?.lat || '',
          'createForm.locationLng': limitedLocations[0]?.lng || ''
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

  onCreateCourseChange(e) {
    const index = e.detail.value;
    const selectedCourse = this.data.allCourses[index];
    
    if (selectedCourse) {
      this.setData({
        'createForm.courseIndex': index,
        'createForm.courseId': selectedCourse.courseId,
        'createForm.courseDisplay': selectedCourse.name
      });
    }
  },

  onCreateRadiusInput(e) {
    const radius = parseFloat(e.detail.value) || 0;
    
    this.setData({
      'createForm.radius': e.detail.value
    });
    
    // 提示用户距离过大
    if (radius > 10) {
      wx.showToast({
        title: '建议签到距离不超过10米，以提高考勤精度',
        icon: 'none',
        duration: 3000
      });
    }
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
      this.setData({
        'createForm.locationIndex': index,
        'createForm.location': selectedLocation.name,
        'createForm.locationDisplay': selectedLocation.name,
        'createForm.locationLat': selectedLocation.lat,
        'createForm.locationLng': selectedLocation.lng
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
    
    if (!createForm.courseId) {
      wx.showToast({
        title: '请选择关联课程',
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

    // 验证时间
    const startTime = new Date(createForm.startTime);
    const endTime = new Date(createForm.endTime);
    if (endTime <= startTime) {
      wx.showToast({
        title: '结束时间必须晚于开始时间',
        icon: 'error'
      });
      return;
    }

    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: '创建中...' });
      
      // 格式化时间为后端期望的格式
      const formatTimeForBackend = (timeStr) => {
        if (!timeStr) return null;
        const date = new Date(timeStr);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        const second = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
      };
      
      // 准备提交数据
      const createData = {
        name: createForm.name.trim(),
        type: 'course',
        courseId: parseInt(createForm.courseId),
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
        wx.showToast({
          title: '创建成功',
          icon: 'success'
        });
        
        // 关闭弹窗并刷新列表
        this.hideCreateForm();
        setTimeout(() => {
          this.resetAndLoadCourses();
        }, 1500);
      } else {
        throw new Error(response.message || '创建失败');
      }
    } catch (error) {
      console.error('[创建课程考勤] 失败:', error);
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
   * 查看课程详情
   */
  viewCourseDetail(e) {
    const course = e.currentTarget.dataset.course;
    console.log('[课程列表] 点击跳转详情，课程数据:', course);
    console.log('[课程列表] planId:', course.planId, '课程名称:', course.name, '类型:', course.type);
    
    wx.navigateTo({
      url: `/pages/attendance/course/detail?planId=${course.planId}`
    });
  },

  /**
   * 查看签到记录
   */
  viewRecords(e) {
    e.stopPropagation(); // 阻止事件冒泡
    const plan = e.currentTarget.dataset.plan;
    
    wx.navigateTo({
      url: `/pages/attendance/records/index?type=course&planId=${plan.planId}`
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