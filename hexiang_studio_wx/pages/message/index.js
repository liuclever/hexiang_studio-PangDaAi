// 公告页面
const { 
  getAnnouncementList, 
  formatAnnouncementType, 
  formatTime, 
  truncateContent 
} = require('../../utils/announcement');

const { checkLoginStatus } = require('../../utils/auth');
const { http } = require('../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 公告列表
    announcements: [],
    
    // 搜索和筛选
    searchKeyword: '', // 搜索关键词
    filterType: '', // 筛选类型：''(全部)、'0'(通知)、'1'(活动)、'2'(新闻)、'3'(其他)
    
    // 分页相关
    currentPage: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    
    // 标记是否第一次加载
    isFirstLoad: true
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 设置页面标题
    wx.setNavigationBarTitle({
      title: '公告'
    });
    
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    // 初始化加载数据
    this.loadAnnouncementList(true);
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
    // 设置TabBar选中状态为公告（索引0）
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateSelected(0);
    }
    
    // 如果不是第一次加载，刷新数据
    if (!this.data.isFirstLoad) {
      this.onRefresh();
    }
    this.setData({ isFirstLoad: false });
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
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.onRefresh();
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMore();
    }
  },

  /**
   * 刷新数据
   */
  onRefresh() {
    this.setData({
      currentPage: 1,
      announcements: [],
      hasMore: true
    });
    this.loadAnnouncementList(true);
  },

  /**
   * 加载公告列表
   * @param {boolean} isRefresh 是否为刷新操作
   */
  async loadAnnouncementList(isRefresh = false) {
    if (this.data.loading) return;
    
    this.setData({ loading: true });
    
    try {
      const params = {
        page: this.data.currentPage,
        pageSize: this.data.pageSize
      };
      
      // 只有当有实际值时才添加筛选条件
      if (this.data.searchKeyword && this.data.searchKeyword.trim()) {
        params.title = this.data.searchKeyword.trim();
      }
      
      if (this.data.filterType && this.data.filterType !== '') {
        params.type = this.data.filterType;
      }
      
      const res = await getAnnouncementList(params);
      
      if (res.code === 200 && res.data) {
        const newAnnouncements = res.data.records || [];
        
        // 添加调试日志
        console.log('公告列表原始数据:', newAnnouncements);
        if (newAnnouncements.length > 0) {
          console.log('第一条公告数据示例:', newAnnouncements[0]);
          console.log('第一条公告的图片数据:', newAnnouncements[0].images);
          console.log('第一条公告的时间字段:', newAnnouncements[0].publishTime);
          console.log('第一条公告的类型字段:', newAnnouncements[0].type);
        }
        
        // 处理数据格式，确保ID字段正确，并预处理显示数据
        const processedAnnouncements = newAnnouncements.map(item => {
          // 预处理图片URL
          const processedImages = (item.images || []).map(img => ({
            ...img,
            fullUrl: this.getImageUrl(img.filePath)
          }));
          
          const processedItem = {
            ...item,
            id: item.noticeId || item.id, // 兼容不同的ID字段
            publishDate: item.publishTime || item.publishDate,
            publishDateFormatted: this.formatTime(item.publishTime || item.publishDate), // 预格式化时间
            typeText: this.getTypeText(item.type), // 预格式化类型文本
            contentTruncated: this.truncateContent(item.content), // 预截断内容
            images: processedImages // 预处理图片
          };
          
          // 调试处理后的数据
          console.log('处理后的公告数据:', {
            id: processedItem.id,
            typeText: processedItem.typeText,
            publishDateFormatted: processedItem.publishDateFormatted,
            imageCount: processedItem.images.length
          });
          
          return processedItem;
        });
        
        let updatedAnnouncements;
        if (isRefresh || this.data.currentPage === 1) {
          updatedAnnouncements = processedAnnouncements;
        } else {
          updatedAnnouncements = [...this.data.announcements, ...processedAnnouncements];
        }
        
        this.setData({
          announcements: updatedAnnouncements,
          hasMore: processedAnnouncements.length >= this.data.pageSize,
          loading: false
        });
        
        // 停止下拉刷新
        if (isRefresh) {
          wx.stopPullDownRefresh();
        }
      } else {
        throw new Error(res.message || '获取公告列表失败');
      }
    } catch (error) {
      console.error('加载公告列表失败:', error);
      this.setData({ loading: false });
      
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      });
      
      // 停止下拉刷新
      if (isRefresh) {
        wx.stopPullDownRefresh();
      }
    }
  },

  /**
   * 加载更多数据
   */
  loadMore() {
    if (!this.data.hasMore || this.data.loading) return;
    
    this.setData({
      currentPage: this.data.currentPage + 1
    });
    this.loadAnnouncementList();
  },

  /**
   * 搜索输入处理
   */
  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    });
  },

  /**
   * 执行搜索
   */
  onSearch() {
    this.setData({
      currentPage: 1,
      announcements: [],
      hasMore: true
    });
    this.loadAnnouncementList(true);
  },

  /**
   * 筛选类型改变
   */
  onFilterChange(e) {
    const type = e.currentTarget.dataset.type;
    
    this.setData({
      filterType: type,
      currentPage: 1,
      announcements: [],
      hasMore: true
    });
    
    this.loadAnnouncementList(true);
  },

  /**
   * 点击公告项，跳转到详情页
   */
  onAnnouncementTap(e) {
    const item = e.currentTarget.dataset.item;
    
    if (!item || !item.id) {
      wx.showToast({
        title: '公告信息有误',
        icon: 'none'
      });
      return;
    }
    
    // 跳转到公告详情页
    wx.navigateTo({
      url: `/pages/announcement/detail?id=${item.id}`
    });
  },

  /**
   * 预览公告图片
   */
  previewAnnouncementImages(e) {
    const { announcement, index } = e.currentTarget.dataset;
    
    if (!announcement.images || announcement.images.length === 0) return;
    
    const urls = announcement.images.map(img => 
      img.fullUrl || this.getImageUrl(img.filePath || img.imagePath)
    );
    
    wx.previewImage({
      current: urls[index || 0],
      urls: urls
    });
  },

  /**
   * 获取图片URL
   */
  getImageUrl(filePath) {
    if (!filePath) return '';
    const { FILE_URL } = require('../../utils/request');
    return `${FILE_URL}/admin/file/view/${filePath}`;
  },

  /**
   * 获取公告类型文本
   */
  getTypeText(type) {
    return formatAnnouncementType(type);
  },

  /**
   * 状态切换按钮点击事件
   */
  async onStatusToggle(e) {
    const { id, status } = e.currentTarget.dataset;
    const newStatus = status === 1 ? 0 : 1;
    
    console.log('切换公告状态:', { id, status, newStatus });
    
    try {
      wx.showLoading({ title: '更新中...' });
      
      const result = await http.post('/wx/notice/update-status', {
        noticeId: id,
        status: newStatus
      });
      
      wx.hideLoading();
      
      if (result.success) {
        wx.showToast({
          title: newStatus === 1 ? '发布成功' : '已撤回',
          icon: 'success'
        });
        
        // 刷新公告列表
        this.loadAnnouncementList(true);
      } else {
        wx.showToast({
          title: result.message || '状态更新失败',
          icon: 'error'
        });
      }
    } catch (error) {
      wx.hideLoading();
      console.error('状态更新失败:', error);
      wx.showToast({
        title: '状态更新失败',
        icon: 'error'
      });
    }
  },

  /**
   * 格式化时间
   */
  formatTime(dateString) {
    return formatTime(dateString);
  },

  /**
   * 截断内容
   */
  truncateContent(content) {
    return truncateContent(content, 60); // 移动端显示更少字符
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    return {
      title: '何湘技能大师工作室 - 公告中心',
      path: '/pages/message/index'
    };
  }
});