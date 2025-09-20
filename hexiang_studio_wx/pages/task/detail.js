// pages/task/detail.js
const { BASE_URL, FILE_URL } = require('../../config/index');
const { previewFile, downloadFile, getFileTypeIcon, formatFileSize } = require('../../utils/fileHelper');

Page({
  data: {
    task: null,
    loading: true,
    userHasSubTasks: false,
    isTaskCreator: false,
    fileBaseUrl: FILE_URL, // 文件访问基础URL
    userSubTaskIds: [], // 用户负责的子任务ID列表
    userSubTaskNames: [], // 用户负责的子任务名称列表（用于选择器）
    submissionData: {
      remark: '',
      files: []
    },
    // 新增状态变量
    activeSubTaskId: null,  // 当前激活的子任务ID
    currentSubTask: null,   // 当前查看的子任务详情
    isCurrentUserAssignee: false, // 当前用户是否为子任务负责人
    showMemberListModal: false,
    currentSubtaskMembers: []
  },

  onLoad: function (options) {
    if (options.id) {
      this.taskId = options.id;
      this.loadTaskDetail();
    } else {
      wx.showToast({
        title: '任务ID不存在',
        icon: 'none'
      });
      setTimeout(() => {
        this.goBack();
      }, 1500);
    }
    
    // 启用分享功能
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },
  
  onPullDownRefresh: function () {
    this.loadTaskDetail();
    wx.stopPullDownRefresh();
  },

  // 阻止滑动穿透
  preventTouchMove: function() {
    return false;
  },

  // 展示子任务详情 - 适配t-swipe-cell组件
  showSubTaskDetail: function(e) {
    let subTaskId;
    
    // 检查是否为swipe-cell的点击事件
    if (e.detail && e.detail.item && e.detail.item.text === '详情') {
      subTaskId = e.currentTarget.dataset.id;
    } else {
      // 兼容直接点击获取dataset方式
      subTaskId = e.currentTarget.dataset.id;
    }
    
    console.log('点击子任务ID:', subTaskId);
    
    if (!subTaskId) return;
    
    const userId = wx.getStorageSync('userId');
    
    // 查找子任务数据
    const task = this.data.task;
    if (task && task.subTasks) {
      const subTask = task.subTasks.find(item => item.id === subTaskId);
      
      if (subTask) {
        console.log('找到子任务:', subTask);
        
        // 检查当前用户是否是该子任务的负责人或参与者
        let isAssignee = false;
        
        // 检查是否为负责人
        if (subTask.leader && subTask.leader.id === userId) {
          isAssignee = true;
        }
        // 检查是否为参与者
        else if (subTask.participantMembers) {
          isAssignee = subTask.participantMembers.some(member => member.id === userId);
        }
        
        // 先设置基本信息
        this.setData({
          activeSubTaskId: subTaskId,
          currentSubTask: subTask,
          isCurrentUserAssignee: isAssignee,
          submissionData: {
            remark: '',
            files: []
          },
          submissionAttachments: []
        });
        
        // 获取子任务提交详情
        wx.request({
          url: `${BASE_URL}/wx/task/submission/detail/${subTaskId}`,
          method: 'GET',
          header: {
            'Authorization': 'Bearer ' + wx.getStorageSync('token')
          },
          success: (res) => {
            if (res.data && res.data.code === 200) {
              const detail = res.data.data;
              console.log('获取提交详情成功:', detail);
              
              // 如果已有提交记录
              if (detail.exists) {
                const submission = detail.submission;
                
                // 更新子任务状态和审核评论
                subTask.status = submission.status;
                subTask.reviewComment = submission.reviewComment;
                
                this.setData({
                  currentSubTask: subTask,
                  submissionData: {
                    remark: submission.submissionNotice || '',
                    files: []
                  },
                  submissionAttachments: detail.attachments || []
                });
              }
            } else {
              console.error('获取提交详情失败:', res.data);
            }
          },
          fail: (err) => {
            console.error('获取提交详情请求失败:', err);
          }
        });
      } else {
        console.error('未找到子任务:', subTaskId);
      }
    }
  },
  
  // 隐藏子任务详情 - 适配t-popup组件
  hideSubTaskDetail: function(e) {
    this.setData({
      activeSubTaskId: null,
      currentSubTask: null,
      isCurrentUserAssignee: false
    });
  },
  
  showSubtaskMembers: function(e) {
    const subtaskId = e.currentTarget.dataset.id;
    console.log('显示子任务成员, ID:', subtaskId);
    
    const subtask = this.data.task.subTasks.find(st => st.id === subtaskId);
    if (subtask && subtask.participantMembers) {
      console.log('子任务成员:', subtask.participantMembers);
      this.setData({
        showMemberListModal: true,
        // 使用处理过的 participantMembers 列表
        currentSubtaskMembers: subtask.participantMembers
      });
    } else {
      console.error('未找到子任务或子任务没有成员:', subtaskId);
    }
  },

  hideSubtaskMembers: function() {
    this.setData({
      showMemberListModal: false,
      currentSubtaskMembers: []
    });
  },

  // 提交子任务工作
  submitSubTaskWork: function() {
    const { activeSubTaskId, submissionData } = this.data;
    const { remark, files } = submissionData;

    console.log('[提交调试] submissionData:', submissionData);
    console.log('[提交调试] remark值:', remark);
    console.log('[提交调试] remark类型:', typeof remark);
    console.log('[提交调试] remark长度:', remark ? remark.length : 'undefined/null');

    if (!activeSubTaskId) return;

    // 检查文件列表（包括新上传的文件和已提交的附件）
    const hasNewFiles = files && files.length > 0;
    const hasExistingAttachments = this.data.submissionAttachments && this.data.submissionAttachments.length > 0;
    
    if (!hasNewFiles && !hasExistingAttachments) {
      wx.showToast({ title: '请至少上传一个附件', icon: 'none' });
      return;
    }

    // 如果有新文件，需要验证和上传；如果没有新文件，只更新提交说明
    if (hasNewFiles) {
      // 过滤出有效文件（有正确路径的文件）
      const validFiles = files.filter(file => {
        const filePath = file.filePath || file.path;
        const fileName = file.name || '未知文件';
        
        console.log(`[文件验证] 文件名: "${fileName}", 路径: "${filePath}"`);
        
        if (!filePath) {
          console.error(`[文件验证] 文件 "${fileName}" 缺少有效路径`);
          wx.showToast({
            title: `文件 "${fileName}" 路径无效`,
            icon: 'none'
          });
          return false;
        }
        return true;
      });

      if (validFiles.length === 0) {
        wx.showToast({ title: '没有有效的文件可以上传', icon: 'none' });
        return;
      }

      if (validFiles.length < files.length) {
        console.warn(`[文件验证] 原有 ${files.length} 个文件，有效文件 ${validFiles.length} 个`);
      }
      
      wx.showLoading({ title: `上传中(0/${validFiles.length})`, mask: true });

    // 记录上传结果
    let successCount = 0;
    let failCount = 0;
    const uploadResults = [];

    // 使用 reduce 实现串行上传
    const uploadChain = validFiles.reduce((promise, file, index) => {
      return promise.then(() => {
        return new Promise((resolve) => {
          
          const fileName = file.name || '未知文件';
          const filePath = file.filePath || file.path;
          
          // 更新上传进度
          wx.showLoading({ title: `上传中(${index + 1}/${validFiles.length})` });
          
          console.log(`[开始上传] 第${index + 1}个文件: "${fileName}", 路径: "${filePath}"`);

          // 构建当前文件的提交数据
          const currentSubmissionDto = {
            submissionNotice: remark || '', // 每个文件请求都包含完整的提交说明
            originalFileNames: [fileName] // 只传当前文件名
          };

          console.log(`[上传参数] 第${index + 1}个文件提交数据:`, currentSubmissionDto);

          wx.uploadFile({
                         url: `${BASE_URL}/wx/task/submission/submit/${activeSubTaskId}`,
            filePath: filePath,
            name: 'files',
            formData: {
              submissionDto: JSON.stringify(currentSubmissionDto)
            },
            header: { 'Authorization': 'Bearer ' + wx.getStorageSync('token') },
            success: (res) => {
              let result;
              try {
                result = JSON.parse(res.data);
              } catch (e) {
                result = { code: 500, msg: '服务器响应异常' };
              }

              console.log(`[上传结果] 文件 "${fileName}" 上传响应:`, result);

              if (result.code === 200) {
                successCount++;
                uploadResults.push({ file: fileName, success: true, result });
                console.log(`[上传成功] 文件 "${fileName}" 上传成功`);
              } else {
                failCount++;
                uploadResults.push({ file: fileName, success: false, error: result.msg });
                console.error(`[上传失败] 文件 "${fileName}" 上传失败:`, result.msg);
              }
              
              // 总是resolve，继续下一个文件
              resolve({ success: result.code === 200, fileName, result });
            },
            fail: (err) => {
              failCount++;
              uploadResults.push({ file: fileName, success: false, error: err });
              console.error(`[网络错误] 文件 "${fileName}" 网络错误:`, err);
              
              // 总是resolve，继续下一个文件
              resolve({ success: false, fileName, error: err });
            }
          });
        });
      });
    }, Promise.resolve());

    uploadChain
      .then(() => {
        wx.hideLoading();
        
        console.log(`[上传完成] 总计: ${validFiles.length}, 成功: ${successCount}, 失败: ${failCount}`);
        console.log('[上传详情]', uploadResults);
        
        if (successCount > 0) {
          const message = failCount > 0 
            ? `上传完成，成功${successCount}个，失败${failCount}个` 
            : `全部${successCount}个文件上传成功`;
          
          wx.showToast({ 
            title: message, 
            icon: successCount === validFiles.length ? 'success' : 'none',
            duration: 3000
          });
        } else {
          wx.showToast({
            title: '所有文件上传失败，请检查网络连接',
            icon: 'none',
            duration: 3000
          });
        }
        
        // 刷新数据
        this.hideSubTaskDetail();
        this.loadTaskDetail();
      })
      .catch(error => {
        wx.hideLoading();
        console.error('[上传异常]', error);
        wx.showToast({
          title: '上传过程中发生异常',
          icon: 'none',
          duration: 3000
        });
        
        // 即使失败，也刷新一下
        this.hideSubTaskDetail();
        this.loadTaskDetail(); 
      });
    } else {
      // 没有新文件，只更新提交说明
      console.log('[无新文件] 只更新提交说明，保留原有附件');
      
      wx.showLoading({ title: '更新中...', mask: true });
      
      // 构建只包含说明的提交数据
      const submissionDto = {
        submissionNotice: remark || '',
        originalFileNames: [] // 空的文件名列表
      };
      
      // 调用提交API（不传文件）
      wx.request({
        url: `${BASE_URL}/wx/task/submission/submit/${activeSubTaskId}`,
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + wx.getStorageSync('token'),
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        data: {
          submissionDto: JSON.stringify(submissionDto)
        },
        success: (res) => {
          wx.hideLoading();
          let result;
          try {
            result = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          } catch (e) {
            result = res.data;
          }
          
          console.log('[无文件提交] 提交结果:', result);
          
          if (result.code === 200) {
            wx.showToast({
              title: '提交成功',
              icon: 'success'
            });
          } else {
            wx.showToast({
              title: result.msg || '提交失败',
              icon: 'none'
            });
          }
          
          // 刷新数据
          this.hideSubTaskDetail();
          this.loadTaskDetail();
        },
        fail: (err) => {
          wx.hideLoading();
          console.error('[无文件提交] 提交失败:', err);
          wx.showToast({
            title: '提交失败，请重试',
            icon: 'none'
          });
        }
      });
    }
  },

  // TDesign文本域输入处理
  onRemarkInput: function(e) {
    console.log('[输入调试] 用户输入:', e.detail.value);
    console.log('[输入调试] 输入长度:', e.detail.value ? e.detail.value.length : 0);
    
    this.setData({
      'submissionData.remark': e.detail.value
    });
    
    console.log('[输入调试] 设置后的submissionData.remark:', this.data.submissionData.remark);
  },
  
  // 删除文件
  removeFile: function(e) {
    const index = e.currentTarget.dataset.index;
    const files = this.data.submissionData.files;
    
    files.splice(index, 1);
    
    this.setData({
      'submissionData.files': files
    });
  },

  // 移除已提交的附件（前端删除，不调用API）
  removeSubmittedFile: function(e) {
    const index = e.currentTarget.dataset.index;
    const fileName = e.currentTarget.dataset.name;
    
    if (index === undefined) {
      wx.showToast({
        title: '删除参数错误',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '确认移除',
      content: `确定要移除附件"${fileName}"吗？\n\n移除后，再次提交时该附件将不会包含在提交内容中。`,
      confirmText: '确认移除',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          const submissionAttachments = [...this.data.submissionAttachments];
          
          // 从数组中移除对应索引的项
          submissionAttachments.splice(index, 1);
          
          this.setData({
            submissionAttachments: submissionAttachments
          });
          
          wx.showToast({
            title: '附件已移除',
            icon: 'success'
          });
          
          console.log(`[附件移除] 已从列表中移除附件: ${fileName}`);
        }
      }
    });
  },
  
  // TDesign上传成功回调
  onUploadSuccess: function(e) {
    const { files } = e.detail;
    
    this.setData({
      'submissionData.files': files
    });
  },
  
  // TDesign删除文件回调
  onRemoveFile: function(e) {
    const { index } = e.detail;
    const { files } = this.data.submissionData;
    
    // 移除指定索引的文件
    files.splice(index, 1);
    
    this.setData({
      'submissionData.files': files
    });
  },
  
  // 从聊天记录选择文件
  chooseMessageFile: function() {
    wx.chooseMessageFile({
      count: 5, // 最多可以选择的文件数量
      type: 'all', // 可以选择所有文件类型
      success: (res) => {
        const tempFiles = res.tempFiles;
        
        // 检查文件类型和大小
        const validFiles = tempFiles.filter(file => {
          // 检查文件大小（限制为10MB，最小1KB）
          if (file.size > 10 * 1024 * 1024) {
            wx.showToast({
              title: '文件大小不能超过10MB',
              icon: 'none'
            });
            return false;
          }
          
          // 检查空文件（小于1KB的文件可能是空文件）
          if (file.size < 1024) {
            wx.showToast({
              title: `文件"${file.name}"太小，可能是空文件`,
              icon: 'none',
              duration: 3000
            });
            console.warn(`[文件检查] 文件 "${file.name}" 大小仅 ${file.size} 字节，可能是空文件`);
            return false;
          }
          
          // 检查文件类型（可以根据需要限制）
          const extension = file.name.split('.').pop().toLowerCase();
          const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'zip', 'rar', '7z'];
          
          // [关键调试日志] 打印每个文件的检查过程
          console.log(`[文件检查] 文件名: "${file.name}", 大小: ${file.size}字节, 后缀: "${extension}", 是否允许: ${allowedExtensions.includes(extension)}`);

          if (!allowedExtensions.includes(extension)) {
            wx.showToast({
              title: `不支持 ${extension} 类型的文件`,
              icon: 'none'
            });
            return false;
          }
          
          return true;
        });
        
        if (validFiles.length === 0) {
          return;
        }
        
        // 显示处理中提示
        wx.showLoading({
          title: '处理文件中...',
          mask: true
        });
        
        console.log(`[文件选择] 开始处理 ${validFiles.length} 个有效文件`);
        
        // 处理文件信息
        const processedFiles = validFiles.map((file, index) => {
          const extension = file.name.split('.').pop().toLowerCase();
          let fileType = 'other';
          
          if (['jpg', 'jpeg', 'png', 'gif'].includes(extension)) {
            fileType = 'image';
          } else if (['doc', 'docx'].includes(extension)) {
            fileType = 'word';
          } else if (['xls', 'xlsx'].includes(extension)) {
            fileType = 'excel';
          } else if (['ppt', 'pptx'].includes(extension)) {
            fileType = 'ppt';
          } else if (extension === 'pdf') {
            fileType = 'pdf';
          }
          
          // 验证文件路径
          if (!file.path) {
            console.error(`[文件处理] 第${index + 1}个文件 "${file.name}" 缺少path属性`);
          }
          
          const processedFile = {
            name: file.name,
            filePath: file.path, // 正确的键名
            path: file.path, // 保留 path 以兼容旧逻辑
            size: file.size,
            fileType: fileType,
            url: file.path // url 通常用于预览
          };
          
          console.log(`[文件处理] 第${index + 1}个文件处理结果:`, {
            原始文件: { name: file.name, path: file.path, size: file.size },
            处理后: processedFile
          });
          
          return processedFile;
        });
        
        // 过滤掉没有有效路径的文件
        const finalFiles = processedFiles.filter(file => {
          if (!file.filePath) {
            console.error(`[文件过滤] 文件 "${file.name}" 没有有效路径，已过滤`);
            wx.showToast({
              title: `文件 "${file.name}" 处理失败`,
              icon: 'none'
            });
            return false;
          }
          return true;
        });
        
        if (finalFiles.length === 0) {
          wx.hideLoading();
          wx.showToast({
            title: '没有可用的文件',
            icon: 'none'
          });
          return;
        }
        
        // 更新提交数据
        const currentFiles = this.data.submissionData.files || [];
        const newFiles = [...currentFiles, ...finalFiles];
        
        // 限制文件数量
        if (newFiles.length > 5) {
          wx.hideLoading();
          wx.showToast({
            title: '最多只能上传5个文件',
            icon: 'none'
          });
          return;
        }
        
        console.log(`[文件选择] 最终添加 ${finalFiles.length} 个文件到列表`);
        
        this.setData({
          'submissionData.files': newFiles
        });
        
        wx.hideLoading();
        
        wx.showToast({
          title: `成功添加${finalFiles.length}个文件`,
          icon: 'success'
        });
      }
    });
  },
  
  // 后退
  goBack: function() {
    wx.navigateBack();
  },
  
  // 展示更多选项
  showMoreOptions: function() {
    wx.showActionSheet({
      itemList: ['编辑任务', '删除任务', '添加子任务'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 编辑任务
          wx.navigateTo({
            url: '/pages/task/edit?id=' + this.taskId
          });
        } else if (res.tapIndex === 1) {
          // 删除任务
          this.confirmDeleteTask();
        } else if (res.tapIndex === 2) {
          // 添加子任务
          wx.navigateTo({
            url: '/pages/task/sub-task/add?taskId=' + this.taskId
          });
        }
      }
    });
  },
  
  // 计算剩余天数（改进版，支持过期显示）
  calculateRemainingDays: function(endTime) {
    if (!endTime) return { text: '-', isOverdue: false, days: null };
    
    const now = new Date();
    const end = new Date(endTime);
    const diffTime = end.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays > 0) {
      // 还有剩余时间
      return {
        text: `${diffDays} 天`,
        isOverdue: false,
        days: diffDays
      };
    } else if (diffDays === 0) {
      // 今天到期
      return {
        text: '今天到期',
        isOverdue: false,
        days: 0
      };
    } else {
      // 已经过期
      const overdueDays = Math.abs(diffDays);
      return {
        text: `已逾期${overdueDays}天`,
        isOverdue: true,
        days: diffDays
      };
    }
  },

  // 加载任务详情
  loadTaskDetail: function() {
    // 设置加载状态
    this.setData({ loading: true });
    
    // 调用后端API获取任务详情
    wx.request({
              url: `${BASE_URL}/wx/task/detail/${this.taskId}`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const taskDetail = res.data.data;
          console.log('API返回的任务详情:', taskDetail);
          
          // 处理API返回的数据
          const processedTask = {
            ...taskDetail,
            id: taskDetail.taskId || taskDetail.id,
            startTime: taskDetail.startTime,
            endTime: taskDetail.endTime,
            // 使用API返回的创建人字段
            creatorName: taskDetail.creatUserName || taskDetail.creatorName || '系统管理员',
            // 直接使用API返回的字段名
            completedSubTaskCount: taskDetail.completedSubTasks,
            totalSubTaskCount: taskDetail.totalSubTasks
          };
          
          // 处理子任务数据
          if (processedTask.subTasks && processedTask.subTasks.length > 0) {
            processedTask.subTasks = processedTask.subTasks.map(subTask => {
              // 处理子任务ID
              const processedSubTask = {
                ...subTask,
                id: subTask.subTaskId || subTask.id, // 确保有id字段
              };
              
              // 处理成员数据
              if (subTask.members && subTask.members.length > 0) {
                // 找出负责人（如果有）
                const leader = subTask.members.find(m => m.role === '负责人');
                
                if (leader) {
                  processedSubTask.leader = {
                    id: leader.userId,
                    name: leader.name,
                    avatar: leader.avatar ? `/upload/${leader.avatar}` : '/images/icons/default-avatar.png'
                  };
                }
                
                // 处理参与者列表
                processedSubTask.participantMembers = subTask.members.map(member => ({
                  id: member.userId,
                  name: member.name,
                  avatar: member.avatar || null,
                  role: member.role || '参与者',
                  note: member.note || ''
                }));
                
                // 设置参与者数量
                processedSubTask.participantCount = processedSubTask.participantMembers.length;
              } else {
                // 确保有空的成员列表
                processedSubTask.participantMembers = [];
                processedSubTask.participantCount = 0;
              }
              
              return processedSubTask;
            });
          }
          
          // 处理附件数据，这部分逻辑已经移至WXML的WXS模块中，此处不再需要
          /*
          if (processedTask.attachments && processedTask.attachments.length > 0) {
            processedTask.attachments = processedTask.attachments.map(attachment => {
              const processedAttachment = { ...attachment };
              
              // 如果没有fileSizeText但有fileSize，则生成fileSizeText
              if (!processedAttachment.fileSizeText && processedAttachment.fileSize) {
                processedAttachment.fileSizeText = this.getFileSizeText(processedAttachment.fileSize);
              }
              
              return processedAttachment;
            });
          }
          */
          
          // 如果API没有返回子任务完成情况，则从子任务列表中计算
          if (processedTask.completedSubTaskCount === undefined && processedTask.subTasks && processedTask.subTasks.length > 0) {
            const completedCount = processedTask.subTasks.filter(task => task.status === 1).length;
            processedTask.completedSubTaskCount = completedCount;
            processedTask.totalSubTaskCount = processedTask.subTasks.length;
          }
          
          // 确保有默认值
          if (processedTask.completedSubTaskCount === undefined) processedTask.completedSubTaskCount = 0;
          if (processedTask.totalSubTaskCount === undefined) processedTask.totalSubTaskCount = 0;
          
          // 计算剩余天数
          if (processedTask.status && processedTask.status.toUpperCase() === 'COMPLETED') {
            // 已完成任务不显示剩余时间
            processedTask.remainingDays = { text: '已完成', isOverdue: false, days: null, isCompleted: true };
          } else {
            processedTask.remainingDays = this.calculateRemainingDays(processedTask.endTime);
          }
          console.log('处理后的任务数据:', processedTask);
          
          // 检查是否为创建者
          const userId = wx.getStorageSync('userId') || '';
          const isTaskCreator = processedTask.creatorId === userId || processedTask.createUser === userId;
          
          // 检查用户是否有参与的子任务
          let userHasSubTasks = false;
          let userSubTaskIds = [];
          let userSubTaskNames = [];
          
          if (processedTask.subTasks && processedTask.subTasks.length > 0) {
            processedTask.subTasks.forEach(subTask => {
              // 检查是否为负责人
              if (subTask.leader && subTask.leader.id === userId) {
                userHasSubTasks = true;
                userSubTaskIds.push(subTask.id);
                userSubTaskNames.push(subTask.title);
              }
              // 检查是否为成员
              else if (subTask.participantMembers && subTask.participantMembers.some(member => member.id === userId)) {
                userHasSubTasks = true;
                userSubTaskIds.push(subTask.id);
                userSubTaskNames.push(subTask.title);
              }
            });
          }
          
          this.setData({
            task: processedTask,
            loading: false,
            isTaskCreator,
            userHasSubTasks,
            userSubTaskIds,
            userSubTaskNames
          });
        } else {
          wx.showToast({
            title: res.data?.message || '获取任务详情失败',
            icon: 'none'
          });
          
          // 使用模拟数据（实际使用时移除）
          this.setMockTaskData();
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
        
        // 使用模拟数据（实际使用时移除）
        this.setMockTaskData();
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  },
  
  // 模拟任务数据（开发测试用）
  setMockTaskData: function() {
      const mockTask = {
        id: this.taskId,
        title: '何湘技能大师工作室网站开发',
        description: '设计并开发何湘技能大师工作室官方网站，包括首页、团队介绍、成果展示、新闻动态和联系我们等模块。要求响应式设计，兼容各主流浏览器。',
        status: 'in_progress', // completed, urgent, overdue, rejected, in_progress, pending
        creatorId: 'user123',
        creatorName: '张教授',
        startTime: '2023-06-01',
        endTime: '2023-07-30',
        progress: 65,
        completedSubTaskCount: 5,
        totalSubTaskCount: 8,
      completedSubTasks: 5,  // 添加API返回的字段名
      totalSubTasks: 8,      // 添加API返回的字段名
        subTasks: [
          {
            id: 'st001',
            title: '首页UI设计',
            deadline: '2023-06-15',
            status: 1, // 1-已完成, 0-进行中, 2-待审核, 3-已退回
            leader: {
              id: 'user456',
              name: '李设计',
              avatar: '/images/icons/default-avatar.png'
            },
            participantCount: 3,
            participantMembers: [
              {id: 'user789', name: '王小明', avatar: '/images/icons/default-avatar.png'},
              {id: 'user790', name: '赵小红', avatar: '/images/icons/default-avatar.png'},
              {id: 'user791', name: '钱小刚', avatar: '/images/icons/default-avatar.png'}
            ]
          },
          {
            id: 'st002',
            title: '数据库设计与实现',
            deadline: '2023-06-20',
            status: 1,
            leader: {
              id: 'user457',
              name: '陈工程',
              avatar: '/images/icons/default-avatar.png'
            },
            participantCount: 2,
            participantMembers: [
              {id: 'user792', name: '孙小亮', avatar: '/images/icons/default-avatar.png'},
              {id: 'user793', name: '周小强', avatar: '/images/icons/default-avatar.png'}
            ]
          },
          {
            id: 'st003',
            title: '前端页面开发',
            deadline: '2023-07-10',
            status: 0,
            leader: {
              id: 'user458',
              name: '黄前端',
              avatar: '/images/icons/default-avatar.png'
            },
            participantCount: 4,
            participantMembers: [
              {id: 'user794', name: '吴小伟', avatar: '/images/icons/default-avatar.png'},
              {id: 'user795', name: '郑小勇', avatar: '/images/icons/default-avatar.png'},
              {id: 'user796', name: '冯小刚', avatar: '/images/icons/default-avatar.png'},
              {id: 'user797', name: '陈小红', avatar: '/images/icons/default-avatar.png'}
            ]
          },
          {
            id: 'st004',
            title: '后端API开发',
            deadline: '2023-07-15',
            status: 0,
            hasLeader: true,
            members: [
              {id: 'user798', name: '林后端', avatar: '/images/icons/default-avatar.png', role: '负责人'},
              {id: 'user799', name: '刘小华', avatar: '/images/icons/default-avatar.png'}
            ],
            participantCount: 1,
            participantMembers: [
              {id: 'user799', name: '刘小华', avatar: '/images/icons/default-avatar.png'}
            ]
          },
          {
            id: 'st005',
            title: '前后端联调',
            deadline: '2023-07-20',
            status: 2,
            leader: null,
            members: [
              {id: 'user458', name: '黄前端', avatar: '/images/icons/default-avatar.png'},
              {id: 'user798', name: '林后端', avatar: '/images/icons/default-avatar.png'}
            ],
            participantCount: 2,
            participantMembers: [
              {id: 'user458', name: '黄前端', avatar: '/images/icons/default-avatar.png'},
              {id: 'user798', name: '林后端', avatar: '/images/icons/default-avatar.png'}
            ]
          }
        ],
        attachments: [
          {
            id: 'att001',
            fileName: '网站设计需求文档.docx',
            fileSizeText: '1.2MB',
            url: 'https://example.com/files/123'
          },
          {
            id: 'att002',
            fileName: '网站原型设计.zip',
            fileSizeText: '5.8MB',
            url: 'https://example.com/files/124'
          },
          {
            id: 'att003',
            fileName: '数据库设计说明.pdf',
            fileSizeText: '843KB',
            url: 'https://example.com/files/125'
          }
        ]
      };
      
      // 计算剩余天数
      mockTask.remainingDays = this.calculateRemainingDays(mockTask.endTime);

    console.log('使用模拟数据:', mockTask);
      
      this.setData({
        task: mockTask,
        loading: false,
      isTaskCreator: true,
      userHasSubTasks: true,
      userSubTaskIds: ['st001', 'st002'],
      userSubTaskNames: ['首页UI设计', '数据库设计与实现']
    });
  },
  
  // 预览文件 - 使用公共方法
  handlePreviewFile: function(e) {
    const url = e.currentTarget.dataset.url;
    const fileName = e.currentTarget.dataset.name;
    // 任务附件使用'task'类型，避免后端自动添加material前缀
    previewFile(url, fileName, { fileType: 'task' });
  },
  
  // 下载文件 - 使用公共方法
  handleDownloadFile: function(e) {
    const url = e.currentTarget.dataset.url;
    const fileName = e.currentTarget.dataset.name;
    // 任务附件使用'task'类型，避免后端自动添加material前缀
    downloadFile(url, fileName, { fileType: 'task' });
  },

  // 通用文件下载方法（兼容性方法）
  downloadFile: function(e) {
    return this.handleDownloadFile(e);
  },
  
  // 完成任务
  completeTask: function() {
    wx.showModal({
      title: '确认完成任务',
      content: '确定要将此任务标记为已完成吗？',
      confirmText: '确认完成',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '处理中...'
          });
          
          // 模拟API请求
          setTimeout(() => {
            wx.hideLoading();
            
            // 更新本地状态
            this.setData({
              'task.status': 'completed'
            });
            
            wx.showToast({
              title: '任务已完成',
              icon: 'success'
            });
          }, 1500);
        }
      }
    });
  },
  
  // 确认删除任务
  confirmDeleteTask: function() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除此任务吗？此操作不可撤销。',
      confirmText: '确认删除',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '删除中...'
          });
          
          // 模拟API请求
          setTimeout(() => {
            wx.hideLoading();
            wx.showToast({
              title: '删除成功',
              icon: 'success',
              duration: 2000
            });
            
            // 返回上一页
            setTimeout(() => {
              wx.navigateBack();
            }, 1500);
          }, 1500);
        }
      }
    });
  },
  
  // 分享回调
  onShareAppMessage: function() {
    const { task } = this.data;
    
    // 安全检查，确保任务数据已加载
    if (!task || !task.title) {
      return {
        title: '任务详情',
        path: `/pages/task/detail?id=${this.taskId || ''}`
      };
    }
    
    return {
      title: `任务: ${task.title}`,
      path: `/pages/task/detail?id=${task.id}`,
      imageUrl: '', // 可以设置自定义分享图片
      desc: task.description ? task.description.substring(0, 50) + '...' : '查看任务详情'
    };
  },
  
  onShareTimeline: function() {
    const { task } = this.data;
    
    // 安全检查，确保任务数据已加载
    if (!task || !task.title) {
      return {
        title: '任务详情',
        query: `id=${this.taskId || ''}`
      };
    }
    
    return {
      title: `任务: ${task.title}`,
      query: `id=${task.id}`,
      imageUrl: '' // 可以设置自定义分享图片
    };
  }
}); 