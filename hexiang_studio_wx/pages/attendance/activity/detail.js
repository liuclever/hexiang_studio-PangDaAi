// pages/attendance/activity/detail.js
const { http } = require('../../../utils/request');
const { getUserRole } = require('../../../utils/auth');

// 时间格式化工具
const tools = {
  formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${year}/${month}/${day} ${hour}:${minute}`;
  },
  
  formatDateTime(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    const second = String(date.getSeconds()).padStart(2, '0');
    return `${year}/${month}/${day} ${hour}:${minute}:${second}`;
  }
};

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 工具函数
    tools: tools,
    
    // 页面参数
    planId: null,
    
    // 考勤计划详情
    planDetail: {},
    
    // 预约学生列表
    reservedStudents: [],
    displayedStudents: [],
    reservedStudentsLoading: false,
    showAllStudents: false,
    
    // 签到记录
    records: [],
    filteredRecords: [],
    
    // 统计数据
    statistics: {
      totalCount: 0,
      presentCount: 0,
      lateCount: 0,
      absentCount: 0,
      leaveCount: 0
    },
    
    // 筛选条件
    showFilter: false,
    statusFilter: 'all', // all, present, late, absent, leave
    
    // 分页参数
    pagination: {
      page: 1,
      size: 20,
      total: 0,
      hasMore: true
    },
    
    // 用户权限
    userRole: 'student',
    
    // 页面状态
    loading: true,
    refreshing: false,
    loadingMore: false,
    
    // 编辑表单
    showEditForm: false,
    editForm: {
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
      radius: '',
      status: 1,
      statusIndex: 0,
      note: ''
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
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('[活动详情] 页面加载, 参数:', options);
    
    if (!options.planId) {
      wx.showToast({
        title: '缺少必要参数',
        icon: 'error'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }
    
    this.setData({
      planId: options.planId,
      userRole: getUserRole()
    });
    
    this.initPageData();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 从其他页面返回时刷新数据
    if (this.data.planId) {
      this.setData({
        'pagination.page': 1,
        'pagination.hasMore': true,
        records: [],
        filteredRecords: []
      });
      this.loadData(true);
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.setData({ 
      refreshing: true,
      'pagination.page': 1,
      'pagination.hasMore': true,
      records: [],
      filteredRecords: []
    });
    this.loadData(true).finally(() => {
      this.setData({ refreshing: false });
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.pagination.hasMore && !this.data.loadingMore) {
      this.loadMoreData();
    }
  },

  /**
   * 初始化页面数据
   */
  async initPageData() {
    try {
      this.setData({ 
        loading: true,
        'pagination.page': 1,
        'pagination.hasMore': true,
        records: [],
        filteredRecords: []
      });
      await this.loadData(true);
    } catch (error) {
      console.error('[活动详情] 初始化失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'error'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 加载数据
   */
  async loadData(isRefresh = false) {
    const { planId, pagination } = this.data;
    
    try {
      const page = isRefresh ? 1 : pagination.page;
      const response = await http.get(`/wx/attendance/plan/${planId}/records`, {
        page,
        size: pagination.size,
        type: 'activity'  // 明确指定为活动考勤
      });
      
      if (response.code === 200 && response.data) {
        const { planDetail, records } = response.data;
        
        // 处理考勤计划详情
        const processedPlanDetail = this.processPlanDetail(planDetail);
        
        // 处理签到记录
        const processedRecords = this.processRecords(records.records || []);
        const currentRecords = isRefresh ? [] : this.data.records;
        const allRecords = [...currentRecords, ...processedRecords];
        
        // 更新分页信息
        const newPagination = {
          ...pagination,
          page: page,
          total: records.total || 0,
          hasMore: processedRecords.length >= pagination.size && allRecords.length < (records.total || 0)
        };
        
        // 计算统计数据（基于所有记录或服务器返回的统计）
        const statistics = records.statistics || this.calculateStatistics(allRecords);
        
        this.setData({
          planDetail: processedPlanDetail,
          records: allRecords,
          statistics,
          pagination: newPagination,
          filteredRecords: this.filterRecords(allRecords, this.data.statusFilter)
        });
        
        // 加载预约学生列表
        this.loadReservedStudents();
        
        console.log('[活动详情] 数据加载成功:', {
          planDetail: processedPlanDetail,
          recordCount: allRecords.length,
          totalCount: newPagination.total,
          hasMore: newPagination.hasMore,
          statistics
        });
      } else {
        throw new Error(response.message || '获取数据失败');
      }
    } catch (error) {
      console.error('[活动详情] 加载数据失败:', error);
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'error'
      });
    }
  },

  /**
   * 加载更多数据
   */
  async loadMoreData() {
    if (this.data.loadingMore || !this.data.pagination.hasMore) {
      return;
    }

    this.setData({ loadingMore: true });
    
    try {
      // 增加页码
      const newPage = this.data.pagination.page + 1;
      this.setData({
        'pagination.page': newPage
      });
      
      await this.loadData(false);
    } catch (error) {
      console.error('[活动详情] 加载更多失败:', error);
      // 回滚页码
      this.setData({
        'pagination.page': this.data.pagination.page - 1
      });
    } finally {
      this.setData({ loadingMore: false });
    }
  },

  /**
   * 加载预约学生列表
   */
  async loadReservedStudents() {
    const { planId } = this.data;
    if (!planId) return;
    
    this.setData({ reservedStudentsLoading: true });
    
    try {
      const response = await http.get(`/wx/attendance/plan/${planId}/reservations`);
      
      if (response.code === 200 && response.data) {
        const reservedStudents = response.data.map(student => ({
          id: student.student_id,
          name: student.student_name,
          studentNumber: student.student_number,
          reservationTime: student.reservation_time,
          status: student.status || 'reserved'
        }));
        
        this.setData({ reservedStudents });
        this.updateDisplayedStudents();
        console.log('[活动详情] 预约学生加载成功:', reservedStudents.length, '人');
      } else {
        console.warn('[活动详情] 预约学生加载失败:', response.message);
        this.setData({ reservedStudents: [], displayedStudents: [] });
      }
    } catch (error) {
      console.error('[活动详情] 预约学生加载异常:', error);
      this.setData({ reservedStudents: [], displayedStudents: [] });
    } finally {
      this.setData({ reservedStudentsLoading: false });
    }
  },

  /**
   * 更新显示的学生列表
   */
  updateDisplayedStudents() {
    const { reservedStudents, showAllStudents } = this.data;
    let displayedStudents;
    
    if (showAllStudents || reservedStudents.length <= 5) {
      displayedStudents = reservedStudents;
    } else {
      displayedStudents = reservedStudents.slice(0, 5);
    }
    
    this.setData({ displayedStudents });
  },

  /**
   * 切换学生列表展开状态
   */
  toggleStudentList() {
    this.setData({
      showAllStudents: !this.data.showAllStudents
    }, () => {
      this.updateDisplayedStudents();
    });
  },

  /**
   * 处理考勤计划详情数据
   */
  processPlanDetail(planDetail) {
    if (!planDetail) return {};
    
    // 字段名转换：后端下划线 -> 前端驼峰
    const convertedDetail = this.convertFieldNames(planDetail);
    
    // 计算活动状态
    const now = new Date();
    const startTime = new Date(convertedDetail.startTime);
    const endTime = new Date(convertedDetail.endTime);
    
    let activityStatus, statusText;
    if (now < startTime) {
      activityStatus = 'upcoming';
      statusText = '即将开始';
    } else if (now >= startTime && now <= endTime) {
      activityStatus = 'ongoing';
      statusText = '进行中';
    } else {
      activityStatus = 'ended';
      statusText = '已结束';
    }
    
    return {
      ...convertedDetail,
      activityStatus,
      statusText
    };
  },

  /**
   * 处理签到记录数据
   */
  processRecords(records) {
    if (!Array.isArray(records)) return [];
    
    return records.map(record => {
      // 字段名转换
      const convertedRecord = this.convertFieldNames(record);
      
      // 确保状态值正确
      if (!convertedRecord.status) {
        convertedRecord.status = 'pending';
      }
      
      // 添加状态文字，确保显示正确
      convertedRecord.statusText = this.getStatusLabel(convertedRecord.status);
      
      // 调试输出
      console.log('[记录处理] 学生:', convertedRecord.studentName || convertedRecord.student_name, '状态:', convertedRecord.status, '状态文字:', convertedRecord.statusText);
      
      return {
        ...convertedRecord,
        recordId: convertedRecord.recordId || `${convertedRecord.planId}_${convertedRecord.studentId}`
      };
    });
  },

  /**
   * 计算统计数据
   */
  calculateStatistics(records) {
    const stats = {
      totalCount: records.length,
      presentCount: 0,
      lateCount: 0,
      absentCount: 0,
      leaveCount: 0
    };
    
    records.forEach(record => {
      switch (record.status) {
        case 'present':
          stats.presentCount++;
          break;
        case 'late':
          stats.lateCount++;
          break;
        case 'absent':
          stats.absentCount++;
          break;
        case 'leave':
          stats.leaveCount++;
          break;
      }
    });
    
    return stats;
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
      'create_user_name': 'createUserName',
      'record_id': 'recordId',
      'student_id': 'studentId',
      'student_name': 'studentName',
      'student_number': 'studentNumber',
      'sign_in_time': 'signInTime'
    };
    
    // 执行字段映射
    for (const [snakeCase, camelCase] of Object.entries(fieldMapping)) {
      if (converted.hasOwnProperty(snakeCase)) {
        converted[camelCase] = converted[snakeCase];
      }
    }
    
    return converted;
  },

  /**
   * 筛选记录
   */
  filterRecords(records, statusFilter) {
    if (statusFilter === 'all') {
      return records;
    }
    return records.filter(record => record.status === statusFilter);
  },

  /**
   * 切换筛选显示
   */
  toggleFilter() {
    this.setData({
      showFilter: !this.data.showFilter
    });
  },

  /**
   * 设置状态筛选
   */
  setStatusFilter(e) {
    const status = e.currentTarget.dataset.status;
    const filteredRecords = this.filterRecords(this.data.records, status);
    
    this.setData({
      statusFilter: status,
      filteredRecords
    });
  },

  /**
   * 获取状态文本
   */
  getStatusText(status) {
    switch (status) {
      case 'upcoming': return '即将开始';
      case 'ongoing': return '进行中';
      case 'ended': return '已结束';
      default: return '未知';
    }
  },

  /**
   * 获取状态标签
   */
  getStatusLabel(status) {
    switch (status) {
      case 'present': return '已签到';
      case 'late': return '迟到';
      case 'absent': return '缺勤';
      case 'leave': return '请假';
      case 'pending': return '待签到';
      default: return '未知';
    }
  },

  /**
   * 获取筛选状态文本
   */
  getStatusFilterText() {
    switch (this.data.statusFilter) {
      case 'all': return '';
      case 'present': return '已签到';
      case 'late': return '迟到';
      case 'absent': return '缺勤';
      case 'leave': return '请假';
      default: return '';
    }
  },

  /**
   * 返回上一页
   */
  goBack() {
    wx.navigateBack();
  },

  /**
   * 显示更多选项
   */
  showMoreOptions() {
    const { userRole } = this.data;
    const actions = ['分享活动'];
    
    if (userRole === 'admin') {
      actions.push('编辑活动', '删除活动');
    }
    
    wx.showActionSheet({
      itemList: actions,
      success: (res) => {
        const action = actions[res.tapIndex];
        switch (action) {
          case '分享活动':
            this.shareActivity();
            break;
          case '编辑活动':
            this.editBasicInfo();
            break;
          case '删除活动':
            this.deleteActivity();
            break;
        }
      }
    });
  },

  /**
   * 编辑基本信息
   */
  async editBasicInfo() {
    // 初始化时间选择器数据
    this.initDateTimeRange();
    
    // 获取常用地点列表
    await this.loadCommonLocations();
    
    // 格式化时间显示
    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '';
      const date = new Date(dateTimeStr);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hour}:${minute}`;
    };

    // 等待一个微任务周期，确保dateTimeRange已设置
    await new Promise(resolve => setTimeout(resolve, 0));

    // 计算时间选择器的索引
    const getTimePickerIndex = (dateTimeStr, useCurrentTime = false) => {
      let date;
      if (!dateTimeStr && useCurrentTime) {
        date = new Date();
      } else if (dateTimeStr) {
        date = new Date(dateTimeStr);
      } else {
        return [0, 0, 0, 0]; // 默认返回第一个选项
      }

      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = date.getHours();
      const minute = Math.floor(date.getMinutes() / 30) * 30; // 向下取整到30分钟

      // 在dateTimeRange中查找对应的索引
      const dateTimeRange = this.data.dateTimeRange;
      if (!dateTimeRange || !dateTimeRange[0]) {
        return [0, 0, 0, 0];
      }

      const yearIndex = dateTimeRange[0].findIndex(y => parseInt(y) === year);
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
    
    // 找到当前地点在常用地点中的索引
    const currentLocation = this.data.planDetail.location;
    const locationIndex = this.data.commonLocations.findIndex(loc => loc.name === currentLocation);

    // 计算开始时间和结束时间的选择器索引
    const startTimeIndex = getTimePickerIndex(this.data.planDetail.startTime);
    const endTimeIndex = getTimePickerIndex(this.data.planDetail.endTime, true); // 如果没有结束时间，使用当前时间
    
    // 如果没有结束时间，设置为当前时间
    let endTime = this.data.planDetail.endTime;
    let endTimeDisplay = formatDateTime(this.data.planDetail.endTime);
    if (!endTime) {
      const now = new Date();
      endTime = now.toISOString();
      endTimeDisplay = formatDateTime(endTime);
    }
    
    // 初始化编辑表单数据
    this.setData({
      showEditForm: true,
      'editForm.name': this.data.planDetail.name || '',
      'editForm.startTime': this.data.planDetail.startTime || '',
      'editForm.endTime': endTime,
      'editForm.startTimeDisplay': formatDateTime(this.data.planDetail.startTime),
      'editForm.endTimeDisplay': endTimeDisplay,
      'editForm.location': currentLocation || '',
      'editForm.locationDisplay': currentLocation || '',
      'editForm.locationIndex': locationIndex >= 0 ? locationIndex : 0,
      'editForm.locationLat': this.data.planDetail.locationLat || '',
      'editForm.locationLng': this.data.planDetail.locationLng || '',
      'editForm.radius': this.data.planDetail.radius || '',
      'editForm.status': parseInt(this.data.planDetail.status) || 1,
      'editForm.statusIndex': parseInt(this.data.planDetail.status) === 0 ? 1 : 0,
      'editForm.note': this.data.planDetail.note || '',
      startTimeIndex: startTimeIndex,
      endTimeIndex: endTimeIndex
    });
  },

  /**
   * 获取常用地点列表
   */
  async loadCommonLocations() {
    try {
      const { http } = require('../../../utils/request');
      const response = await http.get('/admin/locations');
      
      if (response.code === 200 && response.data && response.data.length > 0) {
        // 限制地点数量，避免选择器过长
        const limitedLocations = response.data.slice(0, 8); // 最多显示8个地点
        this.setData({
          commonLocations: limitedLocations
        });
      } else {
        // 如果没有常用地点，提示用户
        console.warn('未获取到常用地点数据');
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
   * 隐藏编辑表单
   */
  hideEditForm() {
    this.setData({
      showEditForm: false
    });
  },

  /**
   * 阻止事件冒泡
   */
  preventClose() {
    // 空方法，用于阻止事件冒泡
  },

  /**
   * 输入框事件处理
   */
  onNameInput(e) {
    this.setData({
      'editForm.name': e.detail.value
    });
  },

  onRadiusInput(e) {
    this.setData({
      'editForm.radius': e.detail.value
    });
  },

  onNoteInput(e) {
    this.setData({
      'editForm.note': e.detail.value
    });
  },

  onLocationChange(e) {
    const index = e.detail.value;
    const selectedLocation = this.data.commonLocations[index];
    
    if (selectedLocation) {
      this.setData({
        'editForm.locationIndex': index,
        'editForm.location': selectedLocation.name,
        'editForm.locationDisplay': selectedLocation.name,
        'editForm.locationLat': selectedLocation.lat,
        'editForm.locationLng': selectedLocation.lng
      });
    }
  },

  onStartTimeChange(e) {
    const values = e.detail.value;
    const ranges = this.data.dateTimeRange;
    const year = ranges[0][values[0]];
    const month = ranges[1][values[1]];
    const day = ranges[2][values[2]];
    const time = ranges[3][values[3]];
    
    const dateTimeStr = `${year}-${month}-${day} ${time}`;
    this.setData({
      startTimeIndex: values,
      'editForm.startTime': dateTimeStr,
      'editForm.startTimeDisplay': dateTimeStr
    });
  },

  onEndTimeChange(e) {
    const values = e.detail.value;
    const ranges = this.data.dateTimeRange;
    const year = ranges[0][values[0]];
    const month = ranges[1][values[1]];
    const day = ranges[2][values[2]];
    const time = ranges[3][values[3]];
    
    const dateTimeStr = `${year}-${month}-${day} ${time}`;
    this.setData({
      endTimeIndex: values,
      'editForm.endTime': dateTimeStr,
      'editForm.endTimeDisplay': dateTimeStr
    });
  },

  onStatusChange(e) {
    const index = e.detail.value;
    this.setData({
      'editForm.statusIndex': index,
      'editForm.status': this.data.statusOptions[index].value
    });
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
   * 保存修改
   */
  async saveChanges() {
    const { editForm, planId } = this.data;
    
    // 基本验证
    if (!editForm.name.trim()) {
      wx.showToast({
        title: '请输入考勤名称',
        icon: 'error'
      });
      return;
    }
    
    if (!editForm.startTime) {
      wx.showToast({
        title: '请选择开始时间',
        icon: 'error'
      });
      return;
    }
    
    if (!editForm.endTime) {
      wx.showToast({
        title: '请选择结束时间',
        icon: 'error'
      });
      return;
    }
    
    if (!editForm.location.trim()) {
      wx.showToast({
        title: '请输入活动地点',
        icon: 'error'
      });
      return;
    }

    // 验证时间
    const startTime = new Date(editForm.startTime);
    const endTime = new Date(editForm.endTime);
    if (endTime <= startTime) {
      wx.showToast({
        title: '结束时间必须晚于开始时间',
        icon: 'error'
      });
      return;
    }

    try {
      wx.showLoading({ title: '保存中...' });
      
      // 格式化时间为后端期望的格式 (yyyy-MM-dd HH:mm:ss)
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
      const updateData = {
        name: editForm.name.trim(),
        type: this.data.planDetail.type || 'activity', // 保持原有类型或默认为activity
        startTime: formatTimeForBackend(editForm.startTime),
        endTime: formatTimeForBackend(editForm.endTime),
        location: editForm.location.trim(),
        radius: parseInt(editForm.radius) || 100,
        status: parseInt(editForm.status) || 1,
        note: editForm.note.trim()
      };
      
      // 如果有经纬度信息，也提交
      if (editForm.locationLat && editForm.locationLng) {
        updateData.locationLat = parseFloat(editForm.locationLat);
        updateData.locationLng = parseFloat(editForm.locationLng);
      }
      
      // 调用更新接口
      const { http } = require('../../../utils/request');
      const response = await http.put(`/wx/attendance/plan/${planId}`, updateData);
      
      if (response.code === 200) {
        wx.showToast({
          title: '修改成功',
          icon: 'success'
        });
        
        // 关闭弹窗并刷新数据
        this.hideEditForm();
        setTimeout(() => {
          this.loadData(true);
        }, 1500);
      } else {
        throw new Error(response.message || '修改失败');
      }
    } catch (error) {
      console.error('[修改考勤] 失败:', error);
      wx.showToast({
        title: error.message || '修改失败',
        icon: 'error'
      });
    } finally {
      wx.hideLoading();
    }
  },

  /**
   * 分享活动
   */
  shareActivity() {
    wx.showToast({
      title: '分享功能开发中',
      icon: 'none'
    });
  },

  /**
   * 编辑活动 (已重命名为editBasicInfo)
   */
  editActivity() {
    // 保留这个方法以防其他地方调用，但实际调用editBasicInfo
    this.editBasicInfo();
  },

  /**
   * 删除活动
   */
  deleteActivity() {
    wx.showModal({
      title: '确认删除',
      content: '删除后无法恢复，确定要删除这个活动吗？',
      confirmText: '删除',
      confirmColor: '#ff4757',
      success: (res) => {
        if (res.confirm) {
          this.performDeleteActivity();
        }
      }
    });
  },

  /**
   * 执行删除活动
   */
  async performDeleteActivity() {
    try {
      wx.showLoading({ title: '删除中...' });
      
      const { http } = require('../../../utils/request');
      const response = await http.delete(`/admin/attendance/plan/${this.data.planId}`);
      if (response.code === 200) {
        wx.showToast({
          title: '删除成功',
          icon: 'success'
        });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        throw new Error(response.message || '删除失败');
      }
    } catch (error) {
      console.error('[活动详情] 删除失败:', error);
      wx.showToast({
        title: error.message || '删除失败',
        icon: 'error'
      });
    } finally {
      wx.hideLoading();
    }
  }
});
