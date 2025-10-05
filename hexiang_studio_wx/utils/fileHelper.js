/**
 * 微信小程序文件处理工具
 * 提供统一的文件下载、预览功能
 */

const { FILE_URL } = require('../config/index');
const storage = require('./storage');

/**
 * 显示下载帮助说明
 */
const showDownloadHelp = () => {
  wx.showModal({
    title: ' 文件下载说明',
    content: `微信小程序文件下载机制：

 图片文件
• 可直接保存到手机相册
• 保存路径：相册 > 微信

📄 文档文件（PDF、Word等）
• 无法直接保存到手机文件夹
• 可通过"分享文件"功能转存：
  - 分享到QQ → QQ文件夹

🔄 查看已下载文件：
微信 → 我 → 收藏 → 文件`,
    showCancel: true,
    cancelText: '知道了',
    confirmText: '查看收藏',
    success: (res) => {
      if (res.confirm) {
        wx.showToast({
          title: '请手动进入：我-收藏-文件',
          icon: 'none',
          duration: 3000
        });
      }
    }
  });
};

/**
 * 构建文件访问URL
 * @param {string} url - 文件路径
 * @param {string} fileName - 文件名（可选）
 * @param {boolean} forceDownload - 是否强制下载
 * @param {string} fileType - 文件类型，用于后端路径前缀判断（可选，默认为'material'）
 * @returns {string} 完整的文件访问URL
 */
const buildFileUrl = (url, fileName = '', forceDownload = false, fileType = 'material') => {
  if (!url) return '';
  
  // 构建完整URL
  let fileUrl = url;
  if (!url.startsWith('http')) {
    const cleanUrl = url.startsWith('/') ? url.substring(1) : url;
    fileUrl = `${FILE_URL}/wx/file/view/${cleanUrl}`;
  }
  
  // 添加查询参数 - 微信小程序不支持URLSearchParams，手动构建
  const params = [];
  if (fileName) {
    params.push(`originalName=${encodeURIComponent(fileName)}`);
  }
  if (forceDownload) {
    params.push('download=true');
    params.push('force=true');
  }
  // 只有当fileType不为null且不为空字符串时才添加fileType参数
  if (fileType && fileType.trim() !== '') {
    params.push(`fileType=${fileType}`);
  }
  params.push(`t=${Date.now()}`); // 防缓存
  
  if (params.length > 0) {
    const separator = fileUrl.includes('?') ? '&' : '?';
    fileUrl += separator + params.join('&');
  }
  
  return fileUrl;
};

/**
 * 预览文件
 * @param {string} url - 文件路径
 * @param {string} fileName - 文件名
 * @param {Object} options - 选项
 * @param {string} options.fileType - 文件类型，用于后端路径判断（可选，默认为'material'）
 */
const previewFile = (url, fileName, options = {}) => {
  console.log('👁️ 预览文件:', url, fileName);
  
  if (!url) {
    wx.showToast({
      title: '文件路径无效',
      icon: 'none'
    });
    return;
  }
  
  const token = storage.getToken() || '';
  const fileType = options.fileType || 'material'; // 默认为material保持兼容性
  const fileUrl = buildFileUrl(url, fileName, false, fileType);
  
  console.log('预览文件URL:', fileUrl);
  
  // 获取文件扩展名
  const fileExt = fileName ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : '';
  
  // 根据文件类型选择预览方式
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(fileExt)) {
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
    // 文档预览 - 需要先下载
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
            title: '文件加载失败',
            icon: 'none'
          });
        }
      },
      fail: (error) => {
        wx.hideLoading();
        console.error('下载失败:', error);
        
        // 提供帮助选项
        wx.showModal({
          title: '预览失败',
          content: '文件预览失败，是否查看下载帮助说明？',
          showCancel: true,
          cancelText: '稍后重试',
          confirmText: '查看帮助',
          success: (modalRes) => {
            if (modalRes.confirm) {
              showDownloadHelp();
            }
          }
        });
      }
    });
  }
};

/**
 * 下载文件
 * @param {string} url - 文件路径
 * @param {string} fileName - 文件名
 * @param {Object} options - 选项
 * @param {string} options.fileType - 文件类型，用于后端路径判断（可选，默认为'material'）
 */
const downloadFile = (url, fileName, options = {}) => {
  console.log('📥 下载文件:', url, fileName);
  
  if (!url) {
    wx.showToast({
      title: '文件路径无效',
      icon: 'none'
    });
    return;
  }
  
  // 首次下载说明
  const isFirstDownload = !wx.getStorageSync('hasDownloadedFile');
  if (isFirstDownload) {
    wx.setStorageSync('hasDownloadedFile', true);
    wx.showModal({
      title: '📱 文件下载说明',
      content: '• 图片：可保存到手机相册\n• 其他文件：可通过分享转存到手机\n\n💡 小程序无法直接下载到文件夹，这是微信的安全限制',
      showCancel: false,
      confirmText: '开始下载'
    });
  }
  
  const token = storage.getToken() || '';
  const fileType = options.fileType || 'material'; // 默认为material保持兼容性
  const fileUrl = buildFileUrl(url, fileName, true, fileType);
  
  console.log('下载文件URL:', fileUrl);
  
  wx.showLoading({
    title: '下载中...'
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
      
      if (res.statusCode === 200) {
        // 保存文件
        wx.saveFile({
          tempFilePath: res.tempFilePath,
          success: (saveRes) => {
            handleDownloadSuccess(res.tempFilePath, saveRes.savedFilePath, fileName);
          },
          fail: (error) => {
            console.error('保存文件失败:', error);
            handleDownloadFallback(res.tempFilePath, fileName);
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
      
      wx.showModal({
        title: '下载失败',
        content: '网络错误或文件不存在\n\n是否查看下载帮助说明？',
        showCancel: true,
        cancelText: '稍后重试',
        confirmText: '查看帮助',
        success: (modalRes) => {
          if (modalRes.confirm) {
            showDownloadHelp();
          }
        }
      });
    }
  });
};

/**
 * 处理下载成功后的操作
 */
const handleDownloadSuccess = (tempPath, savedPath, fileName) => {
  // 统一的文件处理逻辑，不区分图片和文档
  wx.showModal({
    title: '文件下载成功',
    content: `文件"${fileName || '文档'}"已下载\n\n📱 转存到手机：\n• 点击"分享文件"通过QQ/微信等APP保存\n• 点击"打开文件"直接查看\n\n💡 小程序限制无法直接存到手机文件夹`,
    showCancel: true,
    cancelText: '打开文件',
    confirmText: '分享文件',
    success: (modalRes) => {
      if (modalRes.confirm) {
        // 分享文件
        wx.shareFileMessage({
          filePath: savedPath,
          fileName: fileName || '文档',
          success: () => {
            wx.showToast({
              title: '已打开分享',
              icon: 'success'
            });
          },
          fail: () => {
            openDocument(savedPath, fileName);
          }
        });
      } else {
        // 打开文件
        openDocument(savedPath, fileName);
      }
    }
  });
};

/**
 * 下载失败时的备选方案
 */
const handleDownloadFallback = (tempPath, fileName) => {
  wx.showModal({
    title: '文件处理',
    content: `文件"${fileName || '文档'}"下载成功，但保存失败\n\n📱 替代方案：\n• 点击"分享文件"转存到其他APP\n• 点击"直接打开"临时查看`,
    showCancel: true,
    cancelText: '直接打开',
    confirmText: '分享文件',
    success: (modalRes) => {
      if (modalRes.confirm) {
        wx.shareFileMessage({
          filePath: tempPath,
          fileName: fileName || '文档',
          success: () => {
            wx.showToast({
              title: '已打开分享',
              icon: 'success'
            });
          },
          fail: () => {
            openDocument(tempPath, fileName);
          }
        });
      } else {
        openDocument(tempPath, fileName);
      }
    }
  });
};

/**
 * 打开文档/图片
 */
const openDocument = (filePath, fileName) => {
  const fileExt = fileName ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : '';
  
  // 判断是否为图片文件
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(fileExt)) {
    // 图片文件使用previewImage
    wx.previewImage({
      urls: [filePath],
      success: () => {
        wx.showToast({
          title: '图片已打开',
          icon: 'success'
        });
      },
      fail: (error) => {
        console.error('预览图片失败:', error);
        wx.showModal({
          title: '无法打开图片',
          content: `图片"${fileName || '图片'}"无法打开\n\n可能原因：\n• 图片格式不支持\n• 图片文件已损坏`,
          showCancel: false,
          confirmText: '知道了'
        });
      }
    });
  } else {
    // 文档文件使用openDocument
    wx.openDocument({
      filePath: filePath,
      showMenu: true,
      success: () => {
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
  }
};

/**
 * 获取文件类型图标
 * @param {string} fileType - 文件类型
 * @returns {string} TDesign图标名称
 */
const getFileTypeIcon = (fileType) => {
  if (!fileType) return 'file';
  
  const type = fileType.toLowerCase();
  
  // 图片
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(type)) {
    return 'image';
  }
  
  // 文档
  if (['pdf'].includes(type)) {
    return 'file-pdf';
  }
  if (['doc', 'docx'].includes(type)) {
    return 'file-word';
  }
  if (['xls', 'xlsx'].includes(type)) {
    return 'file-excel';
  }
  if (['ppt', 'pptx'].includes(type)) {
    return 'file-powerpoint';
  }
  if (['txt', 'md'].includes(type)) {
    return 'file-text';
  }
  
  // 视频
  if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'].includes(type)) {
    return 'video';
  }
  
  // 音频
  if (['mp3', 'wav', 'ogg', 'flac', 'aac'].includes(type)) {
    return 'sound';
  }
  
  // 压缩包
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(type)) {
    return 'file-zip';
  }
  
  return 'file';
};

/**
 * 格式化文件大小
 * @param {number} size - 文件大小（字节）
 * @returns {string} 格式化后的文件大小
 */
const formatFileSize = (size) => {
  if (!size || size === 0) return '未知大小';
  
  const units = ['B', 'KB', 'MB', 'GB'];
  let index = 0;
  let fileSize = size;
  
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  
  return `${fileSize.toFixed(index === 0 ? 0 : 1)}${units[index]}`;
};

module.exports = {
  previewFile,
  downloadFile,
  showDownloadHelp,
  buildFileUrl,
  getFileTypeIcon,
  formatFileSize
}; 