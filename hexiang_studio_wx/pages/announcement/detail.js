// 公告详情页面
const { 
  getAnnouncementDetail, 
  formatAnnouncementType, 
  formatTime, 
  downloadAnnouncementFile,
  getFileUrl 
} = require('../../utils/announcement');

const { checkLoginStatus } = require('../../utils/auth');

// 引入文件处理工具
const { previewFile, downloadFile } = require('../../utils/fileHelper');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    // 公告详情
    detail: null,
    
    // 加载状态
    loading: true,
    
    // 公告ID
    announcementId: null
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!checkLoginStatus()) {
      return;
    }
    
    const { id } = options;
    if (!id) {
      wx.showToast({
        title: '公告ID缺失',
        icon: 'none',
        complete: () => {
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        }
      });
      return;
    }
    
    this.setData({ announcementId: id });
    this.loadAnnouncementDetail();
  },

  /**
   * 加载公告详情
   */
  async loadAnnouncementDetail() {
    this.setData({ loading: true });
    
    try {
      const res = await getAnnouncementDetail(this.data.announcementId);
      
      if (res.code === 200 && res.data) {
        const detail = res.data;
        
        // 处理数据格式
        const processedDetail = {
          ...detail,
          id: detail.noticeId || detail.id,
          publishDate: detail.publishTime || detail.publishDate,
          // 预格式化显示数据
          typeText: this.getTypeText(detail.type),
          publishDateFormatted: this.formatTime(detail.publishTime || detail.publishDate),
          publishDateFull: this.formatFullTime(detail.publishTime || detail.publishDate),
          // 处理图片数据
          images: detail.images?.map(img => ({
            ...img,
            id: img.imageId || img.id,
            fileName: img.imageName || img.fileName,
            filePath: img.imagePath || img.filePath,
            fileSize: img.imageSize || img.fileSize,
            fullUrl: this.getImageUrl(img.filePath || img.imagePath)
          })) || [],
          // 处理附件数据
          attachments: detail.attachments?.map(att => ({
            ...att,
            id: att.attachmentId || att.id,
            fileName: att.fileName || att.name,
            filePath: att.filePath,
            fileSize: att.fileSize || att.size,
            fileSizeFormatted: this.formatFileSize(att.fileSize || att.size)
          })) || []
        };
        
        this.setData({
          detail: processedDetail,
          loading: false
        });
        
        // 设置页面标题
        wx.setNavigationBarTitle({
          title: detail.title || '公告详情'
        });
        
      } else {
        throw new Error(res.message || '获取公告详情失败');
      }
    } catch (error) {
      console.error('加载公告详情失败:', error);
      this.setData({ loading: false });
      
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none',
        complete: () => {
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        }
      });
    }
  },

  /**
   * 预览图片
   * @param {Object} e 事件对象
   */
  previewImage(e) {
    const { current } = e.currentTarget.dataset;
    const images = this.data.detail.images;
    
    if (!images || images.length === 0) return;
    
    const urls = images.map(img => this.getImageUrl(img.filePath));
    
    wx.previewImage({
      current: urls[current],
      urls: urls
    });
  },

  /**
   * 下载附件 - 使用统一的文件下载方法
   * @param {Object} e 事件对象
   */
  downloadAttachment(e) {
    const { item } = e.currentTarget.dataset;
    
    if (!item || !item.filePath) {
      wx.showToast({
        title: '文件信息有误',
        icon: 'none'
      });
      return;
    }

    console.log('下载公告附件:', item);
    console.log('文件路径:', item.filePath);
    console.log('文件名:', item.fileName);
    
    // 检查文件路径是否已经包含前缀
    let adjustedFilePath = item.filePath;
    let fileTypeParam = null; // 不传递fileType，避免重复添加前缀
    
    // 如果文件路径已经包含notice/前缀，则不需要再添加fileType参数
    if (item.filePath && item.filePath.startsWith('notice/')) {
      console.log('文件路径已包含notice前缀，不传递fileType参数');
      fileTypeParam = null;
    } else {
      console.log('文件路径未包含前缀，使用notice作为fileType');
      fileTypeParam = 'notice';
    }
    
    console.log('调整后的文件路径:', adjustedFilePath);
    console.log('使用的fileType:', fileTypeParam);
    
    // 使用统一的文件下载方法
    downloadFile(adjustedFilePath, item.fileName, fileTypeParam ? { fileType: fileTypeParam } : {});
  },

  /**
   * 预览附件 - 新增预览功能
   * @param {Object} e 事件对象
   */
  previewAttachment(e) {
    const { item } = e.currentTarget.dataset;
    
    if (!item || !item.filePath) {
      wx.showToast({
        title: '文件信息有误',
        icon: 'none'
      });
      return;
    }

    console.log('预览公告附件:', item);
    console.log('文件路径:', item.filePath);
    console.log('文件名:', item.fileName);
    
    // 检查文件路径是否已经包含前缀
    let adjustedFilePath = item.filePath;
    let fileTypeParam = null; // 不传递fileType，避免重复添加前缀
    
    // 如果文件路径已经包含notice/前缀，则不需要再添加fileType参数
    if (item.filePath && item.filePath.startsWith('notice/')) {
      console.log('预览-文件路径已包含notice前缀，不传递fileType参数');
      fileTypeParam = null;
    } else {
      console.log('预览-文件路径未包含前缀，使用notice作为fileType');
      fileTypeParam = 'notice';
    }
    
    console.log('预览-调整后的文件路径:', adjustedFilePath);
    console.log('预览-使用的fileType:', fileTypeParam);
    
    // 使用统一的文件预览方法
    previewFile(adjustedFilePath, item.fileName, fileTypeParam ? { fileType: fileTypeParam } : {});
  },

  /**
   * 获取图片URL
   * @param {string} filePath 文件路径
   * @returns {string} 完整URL
   */
  getImageUrl(filePath) {
    return getFileUrl(filePath);
  },

  /**
   * 获取公告类型文本
   * @param {number} type 类型数字
   * @returns {string} 类型文本
   */
  getTypeText(type) {
    return formatAnnouncementType(type);
  },

  /**
   * 格式化时间（相对时间）
   * @param {string} dateString 时间字符串
   * @returns {string} 格式化时间
   */
  formatTime(dateString) {
    return formatTime(dateString);
  },

  /**
   * 格式化完整时间
   * @param {string} dateString 时间字符串
   * @returns {string} 完整时间格式
   */
  formatFullTime(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}年${month}月${day}日 ${hours}:${minutes}`;
  },

  /**
   * 格式化文件大小
   * @param {number} bytes 字节数
   * @returns {string} 格式化的文件大小
   */
  formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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
    this.loadAnnouncementDetail();
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    const detail = this.data.detail;
    return {
      title: detail?.title || '公告详情',
      path: `/pages/announcement/detail?id=${this.data.announcementId}`,
      imageUrl: detail?.images?.[0] ? this.getImageUrl(detail.images[0].filePath) : ''
    };
  },

  /**
   * 分享到朋友圈
   */
  onShareTimeline() {
    const detail = this.data.detail;
    return {
      title: detail?.title || '公告详情',
      query: `id=${this.data.announcementId}`,
      imageUrl: detail?.images?.[0] ? this.getImageUrl(detail.images[0].filePath) : ''
    };
  }
});