// pages/index/index.js
const { checkLoginStatus, getUserRole, getUserInfo } = require('../../utils/auth');
const { http } = require('../../utils/request');
const { calculateTaskStats, generateTaskReminder } = require('../../utils/task-helper');
const { BASE_URL } = require('../../config/index');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 用户信息
    userInfo: null,
    // 用户角色
    role: 'student', // 'student' 或 'admin'
    // 统计数据
    statistics: {
      activeToday: 0,
      totalCourses: 0
    },
    // 图表数据
    chartData: {
      labels: [],
      values: []
    },
    // 审批统计数据
    approvalStats: {
      pendingTaskSubmissions: 0,
      pendingLeaveRequests: 0,
      totalPendingApprovals: 0,
      todayProcessed: 0
    },
    // 任务进度数据
    taskProgress: [],
    // 任务统计
    hasUncompletedTasks: false,
    urgentTaskCount: 0,
    overdueTaskCount: 0,        // 新增：逾期任务数
    needAttentionCount: 0,      // 新增：需关注任务数
    reminderText: '',           // 新增：提醒文案
    reminderType: 'warning',    // 新增：提醒类型
    // 模块导航
    modules: [
      { id: 'course', name: '课程管理', icon: '/images/icons/course.png' },
      { id: 'user', name: '人员管理', icon: '/images/icons/default-avatar.png' },
      { id: 'task', name: '任务管理', icon: '/images/icons/task.png' },
      { id: 'approval', name: '审批管理', icon: '/images/icons/approval.png' },
      { id: 'achievement', name: '奖章管理', icon: '/images/icons/achievement.png' },
      { id: 'material', name: '资料管理', icon: '/images/icons/material.png' },
      { id: 'announcement', name: '公告管理', icon: '/images/icons/announcement.png' },
      { id: 'attendance', name: '考勤管理', icon: '/images/icons/attendance.png' },
      { id: 'schedule', name: '值班表', icon: '/images/icons/schedule.png' }
    ],
    // 页面是否准备就绪
    isReady: false,
    // 计算说明弹窗
    showCalculationModal: false,
    // 是否有图表数据
    hasChartData: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    // 获取用户信息和角色
    this.setData({
      userInfo: getUserInfo(),
      role: getUserRole()
    });
    
    // 加载首页数据
    this.loadDashboardData();
  },
  
  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 刷新角色信息
    if (this.data.isReady) {
      this.setData({
        role: getUserRole()
      });
      
      // 如果是学生角色，重新加载任务进度
      if (this.data.role === 'student') {
        this.loadTaskProgress();
      }
      
      // 如果是管理员或老师，加载审批统计数据
      if (this.data.role === 'admin' || this.data.role === 'teacher') {
        this.loadApprovalStats();
      }
    }
    
    // 设置TabBar选中状态为工作台（索引1）并更新角色
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      const currentRole = getUserRole();
      this.getTabBar().updateRole(currentRole);
      this.getTabBar().updateSelected(1);
    }
  },

  /**
   * 加载仪表盘数据
   */
  loadDashboardData() {
    // 显示加载中
    wx.showLoading({
      title: '加载中',
    });
    
    // 请求数据概览
    http.get('/wx/dashboard/statistics')
      .then(res => {
        this.setData({
          'statistics.activeToday': res.data.activeToday || 0,
          'statistics.totalCourses': res.data.totalCourses || 0
        });
      })
      .catch(err => {
        console.error('获取统计数据失败:', err);
        // 设置默认空数据
        this.setData({
          'statistics.activeToday': 0,
          'statistics.totalCourses': 0
        });
      });
      
    // 根据角色加载不同数据
    if (this.data.role === 'student') {
      this.loadTaskProgress();
    } else if (this.data.role === 'admin' || this.data.role === 'teacher') {
      // 管理员和老师加载审批统计数据
      this.loadApprovalStats();
      // 隐藏加载中
      wx.hideLoading();
      // 设置页面已就绪
      this.setData({
        isReady: true
      });
      // 加载图表数据并绘制
      setTimeout(() => {
        this.loadChartData();
      }, 300);
    } else {
      // 其他角色
      wx.hideLoading();
      this.setData({
        isReady: true
      });
      setTimeout(() => {
        this.loadChartData();
      }, 300);
    }
  },

  /**
   * 加载图表数据
   */
  loadChartData() {
    http.get('/wx/dashboard/activity-trend')
      .then(res => {
        console.log('活跃度趋势数据:', res.data);
        
        if (res.data && res.data.labels && res.data.values && res.data.labels.length > 0) {
          this.setData({
            'chartData.labels': res.data.labels,
            'chartData.values': res.data.values,
            hasChartData: true
          });
          // 数据加载完成后绘制图表
          this.drawLineChart();
        } else {
          // 无数据
          this.setData({
            hasChartData: false
          });
        }
      })
      .catch(err => {
        console.error('获取活跃度趋势失败:', err);
        // 不使用假数据，保持空状态
        this.setData({
          hasChartData: false
        });
      });
  },

  /**
   * 显示计算说明
   */
  showCalculationInfo() {
    this.setData({
      showCalculationModal: true
    });
  },

  /**
   * 隐藏计算说明
   */
  hideCalculationModal() {
    this.setData({
      showCalculationModal: false
    });
    // 关闭弹窗后，若有图表数据则重新绘制
    if (this.data.hasChartData) {
      setTimeout(() => {
        this.drawLineChart();
      }, 50);
    }
  },

  /**
   * 处理弹窗显示状态变化
   */
  onCalculationModalChange(e) {
    const visible = e.detail.visible;
    this.setData({
      showCalculationModal: visible
    });
    if (!visible && this.data.hasChartData) {
      // 弹窗关闭后补一次重绘，确保安卓端恢复
      setTimeout(() => {
        this.drawLineChart();
      }, 50);
    }
  },

  /**
   * 绘制折线图（带动画效果）
   */
  drawLineChart() {
    // 检查是否有数据，无数据时不绘制
    const { labels, values } = this.data.chartData;
    if (!labels || !values || labels.length === 0 || values.length === 0) {
      console.log('没有图表数据，跳过绘制');
      return;
    }

    const ctx = wx.createCanvasContext('lineChart', this);
    const canvasWidth = 320; // 画布宽度
    const canvasHeight = 150; // 画布高度（增加为X轴标签留空间）
    const padding = 40; // 内边距
    const chartWidth = canvasWidth - padding * 2;
    const chartHeight = canvasHeight - padding * 2;
    
    const maxValue = Math.max(...values);
    const minValue = Math.min(...values);
    const valueRange = maxValue - minValue || 1;
    
    // 计算点的坐标
    const pointSpacing = chartWidth / (labels.length - 1);
    let points = [];
    values.forEach((value, index) => {
      const x = padding + pointSpacing * index;
      const y = padding + chartHeight - ((value - minValue) / valueRange) * chartHeight;
      points.push({ x, y, value });
    });
    
    // 动画参数
    let startTime = Date.now();
    const animationDuration = 1200; // 1.2秒动画
    
    // 缓动函数 - 让动画更平滑
    const easeOutCubic = (t) => {
      return 1 - Math.pow(1 - t, 3);
    };
    
    // 动画函数
    const animate = () => {
      const elapsed = Date.now() - startTime;
      let rawProgress = elapsed / animationDuration;
      if (rawProgress > 1) rawProgress = 1;
      
      // 应用缓动函数
      const animationProgress = easeOutCubic(rawProgress);
      
      // 清空画布
      ctx.clearRect(0, 0, canvasWidth, canvasHeight);
      
      // 设置背景
      ctx.setFillStyle('#ffffff');
      ctx.fillRect(0, 0, canvasWidth, canvasHeight);
      
      // 绘制网格线（淡入效果）
      if (animationProgress > 0.1) {
        ctx.setStrokeStyle(`rgba(240, 240, 240, ${Math.min(1, animationProgress * 2)})`);
        ctx.setLineWidth(1);
        ctx.beginPath();
        for (let i = 0; i <= 4; i++) {
          const y = padding + (chartHeight / 4) * i;
          ctx.moveTo(padding, y);
          ctx.lineTo(canvasWidth - padding, y);
        }
        ctx.stroke();
      }
      
      // 计算当前显示的点数（从左到右逐渐显示）
      const visiblePointCount = Math.floor(points.length * animationProgress);
      const currentPoints = points.slice(0, visiblePointCount + 1);
      
      if (currentPoints.length > 1) {
        // 绘制渐变区域（逐渐显示）
        const gradient = ctx.createLinearGradient(0, padding, 0, canvasHeight - padding);
        const gradientAlpha1 = 0.3 * animationProgress;
        const gradientAlpha2 = 0.05 * animationProgress;
        gradient.addColorStop(0, `rgba(0, 82, 217, ${gradientAlpha1})`);
        gradient.addColorStop(1, `rgba(0, 82, 217, ${gradientAlpha2})`);
        
        ctx.beginPath();
        ctx.moveTo(currentPoints[0].x, canvasHeight - padding);
        currentPoints.forEach(point => {
          ctx.lineTo(point.x, point.y);
        });
        ctx.lineTo(currentPoints[currentPoints.length - 1].x, canvasHeight - padding);
        ctx.closePath();
        ctx.setFillStyle(gradient);
        ctx.fill();
        
        // 绘制主线条（逐渐显示）
        ctx.beginPath();
        ctx.moveTo(currentPoints[0].x, currentPoints[0].y);
        currentPoints.forEach(point => {
          ctx.lineTo(point.x, point.y);
        });
        ctx.setStrokeStyle('#0052d9');
        ctx.setGlobalAlpha(animationProgress);
        ctx.setLineWidth(3);
        ctx.stroke();
        ctx.setGlobalAlpha(1); // 重置透明度
      }
      
      // 绘制数据点（优化的缩放动画）
      currentPoints.forEach((point, index) => {
        const pointDelay = index * 0.15; // 每个点延迟显示
        const pointProgress = Math.max(0, Math.min(1, (animationProgress - pointDelay) / 0.3));
        const scale = pointProgress;
        
        if (scale > 0.1) {
          const alpha = pointProgress;
          
          // 外圆
          ctx.beginPath();
          ctx.arc(point.x, point.y, 6 * scale, 0, 2 * Math.PI);
          ctx.setFillStyle('#ffffff');
          ctx.fill();
          ctx.setStrokeStyle(`rgba(0, 82, 217, ${alpha})`);
          ctx.setLineWidth(2);
          ctx.stroke();
          
          // 内圆
          ctx.beginPath();
          ctx.arc(point.x, point.y, 3 * scale, 0, 2 * Math.PI);
          ctx.setFillStyle(`rgba(0, 82, 217, ${alpha})`);
          ctx.fill();
          
          // 绘制数值标签（延迟淡入）
          if (pointProgress > 0.7) {
            const textAlpha = (pointProgress - 0.7) / 0.3;
            ctx.setFillStyle(`rgba(51, 51, 51, ${textAlpha})`);
            ctx.setFontSize(12);
            ctx.fillText(point.value + '%', point.x - 8, point.y - 12);
          }
        }
      });
      
      // 绘制X轴标签（淡入效果）
      if (animationProgress > 0.2) {
        const labelAlpha = Math.min(1, (animationProgress - 0.2) / 0.3);
        ctx.setFillStyle(`rgba(102, 102, 102, ${labelAlpha})`);
        ctx.setFontSize(10);
        ctx.setTextAlign('center'); // 居中对齐
        labels.forEach((label, index) => {
          if (index <= visiblePointCount) {
            const x = padding + pointSpacing * index;
            // 调整Y坐标，确保标签在canvas范围内显示
            ctx.fillText(label, x, canvasHeight - 15);
          }
        });
      }
      
      ctx.draw();
      
      // 继续动画（使用合理的帧率）
      if (rawProgress < 1) {
        setTimeout(animate, 16); // 约60fps，更流畅
      }
    };
    
    // 开始动画
    animate();
  },

  /**
   * 图表触摸事件
   */
  onTouchStart(e) {
    // 处理触摸开始事件
  },
  
  onTouchMove(e) {
    // 处理触摸移动事件
  },
  
  onTouchEnd(e) {
    // 处理触摸结束事件
  },
  
  /**
   * 加载任务进度
   */
  loadTaskProgress() {
    // 并行请求任务进度数据和全部任务统计
    Promise.all([
      // 获取任务进度显示数据
      this.getTaskProgressData(),
      // 获取全部任务用于统计
      this.getAllTasksForStats()
    ]).then(([progressData, allTasks]) => {
      // 使用全部任务数据计算统计和提醒
      const stats = calculateTaskStats(allTasks);
          const reminder = generateTaskReminder(stats);
          
      // 设置任务进度数据和统计信息
          this.setData({
        taskProgress: progressData,
            hasUncompletedTasks: reminder.hasReminder,
            urgentTaskCount: stats.urgent,
            overdueTaskCount: stats.overdue,
            needAttentionCount: stats.needAttention,
            reminderText: reminder.text,
            reminderType: reminder.type
          });
    }).catch(err => {
      console.error('获取任务数据失败:', err);
      // 设置空数据
      this.setData({
        taskProgress: [],
        hasUncompletedTasks: false,
        urgentTaskCount: 0,
        overdueTaskCount: 0,
        needAttentionCount: 0,
        reminderText: '',
        reminderType: 'warning'
      });
    }).finally(() => {
        // 隐藏加载中
        wx.hideLoading();
        // 设置页面已就绪
        this.setData({
          isReady: true
        });
        // 加载图表数据并绘制
        setTimeout(() => {
          this.loadChartData();
        }, 300);
      });
  },

  /**
   * 获取任务进度显示数据
   */
  getTaskProgressData() {
    return new Promise((resolve, reject) => {
      http.get('/wx/task/recent-progress')
        .then(res => {
          if (res.code === 200 && res.data) {
            // 提取任务进度列表数据
            let taskList = [];
            if (res.data.tasks && Array.isArray(res.data.tasks)) {
              taskList = res.data.tasks.map(task => {
                return {
                  id: task.id,
                  title: task.title,
                  progress: task.progress || 0,
                  status: task.status || 'pending'
                };
              });
            }
            resolve(taskList);
          } else {
            resolve([]);
          }
        })
        .catch(err => {
          reject(err);
        });
    });
  },

  /**
   * 获取全部任务用于统计
   */
  getAllTasksForStats() {
    return new Promise((resolve, reject) => {
      // 使用与全部任务页面相同的API
      wx.request({
        url: `${BASE_URL}/wx/task/list`,
        method: 'GET',
        data: {
          page: 1,
          pageSize: 999 // 获取所有任务用于统计
        },
        header: {
          'Authorization': 'Bearer ' + wx.getStorageSync('token')
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            const tasks = res.data.data.records || [];
            
            // 处理API返回的数据，映射字段名称
            const processedTasks = tasks.map(task => {
              return {
                id: task.taskId,
                title: task.title,
                status: task.status ? task.status.toLowerCase() : 'in_progress',
                progress: task.progress || 0
              };
            });
            
            resolve(processedTasks);
          } else {
            resolve([]);
          }
        },
        fail: (err) => {
          reject(err);
        }
        });
      });
  },

  /**
   * 跳转到模块页面
   */
  navigateToModule(e) {
    const moduleId = e.currentTarget.dataset.id;
    
    // 根据不同模块跳转到不同页面
    switch (moduleId) {
      case 'course':
        // 根据用户角色跳转不同页面
        if (this.data.role === 'student') {
          // 学生端：跳转到我的课程页面
          const userInfo = this.data.userInfo;
          wx.navigateTo({
            url: `/subpackages/course-related/pages/student/index?userId=${userInfo.userId}&userName=${userInfo.name}&isMy=true`
          });
        } else {
          // 管理员/教师：跳转到课程管理页面
          wx.navigateTo({
            url: '/pages/course/index'
          });
        }
        break;
      case 'user':
        // 跳转到人员管理页面（成员管理）
        wx.switchTab({
          url: '/pages/user/index'
        });
        break;
      case 'task':
        wx.navigateTo({
          url: '/pages/task/index'
        });
        break;
      case 'announcement':
        // 跳转到公告管理页面
        wx.switchTab({
          url: '/pages/message/index'
        });
        break;
      case 'approval':
        wx.navigateTo({
          url: '/pages/attendance/leave-approval'
        });
        break;
      case 'achievement':
        // 跳转到我的成就页面
        const userInfo = this.data.userInfo;
        if (userInfo && userInfo.userId) {
          wx.navigateTo({
            url: `/subpackages/user-related/pages/achievements/index?userId=${userInfo.userId}&userName=${userInfo.name || '用户'}`
          });
        } else {
          wx.showToast({
            title: '用户信息异常，请重新登录',
            icon: 'none'
          });
        }
        break;
      case 'material':
        wx.navigateTo({
          url: '/pages/materials/index'
        });
        break;
      case 'attendance':
        wx.navigateTo({
          url: '/pages/attendance/index'
        });
        break;
      case 'schedule':
        wx.navigateTo({
          url: '/pages/schedule/index'
        });
        break;
      case 'organization':
        wx.showToast({
          title: '功能开发中',
          icon: 'none'
        });
        break;
      default:
        wx.showToast({
          title: '未知模块',
          icon: 'none'
        });
    }
  },


  
  /**
   * 跳转到审批中心的任务审批
   */
  goToApprovalRecords() {
    console.log('[首页] 点击工作室管理统计，准备跳转到审批中心的任务审批');
    wx.navigateTo({
      url: '/pages/attendance/leave-approval?tab=task'
    });
  },

  /**
   * 跳转到已审批记录
   */
  goToApprovalHistory() {
    console.log('[首页] 点击审批记录，准备跳转到已审批记录');
    wx.navigateTo({
      url: '/pages/attendance/leave-approval?tab=records'
    });
  },
  
  /**
   * 查看任务进度详情
   */
  viewTaskProgress() {
    wx.navigateTo({
      url: '/pages/task/index'
    });
  },
  
  /**
   * 查看所有任务
   */
  viewAllTasks() {
    wx.navigateTo({
      url: '/pages/task/index'
    });
  },
  
  /**
   * 查看任务详情
   */
  viewTaskDetail(e) {
    const id = e.currentTarget.dataset.id;
    console.log('跳转到任务详情页，任务ID:', id);
    
    wx.navigateTo({
      url: `/pages/task/detail?id=${id}`
    });
  },
  


  /**
   * 加载审批统计数据 (使用与审批中心相同的API)
   */
  async loadApprovalStats() {
    try {
      // 并行请求所有统计数据，与审批中心保持一致
      const [taskCountRes, leaveCountRes, todayProcessedRes] = await Promise.all([
        http.get('/wx/task/submission/pending/count'),
        // 使用与审批中心相同的API获取准确的请假审批数量
        http.get('/admin/approval/leave/list', {
          page: 1,
          pageSize: 1,
          status: 'pending'
        }),
        http.get('/wx/approval/today/processed/count')
      ]);
      
      // 计算统计数据
      const pendingTaskSubmissions = (taskCountRes.code === 200) ? (taskCountRes.data?.count || 0) : 0;
      const pendingLeaveRequests = (leaveCountRes.code === 200) ? (leaveCountRes.data?.total || 0) : 0;
      const todayProcessed = (todayProcessedRes.code === 200) ? (todayProcessedRes.data?.count || 0) : 0;
      const totalPendingApprovals = pendingTaskSubmissions + pendingLeaveRequests;
      
      this.setData({
        'approvalStats.pendingTaskSubmissions': pendingTaskSubmissions,
        'approvalStats.pendingLeaveRequests': pendingLeaveRequests,
        'approvalStats.totalPendingApprovals': totalPendingApprovals,
        'approvalStats.todayProcessed': todayProcessed
      });
      
      console.log('管理员工作台统计数据加载成功:', {
        pendingTaskSubmissions,
        pendingLeaveRequests,
        totalPendingApprovals,
        todayProcessed
      });
    } catch (error) {
      console.error('加载审批统计失败:', error);
    }
  },





  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadDashboardData();
    
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  }
}); 