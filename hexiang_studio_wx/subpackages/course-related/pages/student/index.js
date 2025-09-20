// subpackages/course-related/pages/student/index.js
const app = getApp();
const { http, BASE_URL } = require('../../../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    userId: null,
    studentId: null,
    userName: '',
    loading: true,
    activeTab: 'all', // all, ongoing, completed
    courses: [],
    isMy: false // 是否是"我的课程"模式
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const isMy = options && options.isMy === 'true';
    
    if (isMy) {
      // "我的课程"模式：使用当前登录用户信息
      const app = getApp();
      const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
      
      if (userInfo) {
        this.setData({
          userId: userInfo.userId,
          userName: userInfo.name || '我',
          isMy: true
        });
        
        // 设置导航栏标题
        wx.setNavigationBarTitle({
          title: '我的课程'
        });
        
        // 加载课程数据
        this.loadCourses();
      } else {
        wx.showToast({
          title: '请先登录',
          icon: 'error'
        });
        
        setTimeout(() => {
          wx.reLaunch({
            url: '/pages/login/login'
          });
        }, 1500);
      }
    } else {
      // 查看其他用户课程模式
      if (options && options.userId) {
        this.setData({
          userId: options.userId,
          userName: options.userName || '学员',
          isMy: false
        });
        
        // 设置导航栏标题
        wx.setNavigationBarTitle({
          title: `${this.data.userName}的课程`
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
    }
  },

  /**
   * 加载课程数据
   */
  loadCourses() {
    const { userId, activeTab } = this.data;
    
    // 显示加载状态
    this.setData({ loading: true });
    
    // 先从用户ID获取学生ID
            http.get('/wx/user/getStudentId', { userId })
      .then(res => {
        if (res.data) {
          const studentId = res.data;
          this.setData({ studentId });
          
          console.log('获取到学生ID:', studentId);
          
          // 获取学生课程列表
          return http.get('/wx/course/list', { studentId });
        } else {
          throw new Error('获取学生ID失败');
        }
      })
      .then(res => {
        console.log('获取到课程列表:', res);
        let courses = res.data || [];
        
        // 处理课程数据
        courses = courses.map(course => {
          return {
            id: course.courseId,
            title: course.name,
            instructor: course.teacherName || '未指定',
            courseTime: course.duration || '待定',
            status: course.status === 1 ? 'ongoing' : 'completed',
            coverImage: course.coverImage ? `${BASE_URL}/wx/file/view/${course.coverImage}` : '/images/default-cover.png',
            description: course.description,
            categoryName: course.categoryName || '未分类',
            location: course.location || '未指定',
            studentCount: course.studentCount || 0
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
            title: '暂无课程',
            icon: 'none'
          });
        }
      })
      .catch(err => {
        console.error('加载课程失败:', err);
        this.handleApiError(err.message || '获取课程列表失败');
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
    if (this.data.isMy) {
      return {
        title: '我的课程',
        path: `/subpackages/course-related/pages/student/index?userId=${this.data.userId}&userName=${this.data.userName}`
      };
    } else {
      return {
        title: `${this.data.userName}的课程`,
        path: `/subpackages/course-related/pages/student/index?userId=${this.data.userId}&userName=${this.data.userName}`
      };
    }
  }
});
