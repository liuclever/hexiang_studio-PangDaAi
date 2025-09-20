<template>
  <el-dialog
    title="任务提交审核"
    v-model="dialogVisible"
    width="80%"
    destroy-on-close
    :close-on-click-modal="false"
    class="task-submission-review-dialog"
  >
    <div v-loading="loading">
      <!-- 任务基本信息 -->
      <el-card class="task-info-card">
        <template #header>
          <div class="card-header">
            <h3>{{ task.title }}</h3>
            <el-tag :type="getStatusType(task.status)" effect="dark">
              {{ getStatusText(task.status) }}
            </el-tag>
          </div>
        </template>
        <p class="task-description">{{ task.description || '暂无描述' }}</p>
      </el-card>

      <!-- 子任务提交列表 -->
      <el-card class="submissions-card">
        <template #header>
          <div class="card-header">
            <h3>提交审核</h3>
            <el-tag type="info">{{ pendingCount }} 个待审核</el-tag>
          </div>
        </template>

        <div v-if="task.subTasks && task.subTasks.length > 0" class="subtask-submissions">
          <el-collapse v-model="activeSubTasks" accordion>
            <el-collapse-item 
              v-for="(subTask, index) in task.subTasks" 
              :key="subTask.subTaskId"
              :name="subTask.subTaskId"
            >
              <template #title>
                <div class="subtask-header">
                  <h4>{{ index + 1 }}. {{ subTask.title }}</h4>
                  <div class="header-tags">
                    <el-tag 
                      :type="getSubTaskStatusType(subTask.status)" 
                      size="small" 
                      effect="dark"
                    >
                      {{ getSubTaskStatusText(subTask.status) }}
                    </el-tag>
                    <el-tag 
                      v-if="getSubTaskPendingCount(subTask) > 0"
                      type="warning" 
                      size="small"
                    >
                      {{ getSubTaskPendingCount(subTask) }} 待审核
                    </el-tag>
                  </div>
                </div>
              </template>

              <div class="subtask-content">
                <p class="subtask-description">{{ subTask.description || '暂无描述' }}</p>

                <el-table
                  :data="subTask.members"
                  style="width: 100%"
                  border
                  class="submission-table"
                >
                  <el-table-column label="成员" min-width="150">
                    <template #default="scope">
                      <div class="member-info">
                        <el-avatar 
                          :size="32" 
                          :src="getAvatarUrl(scope.row.avatar)"
                          :alt="scope.row.name"
                        >
                          {{ scope.row.name?.charAt(0) }}
                        </el-avatar>
                        <span>{{ scope.row.name }}</span>
                      </div>
                    </template>
                  </el-table-column>

                  <el-table-column label="角色" prop="role" width="100" />

                  <el-table-column label="提交状态" width="120">
                    <template #default="scope">
                      <el-tag
                        v-if="scope.row.submission"
                        :type="getSubmissionStatusType(scope.row.submission.status)"
                        size="small"
                      >
                        {{ getSubmissionStatusText(scope.row.submission.status) }}
                      </el-tag>
                      <el-tag v-else type="info" size="small">未提交</el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column label="提交时间" width="160">
                    <template #default="scope">
                      {{ scope.row.submission ? formatDateTime(scope.row.submission.submissionTime) : '-' }}
                    </template>
                  </el-table-column>

                  <el-table-column label="操作" width="300">
                    <template #default="scope">
                      <div class="action-buttons">
                        <el-button
                          v-if="scope.row.submission"
                          type="primary"
                          size="small"
                          @click="viewSubmissionDetail(scope.row)"
                        >
                          查看详情
                        </el-button>
                        <el-button
                          v-if="scope.row.submission && scope.row.submission.status === 2"
                          type="success"
                          size="small"
                          @click="reviewSubmission(scope.row, 'approve')"
                        >
                          通过
                        </el-button>
                        <el-button
                          v-if="scope.row.submission && scope.row.submission.status === 2"
                          type="danger"
                          size="small"
                          @click="reviewSubmission(scope.row, 'reject')"
                        >
                          退回
                        </el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <el-empty v-else description="暂无子任务" />
      </el-card>
    </div>

    <!-- 提交详情对话框 -->
    <el-dialog
      v-model="submissionDetailVisible"
      title="提交详情"
      width="60%"
      append-to-body
      destroy-on-close
    >
      <div v-if="selectedSubmission" class="submission-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="提交人">
            <div class="member-info">
              <el-avatar 
                :size="24" 
                :src="getAvatarUrl(selectedSubmission.avatar)" 
                :alt="selectedSubmission.name"
              >
                {{ selectedSubmission.name?.charAt(0) }}
              </el-avatar>
              <span>{{ selectedSubmission.name }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(selectedSubmission.submission.submissionTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="提交备注">
            {{ selectedSubmission.submission.submissionNotice || '无备注' }}
          </el-descriptions-item>
          <el-descriptions-item label="附件">
            <div v-if="selectedSubmission.submissionAttachments && selectedSubmission.submissionAttachments.length > 0">
              <el-link
                v-for="attachment in selectedSubmission.submissionAttachments"
                :key="attachment.attachmentId"
                type="primary"
                @click="downloadSubmissionFile(attachment)"
                style="margin-right: 12px;"
              >
                {{ attachment.fileName }}
              </el-link>
            </div>
            <span v-else>无附件</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getSubmissionStatusType(selectedSubmission.submission.status)">
              {{ getSubmissionStatusText(selectedSubmission.submission.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" v-if="selectedSubmission.submission.reviewComment">
            {{ selectedSubmission.submission.reviewComment }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间" v-if="selectedSubmission.submission.reviewTime">
            {{ formatDateTime(selectedSubmission.submission.reviewTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewAction === 'approve' ? '通过审核' : '退回提交'"
      width="50%"
      append-to-body
      destroy-on-close
    >
      <el-form ref="reviewFormRef" :model="reviewForm">
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            :placeholder="
              reviewAction === 'approve'
                ? '请输入通过审核的备注（可选）'
                : '请输入退回的原因和修改建议'
            "
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReview">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { formatDate } from '@/utils/date';
import { getTaskSubmissions, reviewSubmission as apiReviewSubmission } from '@/utils/api/task';

// 定义props和emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  taskId: {
    type: [Number, String],
    default: '',
  },
});

const emit = defineEmits(['update:visible', 'refresh']);

// 对话框显示状态
const dialogVisible = ref(props.visible);

// 监听visible属性变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val;
  if (val && props.taskId) {
    fetchTaskDetail(props.taskId);
  }
});

// 监听对话框关闭
watch(dialogVisible, (val) => {
  if (!val) emit('update:visible', false);
});

// 数据和状态
const loading = ref(false);
const task = ref<any>({ subTasks: [] });
const activeSubTasks = ref<string[]>([]);

// 提交详情相关
const submissionDetailVisible = ref(false);
const selectedSubmission = ref<any>(null);

// 审核相关
const reviewDialogVisible = ref(false);
const reviewAction = ref<'approve' | 'reject'>('approve');
const reviewForm = ref({ comment: '' });
const currentMember = ref<any>(null);

// 计算待审核数量
const pendingCount = computed(() => {
  let count = 0;
  task.value.subTasks.forEach((subTask: any) => {
    subTask.members.forEach((member: any) => {
      if (member.submission && member.submission.status === 2) { // 2: 待审核
        count++;
      }
    });
  });
  return count;
});

// 获取子任务待审核数量
const getSubTaskPendingCount = (subTask: any) => {
  return subTask.members.filter((member: any) => 
    member.submission && member.submission.status === 2 // 2: 待审核
  ).length;
};

// 获取任务提交审核信息
const fetchTaskDetail = async (taskId: string | number) => {
  loading.value = true;
  try {
    const res: any = await getTaskSubmissions(taskId);
    task.value = res.data;
  } catch (error) {
    ElMessage.error('获取任务提交信息失败');
  } finally {
    loading.value = false;
  }
};

// 查看提交详情
const viewSubmissionDetail = (member: any) => {
  selectedSubmission.value = member;
  submissionDetailVisible.value = true;
};

// 审核提交
const reviewSubmission = (member: any, action: 'approve' | 'reject') => {
  if (!member.submission) return;
  
  currentMember.value = member;
  reviewAction.value = action;
  reviewForm.value.comment = '';
  reviewDialogVisible.value = true;
};

// 确认审核
const confirmReview = async () => {
  if (!currentMember.value || !currentMember.value.submission) {
    ElMessage.error('提交记录不存在');
    return;
  }
  
  const submission = currentMember.value.submission;
  const data = {
    status: reviewAction.value === 'approve' ? 1 : 3, // 1: 通过, 3: 退回
    reviewComment: reviewForm.value.comment || ''
  };
  
  try {
    await apiReviewSubmission(submission.submissionId, data);
    
    ElMessage.success(reviewAction.value === 'approve' ? '审核通过' : '已退回');
    reviewDialogVisible.value = false;
    
    // 重新加载任务详情
    if (props.taskId) {
      fetchTaskDetail(props.taskId);
    }
    
    // 通知父组件刷新
    emit('refresh');
  } catch (error) {
    console.error('审核操作失败:', error);
    ElMessage.error('审核操作失败');
  }
};

// 下载提交的附件
const downloadSubmissionFile = (attachment: any) => {
  const link = document.createElement('a');
  link.href = `/api/admin/file/view/${attachment.filePath}?download=true&originalName=${encodeURIComponent(attachment.fileName)}`;
  link.download = attachment.fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return formatDate(new Date(dateStr), 'YYYY-MM-DD HH:mm');
};

// 状态相关函数
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    URGENT: 'warning',
    PENDING_REVIEW: 'info',
    REJECTED: 'danger',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    OVERDUE: 'danger',
  };
  return types[status] || 'info';
};

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    URGENT: '紧急',
    PENDING_REVIEW: '待审核',
    REJECTED: '被退回',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    OVERDUE: '已逾期',
  };
  return texts[status] || '未知状态';
};

const getSubTaskStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'success',    // 已完成
    2: 'warning',    // 待审核
    3: 'danger',     // 已退回
    0: 'primary',    // 进行中
  };
  return types[status] || 'primary';
};

const getSubTaskStatusText = (status: number) => {
  const texts: Record<number, string> = {
    1: '已完成',
    2: '待审核',
    3: '已退回',
    0: '进行中',
  };
  return texts[status] || '进行中';
};

const getSubmissionStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'info',      // 进行中
    1: 'success',   // 已完成
    2: 'primary',   // 待审核
    3: 'danger',    // 已退回
  };
  return types[status] || 'info';
};

const getSubmissionStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '进行中',
    1: '已通过',
    2: '待审核',
    3: '已退回',
  };
  return texts[status] || '未知状态';
};

// 获取头像URL
const getAvatarUrl = (avatarPath: string | null | undefined) => {
  if (!avatarPath) return '';
  
  // 如果已经是完整URL，直接返回
  if (avatarPath.startsWith('http')) {
    return avatarPath;
  }
  
  // 构建完整的头像URL
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  return `${baseUrl}/admin/file/view/${avatarPath}`;
};
</script>

<style lang="scss" scoped>
.task-submission-review-dialog {
  .task-info-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-weight: 600;
      }
    }
    
    .task-description {
      margin: 0;
      color: #606266;
      line-height: 1.6;
    }
  }
  
  .submissions-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-weight: 600;
      }
    }
  }
  
  .subtask-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    
    h4 {
      margin: 0;
      font-weight: 600;
    }
    
    .header-tags {
      display: flex;
      gap: 8px;
    }
  }
  
  .subtask-content {
    .subtask-description {
      margin: 0 0 16px 0;
      padding: 12px;
      background-color: #f8f9fa;
      border-radius: 6px;
      color: #606266;
    }
  }
  
  .submission-table {
    .member-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .action-buttons {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }
  
  .submission-detail {
    .member-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}
</style> 