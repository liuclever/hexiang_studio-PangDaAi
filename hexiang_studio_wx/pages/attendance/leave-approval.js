// pages/attendance/leave-approval.js
const { http, BASE_URL, FILE_URL } = require('../../utils/request');
const { checkPermission } = require('../../utils/auth');
const { showDownloadHelp } = require('../../utils/fileHelper');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    // 当前激活的标签页
    activeTab: 'leave', // 'task' | 'leave' | 'records'
    
    // 审批统计数据
    pendingTaskSubmissions: 0,
    pendingLeaveRequests: 0,
    todayProcessed: 0,
    
    // 任务提交审批列表
    taskSubmissions: [],
    taskSubmissionLoading: false,
    taskSubmissionHasMore: true,
    taskSubmissionPage: 1,
    
    // 请假申请审批列表
    leaveRequests: [],
    leaveRequestLoading: false,
    leaveRequestHasMore: true,
    leaveRequestPage: 1,

    // 已审批记录
    approvalRecords: [],
    recordsLoading: false,
    timeFilter: 3, // 默认查询3天内的记录
    
    // 弹窗控制
    showTaskSubmissionDetail: false,
    currentTaskSubmission: null,
    showLeaveRequestDetail: false,
    currentLeaveRequest: null,
    showRejectDialog: false,
    rejectType: '', // 'task' | 'leave'
    rejectTargetId: null,
    rejectReason: '',
    
    // 页面状态
    refreshing: false,
    pageLoading: true
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('[审批中心] 页面加载, 参数:', options);
    
    // 检查管理员或老师权限
    const userRole = wx.getStorageSync('role');
    if (userRole !== 'admin' && userRole !== 'teacher') {
      wx.showToast({
        title: '权限不足',
        icon: 'none',
        duration: 2000
      });
      wx.navigateBack();
      return;
    }
    
    // 处理URL参数，设置默认tab
    if (options.tab) {
      console.log('[审批中心] 设置默认tab为:', options.tab);
      this.setData({ 
        activeTab: options.tab 
      });
    }
    
    // 初始化页面数据
    this.initPageData();
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
    // 页面显示时刷新数据
    if (!this.data.pageLoading) {
      this.refreshData();
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
   * 初始化页面数据
   */
  async initPageData() {
    console.log('[审批中心] 初始化页面数据，当前activeTab:', this.data.activeTab);
    
    try {
      this.setData({ pageLoading: true });
      
      // 根据当前tab加载对应数据
      const loadPromises = [this.loadStatistics()];
      
      if (this.data.activeTab === 'task') {
        loadPromises.push(this.loadTaskSubmissions(true));
      } else if (this.data.activeTab === 'leave') {
        loadPromises.push(this.loadLeaveRequests(true));
      } else if (this.data.activeTab === 'records') {
        loadPromises.push(this.loadApprovalRecords());
      }
      
      // 并行加载统计数据和列表数据
      await Promise.all(loadPromises);
      
    } catch (error) {
      console.error('[审批中心] 初始化页面数据失败:', error);
      wx.showToast({
        title: '加载数据失败',
        icon: 'none'
      });
    } finally {
      this.setData({ pageLoading: false });
    }
  },

  /**
   * 加载统计数据 - 并行请求优化
   */
  async loadStatistics() {
    console.log('[审批中心] 加载统计数据（并行请求）');
    
    try {
      // ✅ 并行发送所有统计请求，大幅减少总耗时
      const [taskCountRes, leaveCountRes, todayProcessedRes] = await Promise.all([
        http.get('/wx/task/submission/pending/count'),
        // 使用微信端接口获取请假审批数量
        http.get('/wx/approval/leave/pending/count'),
        http.get('/wx/approval/today/processed/count')
      ]);
      
      console.log('[审批中心] 并行统计请求完成:', {
        taskCount: taskCountRes,
        leaveCount: leaveCountRes.data?.count || 0, // 显示实际数量
        todayProcessed: todayProcessedRes
      });
      
      // 批量更新所有统计数据
      const updateData = {};
      
      if (taskCountRes.code === 200) {
        updateData.pendingTaskSubmissions = taskCountRes.data?.count || 0;
      }
      
      if (leaveCountRes.code === 200) {
        // 从count接口获取准确数量
        updateData.pendingLeaveRequests = leaveCountRes.data?.count || 0;
      }
      
      if (todayProcessedRes.code === 200) {
        updateData.todayProcessed = todayProcessedRes.data?.count || 0;
      }
      
      // 一次性更新所有数据，减少页面重绘
      this.setData(updateData);
      
    } catch (error) {
      console.error('[审批中心] 加载统计数据失败:', error);
      // 显示错误信息给用户
      wx.showToast({
        title: error.message || '加载统计数据失败',
        icon: 'none',
        duration: 2000
      });
    }
  },

  /**
   * 加载任务提交审批列表
   */
  async loadTaskSubmissions(reset = false) {
    console.log('[审批中心] 加载任务提交列表, reset:', reset);
    
    if (this.data.taskSubmissionLoading) return;
    
    const page = reset ? 1 : this.data.taskSubmissionPage;
    
    try {
      this.setData({ taskSubmissionLoading: true });
      
      // 调用真实API
      const response = await http.get('/wx/task/submission/list', {
        page: page,
        pageSize: 10,
        status: 2 // 只查询待审核的提交
      });
      
      if (response.code === 200) {
        const { records, total } = response.data;
        
        // 处理数据格式
        const processedRecords = records.map(item => {
          // 处理头像路径
          let userAvatar = item.userAvatar || '/images/icons/default-avatar.png';
          if (item.userAvatar && !item.userAvatar.startsWith('http') && !item.userAvatar.startsWith('/images/')) {
            userAvatar = `${BASE_URL}/wx/file/view/${item.userAvatar}`;
          }
          
          return {
            submissionId: item.submissionId,
            subTaskId: item.subTaskId,
            subTaskTitle: item.subTaskTitle || '未知子任务',
            taskName: item.taskName || '未知任务',
            userId: item.userId,
            userName: item.userName || '未知用户',
            userAvatar: userAvatar,
            submissionNotice: item.submissionNotice || '无说明',
            status: item.status,
            statusText: this.getStatusText(item.status),
            submissionTime: this.formatTime(item.submissionTime),
            reviewComment: item.reviewComment
          };
        });
        
        this.setData({
          taskSubmissions: reset ? processedRecords : [...this.data.taskSubmissions, ...processedRecords],
          taskSubmissionPage: page + 1,
          taskSubmissionHasMore: records.length === 10 // 如果返回的数量等于pageSize，说明可能还有更多数据
        });
        
        console.log('[审批中心] 任务提交列表加载成功, 数量:', processedRecords.length);
      } else {
        throw new Error(response.message || '获取数据失败');
      }
      
    } catch (error) {
      console.error('[审批中心] 加载任务提交列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ taskSubmissionLoading: false });
    }
  },

  /**
   * 加载请假申请审批列表
   * @param {boolean} reset - 是否重置列表
   */
  async loadLeaveRequests(reset = false) {
    console.log('[审批中心] 加载请假申请列表, reset:', reset);
    
    if (this.data.leaveRequestLoading) return;
    
    const page = reset ? 1 : this.data.leaveRequestPage;
    
    try {
      this.setData({ leaveRequestLoading: true });
      
      // 调用管理员审批接口
      const response = await http.get('/admin/approval/leave/list', {
        page: page,
        pageSize: 10,
        status: 'pending' // 只查询待审核的请假申请
      });
      
      if (response.code === 200) {
        const { records, total } = response.data;
        
        // 处理数据格式
        const processedRecords = records.map(item => {
          // 处理头像路径
          let userAvatar = null;
          if (item.studentAvatar && item.studentAvatar.trim()) {
            // 如果有头像，构建完整URL
            if (item.studentAvatar.startsWith('http')) {
              userAvatar = item.studentAvatar;
            } else {
              userAvatar = `${BASE_URL}/wx/file/view/${item.studentAvatar}`;
          }
          }
          // 如果没有头像，userAvatar保持null，让t-avatar组件显示默认头像
          
          return {
            requestId: item.requestId,
            userName: item.studentName || '未知用户',
            userAvatar: userAvatar,
            userInitial: (item.studentName || '未').charAt(0),
            attendancePlanName: item.attendancePlanName,
            leaveType: this.getLeaveTypeText(item.type) || '事假',
            dateTimeRange: this.formatDateTimeRange(item.startTime, item.endTime),
            startDate: item.startTime,
            endDate: item.endTime,
            reason: item.reason || '无原因说明',
            applyTime: this.formatTime(item.createTime),
            status: item.status || 'pending',
            attachments: item.attachments
          };
        });
        
        this.setData({
          leaveRequests: reset ? processedRecords : [...this.data.leaveRequests, ...processedRecords],
          leaveRequestPage: page + 1,
          leaveRequestHasMore: records.length === 10
        });
        
        console.log('[审批中心] 请假申请列表加载成功, 数量:', processedRecords.length);
      } else {
        throw new Error(response.message || response.msg || '获取数据失败');
      }
      
    } catch (error) {
      console.error('[审批中心] 加载请假申请列表失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ leaveRequestLoading: false });
    }
  },



  /**
   * 切换标签页
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    console.log('[审批中心] 切换标签页:', tab);
    
    this.setData({ activeTab: tab });
    
    // 加载对应标签页的数据
    if (tab === 'task' && this.data.taskSubmissions.length === 0) {
      this.loadTaskSubmissions(true);
    } else if (tab === 'leave' && this.data.leaveRequests.length === 0) {
      this.loadLeaveRequests(true);
    } else if (tab === 'records' && this.data.approvalRecords.length === 0) {
      this.loadApprovalRecords();
    }
  },

  /**
   * 下拉刷新
   */
  async onRefresh() {
    console.log('[审批中心] 下拉刷新');
    
    this.setData({ refreshing: true });
    
    try {
      await this.refreshData();
    } finally {
      this.setData({ refreshing: false });
    }
  },

  /**
   * 刷新数据
   */
  async refreshData() {
    await this.loadStatistics();
    
    if (this.data.activeTab === 'task') {
      await this.loadTaskSubmissions(true);
    } else if (this.data.activeTab === 'leave') {
      await this.loadLeaveRequests(true);
    } else if (this.data.activeTab === 'records') {
      await this.loadApprovalRecords();
    }
  },

  /**
   * 查看任务提交详情
   */
  async viewTaskSubmissionDetail(e) {
    const submissionId = e.currentTarget.dataset.id;
    const submission = this.data.taskSubmissions.find(item => item.submissionId === submissionId);
    
    console.log('[审批中心] 查看任务提交详情:', submission);
    
    if (!submission) return;
    
    try {
      wx.showLoading({ title: '加载详情...' });
      
      // 【修复】直接通过提交ID获取附件信息
      const response = await http.get(`/wx/task/submission/attachments/${submission.submissionId}`);
      
      let attachments = [];
      if (response.code === 200) {
        attachments = response.data || [];
        console.log('[审批中心] 获取附件成功，数量:', attachments.length);
      } else {
        console.warn('[审批中心] 获取附件失败:', response.message);
      }
      
      // 合并基本信息和附件信息
      const fullSubmission = {
        ...submission,
        attachments: attachments
      };
      
      this.setData({
        currentTaskSubmission: fullSubmission,
        showTaskSubmissionDetail: true
      });
      
      console.log('[审批中心] 任务提交详情加载完成');
      console.log('- 提交说明:', submission.submissionNotice || '(无说明)');
      console.log('- 附件数量:', attachments.length);
      console.log('- 审核评论:', submission.reviewComment || '(无评论)');
      
    } catch (error) {
      console.error('[审批中心] 获取任务提交详情失败:', error);
      // 出错时使用基本信息，但不显示附件
      this.setData({
        currentTaskSubmission: { ...submission, attachments: [] },
        showTaskSubmissionDetail: true
      });
    } finally {
      wx.hideLoading();
    }
  },

  /**
   * 关闭任务提交详情
   */
  closeTaskSubmissionDetail() {
    this.setData({
      showTaskSubmissionDetail: false,
      currentTaskSubmission: null
    });
  },

  /**
   * 查看请假申请详情
   */
  async viewLeaveRequestDetail(e) {
    const requestId = e.currentTarget.dataset.id;
    const leaveRequest = this.data.leaveRequests.find(item => item.requestId === requestId);
    
    console.log('[审批中心] 查看请假申请详情:', leaveRequest);
    
    if (!leaveRequest) return;
    
    try {
      wx.showLoading({ title: '加载详情...' });
      
      // 获取请假申请详情（包含附件信息）
      const response = await http.get(`/admin/approval/leave/${requestId}`);
      
      if (response.code === 200 && response.data) {
        const fullRequest = {
          ...leaveRequest,
          ...response.data,
          // 处理附件信息
          attachmentFiles: response.data.attachments ? JSON.parse(response.data.attachments || '[]') : []
        };
        
        this.setData({
          currentLeaveRequest: fullRequest,
          showLeaveRequestDetail: true
        });
        
        console.log('[审批中心] 请假申请详情加载完成');
        console.log('- 请假原因:', fullRequest.reason || '(无原因)');
        console.log('- 附件数量:', fullRequest.attachmentFiles.length);
        console.log('- 审批备注:', fullRequest.remark || '(无备注)');
      } else {
        throw new Error(response.msg || '获取详情失败');
      }
      
    } catch (error) {
      console.error('[审批中心] 获取请假申请详情失败:', error);
      wx.showToast({
        title: '获取详情失败',
        icon: 'error'
      });
    } finally {
      wx.hideLoading();
    }
  },

  /**
   * 关闭请假申请详情
   */
  closeLeaveRequestDetail() {
    this.setData({
      showLeaveRequestDetail: false,
      currentLeaveRequest: null
    });
  },

  /**
   * 任务提交详情弹窗可见性变化
   */
  onTaskSubmissionDetailVisibleChange(e) {
    if (!e.detail.visible) {
      this.closeTaskSubmissionDetail();
    }
  },

  /**
   * 通过任务提交
   */
  async approveTaskSubmission(e) {
    const submissionId = e.currentTarget.dataset.id;
    
    console.log('[审批中心] 通过任务提交:', submissionId);
    
    wx.showModal({
      title: '确认操作',
      content: '确定要通过这个任务提交吗？',
      success: async (res) => {
        if (res.confirm) {
          // ✅ 立即关闭任务详情弹窗，改善用户体验
          const isFromTaskDetail = this.data.showTaskSubmissionDetail;
          if (isFromTaskDetail) {
            console.log('[审批中心] 任务详情通过 - 立即关闭弹窗');
            this.closeTaskSubmissionDetail();
          }
          
          await this.processTaskSubmissionApproval(submissionId, 1, '审核通过');
        }
      }
    });
  },

  /**
   * 退回任务提交
   */
  rejectTaskSubmission(e) {
    const submissionId = e.currentTarget.dataset.id;
    
    console.log('[审批中心] 退回任务提交:', submissionId);
    
    this.setData({
      showRejectDialog: true,
      rejectType: 'task',
      rejectTargetId: submissionId,
      rejectReason: ''
    });
  },

  /**
   * 处理任务提交审批
   */
  async processTaskSubmissionApproval(submissionId, status, comment) {
    console.log('[审批中心] 处理任务提交审批:', { submissionId, status, comment });
    wx.showLoading({ title: status === 1 ? '通过中...' : '退回中...' });
    
    try {
      // 调用真实API
      const response = await http.post(`/wx/task/submission/review/${submissionId}`, {
        status: status,
        reviewComment: comment
      });
      
      console.log('[审批中心] API响应:', response);
      
      if (response.code === 200) {
        wx.hideLoading();
        wx.showToast({
          title: status === 1 ? '审核通过' : '已退回',
          icon: 'success'
        });
        
        // 刷新列表中的状态
        const taskSubmissions = this.data.taskSubmissions.map(item => {
          if (item.submissionId === submissionId) {
            return {
              ...item,
              status: status,
              statusText: this.getStatusText(status),
              reviewComment: comment
            };
          }
          return item;
        });
        
        this.setData({ taskSubmissions });
        
        console.log('[审批中心] 任务提交审批处理完成');
        
        // ✅ 异步刷新统计数据，不阻塞用户操作
        setTimeout(() => {
          this.loadStatistics();
        }, 100);
      } else {
        throw new Error(response.message || response.msg || '处理失败');
      }
      
    } catch (error) {
      console.error('[审批中心] 处理任务提交审批失败:', error);
      wx.hideLoading();
      wx.showToast({
        title: `处理失败: ${error.message || '网络错误'}`,
        icon: 'none',
        duration: 3000
      });
      
      // 重新显示退回弹窗
      this.setData({
        showRejectDialog: true,
        rejectType: 'task',
        rejectTargetId: submissionId,
        rejectReason: comment || ''
      });
    }
  },

  /**
   * 批准请假申请
   */
  async approveLeaveRequest(e) {
    const requestId = e.currentTarget.dataset.id;
    
    console.log('[审批中心] 批准请假申请，ID:', requestId);
    
    wx.showModal({
      title: '确认操作',
      content: '确定要批准这个请假申请吗？',
      success: async (res) => {
        if (res.confirm) {
          await this.processLeaveRequestApproval(requestId, 'approve');
          
          // 关闭详情弹窗
          this.setData({
            showLeaveRequestDetail: false,
            currentLeaveRequest: null
          });
        }
      }
    });
  },

  /**
   * 驳回请假申请
   */
  rejectLeaveRequest(e) {
    const requestId = e.currentTarget.dataset.id;
    
    console.log('[审批中心] 驳回请假申请，ID:', requestId);
    
    this.setData({
      showRejectDialog: true,
      rejectType: 'leave',
      rejectTargetId: requestId,
      rejectReason: ''
    });
  },

  /**
   * 处理请假申请审批
   */
  async processLeaveRequestApproval(requestId, action, reason = '') {
    console.log('[审批中心] 处理请假申请审批:', { requestId, action, reason });
    wx.showLoading({ title: action === 'approve' ? '批准中...' : '驳回中...' });
    
    try {
      // 调用管理员审批接口
      const response = await http.post(`/admin/approval/leave/${requestId}/${action}`, 
        action === 'reject' ? { remark: reason } : {}
      );
      
      console.log('[审批中心] 请假申请审批API响应:', response);
      
      if (response.code === 200) {
        wx.hideLoading();
        wx.showToast({
          title: action === 'approve' ? '已批准' : '已驳回',
          icon: 'success'
        });
        
        // 从列表中移除已处理的申请
        const leaveRequests = this.data.leaveRequests.filter(item => 
          item.requestId !== requestId
        );
        
        this.setData({ leaveRequests });
        
        // 刷新统计数据
        await this.loadStatistics();
        
        // 关闭详情弹窗（如果正在显示）
        if (this.data.showLeaveRequestDetail) {
          this.setData({
            showLeaveRequestDetail: false,
            currentLeaveRequest: null
          });
        }
        
        console.log('[审批中心] 请假申请审批处理完成');
      } else {
        throw new Error(response.message || response.msg || '处理失败');
      }
      
    } catch (error) {
      console.error('[审批中心] 处理请假申请审批失败:', error);
      wx.hideLoading();
      wx.showToast({
        title: `处理失败: ${error.message || '网络错误'}`,
        icon: 'none',
        duration: 3000
      });
      
      // 如果是驳回失败，重新显示驳回弹窗
      if (action === 'reject') {
        this.setData({
          showRejectDialog: true,
          rejectType: 'leave',
          rejectTargetId: requestId,
          rejectReason: reason || ''
        });
      }
    }
  },

  /**
   * 驳回原因输入事件 (input)
   */
  onRejectReasonInput(e) {
    console.log('[审批中心] ========== INPUT事件触发 ==========');
    this._handleRejectReasonEvent(e, 'INPUT');
  },

  /**
   * 驳回原因变化事件 (change)
   */
  onRejectReasonChange(e) {
    console.log('[审批中心] ========== CHANGE事件触发 ==========');
    this._handleRejectReasonEvent(e, 'CHANGE');
  },

  /**
   * 驳回原因失焦事件 (blur)
   */
  onRejectReasonBlur(e) {
    console.log('[审批中心] ========== BLUR事件触发 ==========');
    this._handleRejectReasonEvent(e, 'BLUR');
  },

  /**
   * 驳回原因聚焦事件 (focus)
   */
  onRejectReasonFocus(e) {
    console.log('[审批中心] ========== FOCUS事件触发 ==========');
    this._handleRejectReasonEvent(e, 'FOCUS');
  },

  /**
   * 统一处理退回原因相关事件
   */
  _handleRejectReasonEvent(e, eventType) {
    console.log(`[审批中心] [${eventType}] 完整事件对象:`, e);
    console.log(`[审批中心] [${eventType}] 事件类型:`, e.type);
    console.log(`[审批中心] [${eventType}] 事件目标:`, e.target);
    console.log(`[审批中心] [${eventType}] 事件detail:`, e.detail);
    console.log(`[审批中心] [${eventType}] e.detail.value:`, e.detail.value);
    console.log(`[审批中心] [${eventType}] e.detail.value类型:`, typeof e.detail.value);
    console.log(`[审批中心] [${eventType}] e.detail.value长度:`, e.detail.value ? e.detail.value.length : 'no length');
    console.log(`[审批中心] [${eventType}] 当前数据状态:`, this.data.rejectReason);
    
    const value = e.detail.value;
    
    // 只在有实际值变化时更新
    if (eventType === 'INPUT' || eventType === 'CHANGE' || eventType === 'BLUR') {
      this.setData({
        rejectReason: value
      });
      
      console.log(`[审批中心] [${eventType}] 设置后的数据状态:`, this.data.rejectReason);
      console.log(`[审批中心] [${eventType}] 设置后的数据状态类型:`, typeof this.data.rejectReason);
      console.log(`[审批中心] [${eventType}] 设置后的数据状态长度:`, this.data.rejectReason ? this.data.rejectReason.length : 'no length');
    }
  },

  /**
   * 确认驳回/退回
   */
  async confirmReject() {
    console.log('[审批中心] ========== 确认退回函数开始 ==========');
    console.log('[审批中心] 完整的数据状态:', JSON.stringify(this.data, null, 2));
    
    const { rejectType, rejectTargetId, rejectReason } = this.data;
    
    console.log('[审批中心] 解构后的数据:');
    console.log('[审批中心] - rejectType:', rejectType, '(类型:', typeof rejectType, ')');
    console.log('[审批中心] - rejectTargetId:', rejectTargetId, '(类型:', typeof rejectTargetId, ')');
    console.log('[审批中心] - rejectReason:', rejectReason, '(类型:', typeof rejectReason, ')');
    console.log('[审批中心] - rejectReason长度:', typeof rejectReason === 'string' ? rejectReason.length : 'not a string');
    console.log('[审批中心] - rejectReason.trim():', typeof rejectReason === 'string' ? `"${rejectReason.trim()}"` : 'cannot trim');
    
    // 检查退回原因是否为空
    const isReasonEmpty = !rejectReason || typeof rejectReason !== 'string' || rejectReason.trim() === '';
    
    console.log('[审批中心] 退回原因检查:');
    console.log('[审批中心] - 原因是否为空:', isReasonEmpty);
    console.log('[审批中心] - !rejectReason:', !rejectReason);
    console.log('[审批中心] - typeof rejectReason !== "string":', typeof rejectReason !== 'string');
    console.log('[审批中心] - rejectReason.trim() === "":', typeof rejectReason === 'string' ? rejectReason.trim() === '' : 'cannot check');
    
    if (isReasonEmpty) {
      console.log('[审批中心] ❌ 退回原因为空，要求用户重新输入');
      wx.showToast({
        title: '请输入退回原因',
        icon: 'none'
      });
      return;
    }
    
    if (!rejectTargetId) {
      console.log('[审批中心] 退回目标ID为空');
      wx.showToast({
        title: '参数错误，请重试',
        icon: 'none'
      });
      return;
    }
    
    try {
      console.log('[审批中心] ✅ 退回原因验证通过，开始处理...');
      
      // 先保存原因，避免在关闭弹窗时被清空
      const savedReason = rejectReason.trim();
      const savedType = rejectType;
      const savedTargetId = rejectTargetId;
      
      console.log('[审批中心] 保存的数据:');
      console.log('[审批中心] - savedReason:', savedReason, '(长度:', savedReason.length, ')');
      console.log('[审批中心] - savedType:', savedType);
      console.log('[审批中心] - savedTargetId:', savedTargetId);
      
      console.log('[审批中心] 准备关闭弹窗并清空状态...');
      
      // 关闭对话框
      this.setData({ 
        showRejectDialog: false,
        rejectReason: '',
        rejectType: '',
        rejectTargetId: null
      });
      
      console.log('[审批中心] 弹窗已关闭，数据已清空');
      console.log('[审批中心] 当前data状态:', {
        showRejectDialog: this.data.showRejectDialog,
        rejectReason: this.data.rejectReason,
        rejectType: this.data.rejectType,
        rejectTargetId: this.data.rejectTargetId
      });
      
      if (savedType === 'task') {
        console.log('[审批中心] 🔄 开始处理任务提交审批');
        console.log('[审批中心] 调用参数: submissionId =', savedTargetId, ', status = 3, comment =', savedReason);
        
        // ✅ 如果在任务详情弹窗中操作，立即关闭弹窗改善用户体验
        const isFromTaskDetail = this.data.showTaskSubmissionDetail;
        if (isFromTaskDetail) {
          console.log('[审批中心] 任务详情退回 - 立即关闭弹窗');
          this.closeTaskSubmissionDetail();
        }
        
        await this.processTaskSubmissionApproval(savedTargetId, 3, savedReason);
      } else {
        console.log('[审批中心] 🔄 开始处理请假申请审批');
        console.log('[审批中心] 调用参数: requestId =', savedTargetId, ', action = reject, reason =', savedReason);
        
        // ✅ 如果在请假详情弹窗中操作，立即关闭弹窗改善用户体验
        const isFromLeaveDetail = this.data.showLeaveRequestDetail;
        if (isFromLeaveDetail) {
          console.log('[审批中心] 请假详情驳回 - 立即关闭弹窗');
          this.setData({
            showLeaveRequestDetail: false,
            currentLeaveRequest: null
          });
        }
        
        await this.processLeaveRequestApproval(savedTargetId, 'reject', savedReason);
      }
      
    } catch (error) {
      console.error('[审批中心] 确认退回失败:', error);
      wx.showToast({
        title: '操作失败，请重试',
        icon: 'none'
      });
    }
  },

  /**
   * 取消驳回
   */
  cancelReject() {
    console.log('[审批中心] ========== 取消驳回函数调用 ==========');
    console.log('[审批中心] 取消前的数据状态:', {
      showRejectDialog: this.data.showRejectDialog,
      rejectReason: this.data.rejectReason,
      rejectType: this.data.rejectType,
      rejectTargetId: this.data.rejectTargetId
    });
    
    this.setData({
      showRejectDialog: false,
      rejectType: '',
      rejectTargetId: null,
      rejectReason: ''
    });
    
    console.log('[审批中心] 取消后的数据状态:', {
      showRejectDialog: this.data.showRejectDialog,
      rejectReason: this.data.rejectReason,
      rejectType: this.data.rejectType,
      rejectTargetId: this.data.rejectTargetId
    });
  },

  /**
   * 驳回弹窗可见性变化
   */
  onRejectDialogVisibleChange(e) {
    console.log('[审批中心] ========== 弹窗可见性变化事件 ==========');
    console.log('[审批中心] 事件详情:', e.detail);
    console.log('[审批中心] 弹窗是否可见:', e.detail.visible);
    console.log('[审批中心] 当前退回原因:', this.data.rejectReason);
    console.log('[审批中心] 当前退回原因长度:', this.data.rejectReason ? this.data.rejectReason.length : 'null/undefined');
    
    // 当弹窗关闭时，只有在数据还存在的情况下才清空（说明是用户取消而不是确认）
    if (!e.detail.visible && this.data.rejectReason) {
      console.log('[审批中心] 🔄 弹窗关闭且存在退回原因，调用取消函数');
      this.cancelReject();
    } else {
      console.log('[审批中心] ⏹️ 弹窗关闭但无退回原因，不调用取消函数');
    }
  },

  /**
   * 预览附件
   */
  previewAttachment(e) {
    const attachment = e.currentTarget.dataset.attachment;
    console.log('[审批中心] 预览附件:', attachment);
    
    wx.showToast({
      title: '附件预览功能开发中',
      icon: 'none'
    });
  },

  /**
   * 查看请假详情
   */
  viewLeaveDetail(e) {
    const request = e.currentTarget.dataset.request;
    console.log('[审批中心] 查看请假详情:', request);
    
    wx.showToast({
      title: '请假详情功能开发中',
      icon: 'none'
    });
  },

  /**
   * 阻止事件冒泡
   */
  stopPropagation(e) {
    console.log('[审批中心] stopPropagation调用，事件对象:', e);
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation();
      console.log('[审批中心] 成功阻止事件冒泡');
    } else {
      console.log('[审批中心] 事件对象没有stopPropagation方法');
    }
  },



  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.onRefresh();
    wx.stopPullDownRefresh();
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {
    if (this.data.activeTab === 'task' && this.data.taskSubmissionHasMore) {
      this.loadTaskSubmissions();
    } else if (this.data.activeTab === 'leave' && this.data.leaveRequestHasMore) {
      this.loadLeaveRequests();
    }
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    return {
      title: '何湘工作室审批中心',
      path: '/pages/attendance/leave-approval/index'
    };
  },

  /**
   * 获取任务状态文本
   */
  getStatusText(status) {
    switch (status) {
      case 1:
        return '已通过';
      case 2:
        return '待审核';
      case 3:
        return '已退回';
      default:
        return '未知状态';
    }
  },

  /**
   * 获取请假类型显示文本
   */
  getLeaveTypeText(type) {
    switch (type) {
      case 'sick_leave':
        return '病假';
      case 'personal_leave':
        return '事假';
      case 'annual_leave':
        return '年假';
      case 'maternity_leave':
        return '产假';
      case 'paternity_leave':
        return '陪产假';
      case 'marriage_leave':
        return '婚假';
      case 'funeral_leave':
        return '丧假';
      case 'other':
        return '其他';
      default:
        return '事假';
    }
  },

  /**
   * 切换时间筛选
   */
  switchTimeFilter(e) {
    const days = parseInt(e.currentTarget.dataset.days);
    console.log('[审批中心] 切换时间筛选:', days);
    
    this.setData({ timeFilter: days });
    this.loadApprovalRecords();
  },

  /**
   * 加载已审批记录
   */
  async loadApprovalRecords() {
    console.log('[审批中心] 加载已审批记录, 时间范围:', this.data.timeFilter, '天');
    
    this.setData({ recordsLoading: true });
    
    try {
      const response = await http.get('/wx/approval/records', {
        days: this.data.timeFilter
      });
      
      console.log('[审批中心] 已审批记录API响应:', response);
      
      if (response.code === 200 && response.data) {
        // 处理时间格式和头像路径
        const processedRecords = response.data.map(record => {
          // 处理头像路径
          let applicantAvatar = record.applicantAvatar || '/images/icons/default-avatar.png';
          if (record.applicantAvatar && !record.applicantAvatar.startsWith('http') && !record.applicantAvatar.startsWith('/images/')) {
            applicantAvatar = `${BASE_URL}/wx/file/view/${record.applicantAvatar}`;
          }
          
          return {
            ...record,
            applicantAvatar: applicantAvatar,
            reviewTime: this.formatTime(record.reviewTime),
            applicationTime: this.formatTime(record.applicationTime)
          };
        });
        
        this.setData({
          approvalRecords: processedRecords
        });
        
        console.log('[审批中心] 已审批记录加载成功, 数量:', processedRecords.length);
      } else {
        console.error('[审批中心] 加载已审批记录失败:', response.msg || '未知错误');
        wx.showToast({
          title: response.msg || '加载记录失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('[审批中心] 加载已审批记录失败:', error);
      wx.showToast({
        title: '加载记录失败',
        icon: 'none'
      });
    } finally {
      this.setData({ recordsLoading: false });
    }
  },

  /**
   * 格式化时间显示
   */
  formatTime(timeString) {
    if (!timeString) return '';
    
    const time = new Date(timeString);
    const now = new Date();
    const diffMs = now - time;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);
    
    // 处理未来时间或时间异常的情况
    if (diffMs < 0) {
      // 如果是未来时间，显示具体日期
      const month = String(time.getMonth() + 1).padStart(2, '0');
      const day = String(time.getDate()).padStart(2, '0');
      const hours = String(time.getHours()).padStart(2, '0');
      const minutes = String(time.getMinutes()).padStart(2, '0');
      return `${month}-${day} ${hours}:${minutes}`;
    }
    
    if (diffDays === 0) {
      if (diffHours === 0) {
        const diffMinutes = Math.floor(diffMs / (1000 * 60));
        return diffMinutes <= 0 ? '刚刚' : `${diffMinutes}分钟前`;
      }
      return `${diffHours}小时前`;
    } else if (diffDays > 0 && diffDays <= 7) {
      return `${diffDays}天前`;
    } else {
      // 超过7天或时间异常，显示具体日期
      const year = time.getFullYear();
      const currentYear = now.getFullYear();
      const month = String(time.getMonth() + 1).padStart(2, '0');
      const day = String(time.getDate()).padStart(2, '0');
      const hours = String(time.getHours()).padStart(2, '0');
      const minutes = String(time.getMinutes()).padStart(2, '0');
      
      // 如果是当年的日期，不显示年份
      if (year === currentYear) {
        return `${month}-${day} ${hours}:${minutes}`;
      } else {
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      }
    }
  },

  /**
   * 格式化日期时间范围显示
   */
  formatDateTimeRange(startTime, endTime) {
    if (!startTime || !endTime) return '';
    
    const start = new Date(startTime);
    const end = new Date(endTime);
    
    const formatDateTime = (date) => {
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${month}-${day} ${hours}:${minutes}`;
    };
    
    const startStr = formatDateTime(start);
    const endStr = formatDateTime(end);
    
    // 如果是同一天，只显示一次日期
    const startDate = `${start.getMonth() + 1}-${start.getDate()}`;
    const endDate = `${end.getMonth() + 1}-${end.getDate()}`;
    
    if (startDate === endDate) {
      const startTime = `${String(start.getHours()).padStart(2, '0')}:${String(start.getMinutes()).padStart(2, '0')}`;
      const endTime = `${String(end.getHours()).padStart(2, '0')}:${String(end.getMinutes()).padStart(2, '0')}`;
      return `${startDate} ${startTime}-${endTime}`;
    } else {
      return `${startStr} 至 ${endStr}`;
    }
  },

  /**
   * 预览文件
   */
  handlePreviewFile: function(e) {
    console.log('👁️ 审批中心预览文件被点击', e);
    
    // 强制阻止事件冒泡和默认行为
    if (e) {
      if (typeof e.stopPropagation === 'function') {
        e.stopPropagation();
      }
      if (typeof e.preventDefault === 'function') {
        e.preventDefault();
      }
    }
    
    // 获取数据
    const url = e.currentTarget.dataset.url;
    const name = e.currentTarget.dataset.name;
    
    console.log('预览文件参数:', url, name);
    
    // 检查URL是否有效
    if (!url) {
      wx.showToast({
        title: '文件路径无效',
        icon: 'none'
      });
      return;
    }
    
    // 获取token
    const token = wx.getStorageSync('token') || '';
    
    // 构建完整URL
    let fileUrl = url;
    if (!url.startsWith('http')) {
      const cleanUrl = url.startsWith('/') ? url.substring(1) : url;
      fileUrl = `${FILE_URL}/wx/file/view/${cleanUrl}`;
    }
    
    // 关键修复：将原始文件名作为查询参数传递
    if (name) {
      const separator = fileUrl.includes('?') ? '&' : '?';
      fileUrl += `${separator}originalName=${encodeURIComponent(name)}`;
    }
    
    console.log('预览文件URL:', fileUrl);
    
    // 获取文件扩展名
    const fileExt = name ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : '';
    
    // 根据文件类型选择预览方式
    if (['jpg', 'jpeg', 'png', 'gif'].includes(fileExt)) {
      // 图片预览
      wx.previewImage({
        urls: [fileUrl],
        fail: (error) => {
          console.error('预览图片失败:', error);
          wx.showToast({
            title: '预览失败',
            icon: 'none'
          });
        }
      });
    } else {
      // 下载后预览
      wx.showLoading({
        title: '加载中...'
      });
      
      wx.downloadFile({
        url: fileUrl,
        header: {
          'Authorization': `Bearer ${token}`,
          'Accept': '*/*'
        },
        timeout: 30000,
        success: (res) => {
          wx.hideLoading();
          console.log('下载结果:', res);
          
          if (res.statusCode === 200) {
            wx.openDocument({
              filePath: res.tempFilePath,
              showMenu: true,
              success: () => {
                console.log('打开文档成功');
              },
              fail: (error) => {
                console.error('打开文档失败:', error);
                wx.showToast({
                  title: '无法预览该文件',
                  icon: 'none'
                });
              }
            });
          } else {
            wx.showToast({
              title: '文件下载失败',
              icon: 'none'
            });
          }
        },
        fail: (error) => {
          wx.hideLoading();
          console.error('下载失败:', error);
          wx.showToast({
            title: '预览失败',
            icon: 'none'
          });
        }
      });
    }
  },

  /**
   * 下载文件
   */
  downloadFile: function(e) {
    console.log('📥 审批中心下载文件被点击', e);
    
    // 强制阻止事件冒泡和默认行为
    if (e) {
      if (typeof e.stopPropagation === 'function') {
        e.stopPropagation();
      }
      if (typeof e.preventDefault === 'function') {
        e.preventDefault();
      }
    }
    
    // 获取数据
    const url = e.currentTarget.dataset.url;
    const name = e.currentTarget.dataset.name;
    
    console.log('下载文件参数:', url, name);
    
    // 检查URL是否有效
    if (!url) {
      wx.showToast({
        title: '文件路径无效',
        icon: 'none'
      });
      return;
    }
    
    // 获取token
    const token = wx.getStorageSync('token') || '';
    
    // 构建完整URL
    let fileUrl = url;
    if (!url.startsWith('http')) {
      const cleanUrl = url.startsWith('/') ? url.substring(1) : url;
      fileUrl = `${FILE_URL}/wx/file/view/${cleanUrl}`;
    }
    
    // 强制下载参数
    const separator = fileUrl.includes('?') ? '&' : '?';
    fileUrl = `${fileUrl}${separator}download=true&force=true`;
    
    // 如果有文件名，添加到URL
    if (name) {
      fileUrl = `${fileUrl}&originalName=${encodeURIComponent(name)}`;
    }
    
    // 添加时间戳防止缓存
    fileUrl = `${fileUrl}&t=${Date.now()}`;
    
    console.log('下载文件URL:', fileUrl);
    
    // 显示加载中
    wx.showLoading({
      title: '下载中...'
    });
    
    // 下载文件
    wx.downloadFile({
      url: fileUrl,
      header: {
        'Authorization': `Bearer ${token}`,
        'Accept': '*/*'
      },
      timeout: 30000,
      success: (res) => {
        wx.hideLoading();
        console.log('下载结果:', res);
        
        if (res.statusCode === 200) {
          // 保存文件到本地
          wx.saveFile({
            tempFilePath: res.tempFilePath,
            success: (saveRes) => {
              // 智能处理不同文件类型
              const fileExt = name ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : '';
              
              if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(fileExt)) {
                // 图片文件：提供保存到相册的选项
                wx.showModal({
                  title: '图片下载成功',
                  content: `图片"${name || '图片'}"已下载\n\n📱 保存到手机：\n• 点击"保存到相册"存入手机相册\n• 点击"查看图片"直接预览`,
                  showCancel: true,
                  cancelText: '查看图片', 
                  confirmText: '保存到相册',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      // 保存图片到相册
                      wx.saveImageToPhotosAlbum({
                        filePath: res.tempFilePath,
                        success: () => {
                          wx.showToast({
                            title: '已保存到手机相册',
                            icon: 'success'
                          });
                        },
                        fail: (err) => {
                          if (err.errMsg.includes('auth')) {
                            wx.showModal({
                              title: '需要相册权限',
                              content: '请在手机设置中允许微信访问相册，然后重试',
                              showCancel: false
                            });
                          } else {
                            wx.showToast({
                              title: '保存失败',
                              icon: 'none'
                            });
                          }
                        }
                      });
                    } else {
                      // 预览图片
                      wx.previewImage({
                        urls: [res.tempFilePath]
                      });
                    }
                  }
                });
              } else {
                // 其他文件类型：提供分享和打开选项
                wx.showModal({
                  title: '文件下载成功',
                  content: `文件"${name || '文档'}"已下载\n\n📱 转存到手机：\n• 点击"分享文件"通过QQ/微信等APP保存\n• 点击"打开文件"直接查看`,
                  showCancel: true,
                  cancelText: '打开文件',
                  confirmText: '分享文件',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      // 分享文件
                      wx.shareFileMessage({
                        filePath: saveRes.savedFilePath,
                        fileName: name || '文档',
                        success: () => {
                          wx.showToast({
                            title: '已打开分享',
                            icon: 'success'
                          });
                        },
                        fail: () => {
                          // 分享失败，回退到打开文件
                          this.openDocument(saveRes.savedFilePath, name);
                        }
                      });
                    } else {
                      // 打开文件
                      this.openDocument(saveRes.savedFilePath, name);
                    }
                  }
                });
              }
            },
            fail: (error) => {
              console.error('保存文件失败:', error);
              wx.showToast({
                title: '保存文件失败',
                icon: 'none'
              });
            }
          });
        } else {
          wx.showToast({
            title: '文件下载失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('下载请求失败:', error);
        
        // 提供更详细的错误信息和帮助
        let errorMsg = '网络错误';
        if (error.errMsg && error.errMsg.includes('404')) {
          errorMsg = '文件不存在';
        } else if (error.errMsg && error.errMsg.includes('timeout')) {
          errorMsg = '连接超时';
        } else if (error.errMsg && error.errMsg.includes('fail')) {
          errorMsg = '服务器错误';
        }
        
        // 下载失败时提供帮助选项
        wx.showModal({
          title: '下载失败',
          content: `${errorMsg}\n\n是否查看下载帮助说明？`,
          showCancel: true,
          cancelText: '稍后重试',
          confirmText: '查看帮助',
          success: (modalRes) => {
            if (modalRes.confirm) {
              showDownloadHelp();
            }
          }
        });
      }
    });
  },

  /**
   * 打开文档的辅助方法
   */
  openDocument: function(filePath, fileName) {
    wx.openDocument({
      filePath: filePath,
      showMenu: true,
      success: () => {
        console.log('打开文档成功');
        wx.showToast({
          title: '文档已打开',
          icon: 'success'
        });
      },
      fail: (error) => {
        console.error('打开文档失败:', error);
        wx.showModal({
          title: '无法打开文件',
          content: `文件"${fileName || '文档'}"无法直接打开\n\n可能原因：\n• 文件格式不支持\n• 文件已损坏`,
          showCancel: false,
          confirmText: '知道了'
        });
      }
    });
  }
});