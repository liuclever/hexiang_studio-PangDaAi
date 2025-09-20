// subpackages/user-related/pages/achievements/index.js
const app = getApp();
const { http, BASE_URL } = require('../../../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    userId: null,
    userName: '',
    loading: true,
    achievements: [],
    certificates: [],
    BASE_URL: BASE_URL,
    // 添加统计数据
    stats: {
      honorsCount: 0,
      certificatesCount: 0,
      totalAchievements: 0
    },
    // 详情弹窗相关
    showDetailModal: false,
    currentDetail: null,
    animationModal: null
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 获取传递的用户ID和姓名
    if (options && options.userId) {
      this.setData({
        userId: options.userId,
        userName: options.userName || '用户'
      });
      
      // 设置导航栏标题
      wx.setNavigationBarTitle({
        title: `${this.data.userName}的荣誉证书`
      });
      
      // 初始化动画
      this.initAnimations();
      
      // 加载荣誉证书数据
      this.loadAchievements();
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
   * 初始化动画
   */
  initAnimations() {
    this.modalAnimation = wx.createAnimation({
      duration: 250,
      timingFunction: 'ease-out',
      transformOrigin: '50% 50%',
      delay: 0
    });
  },

  /**
   * 加载荣誉证书数据
   */
  loadAchievements() {
    const { userId } = this.data;
    
    // 显示加载状态
    this.setData({ loading: true });
    
    // 请求用户成就统计
          http.get('/wx/user/achievements/stats', { userId })
      .then(statsRes => {
        if (statsRes.code === 200) {
          this.setData({
            stats: statsRes.data || {
              honorsCount: 0,
              certificatesCount: 0,
              totalAchievements: 0
            }
          });
        }
      })
      .catch(err => {
        console.error('获取成就统计失败:', err);
      });
    
    // 请求用户荣誉列表
          const honorsPromise = http.get('/wx/user/honors', { userId })
      .then(res => {
        if (res.code === 200 && Array.isArray(res.data)) {
          return res.data.map(honor => {
            // 处理图片URL
            if (honor.attachment && !honor.attachment.startsWith('http')) {
              honor.imagePath = `${BASE_URL}/wx/file/view/${honor.attachment}`;
            }
            
            // 规范化字段名
            honor.honorTitle = honor.honor_name;
            honor.honorLevel = honor.honor_level;
            honor.issuingAuthority = honor.issue_org;
            honor.awardDate = honor.issue_date;
            
            return honor;
          });
        }
        return [];
      })
      .catch(err => {
        console.error('获取用户荣誉失败:', err);
        return [];
      });
    
    // 请求用户证书列表
          const certificatesPromise = http.get('/wx/user/certificates', { userId })
      .then(res => {
        if (res.code === 200 && Array.isArray(res.data)) {
          return res.data.map(certificate => {
            // 处理图片URL
            if (certificate.attachment && !certificate.attachment.startsWith('http')) {
              certificate.imagePath = `${BASE_URL}/wx/file/view/${certificate.attachment}`;
            } else if (certificate.certificateImage && !certificate.certificateImage.startsWith('http')) {
                              certificate.imagePath = `${BASE_URL}/wx/file/view/${certificate.certificateImage}`;
            }
            
            // 规范化字段名 - 根据可能的字段名
            certificate.certificateName = certificate.certificate_name || certificate.certificateName;
            certificate.certificateLevel = certificate.certificate_level || certificate.certificateLevel;
            certificate.issueOrg = certificate.issue_org || certificate.issueOrg;
            certificate.issueDate = certificate.issue_date || certificate.issueDate;
            certificate.certificateNo = certificate.certificate_no || certificate.certificateNo;
            
            return certificate;
          });
        }
        return [];
      })
      .catch(err => {
        console.error('获取用户证书失败:', err);
        return [];
      });
    
    // 等待所有请求完成
    Promise.all([honorsPromise, certificatesPromise])
      .then(([honors, certificates]) => {
        this.setData({
          achievements: honors,
          certificates: certificates,
          loading: false
        });
        console.log('荣誉数据:', honors);
        console.log('证书数据:', certificates);
      })
      .catch(err => {
        console.error('加载数据失败:', err);
        this.setData({ loading: false });
        
        wx.showToast({
          title: '加载失败，请重试',
          icon: 'none'
        });
      });
  },

  /**
   * 查看荣誉详情
   */
  viewHonorDetail(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      currentDetail: item
    }, () => {
      this.showDetailModal();
    });
  },
  
  /**
   * 查看证书详情
   */
  viewCertificateDetail(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      currentDetail: item
    }, () => {
      this.showDetailModal();
    });
  },
  
  /**
   * 显示详情弹窗
   */
  showDetailModal() {
    // 设置弹窗可见
    this.setData({
      showDetailModal: true
    });
    
    // 动画效果
    setTimeout(() => {
      this.modalAnimation.scale(1).opacity(1).step();
      this.setData({
        animationModal: this.modalAnimation.export()
      });
    }, 50);
  },

  /**
   * 关闭详情弹窗
   */
  closeDetailModal() {
    // 关闭动画
    this.modalAnimation.scale(0.9).opacity(0).step();
    this.setData({
      animationModal: this.modalAnimation.export()
    });
    
    // 延迟隐藏弹窗
    setTimeout(() => {
      this.setData({
        showDetailModal: false,
        currentDetail: null
      });
    }, 200);
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    if (url && !url.includes('default-certificate')) {
      wx.previewImage({
        current: url,
        urls: [url]
      });
    } else {
      // 尝试从attachment字段获取图片
      const detail = this.data.currentDetail;
      if (detail && detail.attachment) {
        const attachmentUrl = `${BASE_URL}/wx/file/view/${detail.attachment}`;
        wx.previewImage({
          current: attachmentUrl,
          urls: [attachmentUrl]
        });
        return;
      }
      
      wx.showToast({
        title: '暂无图片',
        icon: 'none'
      });
    }
  },
  
  /**
   * 打开验证链接
   */
  openVerificationUrl(e) {
    const url = e.currentTarget.dataset.url;
    if (url) {
      // 复制链接到剪贴板
      wx.setClipboardData({
        data: url,
        success() {
          wx.showToast({
            title: '已复制链接，请在浏览器中打开',
            icon: 'none',
            duration: 2000
          });
        }
      });
    }
  },
  
  /**
   * 阻止弹窗内容滑动穿透
   */
  preventTouchMove() {
    return;
  },
  
  /**
   * 阻止事件冒泡
   */
  preventBubble(e) {
    e.stopPropagation();
  },
  
  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadAchievements();
    wx.stopPullDownRefresh();
  },

  /**
   * 分享
   */
  onShareAppMessage() {
    return {
      title: `${this.data.userName}的荣誉证书`,
      path: `/pages/user/achievements/index?userId=${this.data.userId}&userName=${this.data.userName}`
    };
  }
});
