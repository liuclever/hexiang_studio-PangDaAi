// subpackages/course-related/pages/teaching/index.js
const app = getApp();
const { http, BASE_URL } = require('../../../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    userId: null,
    teacherId: null,
    userName: '',
    loading: true,
    activeTab: 'all', // all, ongoing, completed
    courses: []
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 获取传递的用户ID和姓名
    if (options && options.userId) {
      this.setData({
        userId: options.userId,
        userName: options.userName || '老师'
      });
      
      // 设置导航栏标题
      wx.setNavigationBarTitle({
        title: `${this.data.userName}的授课安排`
      });
      
      // 加载课程数据
      this.loadCourses();
    } else {
      wx.showToast({
        title: '缺少用户信息',
        icon: 'error'
      });
      
      // 返回上一页
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  /**
   * 加载课程数据
   */
  loadCourses() {
    const { userId, activeTab } = this.data;
    
    // 显示加载状态
    this.setData({ loading: true });
    
    // 先从用户ID获取老师ID
            http.get('/wx/user/getTeacherId', { userId })
      .then(res => {
        if (res.data) {
          const teacherId = res.data;
          this.setData({ teacherId });
          
          console.log('获取到老师ID:', teacherId);
          
          // 获取老师授课列表
          return http.get('/wx/course/teaching', { teacherId });
        } else {
          throw new Error('获取老师ID失败');
        }
      })
      .then(res => {
        console.log('获取到课程列表:', res);
        let courses = res.data || [];
        
        // 处理课程数据，添加状态信息
        courses = courses.map(course => {
          // 根据课程状态字段确定显示状态
          // 状态: 1-进行中, 2-已结束
          const status = course.status === 1 ? 'ongoing' : 'completed';
          
          return {
            id: course.courseId,           // 修复：添加 id 字段
            courseId: course.courseId,     // 保留 courseId 字段以备使用
            title: course.name,            // 修复：WXML中使用 title 字段
            name: course.name,
            courseTime: course.duration || '待定',
            status: status,                // 修复：使用转换后的状态字符串
            originalStatus: course.status, // 保留原始状态数字
            coverImage: course.coverImage ? `${BASE_URL}/wx/file/view/${course.coverImage}` : '/images/default-cover.png',
            description: course.description,
            studentCount: course.studentCount || 0,
            location: course.location || '未指定',
            categoryName: course.categoryName || '未分类'
          };
        });
        
        // 根据选项卡筛选课程
        if (activeTab === 'ongoing') {
          courses = courses.filter(course => course.status === 'ongoing');
        } else if (activeTab === 'completed') {
          courses = courses.filter(course => course.status === 'completed');
        }
        
        this.setData({
          courses: courses,
          loading: false
        });
        
        // 如果没有课程，显示提示
        if (courses.length === 0) {
          wx.showToast({
            title: '暂无授课安排',
            icon: 'none'
          });
        }
      })
      .catch(err => {
        console.error('加载课程失败:', err);
        this.handleApiError(err.message || '获取授课安排失败');
      });
  },
  
  /**
   * 处理API错误
   */
  handleApiError(message) {
    this.setData({ loading: false, courses: [] });
    wx.showToast({
      title: message,
      icon: 'none'
    });
  },

  /**
   * 切换选项卡
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    
    this.setData({
      activeTab: tab
    });
    
    // 重新加载课程
    this.loadCourses();
  },

  /**
   * 查看课程详情
   */
  viewCourseDetail(e) {
    const courseId = e.currentTarget.dataset.id;
    
    wx.navigateTo({
      url: `/pages/course/detail?id=${courseId}`
    });
  },
  
  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadCourses();
    wx.stopPullDownRefresh();
  },

  /**
   * 分享
   */
  onShareAppMessage() {
    return {
      title: `${this.data.userName}的授课安排`,
      path: `/pages/course/teaching/index?userId=${this.data.userId}&userName=${this.data.userName}`
    };
  }
});
