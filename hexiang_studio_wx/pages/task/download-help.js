/**
 * 微信小程序文件下载帮助说明
 */

const showDownloadHelp = () => {
  wx.showModal({
    title: '📱 文件下载说明',
    content: `微信小程序文件下载机制：

🖼️ 图片文件
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
        // 引导用户到微信收藏
        wx.showToast({
          title: '请手动进入：我-收藏-文件',
          icon: 'none',
          duration: 3000
        });
      }
    }
  });
};

module.exports = {
  showDownloadHelp
}; 