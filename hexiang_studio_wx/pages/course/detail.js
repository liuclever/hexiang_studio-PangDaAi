// pages/course/detail.js
const app = getApp();
const { http, BASE_URL } = require('../../utils/request');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    courseId: null,
    loading: true,
    course: null,
    showMaterials: false,
    showStudents: false,
    students: [],
    loadingStudents: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (options && options.id) {
      this.setData({
        courseId: options.id
      });
      
      // 加载课程详情
      this.loadCourseDetail();
    } else {
      wx.showToast({
        title: '缺少课程ID',
        icon: 'error'
      });
      
      // 返回上一页
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  /**
   * 加载课程详情
   */
  loadCourseDetail() {
    const { courseId } = this.data;
    
    // 显示加载状态
    this.setData({ loading: true });
    
    // 获取课程详情
    http.get('/wx/course/detail', { id: courseId })
      .then(res => {
        const courseData = res.data;
        
        if (courseData) {
          // 格式化日期
          const formatDate = dateString => {
            if (!dateString) return '';
            const date = new Date(dateString);
            return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
          };
          
          // 优先使用课程的status字段，而不是根据时间计算
          let status = 'ongoing'; // 默认进行中
          
          // 定义时间变量（用于后续进度计算）
          const now = new Date();
          const startDate = courseData.startTime ? new Date(courseData.startTime) : null;
          const endDate = courseData.endTime ? new Date(courseData.endTime) : null;
          
          // 根据课程数据库中的status字段确定状态
          if (courseData.status !== undefined) {
            switch (courseData.status) {
              case 0:
                status = 'draft'; // 草稿
                break;
              case 1:
                status = 'published'; // 已发布（进行中）
                break;
              case 2:
                status = 'offline'; // 已下架
                break;
              default:
                status = 'ongoing'; // 默认进行中
            }
          } else {
            // 如果没有status字段，则根据时间计算（兼容旧数据）
            if (startDate && endDate) {
              if (now < startDate) {
                status = 'not-started'; // 未开始
              } else if (now > endDate) {
                status = 'completed'; // 已完成
              } else {
                status = 'ongoing'; // 进行中
              }
            }
          }
          
          console.log('课程状态确定:', {
            courseStatus: courseData.status,
            finalStatus: status,
            courseName: courseData.name
          });
          
          let progress = 0;
          if (startDate && endDate) {
            const total = endDate.getTime() - startDate.getTime();
            const elapsed = now.getTime() - startDate.getTime();
            progress = Math.min(Math.max(Math.floor((elapsed / total) * 100), 0), 100);
          }
          
          // 处理课程资料
          const materials = courseData.materials || [];
          
          // 构建课程对象
          const course = {
            id: courseData.courseId,
            title: courseData.name,
            instructor: courseData.teacherName || '未指定',
            courseTime: courseData.duration || '待定', // 使用 duration 字段
            progress: progress,
            status: status,
            coverImage: courseData.coverImage ? `${BASE_URL}/wx/file/view/${courseData.coverImage}` : '/images/default-cover.svg',
            description: courseData.description || '暂无课程介绍',
            location: courseData.location || '未指定',
            schedule: courseData.schedule || '未指定',
            studentCount: courseData.studentCount || 0,
            categoryName: courseData.categoryName || '未分类',
            materials: materials.map(material => ({
              id: material.materialId,
              name: material.fileName,
              url: material.filePath, // 这里只存相对路径
              size: this.formatFileSize(material.fileSize || 0),
              type: material.fileType,
              downloadCount: material.downloadCount || 0
            }))
          };
          
          // 设置导航栏标题
          wx.setNavigationBarTitle({
            title: course.title
          });
          
          this.setData({
            course: course,
            loading: false
          });
          
          // 加载学生列表
          this.loadEnrolledStudents();
        } else {
          this.handleApiError('未找到课程信息');
        }
      })
      .catch(err => {
        console.error('加载课程详情失败:', err);
        this.handleApiError(err.message || '获取课程详情失败');
      });
  },
  
  /**
   * 加载已选课学生列表
   */
  loadEnrolledStudents() {
    const { courseId } = this.data;
    
    this.setData({ loadingStudents: true });
    
    // 获取已选课学生列表
    http.get('/wx/course/students', { courseId })
      .then(res => {
        const students = res.data || [];
        this.setData({
          students: students,
          loadingStudents: false
        });
      })
      .catch(err => {
        console.error('加载学生列表失败:', err);
        this.setData({ loadingStudents: false });
        wx.showToast({
          title: '获取学生列表失败',
          icon: 'none'
        });
      });
  },
  
  /**
   * 格式化文件大小
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  },
  
  /**
   * 处理API错误
   */
  handleApiError(message) {
    this.setData({ loading: false });
    wx.showToast({
      title: message,
      icon: 'none'
    });
  },
  
  /**
   * 切换资料展示
   */
  toggleMaterials() {
    this.setData({
      showMaterials: !this.data.showMaterials
    });
  },
  
  /**
   * 切换学生列表展示
   */
  toggleStudents() {
    this.setData({
      showStudents: !this.data.showStudents
    });
  },
  
  /**
   * 下载课程资料
   */
  downloadMaterial: function(e) {
    console.log('📥 下载文件被点击', e);
    
    // 强制阻止事件冒泡和默认行为
    if (e) {
      if (typeof e.stopPropagation === 'function') {
        e.stopPropagation();
      }
      if (typeof e.preventDefault === 'function') {
        e.preventDefault();
      }
    }
    
    // 获取数据
    const url = e.currentTarget.dataset.url;
    const name = e.currentTarget.dataset.name || '课程资料';
    const materialId = e.currentTarget.dataset.id;
    
    console.log('下载文件参数:', url, name, materialId);
    
    // 检查URL是否有效
    if (!url) {
      wx.showToast({
        title: '文件路径无效',
        icon: 'none'
      });
      return;
    }
    
    // 获取token
    const token = wx.getStorageSync('token') || '';
    
    // 构建完整URL
    let fileUrl = url;
    if (!url.startsWith('http')) {
      const cleanUrl = url.startsWith('/') ? url.substring(1) : url;
      fileUrl = `${BASE_URL}/wx/file/view/${cleanUrl}?download=true`;
      if (materialId) {
        fileUrl += `&materialId=${materialId}`;
      }
    }
    
    // 将原始文件名作为查询参数传递
    if (name) {
      const separator = fileUrl.includes('?') ? '&' : '?';
      fileUrl += `${separator}originalName=${encodeURIComponent(name)}`;
    }
    
    console.log('下载文件URL:', fileUrl);
    
    // 显示下载状态
    wx.showLoading({
      title: '下载中...'
    });
    
    // 下载文件
    wx.downloadFile({
      url: fileUrl,
      header: {
        'Authorization': `Bearer ${token}`,
        'Accept': '*/*'
      },
      timeout: 30000,
      success: (res) => {
        wx.hideLoading();
        console.log('下载结果:', res);
        
        if (res.statusCode === 200) {
          // 尝试保存到相册（仅图片）或提供分享/打开选项
          const fileExt = name ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : '';
          
          if (['jpg', 'jpeg', 'png'].includes(fileExt)) {
            // 图片：保存到相册
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                wx.showToast({
                  title: '已保存到相册',
                  icon: 'success'
                });
                
                // 同时提供预览选项
                setTimeout(() => {
                  wx.previewImage({
                    urls: [res.tempFilePath]
                  });
                }, 500);
              },
              fail: () => {
                // 保存失败，提供预览
                wx.previewImage({
                  urls: [res.tempFilePath]
                });
              }
            });
          } else {
            // 其他文件类型：提供分享和打开选项
            wx.showModal({
              title: '文件下载成功',
              content: `文件"${name}"已下载\n\n📱 转存到手机：\n• 点击"分享文件"通过QQ/微信等APP保存\n• 点击"打开文件"直接查看`,
              showCancel: true,
              cancelText: '打开文件',
              confirmText: '分享文件',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  // 分享文件
                  wx.shareFileMessage({
                    filePath: res.tempFilePath,
                    fileName: name,
                    success: () => {
                      wx.showToast({
                        title: '已打开分享',
                        icon: 'success'
                      });
                    },
                    fail: () => {
                      // 分享失败，回退到打开文件
                      this.openDocument(res.tempFilePath, name);
                    }
                  });
                } else {
                  // 打开文件
                  this.openDocument(res.tempFilePath, name);
                }
              }
            });
          }
        } else {
          wx.showToast({
            title: '文件下载失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('下载请求失败:', error);
        console.error('失败的URL:', fileUrl);
        
        // 提供更详细的错误信息
        let errorMsg = '网络错误';
        if (error.errMsg && error.errMsg.includes('404')) {
          errorMsg = '文件不存在';
        } else if (error.errMsg && error.errMsg.includes('timeout')) {
          errorMsg = '连接超时';
        } else if (error.errMsg && error.errMsg.includes('fail')) {
          errorMsg = '服务器错误';
        }
        
        wx.showModal({
          title: '下载失败',
          content: `${errorMsg}\n\n请稍后重试或联系管理员`,
          showCancel: false,
          confirmText: '知道了'
        });
      }
    });
  },
  
  /**
   * 预览课程资料
   */
  previewMaterial: function(e) {
    console.log('👁️ 预览文件被点击', e);
    
    // 强制阻止事件冒泡和默认行为
    if (e) {
      if (typeof e.stopPropagation === 'function') {
        e.stopPropagation();
      }
      if (typeof e.preventDefault === 'function') {
        e.preventDefault();
      }
    }
    
    // 获取数据
    const url = e.currentTarget.dataset.url;
    const name = e.currentTarget.dataset.name || '课程资料';
    const materialId = e.currentTarget.dataset.id;
    
    console.log('预览文件参数:', url, name, materialId);
    
    // 检查URL是否有效
    if (!url) {
      wx.showToast({
        title: '文件路径无效',
        icon: 'none'
      });
      return;
    }
    
    // 获取token
    const token = wx.getStorageSync('token') || '';
    
    // 构建完整URL
    let fileUrl = url;
    if (!url.startsWith('http')) {
      const cleanUrl = url.startsWith('/') ? url.substring(1) : url;
      fileUrl = `${BASE_URL}/wx/file/view/${cleanUrl}`;
      if (materialId) {
        fileUrl += `?materialId=${materialId}`;
      }
    }
    
    // 将原始文件名作为查询参数传递
    if (name) {
      const separator = fileUrl.includes('?') ? '&' : '?';
      fileUrl += `${separator}originalName=${encodeURIComponent(name)}`;
    }
    
    console.log('预览文件URL:', fileUrl);
    
    // 获取文件扩展名
    const fileExt = name ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : '';
    
    // 根据文件类型选择预览方式
    if (['jpg', 'jpeg', 'png', 'gif'].includes(fileExt)) {
      // 图片预览
      wx.previewImage({
        urls: [fileUrl],
        fail: (error) => {
          console.error('预览图片失败:', error);
          wx.showToast({
            title: '预览失败',
            icon: 'none'
          });
        }
      });
    } else {
      // 下载后预览
      wx.showLoading({
        title: '加载中...'
      });
      
      wx.downloadFile({
        url: fileUrl,
        header: {
          'Authorization': `Bearer ${token}`,
          'Accept': '*/*'
        },
        timeout: 30000,
        success: (res) => {
          wx.hideLoading();
          console.log('下载结果:', res);
          
          if (res.statusCode === 200) {
            wx.openDocument({
              filePath: res.tempFilePath,
              showMenu: true,
              success: () => {
                console.log('打开文档成功');
              },
              fail: (error) => {
                console.error('打开文档失败:', error);
                wx.showToast({
                  title: '无法预览该文件',
                  icon: 'none'
                });
              }
            });
          } else {
            wx.showToast({
              title: '文件下载失败',
              icon: 'none'
            });
          }
        },
        fail: (error) => {
          wx.hideLoading();
          console.error('下载失败:', error);
          console.error('失败的URL:', fileUrl);
          
          // 提供更详细的错误信息
          let errorMsg = '网络错误';
          if (error.errMsg && error.errMsg.includes('404')) {
            errorMsg = '文件不存在';
          } else if (error.errMsg && error.errMsg.includes('timeout')) {
            errorMsg = '连接超时';
          } else if (error.errMsg && error.errMsg.includes('fail')) {
            errorMsg = '服务器错误';
          }
          
          wx.showModal({
            title: '预览失败',
            content: `${errorMsg}\n\n请稍后重试或联系管理员`,
            showCancel: false,
            confirmText: '知道了'
          });
        }
      });
    }
  },

  /**
   * 打开文档的辅助方法
   */
  openDocument: function(filePath, fileName) {
    wx.openDocument({
      filePath: filePath,
      showMenu: true,
      success: () => {
        console.log('打开文档成功');
        wx.showToast({
          title: '文档已打开',
          icon: 'success'
        });
      },
      fail: (error) => {
        console.error('打开文档失败:', error);
        wx.showModal({
          title: '无法打开文件',
          content: `文件"${fileName || '文档'}"无法直接打开\n\n可能原因：\n• 文件格式不支持\n• 文件已损坏\n\n建议通过分享功能发送给其他应用打开`,
          showCancel: false,
          confirmText: '知道了'
        });
      }
    });
  },
  
  /**
   * 返回上一页
   */
  goBack() {
    wx.navigateBack();
  },

  /**
   * 显示更多选项
   */
  showMoreOptions() {
    wx.showActionSheet({
      itemList: ['分享课程', '收藏课程', '举报问题'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 分享课程
          this.onShareAppMessage();
        } else if (res.tapIndex === 1) {
          // 收藏课程
          this.collectCourse();
        } else if (res.tapIndex === 2) {
          // 举报问题
          this.reportIssue();
        }
      }
    });
  },

  /**
   * 收藏课程
   */
  collectCourse() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  /**
   * 举报问题
   */
  reportIssue() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  /**
   * 查看学生详情
   */
  viewStudentDetail(e) {
    const studentId = e.currentTarget.dataset.id;
    if (studentId) {
      wx.navigateTo({
        url: `/pages/user/detail?userId=${studentId}`
      });
    }
  },
  
  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadCourseDetail();
    wx.stopPullDownRefresh();
  },
  
  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    const { course } = this.data;
    return {
      title: course ? `课程：${course.title}` : '课程详情',
      path: `/pages/course/detail?id=${this.data.courseId}`
    };
  }
});