// pages/materials/index.js
const { BASE_URL } = require('../../config/index');
const { previewFile, downloadFile, getFileTypeIcon, formatFileSize } = require('../../utils/fileHelper');

Page({
  data: {
    // 资料列表数据
    materials: [],
    loading: true,
    loadingMore: false,
    hasMore: true,
    
    // 分页参数
    currentPage: 1,
    pageSize: 20,
    total: 0,
    
    // 搜索筛选
    searchQuery: '',
    selectedCategory: null,
    selectedFileType: '',
    categories: [],
    
    // 筛选选项
    fileTypes: [
      { label: '全部', value: '' },
      { label: '图片', value: '图片' },
      { label: '文档', value: '文档' },
      { label: '视频', value: '视频' },
      { label: '音频', value: '音频' },
      { label: '压缩包', value: '压缩包' },
      { label: '其他', value: '其他' }
    ],
    
    // UI状态
    showFilterModal: false,
    showCategoryPicker: false,
    showFileTypePicker: false,
    categoryPickerIndex: 0,
    fileTypePickerIndex: 0,
    
    // 搜索状态
    searchFocused: false,
    showSearchResult: false,
    
    // 权限信息
    currentUser: null,
    isAdmin: false
  },

  onLoad: function (options) {
    console.log('页面加载开始');
    this.loadUserInfo();
    this.loadCategories();
    // 稍微延迟加载资料，确保分类先加载完成
    setTimeout(() => {
      this.loadMaterials();
    }, 100);
  },

  onPullDownRefresh: function () {
    this.refreshData();
  },

  onReachBottom: function () {
    if (this.data.hasMore && !this.data.loadingMore) {
      this.loadMoreMaterials();
    }
  },

  onShareAppMessage: function () {
    return {
      title: '河湘工作室 - 资料库',
      path: '/pages/materials/index'
    };
  },

  // 加载用户信息
  loadUserInfo: function() {
    const userInfo = wx.getStorageSync('userInfo');
    const roleId = wx.getStorageSync('roleId');
    
    this.setData({
      currentUser: userInfo,
      isAdmin: roleId === 3 // 管理员角色
    });
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
        console.log('分类接口返回:', res.data);
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const rawCategories = res.data.data || [];
          console.log('原始分类数据:', rawCategories);
          
          // 为选择器格式化数据：添加"全部分类"选项
          const categories = [
            { id: null, name: '全部分类', label: '全部分类', value: 0 },
            ...rawCategories.map((item, index) => ({
              ...item,
              label: item.name,
              value: index + 1
            }))
          ];
          
          console.log('格式化后的分类数据:', categories);
          
          this.setData({
            categories
          });
        } else {
          console.error('分类接口返回错误:', res.data);
        }
      },
      fail: (error) => {
        console.error('加载分类失败:', error);
      }
    });
  },

  // 加载资料列表
  loadMaterials: function() {
    this.setData({ loading: true });
    
    const params = this.buildQueryParams();
    
    console.log('发送资料列表请求，参数:', params);
    
    wx.request({
      url: `${BASE_URL}/wx/material/list`,
      method: 'POST',
      data: params,
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token'),
        'Content-Type': 'application/json'
      },
      success: (res) => {
        console.log('资料列表接口返回:', res.data);
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const data = res.data.data;
          const materials = this.processMaterialsData(data.records || []);
          
          console.log('处理后的资料数据:', materials);
          
          this.setData({
            materials,
            total: data.total || 0,
            currentPage: 1,
            hasMore: materials.length < (data.total || 0),
            showSearchResult: !!this.data.searchQuery
          });
        } else {
          console.error('资料列表接口返回错误:', res.data);
          this.showError('加载资料失败', res.data.msg);
        }
      },
      fail: (error) => {
        console.error('加载资料失败:', error);
        this.showError('网络错误', '请检查网络连接后重试');
      },
      complete: () => {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
      }
    });
  },

  // 加载更多资料
  loadMoreMaterials: function() {
    if (this.data.loadingMore || !this.data.hasMore) return;
    
    this.setData({ loadingMore: true });
    
    const params = this.buildQueryParams();
    params.page = this.data.currentPage + 1;
    
    wx.request({
      url: `${BASE_URL}/wx/material/list`,
      method: 'POST',
      data: params,
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token'),
        'Content-Type': 'application/json'
      },
      success: (res) => {
        if (res.data && (res.data.code === 200 || res.data.code === 1)) {
          const data = res.data.data;
          const newMaterials = this.processMaterialsData(data.records || []);
          
          // 去重：过滤掉已存在的材料
          const existingIds = this.data.materials.map(item => item.id);
          const uniqueNewMaterials = newMaterials.filter(item => !existingIds.includes(item.id));
          
          this.setData({
            materials: [...this.data.materials, ...uniqueNewMaterials],
            currentPage: params.page,
            hasMore: (this.data.materials.length + uniqueNewMaterials.length) < (data.total || 0)
          });
        }
      },
      fail: (error) => {
        console.error('加载更多资料失败:', error);
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({ loadingMore: false });
      }
    });
  },

  // 将分类名称转换为文件类型列表
  getFileTypesByCategory: function(category) {
    const typeMap = {
      '图片': ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'],
      '文档': ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'],
      '视频': ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'],
      '音频': ['mp3', 'wav', 'ogg', 'flac', 'aac'],
      '压缩包': ['zip', 'rar', '7z', 'tar', 'gz'],
      '其他': ['other'] // 其他类型的标识
    };
    
    return typeMap[category] || [];
  },

  // 构建查询参数
  buildQueryParams: function() {
    const params = {
      page: this.data.currentPage,
      pageSize: this.data.pageSize
    };
    
    if (this.data.searchQuery) {
      params.name = this.data.searchQuery;
    }
    
    if (this.data.selectedCategory && this.data.selectedCategory.id) {
      params.categoryId = this.data.selectedCategory.id;
    }
    
    if (this.data.selectedFileType) {
      // 如果选择了具体的文件类型分类，转换为文件扩展名
      const fileTypes = this.getFileTypesByCategory(this.data.selectedFileType);
      if (fileTypes.length > 0) {
        params.fileTypes = fileTypes; // 发送文件类型数组给后端
      }
    }
    
    console.log('构建的查询参数:', params);
    return params;
  },

  // 处理资料数据
  processMaterialsData: function(materials) {
    return materials.map(item => ({
      ...item,
      fileTypeIcon: getFileTypeIcon(item.fileType),
      formattedSize: formatFileSize(item.fileSize),
      formattedUploadTime: this.formatTime(item.uploadTime),
      categoryName: this.getCategoryName(item.categoryId)
    }));
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
    
    // 修复iOS兼容性：将 "yyyy-MM-dd HH:mm:ss" 转换为 "yyyy/MM/dd HH:mm:ss"
    const fixedTimeStr = timeStr.replace(/-/g, '/');
    const time = new Date(fixedTimeStr);
    const now = new Date();
    const diff = now.getTime() - time.getTime();
    
    // 24小时内显示相对时间
    if (diff < 24 * 60 * 60 * 1000) {
      if (diff < 60 * 60 * 1000) {
        const minutes = Math.floor(diff / (60 * 1000));
        return minutes <= 0 ? '刚刚' : `${minutes}分钟前`;
      } else {
        const hours = Math.floor(diff / (60 * 60 * 1000));
        return `${hours}小时前`;
      }
    }
    
    // 超过24小时显示具体日期
    const year = time.getFullYear();
    const month = String(time.getMonth() + 1).padStart(2, '0');
    const day = String(time.getDate()).padStart(2, '0');
    
    return `${year}-${month}-${day}`;
  },

  // 搜索功能
  onSearchInput: function(e) {
    this.setData({
      searchQuery: e.detail.value
    });
  },

  onSearchConfirm: function() {
    this.performSearch();
  },

  onSearchFocus: function() {
    this.setData({ searchFocused: true });
  },

  onSearchBlur: function() {
    this.setData({ searchFocused: false });
  },

  performSearch: function() {
    this.setData({
      currentPage: 1,
      materials: [],
      hasMore: true
    });
    this.loadMaterials();
  },

  clearSearch: function() {
    this.setData({
      searchQuery: '',
      showSearchResult: false,
      currentPage: 1,
      materials: [],
      hasMore: true
    });
    this.loadMaterials();
  },

  // 筛选功能
  showFilter: function() {
    this.setData({ showFilterModal: true });
  },

  hideFilter: function() {
    this.setData({ showFilterModal: false });
  },

  // 分类选择
  showCategoryPicker: function() {
    this.setData({ showCategoryPicker: true });
  },

  onCategoryPickerChange: function(e) {
    let index;
    // 处理快速筛选点击和选择器选择两种情况
    if (e.detail && e.detail.value !== undefined) {
      // 选择器选择 - value是数组格式
      index = Array.isArray(e.detail.value) ? e.detail.value[0] : e.detail.value;
    } else {
      // 快速筛选点击
      index = e.currentTarget.dataset.value;
    }
    
    console.log('选择的分类索引:', index);
    const selectedCategory = this.data.categories[index];
    console.log('选择的分类:', selectedCategory);
    
    this.setData({
      categoryPickerIndex: index,
      selectedCategory,
      showCategoryPicker: false,
      currentPage: 1,
      materials: [],
      hasMore: true
    });
    
    // 立即刷新数据
    this.loadMaterials();
  },

  onCategoryPickerCancel: function() {
    this.setData({ showCategoryPicker: false });
  },

  // 文件类型选择
  showFileTypePicker: function() {
    console.log('点击文件类型选择器');
    console.log('当前文件类型数据:', this.data.fileTypes);
    console.log('当前选择器状态（设置前）:', this.data.showFileTypePicker);
    
    this.setData({ 
      showFileTypePicker: true 
    }, () => {
      console.log('文件类型选择器状态（设置后）:', this.data.showFileTypePicker);
    });
  },

  onFileTypePickerChange: function(e) {
    // 处理数组格式的值
    let selectedValue = Array.isArray(e.detail.value) ? e.detail.value[0] : e.detail.value;
    
    console.log('原始选择值:', e.detail.value);
    console.log('选择的值:', selectedValue);
    console.log('文件类型数组:', this.data.fileTypes);
    
    // 根据选择的label找到对应的索引
    let index = 0;
    if (typeof selectedValue === 'string') {
      // 如果选择的是label，找到对应的索引
      index = this.data.fileTypes.findIndex(item => item.label === selectedValue);
      if (index === -1) {
        console.error('未找到匹配的文件类型，使用默认值0');
        index = 0;
      }
    } else if (typeof selectedValue === 'number') {
      // 如果选择的是数字索引，直接使用
      index = selectedValue;
    }
    
    // 确保index在有效范围内
    if (index < 0 || index >= this.data.fileTypes.length) {
      console.error('索引超出范围，使用默认值0');
      index = 0;
    }
    
    const selectedType = this.data.fileTypes[index];
    
    console.log('最终选择的索引:', index);
    console.log('选择的文件类型:', selectedType);
    
    this.setData({
      fileTypePickerIndex: index,
      selectedFileType: selectedType.value,
      showFileTypePicker: false,
      currentPage: 1,
      materials: [],
      hasMore: true
    });
    
    // 立即刷新数据
    this.loadMaterials();
  },

  onFileTypePickerCancel: function() {
    this.setData({ showFileTypePicker: false });
  },

  // 应用筛选
  applyFilter: function() {
    this.setData({
      currentPage: 1,
      materials: [],
      hasMore: true,
      showFilterModal: false
    });
    this.loadMaterials();
  },

  // 重置筛选
  resetFilter: function() {
    this.setData({
      selectedCategory: this.data.categories[0],
      selectedFileType: '',
      categoryPickerIndex: 0,
      fileTypePickerIndex: 0
    });
  },

  // 文件操作
  previewMaterial: function(e) {
    const { url, filename } = e.currentTarget.dataset;
    previewFile(url, filename);
    
    // 记录预览统计（可选）
    this.recordMaterialView(e.currentTarget.dataset.id);
  },

  downloadMaterial: function(e) {
    const { url, filename } = e.currentTarget.dataset;
    downloadFile(url, filename);
    
    // 记录下载统计
    this.recordMaterialDownload(e.currentTarget.dataset.id);
  },

  // 记录资料查看
  recordMaterialView: function(materialId) {
    if (!materialId) return;
    
    wx.request({
      url: `${BASE_URL}/wx/material/view/${materialId}`,
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
  recordMaterialDownload: function(materialId) {
    if (!materialId) return;
    
    wx.request({
      url: `${BASE_URL}/wx/material/download/${materialId}`,
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        console.log('记录下载成功:', res.data);
        
        // 更新下载次数显示
        const materials = this.data.materials.map(item => {
          if (item.id === materialId) {
            return { ...item, downloadCount: item.downloadCount + 1 };
          }
          return item;
        });
        
        this.setData({ materials });
      },
      fail: (error) => {
        console.error('记录下载失败:', error);
      }
    });
  },

  // 查看资料详情
  viewMaterialDetail: function(e) {
    const materialId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/materials/detail?id=${materialId}`
    });
  },

  // 刷新数据
  refreshData: function() {
    this.setData({
      currentPage: 1,
      materials: [],
      hasMore: true
    });
    this.loadCategories();
    this.loadMaterials();
  },

  // 错误处理
  showError: function(title, message) {
    wx.showModal({
      title: title || '提示',
      content: message || '操作失败，请重试',
      showCancel: false
    });
  },

  // 阻止滑动穿透
  preventTouchMove: function() {
    return false;
  },

  // 调试方法：检查数据状态
  debugDataStatus: function() {
    console.log('=== 数据状态调试 ===');
    console.log('categories:', this.data.categories);
    console.log('materials:', this.data.materials);
    console.log('selectedCategory:', this.data.selectedCategory);
    console.log('fileTypes:', this.data.fileTypes);
    console.log('selectedFileType:', this.data.selectedFileType);
    console.log('showFileTypePicker:', this.data.showFileTypePicker);
    console.log('===================');
  },

  // 测试文件类型选择器
  testFileTypePicker: function() {
    console.log('测试文件类型选择器');
    this.setData({ 
      showFileTypePicker: !this.data.showFileTypePicker 
    });
  }
}); 