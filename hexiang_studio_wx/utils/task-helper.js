/**
 * 任务状态计算帮助工具
 * 提供统一的任务统计和状态判断逻辑
 */

/**
 * 任务状态枚举
 */
const TASK_STATUS = {
  PENDING: 'pending',           // 待处理
  IN_PROGRESS: 'in_progress',   // 进行中
  URGENT: 'urgent',             // 紧急
  OVERDUE: 'overdue',           // 已逾期
  COMPLETED: 'completed',       // 已完成
  REJECTED: 'rejected',         // 已退回
  PENDING_REVIEW: 'pending_review' // 待审核
};

/**
 * 计算任务统计信息
 * @param {Array} tasks - 任务列表
 * @returns {Object} 统计结果
 */
const calculateTaskStats = (tasks) => {
  if (!Array.isArray(tasks) || tasks.length === 0) {
    return {
      total: 0,
      urgent: 0,           // 真正的紧急任务
      overdue: 0,          // 逾期任务
      needAttention: 0,    // 需要关注的任务（紧急+逾期）
      completed: 0,        // 已完成
      inProgress: 0,       // 进行中
      pending: 0,          // 待处理
      rejected: 0          // 已退回
    };
  }

  const stats = {
    total: tasks.length,
    urgent: 0,
    overdue: 0,
    needAttention: 0,
    completed: 0,
    inProgress: 0,
    pending: 0,
    rejected: 0
  };

  tasks.forEach(task => {
    const status = task.status?.toLowerCase();
    
    switch (status) {
      case TASK_STATUS.URGENT:
        stats.urgent++;
        stats.needAttention++;
        break;
      case TASK_STATUS.OVERDUE:
        stats.overdue++;
        stats.needAttention++;
        break;
      case TASK_STATUS.COMPLETED:
        stats.completed++;
        break;
      case TASK_STATUS.IN_PROGRESS:
        stats.inProgress++;
        break;
      case TASK_STATUS.PENDING:
        stats.pending++;
        break;
      case TASK_STATUS.REJECTED:
        stats.rejected++;
        break;
      case TASK_STATUS.PENDING_REVIEW:
        stats.pending++;
        break;
    }
  });

  return stats;
};

/**
 * 生成任务提醒文案
 * @param {Object} stats - 任务统计结果
 * @returns {Object} 提醒信息
 */
const generateTaskReminder = (stats) => {
  const { urgent, overdue, needAttention } = stats;
  
  if (needAttention === 0) {
    return {
      hasReminder: false,
      text: '',
      type: 'none'
    };
  }

  let text = '';
  let type = 'warning';

  if (urgent > 0 && overdue > 0) {
    text = `您有 ${urgent} 个紧急任务和 ${overdue} 个逾期任务待处理`;
    type = 'danger';
  } else if (urgent > 0) {
    text = `您有 ${urgent} 个紧急任务待处理`;
    type = 'warning';
  } else if (overdue > 0) {
    text = `您有 ${overdue} 个逾期任务待处理`;
    type = 'danger';
  }

  return {
    hasReminder: true,
    text,
    type,
    urgent,
    overdue,
    total: needAttention
  };
};

/**
 * 判断任务是否需要关注
 * @param {Object} task - 任务对象
 * @returns {Boolean} 是否需要关注
 */
const isTaskNeedAttention = (task) => {
  if (!task || !task.status) return false;
  
  const status = task.status.toLowerCase();
  return status === TASK_STATUS.URGENT || status === TASK_STATUS.OVERDUE;
};

/**
 * 获取任务状态显示文本
 * @param {String} status - 任务状态
 * @returns {String} 显示文本
 */
const getTaskStatusText = (status) => {
  if (!status) return '未知';
  
  const statusMap = {
    [TASK_STATUS.PENDING]: '待处理',
    [TASK_STATUS.IN_PROGRESS]: '进行中',
    [TASK_STATUS.URGENT]: '紧急',
    [TASK_STATUS.OVERDUE]: '已逾期',
    [TASK_STATUS.COMPLETED]: '已完成',
    [TASK_STATUS.REJECTED]: '已退回',
    [TASK_STATUS.PENDING_REVIEW]: '待审核'
  };
  
  return statusMap[status.toLowerCase()] || '未知';
};

/**
 * 根据截止时间计算任务紧急程度
 * @param {String} endTime - 截止时间
 * @returns {Object} 紧急程度信息
 */
const calculateUrgency = (endTime) => {
  if (!endTime) return { level: 'normal', text: '无期限', days: null };
  
  const now = new Date();
  const end = new Date(endTime);
  const diffTime = end.getTime() - now.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays < 0) {
    return { level: 'overdue', text: '已逾期', days: diffDays };
  } else if (diffDays === 0) {
    return { level: 'urgent', text: '今天到期', days: 0 };
  } else if (diffDays <= 1) {
    return { level: 'urgent', text: '明天到期', days: diffDays };
  } else if (diffDays <= 3) {
    return { level: 'warning', text: `${diffDays}天后到期`, days: diffDays };
  } else {
    return { level: 'normal', text: `${diffDays}天后到期`, days: diffDays };
  }
};

module.exports = {
  TASK_STATUS,
  calculateTaskStats,
  generateTaskReminder,
  isTaskNeedAttention,
  getTaskStatusText,
  calculateUrgency
}; 