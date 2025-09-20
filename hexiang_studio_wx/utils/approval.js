/**
 * 审批管理相关API
 */

const { request } = require('./request');

/**
 * 审批API模块
 */
const approvalApi = {
  
  /**
   * 获取审批统计数据
   */
  getApprovalStatistics() {
    return request({
      url: '/admin/approval/statistics',
      method: 'GET'
    });
  },

  /**
   * 获取待审批任务提交数量
   */
  getPendingTaskSubmissionCount() {
    return request({
      url: '/admin/task-submission/pending/count',
      method: 'GET'
    });
  },

  /**
   * 获取待审批请假申请数量
   */
  getPendingLeaveRequestCount() {
    return request({
      url: '/admin/approval/leave/pending/count',
      method: 'GET'
    });
  },

  /**
   * 获取今日已处理审批数量
   */
  getTodayProcessedCount() {
    return request({
      url: '/admin/approval/today/processed/count',
      method: 'GET'
    });
  },

  /**
   * 获取待审批任务提交列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.pageSize - 每页大小
   * @param {string} params.status - 状态筛选
   */
  getPendingTaskSubmissions(params = {}) {
    const { page = 1, pageSize = 10, status = 'pending' } = params;
    
    return request({
      url: '/admin/task-submission/list',
      method: 'GET',
      data: {
        page,
        pageSize,
        status
      }
    });
  },

  /**
   * 获取待审批请假申请列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.pageSize - 每页大小
   * @param {string} params.status - 状态筛选
   */
  getPendingLeaveRequests(params = {}) {
    const { page = 1, pageSize = 10, status = 'pending' } = params;
    
    return request({
      url: '/admin/approval/leave/list',
      method: 'GET',
      data: {
        page,
        pageSize,
        status
      }
    });
  },

  /**
   * 获取任务提交详情
   * @param {number} submissionId - 提交ID
   */
  getTaskSubmissionDetail(submissionId) {
    return request({
      url: `/admin/task-submission/detail/${submissionId}`,
      method: 'GET'
    });
  },

  /**
   * 获取请假申请详情
   * @param {number} requestId - 申请ID
   */
  getLeaveRequestDetail(requestId) {
    return request({
      url: `/admin/approval/leave/detail/${requestId}`,
      method: 'GET'
    });
  },

  /**
   * 审核任务提交
   * @param {number} submissionId - 提交ID
   * @param {Object} reviewData - 审核数据
   * @param {number} reviewData.status - 审核状态 (1: 通过, 3: 退回)
   * @param {string} reviewData.reviewComment - 审核评论
   */
  reviewTaskSubmission(submissionId, reviewData) {
    return request({
              url: `/wx/task/submission/review/${submissionId}`,
      method: 'POST',
      data: reviewData
    });
  },

  /**
   * 批准请假申请
   * @param {number} requestId - 申请ID
   */
  approveLeaveRequest(requestId) {
    return request({
      url: `/admin/approval/leave/${requestId}/approve`,
      method: 'POST'
    });
  },

  /**
   * 驳回请假申请
   * @param {number} requestId - 申请ID
   * @param {Object} rejectData - 驳回数据
   * @param {string} rejectData.remark - 驳回原因
   */
  rejectLeaveRequest(requestId, rejectData) {
    return request({
      url: `/admin/approval/leave/${requestId}/reject`,
      method: 'POST',
      data: rejectData
    });
  },

  /**
   * 批量审批任务提交
   * @param {Array} submissionIds - 提交ID数组
   * @param {Object} reviewData - 审核数据
   */
  batchReviewTaskSubmissions(submissionIds, reviewData) {
    return request({
      url: '/admin/task-submission/batch-review',
      method: 'POST',
      data: {
        submissionIds,
        ...reviewData
      }
    });
  },

  /**
   * 批量处理请假申请
   * @param {Array} requestIds - 申请ID数组
   * @param {string} action - 操作类型 ('approve' | 'reject')
   * @param {string} remark - 备注
   */
  batchProcessLeaveRequests(requestIds, action, remark = '') {
    return request({
      url: '/admin/approval/leave/batch',
      method: 'POST',
      data: {
        requestIds,
        action,
        remark
      }
    });
  }
};

// 导出API模块
module.exports = {
  approvalApi
}; 