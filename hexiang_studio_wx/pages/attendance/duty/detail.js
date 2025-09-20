// pages/attendance/duty/detail.js
const { http } = require('../../../utils/request');
const { getUserRole, getUserInfo } = require('../../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 页面参数
    planId: null,
    
    // 值班计划详情
    planDetail: {},
    
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
    
    // 用户信息
    userInfo: null,
    userRole: 'student',
    
    // 页面状态
    loading: true,
    refreshing: false,
    loadingMore: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('[值班详情] 页面加载, 参数:', options);
    
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
      userInfo: getUserInfo(),
      userRole: getUserRole()
    });

    this.initPageData();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 页面显示时刷新数据
    if (this.data.planId) {
      this.loadData(true);
    }
  },

  /**
   * 初始化页面数据
   */
  async initPageData() {
    try {
      await this.loadData(true);
    } catch (error) {
      console.error('[值班详情] 初始化失败:', error);
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
      console.log('[值班详情] 发起API请求，planId:', planId, '页码:', page);
      
      const response = await http.get(`/wx/attendance/plan/${planId}/records`, {
        page,
        size: pagination.size,
        type: 'duty'  // 明确指定为值班考勤
      });
      
      console.log('[值班详情] API响应:', response);
      
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
        
        // 计算统计数据
        const statistics = records.statistics || this.calculateStatistics(allRecords);
        
        this.setData({
          planDetail: processedPlanDetail,
          records: allRecords,
          statistics,
          pagination: newPagination,
          filteredRecords: this.filterRecords(allRecords, this.data.statusFilter)
        });
        
        console.log('[值班详情] 数据加载成功:', {
          planDetail: processedPlanDetail,
          recordCount: allRecords.length,
          statistics
        });
      } else {
        throw new Error(response.message || '获取数据失败');
      }
    } catch (error) {
      console.error('[值班详情] 加载数据失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'error'
      });
    }
  },

  /**
   * 处理值班计划详情
   */
  processPlanDetail(planDetail) {
    if (!planDetail) return {};
    
    const now = new Date();
    const startTime = new Date(planDetail.start_time || planDetail.startTime);
    const endTime = new Date(planDetail.end_time || planDetail.endTime);
    
    // 判断值班状态
    let dutyStatus, statusText;
    if (now < startTime) {
      dutyStatus = 'upcoming';
      statusText = '即将开始';
    } else if (now >= startTime && now <= endTime) {
      dutyStatus = 'ongoing';
      statusText = '进行中';
    } else {
      dutyStatus = 'ended';
      statusText = '已结束';
    }
    
    // 计算持续时间
    const durationMs = endTime.getTime() - startTime.getTime();
    const hours = Math.floor(durationMs / (1000 * 60 * 60));
    const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60));
    const duration = `${hours}小时${minutes > 0 ? minutes + '分钟' : ''}`;
    
    return {
      name: planDetail.name || '值班考勤',
      startTime: planDetail.start_time || planDetail.startTime,
      endTime: planDetail.end_time || planDetail.endTime,
      location: planDetail.location || '工作室',
      note: planDetail.note || '',
      createUserName: planDetail.create_user_name || planDetail.createUserName || '系统管理员',
      dutyStatus,
      statusText,
      duration,
      totalStudents: planDetail.total_students || planDetail.totalStudents || 0
    };
  },

  /**
   * 处理签到记录
   */
  processRecords(records) {
    if (!Array.isArray(records)) return [];
    
    return records.map(record => ({
      studentId: record.student_id || record.studentId,
      studentName: record.student_name || record.studentName,
      studentNumber: record.student_number || record.studentNumber,
      signInTime: record.sign_in_time || record.signInTime,
      attendanceStatus: record.status || record.attendance_status || record.attendanceStatus || 'pending',
      location: record.location,
      isLate: record.is_late || record.isLate || false,
      avatar: record.avatar || `/images/avatars/default.png`
    }));
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
      switch (record.attendanceStatus) {
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
   * 筛选记录
   */
  filterRecords(records, filter) {
    if (filter === 'all') return records;
    return records.filter(record => record.attendanceStatus === filter);
  },

  /**
   * 切换筛选器
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
    const filter = e.currentTarget.dataset.filter;
    const filteredRecords = this.filterRecords(this.data.records, filter);
    
    this.setData({
      statusFilter: filter,
      filteredRecords,
      showFilter: false
    });
  },



  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    console.log('[值班详情] 用户下拉刷新');
    this.setData({ refreshing: true });
    
    this.loadData(true).finally(() => {
      this.setData({ refreshing: false });
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (!this.data.pagination.hasMore || this.data.loadingMore) {
      return;
    }
    
    console.log('[值班详情] 用户上拉加载更多');
    this.setData({ loadingMore: true });
    
    this.setData({
      'pagination.page': this.data.pagination.page + 1
    });
    
    this.loadData(false).finally(() => {
      this.setData({ loadingMore: false });
    });
  },

  /**
   * 页面分享
   */
  onShareAppMessage() {
    return {
      title: `值班详情 - ${this.data.planDetail.name}`,
      path: `/pages/attendance/duty/detail?planId=${this.data.planId}`
    };
  }
}); 