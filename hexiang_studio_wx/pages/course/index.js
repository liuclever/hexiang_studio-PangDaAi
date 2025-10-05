// pages/course/index.js
const app = getApp();
const { http, BASE_URL } = require('../../utils/request');
const { checkLoginStatus, isAdmin, isTeacher, getUserRole } = require('../../utils/auth');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    loading: false,
    activeTab: 'all', // all, draft, published, offline
    courses: [],
    totalCount: 0,
    currentPage: 1,
    pageSize: 10,
    hasMore: true,
    isAdmin: false,
    userInfo: null
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    console.log('课程管理页面 onLoad 开始');
    
    // 检查登录状态
    if (!checkLoginStatus()) {
      console.log('登录状态检查失败');
      return;
    }
    console.log('登录状态检查通过');
    
    // 检查管理员或老师权限
    if (!isAdmin() && !isTeacher()) {
      console.log('权限检查失败：需要管理员或老师权限');
      wx.showToast({
        title: '权限不足',
        icon: 'error'
      });
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
      return;
    }
    console.log('权限检查通过：管理员或老师权限');
    
    // 获取用户信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    console.log('获取用户信息:', userInfo);
    
    this.setData({
      isAdmin: true,
      userInfo: userInfo
    });
    
    // 设置导航栏标题
    wx.setNavigationBarTitle({
      title: '课程管理'
    });
    
    // 加载课程数据
    console.log('准备调用 loadCourses');
    this.loadCourses();
  },

  /**
   * 加载课程数据
   * 注意：wx/course/list需要studentId，但管理员需要看所有课程
   * 暂时使用模拟数据，建议后端添加管理员专用API
   */
  loadCourses(refresh = false) {
    const { activeTab, currentPage, pageSize, loading } = this.data;
    console.log('loadCourses 调用，loading:', loading, 'refresh:', refresh);
    
    if (loading && !refresh) {
      console.log('loading为true且非刷新，提前返回');
      return;
    }
    console.log('通过loading检查，继续执行');
    
    // 显示加载状态
    this.setData({ loading: true });
    
    // 如果是刷新，重置分页
    if (refresh) {
      this.setData({
        currentPage: 1,
        courses: [],
        hasMore: true
      });
    }
    
    // 尝试调用wx/course API，但这个API需要studentId
    // 现在先使用admin API，建议后端添加wx管理员API
    const params = {
      page: refresh ? 1 : currentPage,
      pageSize: pageSize
    };
    
    // 根据选项卡设置状态筛选
    if (activeTab === 'draft') {
      params.status = 0;
    } else if (activeTab === 'published') {
      params.status = 1;
    } else if (activeTab === 'offline') {
      params.status = 2;
    }
    
    // 使用微信端API获取所有课程
    console.log('开始请求课程列表，URL: /wx/course/all-courses, 参数:', params);
    http.get('/wx/course/all-courses', params)
      .then(res => {
        console.log('获取课程列表成功:', res);
        
        if (res.data && res.data.records) {
          let courses = res.data.records || [];
          
          // 处理课程数据
          courses = courses.map(course => {
            console.log('处理课程数据:', course.name, 'status:', course.status, 'coverImage:', course.coverImage);
            const finalCoverImage = course.coverImage ? `${BASE_URL}/wx/file/view/${course.coverImage}` : '/images/icons/logo.jpg';
            console.log('最终图片路径:', finalCoverImage);
            
            return {
              id: course.courseId,
              courseId: course.courseId,
              title: course.name,
              instructor: course.teacherName || '未指定',
              courseTime: course.duration || '待定',
              status: course.status, // 0-草稿，1-已发布，2-已下架
              statusText: this.getStatusText(course.status),
              coverImage: finalCoverImage,
              description: course.description || '暂无描述',
              categoryName: course.categoryName || '未分类',
              location: course.location || '未指定',
              studentCount: course.studentCount || 0,
              createTime: course.createTime
            };
          });
          
        this.setData({
            courses: refresh ? courses : [...this.data.courses, ...courses],
            totalCount: res.data.total || 0,
            hasMore: courses.length === pageSize,
            currentPage: refresh ? 2 : currentPage + 1,
            loading: false
          });
        } else {
          throw new Error('数据格式错误');
        }
      })
      .catch(err => {
        console.error('加载课程失败，错误详情:', err);
        console.error('错误信息:', err.message || '未知错误');
        console.error('使用模拟数据');
        
        // 使用模拟数据
        const mockCourses = this.getMockCourses(activeTab);
        this.setData({
          courses: refresh ? mockCourses : [...this.data.courses, ...mockCourses],
          totalCount: mockCourses.length,
          hasMore: false,
          loading: false
        });
      });
  },

  /**
   * 获取模拟课程数据（当API不可用时使用）
   */
  getMockCourses(activeTab) {
    const allCourses = [
      {
        id: 1,
        courseId: 1,
        title: 'Web前端开发实战',
        instructor: '李老师',
        courseTime: '24课时',
        status: 1,
        statusText: '已发布',
        coverImage: '/images/default-cover.png',
        description: '全面学习HTML、CSS、JavaScript等前端技术',
        categoryName: '技术实操',
        location: 'E425',
        studentCount: 35,
        createTime: '2024-01-01'
      },
      {
        id: 2,
        courseId: 2,
        title: 'Python数据分析入门',
        instructor: '王教授',
        courseTime: '32课时',
        status: 2,
        statusText: '已下架',
        coverImage: '/images/default-cover.png',
        description: '使用Python进行数据处理和分析',
        categoryName: '数据科学',
        location: 'E426',
        studentCount: 28,
        createTime: '2024-01-02'
      },
      {
        id: 3,
        courseId: 3,
        title: 'UI/UX设计基础',
        instructor: '张设计师',
        courseTime: '20课时',
        status: 0,
        statusText: '草稿',
        coverImage: '/images/default-cover.png',
        description: '学习用户界面和用户体验设计',
        categoryName: '设计艺术',
        location: 'E427',
        studentCount: 42,
        createTime: '2024-01-03'
      }
    ];
    
    // 根据状态筛选
    if (activeTab === 'all') {
      return allCourses;
    } else if (activeTab === 'draft') {
      return allCourses.filter(course => course.status === 0);
    } else if (activeTab === 'published') {
      return allCourses.filter(course => course.status === 1);
    } else if (activeTab === 'offline') {
      return allCourses.filter(course => course.status === 2);
    }
    
    return allCourses;
  },

  /**
   * 获取状态文本
   */
  getStatusText(status) {
    switch (status) {
      case 0:
        return '草稿';
      case 1:
        return '已发布';
      case 2:
        return '已下架';
      default:
        return '未知';
    }
  },

  /**
   * 切换选项卡
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    
    if (tab === this.data.activeTab) {
      return;
    }
    
    this.setData({
      activeTab: tab,
      courses: [],
      currentPage: 1,
      hasMore: true
    });
    
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
   * 修改课程状态
   */
  changeStatus(e) {
    const courseId = e.currentTarget.dataset.id;
    const currentStatus = e.currentTarget.dataset.status;
    
    // 显示状态选择菜单
    const statusOptions = [
      { text: '草稿', value: 0 },
      { text: '已发布', value: 1 },
      { text: '已下架', value: 2 }
    ];
    
    // 过滤掉当前状态
    const availableOptions = statusOptions.filter(option => option.value !== currentStatus);
    
    wx.showActionSheet({
      itemList: availableOptions.map(option => option.text),
      success: (res) => {
        const selectedOption = availableOptions[res.tapIndex];
        this.updateCourseStatus(courseId, selectedOption.value);
      }
    });
  },

  /**
   * 更新课程状态
   */
  updateCourseStatus(courseId, newStatus) {
    wx.showLoading({
      title: '更新中...'
    });
    
    // 使用微信端API更新状态
    http.put('/wx/course/update-status', {
      id: courseId,
      status: newStatus
    })
      .then(res => {
        wx.hideLoading();
        wx.showToast({
          title: '状态更新成功',
          icon: 'success'
        });
        
        // 更新本地数据
        const courses = this.data.courses.map(course => {
          if (course.courseId === courseId) {
            return {
              ...course,
              status: newStatus,
              statusText: this.getStatusText(newStatus)
            };
          }
          return course;
        });
        
        this.setData({ courses });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('更新状态失败:', err);
        wx.showToast({
          title: '功能暂不可用',
          icon: 'error'
        });
    });
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadCourses(true);
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadCourses();
    }
  }
});