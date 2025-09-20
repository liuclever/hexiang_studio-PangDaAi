// pages/attendance/index.js
const { checkLoginStatus, getUserInfo, getUserRole } = require('../../utils/auth');
const { http } = require('../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户信息
    userInfo: null,
    userRole: 'student',
    
    // 统计数据
    statistics: {
      todayAttendance: 0,
      monthAttendance: 0,
      pendingPlans: 0
    },
    
    // 模块统计
    moduleStats: {
      duty: 0,
      activity: 0,
      course: 0
    },
    
    // 🔧 动态页面标题
    pageTitle: '我的考勤',
    pageSubtitle: '查看我的考勤活动',
    
    // 请假申请弹窗
    showLeaveForm: false,
    leaveForm: {
      type: '',
      attendancePlanId: '', // 新增：关联的考勤计划ID
      startTime: '',
      endTime: '',
      reason: '',
      attachments: []
    },
    leaveTypes: [
      { value: 'sick_leave', label: '病假' },
      { value: 'personal_leave', label: '事假' },
      { value: 'public_leave', label: '公假' },
      { value: 'annual_leave', label: '年假' }
    ],
    leaveTypeIndex: -1, // 当前选择的请假类型索引
    
    // 考勤计划相关
    attendancePlans: [], // 可用的考勤计划列表
    attendancePlanIndex: -1, // 当前选择的考勤计划索引
    loadingPlans: false,
    
    // 时间选择器相关（复制考勤页面的实现）
    dateTimeRange: [], // 时间选择器数据：[年, 月, 日, 时间]
    startTimeIndex: [0, 0, 0, 0], // 开始时间索引
    endTimeIndex: [0, 0, 0, 0], // 结束时间索引
    
    // 请假记录相关
    showLeaveRecords: false, // 是否显示请假记录列表
    leaveRecords: [], // 请假记录列表
    loadingLeaveRecords: false, // 是否正在加载请假记录
    
    // 页面状态
    loading: false,
    submitting: false,
    
    // 工具函数
    tools: {
      formatDateTime: function(timeStr) {
        if (!timeStr) return '';
        const date = new Date(timeStr);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      },
      
      formatTime: function(timeStr) {
        if (!timeStr) return '';
        const date = new Date(timeStr);
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
      }
    }
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
    const userRole = getUserRole();
    
    // 🔧 根据用户角色设置不同的页面标题
    let pageTitle, pageSubtitle;
    if (userRole === 'admin') {
      pageTitle = '考勤管理';
      pageSubtitle = '管理各类考勤活动';
    } else {
      pageTitle = '我的考勤';
      pageSubtitle = '查看我的考勤活动';
    }
    
    this.setData({
      userInfo: getUserInfo(),
      userRole: userRole,
      pageTitle: pageTitle,
      pageSubtitle: pageSubtitle
    });
    
    // 加载页面数据
    this.loadPageData();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 刷新数据
    if (this.data.userInfo) {
      this.loadPageData();
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadPageData().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 加载页面数据
   */
  async loadPageData() {
    this.setData({ loading: true });
    
    try {
      // 并行加载多个数据
      await Promise.all([
        this.loadStatistics(),
        this.loadModuleStats()
      ]);
    } catch (error) {
      console.error('加载页面数据失败:', error);
      wx.showToast({
        title: '加载数据失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 加载统计数据
   */
  async loadStatistics() {
    try {
      const response = await http.get('/wx/attendance/statistics');
      if (response.code === 200) {
        this.setData({
          statistics: {
            todayAttendance: response.data.todayAttendance || 0,
            monthAttendance: response.data.monthAttendance || 0,
            pendingPlans: response.data.pendingPlans || 0
          }
        });
      }
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  },

  /**
   * 加载模块统计
   */
  async loadModuleStats() {
    try {
      const response = await http.get('/wx/attendance/module-stats');
      if (response.code === 200) {
        this.setData({
          moduleStats: {
            duty: response.data.dutyCount || 0,
            activity: response.data.activityCount || 0,
            course: response.data.courseCount || 0
          }
        });
      }
    } catch (error) {
      console.error('加载模块统计失败:', error);
    }
  },





  /**
   * 导航到考勤模块
   */
  navigateToModule(e) {
    const type = e.currentTarget.dataset.type;
    
    switch (type) {
      case 'duty':
        wx.navigateTo({
          url: '/pages/attendance/duty/index'
        });
        break;
      case 'activity':
        wx.navigateTo({
          url: '/pages/attendance/activity/index'
        });
        break;
      case 'course':
        wx.navigateTo({
          url: '/pages/attendance/course/index'
        });
        break;
    }
  },

  /**
   * 导航到功能页面
   */
  navigateToPage(e) {
    const page = e.currentTarget.dataset.page;
    
    switch (page) {
      case 'records':
        wx.navigateTo({
          url: '/pages/attendance/records/index'
        });
        break;
      case 'leave-approval':
        // 🔧 根据用户身份显示不同功能
        if (this.data.userRole === 'admin') {
          // 管理员 → 审批中心
          wx.navigateTo({
            url: '/pages/attendance/leave-approval'
          });
        } else {
          // 学生 → 弹出请假申请表单
          this.showLeaveRequestForm();
        }
        break;

    }
  },



  // ==================== 请假申请相关方法 ====================
  
  /**
   * 显示请假申请表单
   */
  async showLeaveRequestForm() {
    // 先加载可用的考勤计划
    await this.loadAvailableAttendancePlans();
    
    // 初始化时间选择器数据（复制考勤页面的实现）
    this.initDateTimeRange();
    
    this.setData({
      showLeaveForm: true,
      leaveForm: {
        type: '',
        attendancePlanId: '',
        startTime: '',
        endTime: '',
        reason: '',
        attachments: [] // 确保是空数组
      },
      leaveTypeIndex: -1,
      attendancePlanIndex: -1,
      startTimeIndex: [0, 0, 0, 0],
      endTimeIndex: [0, 0, 0, 0]
    });
  },

  /**
   * 隐藏请假申请表单
   */
  hideLeaveForm() {
    this.setData({
      showLeaveForm: false
    });
  },

  /**
   * 选择请假类型
   */
  onLeaveTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      'leaveForm.type': this.data.leaveTypes[index].value,
      leaveTypeIndex: index
    });
  },

  /**
   * 选择考勤计划
   */
  onAttendancePlanChange(e) {
    const index = e.detail.value;
    const selectedPlan = this.data.attendancePlans[index];
    
    // 格式化时间并计算选择器索引
    let startTime = '';
    let endTime = '';
    let startTimeIndex = [0, 0, 0, 0];
    let endTimeIndex = [0, 0, 0, 0];
    
    const ranges = this.data.dateTimeRange;
    
    if (selectedPlan.startTime && ranges.length > 0) {
      const startDate = new Date(selectedPlan.startTime);
      if (!isNaN(startDate.getTime())) {
        const year = startDate.getFullYear();
        const month = String(startDate.getMonth() + 1).padStart(2, '0');
        const day = String(startDate.getDate()).padStart(2, '0');
        const hours = String(startDate.getHours()).padStart(2, '0');
        const minutes = String(Math.floor(startDate.getMinutes() / 30) * 30).padStart(2, '0');
        startTime = `${year}-${month}-${day} ${hours}:${minutes}`;
        
        // 计算选择器索引
        const yearIndex = ranges[0].findIndex(y => y === year.toString());
        const monthIndex = ranges[1].findIndex(m => m === month);
        const dayIndex = ranges[2].findIndex(d => d === day);
        const timeIndex = ranges[3].findIndex(t => t === `${hours}:${minutes}`);
        
        startTimeIndex = [
          Math.max(0, yearIndex),
          Math.max(0, monthIndex),
          Math.max(0, dayIndex),
          Math.max(0, timeIndex)
        ];
      }
    }
    
    if (selectedPlan.endTime && ranges.length > 0) {
      const endDate = new Date(selectedPlan.endTime);
      if (!isNaN(endDate.getTime())) {
        const year = endDate.getFullYear();
        const month = String(endDate.getMonth() + 1).padStart(2, '0');
        const day = String(endDate.getDate()).padStart(2, '0');
        const hours = String(endDate.getHours()).padStart(2, '0');
        const minutes = String(Math.floor(endDate.getMinutes() / 30) * 30).padStart(2, '0');
        endTime = `${year}-${month}-${day} ${hours}:${minutes}`;
        
        // 计算选择器索引
        const yearIndex = ranges[0].findIndex(y => y === year.toString());
        const monthIndex = ranges[1].findIndex(m => m === month);
        const dayIndex = ranges[2].findIndex(d => d === day);
        const timeIndex = ranges[3].findIndex(t => t === `${hours}:${minutes}`);
        
        endTimeIndex = [
          Math.max(0, yearIndex),
          Math.max(0, monthIndex),
          Math.max(0, dayIndex),
          Math.max(0, timeIndex)
        ];
      }
    }
    
    this.setData({
      'leaveForm.attendancePlanId': selectedPlan.planId,
      attendancePlanIndex: index,
      // 自动填充考勤计划的时间
      'leaveForm.startTime': startTime,
      'leaveForm.endTime': endTime,
      startTimeIndex: startTimeIndex,
      endTimeIndex: endTimeIndex
    });
    
    wx.showToast({
      title: `已选择并自动填充${this.getAttendanceTypeName(selectedPlan.type)}时间`,
      icon: 'success',
      duration: 2000
    });
  },

  /**
   * 初始化时间选择器数据（复制考勤页面的实现）
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
   * 选择开始时间（已禁用，仅用于兼容性）
   */
  onStartTimeChange(e) {
    // 时间选择器已禁用，此方法不再执行任何操作
    return;
  },

  /**
   * 选择结束时间（已禁用，仅用于兼容性）
   */
  onEndTimeChange(e) {
    // 时间选择器已禁用，此方法不再执行任何操作
      return;
  },

  /**
   * 输入请假原因
   */
  onReasonInput(e) {
    this.setData({
      'leaveForm.reason': e.detail.value
    });
  },

  /**
   * 选择文件（参考任务页面逻辑）
   */
  chooseFiles() {
    wx.chooseMessageFile({
      count: 3 - this.data.leaveForm.attachments.length, // 最多3个文件
      type: 'all', // 可以选择所有文件类型
      success: (res) => {
        const tempFiles = res.tempFiles;
        
        // 检查文件类型和大小（参考任务页面逻辑）
        const validFiles = tempFiles.filter(file => {
          // 检查文件大小（限制为10MB，最小1KB）
          if (file.size > 10 * 1024 * 1024) {
            wx.showToast({
              title: '文件大小不能超过10MB',
              icon: 'none'
            });
            return false;
          }
          
          // 检查空文件
          if (file.size < 1024) {
            wx.showToast({
              title: `文件"${file.name}"太小，可能是空文件`,
              icon: 'none',
              duration: 3000
            });
            return false;
          }
          
          // 检查文件类型（支持常用格式）
          const extension = file.name.split('.').pop().toLowerCase();
          const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'doc', 'docx', 'pdf', 'xls', 'xlsx', 'zip', 'rar'];
          
          console.log(`[文件检查] 文件名: "${file.name}", 大小: ${file.size}字节, 后缀: "${extension}"`);

          if (!allowedExtensions.includes(extension)) {
            wx.showToast({
              title: `不支持 ${extension} 类型的文件`,
              icon: 'none'
            });
            return false;
          }
          
          return true;
        });
        
        if (validFiles.length === 0) {
          return;
        }
        
        // 显示处理中提示
        wx.showLoading({
          title: '处理文件中...',
          mask: true
        });
        
        console.log(`[文件选择] 开始处理 ${validFiles.length} 个有效文件`);
        
        // 处理文件信息（参考任务页面）
        const processedFiles = validFiles.map((file, index) => {
          const extension = file.name.split('.').pop().toLowerCase();
          let fileType = 'other';
          
          if (['jpg', 'jpeg', 'png', 'gif'].includes(extension)) {
            fileType = 'image';
          } else if (['doc', 'docx'].includes(extension)) {
            fileType = 'word';
          } else if (['xls', 'xlsx'].includes(extension)) {
            fileType = 'excel';
          } else if (extension === 'pdf') {
            fileType = 'pdf';
          } else if (['zip', 'rar'].includes(extension)) {
            fileType = 'archive';
          }
          
          const processedFile = {
            name: file.name,
            filePath: file.path,
            path: file.path,
            size: file.size,
            fileType: fileType,
            // 格式化文件大小显示
            sizeText: this.formatFileSize(file.size)
          };
          
          console.log(`[文件处理] 第${index + 1}个文件处理结果:`, processedFile);
          
          return processedFile;
        });
        
        // 添加到附件列表
        this.setData({
          'leaveForm.attachments': [...this.data.leaveForm.attachments, ...processedFiles]
        });
        
        wx.hideLoading();
        
        wx.showToast({
          title: `成功添加 ${processedFiles.length} 个文件`,
          icon: 'success'
        });
      },
      fail: (err) => {
        console.error('选择文件失败:', err);
        wx.showToast({
          title: '选择文件失败',
          icon: 'none'
        });
      }
    });
  },

  /**
   * 格式化文件大小
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  },

  /**
   * 移除文件
   */
  removeFile(e) {
    const index = e.currentTarget.dataset.index;
    const attachments = this.data.leaveForm.attachments;
    attachments.splice(index, 1);
    this.setData({
      'leaveForm.attachments': attachments
    });
  },

  /**
   * 加载可用的考勤计划列表
   */
  async loadAvailableAttendancePlans() {
    try {
      this.setData({ loadingPlans: true });
      
      // 获取当前用户可参与的考勤计划
      const response = await http.get('/wx/attendance/current-plans');
      
      if (response.success && response.data) {
        const plans = response.data.map(plan => ({
          planId: plan.planId,
          name: plan.name,
          type: plan.type,
          typeName: this.getAttendanceTypeName(plan.type),
          startTime: plan.startTime,
          endTime: plan.endTime,
          location: plan.location,
          displayText: `${plan.name} (${this.getAttendanceTypeName(plan.type)}) - ${plan.location}`
        }));
        
        this.setData({
          attendancePlans: plans
        });
      } else {
        wx.showToast({
          title: '获取考勤计划失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('加载考勤计划失败:', error);
      wx.showToast({
        title: '网络请求失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loadingPlans: false });
    }
  },

  /**
   * 获取考勤类型名称
   */
  getAttendanceTypeName(type) {
    const typeMap = {
      'course': '课程考勤',
      'activity': '活动考勤',
      'duty': '值班考勤'
    };
    return typeMap[type] || type;
  },

  /**
   * 提交请假申请
   */
  async submitLeaveRequest() {
    const form = this.data.leaveForm;
    
    // 基础表单验证
    if (!form.type) {
      wx.showToast({ title: '请选择请假类型', icon: 'none' });
      return;
    }
    if (!form.attendancePlanId) {
      wx.showToast({ title: '请选择考勤计划', icon: 'none' });
      return;
    }
    if (!form.startTime) {
      wx.showToast({ title: '请选择开始时间', icon: 'none' });
      return;
    }
    if (!form.endTime) {
      wx.showToast({ title: '请选择结束时间', icon: 'none' });
      return;
    }
    if (!form.reason.trim()) {
      wx.showToast({ title: '请填写请假原因', icon: 'none' });
      return;
    }

    // 时间验证已取消，仅保留基础验证

    try {
      this.setData({ submitting: true });
      
        // 格式化时间为后端需要的格式 (YYYY-MM-DD HH:mm:ss)
      let startTime = form.startTime;
      let endTime = form.endTime;
      
      // 确保时间格式包含秒数
      if (startTime) {
        // 如果时间格式是 "YYYY-MM-DD HH:mm"，添加 ":00"
        if (startTime.match(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/)) {
          startTime += ':00';
        }
      }
      
      if (endTime) {
        // 如果时间格式是 "YYYY-MM-DD HH:mm"，添加 ":00"
        if (endTime.match(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/)) {
          endTime += ':00';
        }
      }
      
      console.log('时间格式化:', {
        原始开始时间: form.startTime,
        原始结束时间: form.endTime,
        格式化后开始时间: startTime,
        格式化后结束时间: endTime
      });
      
      // 如果有附件，先上传文件
      let attachmentUrls = [];
      if (form.attachments && Array.isArray(form.attachments) && form.attachments.length > 0) {
        wx.showLoading({ title: '上传附件中...' });
        try {
        attachmentUrls = await this.uploadAttachments(form.attachments);
          // 确保attachmentUrls是字符串数组，过滤掉无效值
          attachmentUrls = attachmentUrls.filter(url => url && typeof url === 'string' && url.trim() !== '');
          
          if (attachmentUrls.length === 0) {
            throw new Error('没有成功上传的附件');
          }
        } catch (error) {
          console.error('上传附件失败:', error);
          wx.showToast({
            title: '附件上传失败，请重试',
            icon: 'none'
          });
          return;
        } finally {
        wx.hideLoading();
        }
      }
      
      const requestData = {
        type: form.type,
        attendancePlanId: form.attendancePlanId, // 新增：考勤计划ID
        startTime: startTime,
        endTime: endTime,
        reason: form.reason.trim(),
        attachments: attachmentUrls || [] // 确保是字符串数组，没有附件时为空数组
      };
      
      console.log('请假申请数据:', {
        ...requestData,
        attachmentsCount: attachmentUrls.length,
        attachmentsList: attachmentUrls
      });
      
      console.log('提交请假申请:', requestData);
      const response = await http.post('/wx/leave/apply', requestData);
      
      if (response.success) {
        wx.showToast({
          title: '申请提交成功',
          icon: 'success'
        });
        this.hideLeaveForm();
        this.loadStatistics(); // 刷新统计数据
      } else {
        wx.showToast({
          title: response.msg || '提交失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('提交请假申请失败:', error);
      wx.showToast({
        title: '网络请求失败',
        icon: 'none'
      });
    } finally {
      this.setData({ submitting: false });
    }
  },

  /**
   * 上传附件文件（使用通用文件上传接口）
   */
  async uploadAttachments(files) {
    const { BASE_URL } = require('../../config/index');
    const uploadResults = [];
    
    // 串行上传文件
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      try {
        console.log(`[上传附件] 开始上传第${i + 1}个文件: ${file.name}`);
        
        const result = await new Promise((resolve, reject) => {
          wx.uploadFile({
            url: `${BASE_URL}/wx/file/upload`,
            filePath: file.filePath,
            name: 'file', // 通用接口使用 'file' 作为参数名
            formData: {
              type: 'leave_attachment' // 使用专门的请假申请附件类型
            },
            header: { 
              'Authorization': 'Bearer ' + wx.getStorageSync('token') 
            },
            success: (res) => {
              try {
                const result = JSON.parse(res.data);
                console.log(`[上传响应] 文件 ${file.name}:`, result);
                
                if ((result.code === 1 || result.code === 200) && result.data) {
                  // 通用接口返回的是相对路径
                  resolve(result.data); 
                } else {
                  reject(new Error(result.msg || '上传失败'));
                }
              } catch (e) {
                console.error(`[解析响应失败] 文件 ${file.name}:`, e, res.data);
                reject(new Error('服务器响应异常'));
              }
            },
            fail: (err) => {
              console.error(`[上传网络错误] 文件 ${file.name}:`, err);
              reject(err);
            }
          });
        });
        
        uploadResults.push(result);
        console.log(`[上传成功] 文件 ${file.name} 上传成功, 路径:`, result);
        
      } catch (error) {
        console.error(`[上传失败] 文件 ${file.name} 上传失败:`, error);
        // 上传失败时，可以选择继续或停止
        wx.showToast({
          title: `文件"${file.name}"上传失败`,
          icon: 'none'
        });
        // 上传失败也要抛出错误，让调用方知道
        throw error;
      }
    }
    
    console.log('[上传附件] 所有文件上传完成，最终结果:', uploadResults);
    return uploadResults;
  },

  /**
   * 切换请假记录显示/隐藏
   */
  toggleLeaveRecords() {
    const showLeaveRecords = !this.data.showLeaveRecords;
    this.setData({ showLeaveRecords });
    
    // 如果是显示状态且还没有加载过数据，则加载请假记录
    if (showLeaveRecords && this.data.leaveRecords.length === 0) {
      this.loadLeaveRecords();
    }
  },

  /**
   * 加载用户的请假记录
   */
  async loadLeaveRecords() {
    try {
      this.setData({ loadingLeaveRecords: true });
      
      const { BASE_URL } = require('../../config/index');
      const result = await http.get(`${BASE_URL}/wx/leave/my-requests`, {
        page: 1,
        pageSize: 10 // 只显示最近10条记录
      });
      
      if (result.success && result.data) {
        this.setData({
          leaveRecords: result.data.records || []
        });
      } else {
        wx.showToast({
          title: result.msg || '加载失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('加载请假记录失败:', error);
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    } finally {
      this.setData({ loadingLeaveRecords: false });
    }
  },

  /**
   * 查看请假记录详情
   */
  viewLeaveDetail(e) {
    const record = e.currentTarget.dataset.record;
    const detail = {
      ...record,
      statusText: record.status === 'pending' ? '待审批' : 
                  record.status === 'approved' ? '已批准' : '已拒绝'
    };
    
    // 转换请假类型为中文
    const getLeaveTypeText = (type) => {
      switch(type) {
        case 'personal_leave': return '事假';
        case 'sick_leave': return '病假';
        case 'public_leave': return '公假';
        case 'annual_leave': return '年假';
        default: return type;
      }
    };
    
    // 格式化显示时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    };
    
    let content = `请假类型：${getLeaveTypeText(detail.type)}\n`;
    content += `请假时间：${formatTime(detail.startTime)} - ${formatTime(detail.endTime)}\n`;
    content += `请假原因：${detail.reason}\n`;
    content += `申请状态：${detail.statusText}\n`;
    content += `申请时间：${formatTime(detail.createTime)}`;
    
    if (detail.approverName) {
      content += `\n审批人：${detail.approverName}`;
    }
    if (detail.approvedAt) {
      content += `\n审批时间：${formatTime(detail.approvedAt)}`;
    }
    if (detail.remark) {
      content += `\n审批备注：${detail.remark}`;
    }
    
    wx.showModal({
      title: '请假详情',
      content: content,
      showCancel: false,
      confirmText: '知道了'
    });
  }
}); 