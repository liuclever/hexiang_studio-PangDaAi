// pages/materials/detail.js
const { BASE_URL } = require('../../config/index');
const { previewFile, downloadFile, getFileTypeIcon, formatFileSize } = require('../../utils/fileHelper');

Page({
  data: {
    material: null,
    loading: true,
    error: false,
    
    // 权限信息
    currentUser: null,
    isAdmin: false,
    canEdit: false,
    
    // 分类数据
    categories: [],
    
    // 相关资料
    relatedMaterials: [],
    loadingRelated: false
  },

  onLoad: function (options) {
    if (options.id) {
      this.materialId = options.id;
      this.loadUserInfo();
      this.loadCategories();
      this.loadMaterialDetail();
    } else {
      this.showError('参数错误', '资料ID不存在');
    }
  },

  onPullDownRefresh: function () {
    this.refreshData();
  },

  onShareAppMessage: function () {
    const material = this.data.material;
    return {
      title: material ? `资料：${material.fileName}` : '河湘工作室 - 资料详情',
      path: `/pages/materials/detail?id=${this.materialId}`
    };
  },

  // 加载用户信息
  loadUserInfo: function() {
    const userInfo = wx.getStorageSync('userInfo');
    const roleId = wx.getStorageSync('roleId');
    const userId = wx.getStorageSync('userId');
    
    this.setData({
      currentUser: userInfo,
      isAdmin: roleId === 3 // 管理员角色
    });
    
    this.currentUserId = userId;
  },

  // 加载分类列表
  loadCategories: function() {
    wx.request({
      url: `${BASE_URL}/wx/material/categories`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        console.log('详情页分类接口返回:', res.data);
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const categories = res.data.data || [];
          console.log('详情页分类数据:', categories);
          this.setData({
            categories
          });
        } else {
          console.error('详情页分类接口返回错误:', res.data);
        }
      },
      fail: (error) => {
        console.error('详情页加载分类失败:', error);
      }
    });
  },

  // 加载资料详情
  loadMaterialDetail: function() {
    this.setData({ loading: true, error: false });
    
    wx.request({
      url: `${BASE_URL}/wx/material/detail/${this.materialId}`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const material = this.processMaterialData(res.data.data);
          
          this.setData({
            material,
            canEdit: this.data.isAdmin || (material.uploaderId === this.currentUserId)
          });
          
          // 设置页面标题
          wx.setNavigationBarTitle({
            title: material.fileName
          });
          
          // 加载相关资料
          this.loadRelatedMaterials();
          
        } else {
          this.showError('加载失败', res.data.msg || '资料不存在或无权限访问');
        }
      },
      fail: (error) => {
        console.error('加载资料详情失败:', error);
        this.showError('网络错误', '请检查网络连接后重试');
      },
      complete: () => {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
      }
    });
  },

  // 处理资料数据
  processMaterialData: function(material) {
    return {
      ...material,
      fileTypeIcon: getFileTypeIcon(material.fileType),
      formattedSize: formatFileSize(material.fileSize),
      formattedUploadTime: this.formatTime(material.uploadTime),
      categoryName: this.getCategoryName(material.categoryId),
      uploaderName: material.uploader || '未知',
      isPublicText: material.isPublic === 1 ? '公开' : '私有'
    };
  },

  // 获取分类名称
  getCategoryName: function(categoryId) {
    if (!categoryId || !this.data.categories || this.data.categories.length === 0) {
      return '未分类';
    }
    
    const category = this.data.categories.find(item => item.id === categoryId);
    return category ? category.name : '未分类';
  },

  // 格式化时间
  formatTime: function(timeStr) {
    if (!timeStr) return '';
    
    const time = new Date(timeStr);
    const year = time.getFullYear();
    const month = String(time.getMonth() + 1).padStart(2, '0');
    const day = String(time.getDate()).padStart(2, '0');
    const hour = String(time.getHours()).padStart(2, '0');
    const minute = String(time.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hour}:${minute}`;
  },

  // 加载相关资料
  loadRelatedMaterials: function() {
    if (!this.data.material) return;
    
    this.setData({ loadingRelated: true });
    
    const params = {
      categoryId: this.data.material.categoryId,
      fileType: this.data.material.fileType,
      page: 1,
      pageSize: 6,
      excludeId: parseInt(this.materialId) // 确保传递数字类型
    };
    
    console.log('加载相关资料，参数:', params);
    
    wx.request({
      url: `${BASE_URL}/wx/material/related`,
      method: 'GET',
      data: params,
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const relatedMaterials = (res.data.data.records || []).map(item => ({
            ...item,
            fileTypeIcon: getFileTypeIcon(item.fileType),
            formattedSize: formatFileSize(item.fileSize)
          }));
          
          this.setData({ relatedMaterials });
        }
      },
      fail: (error) => {
        console.error('加载相关资料失败:', error);
      },
      complete: () => {
        this.setData({ loadingRelated: false });
      }
    });
  },

  // 预览资料
  previewMaterial: function() {
    const material = this.data.material;
    if (!material) return;
    
    previewFile(material.url, material.fileName);
    this.recordMaterialView();
  },

  // 下载资料
  downloadMaterial: function() {
    const material = this.data.material;
    if (!material) return;
    
    downloadFile(material.url, material.fileName);
    this.recordMaterialDownload();
  },

  // 分享资料
  shareMaterial: function() {
    const material = this.data.material;
    if (!material) return;
    
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
    
    wx.showToast({
      title: '点击右上角分享',
      icon: 'none'
    });
  },

  // 编辑资料（管理员或上传者）
  editMaterial: function() {
    if (!this.data.canEdit) {
      wx.showToast({
        title: '无编辑权限',
        icon: 'none'
      });
      return;
    }
    
    wx.navigateTo({
      url: `/pages/materials/edit?id=${this.materialId}`
    });
  },

  // 删除资料（管理员或上传者）
  deleteMaterial: function() {
    if (!this.data.canEdit) {
      wx.showToast({
        title: '无删除权限',
        icon: 'none'
      });
      return;
    }
    
    const material = this.data.material;
    
    wx.showModal({
      title: '删除确认',
      content: `确定要删除文件"${material.fileName}"吗？删除后无法恢复。`,
      confirmColor: '#ff4757',
      success: (res) => {
        if (res.confirm) {
          this.performDelete();
        }
      }
    });
  },

  // 执行删除
  performDelete: function() {
    wx.showLoading({
      title: '删除中...'
    });
    
    wx.request({
      url: `${BASE_URL}/wx/material/delete/${this.materialId}`,
      method: 'DELETE',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          wx.showToast({
            title: '删除成功',
            icon: 'success'
          });
          
          // 返回上一页并刷新
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        } else {
          wx.showToast({
            title: res.data.msg || '删除失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        console.error('删除资料失败:', error);
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  // 查看相关资料
  viewRelatedMaterial: function(e) {
    const materialId = e.currentTarget.dataset.id;
    wx.redirectTo({
      url: `/pages/materials/detail?id=${materialId}`
    });
  },

  // 复制文件链接
  copyFileLink: function() {
    const material = this.data.material;
    if (!material) return;
    
    const link = `${BASE_URL}/admin/file/view/${material.url}?fileType=material`;
    
    wx.setClipboardData({
      data: link,
      success: () => {
        wx.showToast({
          title: '链接已复制',
          icon: 'success'
        });
      },
      fail: () => {
        wx.showToast({
          title: '复制失败',
          icon: 'none'
        });
      }
    });
  },

  // 记录资料查看
  recordMaterialView: function() {
    wx.request({
      url: `${BASE_URL}/wx/material/view/${this.materialId}`,
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        console.log('记录查看成功:', res.data);
      },
      fail: (error) => {
        console.error('记录查看失败:', error);
      }
    });
  },

  // 记录资料下载
  recordMaterialDownload: function() {
    wx.request({
      url: `${BASE_URL}/wx/material/download/${this.materialId}`,
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        console.log('记录下载成功:', res.data);
        
        // 更新下载次数显示
        const material = this.data.material;
        if (material) {
          this.setData({
            material: {
              ...material,
              downloadCount: material.downloadCount + 1
            }
          });
        }
      },
      fail: (error) => {
        console.error('记录下载失败:', error);
      }
    });
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack();
  },

  // 刷新数据
  refreshData: function() {
    this.loadCategories();
    this.loadMaterialDetail();
  },

  // 显示更多操作
  showMoreActions: function() {
    const itemList = [];
    
    if (this.data.canEdit) {
      itemList.push('编辑资料', '删除资料');
    }
    
    itemList.push('复制链接', '分享资料');
    
    wx.showActionSheet({
      itemList,
      success: (res) => {
        const action = itemList[res.tapIndex];
        
        switch (action) {
          case '编辑资料':
            this.editMaterial();
            break;
          case '删除资料':
            this.deleteMaterial();
            break;
          case '复制链接':
            this.copyFileLink();
            break;
          case '分享资料':
            this.shareMaterial();
            break;
        }
      }
    });
  },

  // 错误处理
  showError: function(title, message) {
    this.setData({ error: true });
    
    wx.showModal({
      title: title || '错误',
      content: message || '未知错误',
      showCancel: true,
      cancelText: '重试',
      confirmText: '返回',
      success: (res) => {
        if (res.confirm) {
          wx.navigateBack();
        } else {
          this.refreshData();
        }
      }
    });
  }
}); 