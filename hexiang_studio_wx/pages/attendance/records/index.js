// pages/attendance/records/index.js
const { checkLoginStatus, getUserInfo } = require('../../../utils/auth');
const { http } = require('../../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 考勤记录列表
    records: [],
    loading: false,
    hasMore: true,
    page: 1,
    pageSize: 15,
    
    // 筛选条件
    filters: {
      status: '', // 签到状态
      type: '',   // 考勤类型
      timeRange: 'month' // 时间范围
    },
    
    // 统计数据
    statistics: {
      totalRecords: 0,
      presentCount: 0,
      lateCount: 0,
      absentCount: 0,
      leaveCount: 0
    },
    
    // UI状态
    refreshing: false,
    isEmpty: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadUserInfo();
    this.loadRecords(true);
    this.loadStatistics();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 从其他页面返回时刷新数据
    if (this.data.records.length > 0) {
      this.loadRecords(true);
      this.loadStatistics();
    }
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadRecords(true).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords(false);
    }
  },

  /**
   * 加载用户信息
   */
  async loadUserInfo() {
    try {
      const userInfo = await getUserInfo();
      this.setData({ userInfo });
    } catch (error) {
      // 用户信息获取失败不影响主要功能
    }
  },

  /**
   * 快速筛选
   */
  setQuickFilter(e) {
    const { type, value } = e.currentTarget.dataset;
    
    this.setData({
      [`filters.${type}`]: value,
      page: 1,  // 重置页码
      hasMore: true  // 重置分页状态
    });
    
    this.loadRecords(true);
    this.loadStatistics();
  },

  /**
   * 加载考勤记录
   */
  async loadRecords(refresh = true) {
    if (this.data.loading) return;
    
    try {
      this.setData({ 
        loading: true,
        refreshing: refresh 
      });
      
      const page = refresh ? 1 : this.data.page + 1;
      const { BASE_URL } = require('../../../config/index');
      
      // 构建查询参数
      const params = {
        page: page,
        pageSize: this.data.pageSize
      };
      
      // 只添加非空的筛选参数
      if (this.data.filters.status && this.data.filters.status !== '') {
        params.status = this.data.filters.status;
      }
      if (this.data.filters.type && this.data.filters.type !== '') {
        params.type = this.data.filters.type;
      }
      
      // 根据时间范围设置日期过滤
      const timeRange = this.getTimeRange(this.data.filters.timeRange);
      if (timeRange.startDate) {
        params.startDate = timeRange.startDate;
      }
      if (timeRange.endDate) {
        params.endDate = timeRange.endDate;
      }
      
      const result = await http.get(`${BASE_URL}/wx/attendance/records`, params);
      
      if ((result.success || result.code === 200 || result.code === 0) && result.data) {
        let newRecords = result.data.records || [];
        
        const originalCount = newRecords.length;
        
        // 前端筛选作为备用方案（以防后端筛选有问题）
        if (this.data.filters.status && this.data.filters.status !== '') {
          const beforeFilter = newRecords.length;
          newRecords = newRecords.filter(record => record.status === this.data.filters.status);
          if (beforeFilter > newRecords.length && refresh) {
            wx.showToast({
              title: `筛选完成，共${newRecords.length}条记录`,
              icon: 'none',
              duration: 1500
            });
          }
        }
        
        if (this.data.filters.type && this.data.filters.type !== '') {
          const beforeFilter = newRecords.length;
          newRecords = newRecords.filter(record => {
            const recordType = record.plan_type || record.type;
            return recordType === this.data.filters.type;
          });
          if (beforeFilter > newRecords.length && refresh) {
            wx.showToast({
              title: `筛选完成，共${newRecords.length}条记录`,
              icon: 'none',
              duration: 1500
            });
          }
        }
        
        const records = refresh ? newRecords : [...this.data.records, ...newRecords];
        
        // 如果前端筛选生效了，需要调整分页逻辑
        const frontendFiltered = (this.data.filters.status && this.data.filters.status !== '') || 
                                (this.data.filters.type && this.data.filters.type !== '');
        
        let hasMore;
        if (frontendFiltered && originalCount > newRecords.length) {
          // 前端筛选生效时，如果原始记录数等于pageSize，可能还有更多数据
          hasMore = originalCount >= this.data.pageSize;
        } else {
          // 正常情况或后端筛选生效
          hasMore = newRecords.length >= this.data.pageSize;
        }
        
        this.setData({
          records: records,
          page: page,
          hasMore: hasMore,
          isEmpty: records.length === 0
        });
        

      } else {
        wx.showToast({
          title: result.message || result.msg || '加载失败',
          icon: 'none'
        });
      }
    } catch (error) {
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    } finally {
      this.setData({ 
        loading: false,
        refreshing: false 
      });
    }
  },

  /**
   * 加载统计数据
   */
  async loadStatistics() {
    try {
      const { BASE_URL } = require('../../../config/index');
      
      // 获取本月统计
      const params = {
        page: 1,
        pageSize: 1000,
        startDate: this.getTimeRange('month').startDate
      };
      
      // 应用当前筛选条件到统计数据
      if (this.data.filters.status && this.data.filters.status !== '') {
        params.status = this.data.filters.status;
      }
      if (this.data.filters.type && this.data.filters.type !== '') {
        params.type = this.data.filters.type;
      }
      
      const result = await http.get(`${BASE_URL}/wx/attendance/records`, params);
      
      if ((result.success || result.code === 200 || result.code === 0) && result.data) {
        let records = result.data.records || [];
        
        // 应用前端筛选（与主列表保持一致）
        if (this.data.filters.status && this.data.filters.status !== '') {
          records = records.filter(record => record.status === this.data.filters.status);
        }
        
        if (this.data.filters.type && this.data.filters.type !== '') {
          records = records.filter(record => {
            const recordType = record.plan_type || record.type;
            return recordType === this.data.filters.type;
          });
        }
        
        const statistics = {
          totalRecords: records.length,
          presentCount: records.filter(r => r.status === 'present').length,
          lateCount: records.filter(r => r.status === 'late').length,
          absentCount: records.filter(r => r.status === 'absent').length,
          leaveCount: records.filter(r => r.status === 'leave').length
        };
        
        this.setData({ statistics });
      }
    } catch (error) {
      // 统计数据加载失败不影响主要功能
    }
  },

  /**
   * 获取时间范围
   */
  getTimeRange(type) {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    switch (type) {
      case 'week':
        const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
        return {
          startDate: this.formatDate(weekAgo),
          endDate: this.formatDate(today)
        };
      case 'month':
        const monthAgo = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
        return {
          startDate: this.formatDate(monthAgo),
          endDate: this.formatDate(today)
        };
      default:
        return {};
    }
  },

  /**
   * 格式化日期为 YYYY-MM-DD
   */
  formatDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  },

  /**
   * 查看记录详情
   */
  viewRecordDetail(e) {
    const record = e.currentTarget.dataset.record;
    
    // 格式化显示内容
    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    };
    
    const getStatusText = (status) => {
      switch(status) {
        case 'present': return '已签到';
        case 'late': return '迟到';
        case 'absent': return '缺勤';
        case 'leave': return '请假';
        case 'pending': return '待签到';
        default: return status;
      }
    };
    
    const getTypeText = (type) => {
      switch(type) {
        case 'activity': return '活动考勤';
        case 'course': return '课程考勤';
        case 'duty': return '值班考勤';
        default: return type;
      }
    };
    
    // 兼容不同字段名（后端可能返回 plan_name 或 planName）
    const planName = record.plan_name || record.planName || '考勤计划';
    const planType = record.plan_type || record.type;
    const startTime = record.start_time || record.startTime;
    const endTime = record.end_time || record.endTime;
    const signInTime = record.sign_in_time || record.signInTime;
    
    let content = `考勤计划：${planName}\n`;
    content += `考勤类型：${getTypeText(planType)}\n`;
    content += `考勤时间：${formatTime(startTime)} - ${formatTime(endTime)}\n`;
    content += `签到状态：${getStatusText(record.status)}\n`;
    
    if (signInTime) {
      content += `签到时间：${formatTime(signInTime)}\n`;
    }
    if (record.location) {
      content += `签到地点：${record.location}\n`;
    }
    if (record.remark) {
      content += `备注：${record.remark}`;
    }
    
    wx.showModal({
      title: '考勤详情',
      content: content,
      showCancel: false,
      confirmText: '知道了'
    });
  }
});