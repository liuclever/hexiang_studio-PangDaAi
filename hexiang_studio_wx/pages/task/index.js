// pages/task/index.js
const { BASE_URL } = require('../../config/index');
const { calculateTaskStats, generateTaskReminder } = require('../../utils/task-helper');
const { getUserRole } = require('../../utils/auth');

Page({
  data: {
    filter: 'all', // 默认显示全部任务
    tasks: [],
    filteredTasks: [],
    hasUncompletedTasks: false,
    urgentTaskCount: 0,
    overdueTaskCount: 0,        // 新增：逾期任务数
    needAttentionCount: 0,      // 新增：需关注任务数
    reminderText: '',           // 新增：提醒文案
    reminderType: 'warning',    // 新增：提醒类型
    loading: false,
    page: 1,
    pageSize: 10,
    hasMore: true,
    pageTitle: '我的任务'       // 页面标题
  },

  onLoad: function (options) {
    // 根据用户角色设置页面标题
    const userRole = getUserRole();
    const pageTitle = (userRole === 'admin' || userRole === 'teacher') ? '任务管理' : '我的任务';
    this.setData({ pageTitle });
    
    // 如果从其他页面传递过来筛选参数
    if (options.filter) {
      this.setData({ filter: options.filter });
    }
    
    this.loadTasks();
  },
  
  onPullDownRefresh: function () {
    // 重置页码
    this.setData({ page: 1, hasMore: true });
    this.loadTasks();
    wx.stopPullDownRefresh();
  },
  
  onShow: function() {
    // ✅ 页面显示时刷新数据，确保状态同步
    console.log('[任务列表] 页面显示，刷新任务数据');
    this.setData({ page: 1, hasMore: true });
    this.loadTasks();
  },
  
  onReachBottom: function() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreTasks();
    }
  },
  
  // 加载更多任务
  loadMoreTasks: function() {
    if (!this.data.hasMore || this.data.loading) return;
    
    const nextPage = this.data.page + 1;
    this.setData({ page: nextPage });
    this.loadTasks(true);
  },
  
  // 加载任务数据
  loadTasks: function (isLoadMore = false) {
    if (!isLoadMore) {
      this.setData({ loading: true });
    }
    
    // 调用后端API获取任务列表
    const { page, pageSize } = this.data;
    
    wx.request({
              url: `${BASE_URL}/wx/task/list`, // 使用统一配置地址
      method: 'GET',
      data: {
        page: page,
        pageSize: pageSize
      },
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const newTasks = res.data.data.records || [];
          const total = res.data.data.total || 0;
          
          // 处理API返回的数据，映射字段名称
          const processedTasks = newTasks.map(task => {
            const completedSubTasks = task.completedSubTasks || 0;
            const totalSubTasks = task.totalSubTasks || 0;
            
            // 计算进度百分比
            let progress = 0;
            if (totalSubTasks > 0) {
              progress = Math.round((completedSubTasks / totalSubTasks) * 100);
            }
            
            // 处理任务状态映射
            let taskStatus = 'in_progress'; // 默认状态
            if (task.status) {
              const status = task.status.toLowerCase();
              switch (status) {
                case 'rejected':
                  taskStatus = 'rejected';
                  break;
                case 'completed':
                  taskStatus = 'completed';
                  break;
                case 'overdue':
                  taskStatus = 'overdue';
                  break;
                case 'urgent':
                  taskStatus = 'urgent';
                  break;
                case 'pending_review':
                  taskStatus = 'pending_review';
                  break;
                default:
                  taskStatus = 'in_progress';
              }
            }

            return {
              id: task.taskId,
              title: task.title,
              description: task.description,
              status: taskStatus,
              progress: progress,
              completedSubTaskCount: completedSubTasks,
              totalSubTaskCount: totalSubTasks,
              creatorName: task.creatUserName || task.creatorName || '系统管理员',
              endTime: task.endTime
            };
          });
          
          // 计算是否还有更多数据
          const hasMore = page * pageSize < total;
          
          // 如果是加载更多，则追加数据，否则替换数据
          const tasks = isLoadMore ? [...this.data.tasks, ...processedTasks] : processedTasks;
          
          // 使用统一的任务统计逻辑
          const stats = calculateTaskStats(tasks);
          const reminder = generateTaskReminder(stats);
          
          this.setData({
            tasks,
            urgentTaskCount: stats.urgent,           // 真正的紧急任务数
            overdueTaskCount: stats.overdue,        // 逾期任务数
            needAttentionCount: stats.needAttention, // 需关注任务数
            hasUncompletedTasks: reminder.hasReminder,
            reminderText: reminder.text,
            reminderType: reminder.type,
            hasMore
          });
          
          // 应用筛选
          this.applyFilter();
        } else {
          wx.showToast({
            title: res.data?.message || '获取任务失败',
            icon: 'none'
          });
          
          // 设置空数据
          this.setData({
            tasks: [],
            hasMore: false
          });
          this.applyFilter();
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
        
        // 设置空数据
        this.setData({
          tasks: [],
          hasMore: false
        });
        this.applyFilter();
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  },
  
  // 设置筛选条件
  setFilter: function(e) {
    const filter = e.currentTarget.dataset.filter;
    this.setData({ filter });
    this.applyFilter();
  },
  
  // 应用筛选
  applyFilter: function() {
    const { tasks, filter } = this.data;
    let filteredTasks = [];
    
    switch (filter) {
      case 'in_progress':
        filteredTasks = tasks.filter(task => 
          task.status === 'in_progress' || task.status === 'not_started'
        );
        break;
      case 'urgent':
        filteredTasks = tasks.filter(task => 
          task.status === 'urgent'  // 只筛选真正的紧急任务
        );
        break;
      case 'overdue':
        filteredTasks = tasks.filter(task => 
          task.status === 'overdue'  // 只筛选逾期任务
        );
        break;
      case 'completed':
        filteredTasks = tasks.filter(task => 
          task.status === 'completed'
        );
        break;
      default: // 'all'
        filteredTasks = tasks;
    }
    
    this.setData({ filteredTasks });
  },
  
  // 查看任务详情
  viewTaskDetail: function(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/task/detail?id=${id}`
    });
  },
  

}); 