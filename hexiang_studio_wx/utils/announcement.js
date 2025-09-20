/**
 * 公告相关API调用函数
 * 基于后端NoticeController的接口设计
 */

const { http } = require('./request');

/**
 * 获取公告列表（分页）
 * @param {Object} params 查询参数
 * @param {number} params.page 页码，从1开始
 * @param {number} params.pageSize 每页数量
 * @param {string} params.title 标题关键词（可选）
 * @param {string} params.type 公告类型（可选）：0-通知，1-活动，2-新闻，3-其他
 * @param {string} params.status 状态（可选）：0-草稿，1-已发布
 * @param {string} params.beginTime 开始时间（可选）
 * @param {string} params.endTime 结束时间（可选）
 * @returns {Promise} 公告列表
 */
const getAnnouncementList = (params) => {
  return http.get('/wx/notice/list', params);
};

/**
 * 获取公告详情
 * @param {number} id 公告ID
 * @returns {Promise} 公告详情
 */
const getAnnouncementDetail = (id) => {
  return http.get('/wx/notice/detail', { id });
};

/**
 * 获取近期公告（首页显示）
 * 限制最新3条系统公告
 * @returns {Promise} 近期公告列表
 */
const getRecentAnnouncements = () => {
  return http.get('/wx/notice/recent');
};

/**
 * 获取近期活动公告
 * @returns {Promise} 近期活动公告列表
 */
const getRecentActivityAnnouncements = () => {
  return http.get('/wx/notice/recent-activities');
};

/**
 * 获取近期所有公告
 * @returns {Promise} 近期所有公告列表
 */
const getAllRecentAnnouncements = () => {
  return http.get('/wx/notice/all-recent');
};

/**
 * 下载公告附件
 * @param {string} filePath 文件路径
 * @param {string} fileName 文件名
 */
const downloadAnnouncementFile = (filePath, fileName) => {
  const { FILE_URL } = require('./request');
  const downloadUrl = `${FILE_URL}/admin/file/view/${filePath}?download=true&originalName=${encodeURIComponent(fileName)}`;
  
  // 小程序中使用downloadFile
  wx.showLoading({ title: '下载中...' });
  
  return wx.downloadFile({
    url: downloadUrl,
    success: (res) => {
      wx.hideLoading();
      if (res.statusCode === 200) {
        // 保存到相册或打开文件
        wx.saveFile({
          tempFilePath: res.tempFilePath,
          success: (saveRes) => {
            wx.showToast({
              title: '下载成功',
              icon: 'success'
            });
            // 可以选择打开文件
            wx.openDocument({
              filePath: saveRes.savedFilePath,
              success: () => {
                console.log('打开文档成功');
              }
            });
          },
          fail: () => {
            wx.showToast({
              title: '保存失败',
              icon: 'none'
            });
          }
        });
      }
    },
    fail: () => {
      wx.hideLoading();
      wx.showToast({
        title: '下载失败',
        icon: 'none'
      });
    }
  });
};

/**
 * 获取文件预览URL
 * @param {string} filePath 文件路径
 * @returns {string} 完整的文件访问URL
 */
const getFileUrl = (filePath) => {
  const { FILE_URL } = require('./request');
  return `${FILE_URL}/admin/file/view/${filePath}`;
};

/**
 * 格式化公告类型
 * @param {number} type 类型数字
 * @returns {string} 类型文本
 */
const formatAnnouncementType = (type) => {
  const typeMap = {
    0: '通知',
    1: '活动', 
    2: '新闻',
    3: '其他'
  };
  return typeMap[type] || '未知';
};

/**
 * 格式化时间
 * @param {string} dateString 时间字符串
 * @returns {string} 格式化后的时间
 */
const formatTime = (dateString) => {
  if (!dateString) return '';
  
  const date = new Date(dateString);
  const now = new Date();
  const diff = now - date;
  
  // 计算时间差
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  
  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`;
  } else if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`;
  } else if (diff < 7 * day) {
    return `${Math.floor(diff / day)}天前`;
  } else {
    // 格式化为 MM-DD HH:mm
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const dayStr = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${month}-${dayStr} ${hours}:${minutes}`;
  }
};

/**
 * 截断文本内容
 * @param {string} content 原始内容
 * @param {number} maxLength 最大长度，默认80
 * @returns {string} 截断后的内容
 */
const truncateContent = (content, maxLength = 80) => {
  if (!content) return '';
  if (content.length <= maxLength) return content;
  return `${content.substring(0, maxLength)}...`;
};

module.exports = {
  getAnnouncementList,
  getAnnouncementDetail,
  getRecentAnnouncements,
  getRecentActivityAnnouncements,
  getAllRecentAnnouncements,
  downloadAnnouncementFile,
  getFileUrl,
  formatAnnouncementType,
  formatTime,
  truncateContent
}; 