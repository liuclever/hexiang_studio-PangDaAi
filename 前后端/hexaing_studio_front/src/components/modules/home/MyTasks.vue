<template>
  <el-card shadow="hover" class="my-tasks-card">
    <template #header>
      <div class="card-header">
        <span>我的待办</span>
        <router-link to="/task/management" class="view-all-link">
          查看全部
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </router-link>
      </div>
    </template>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="tasks.length === 0" class="empty-state">
      <el-empty description="太棒了！所有任务都已完成。" />
    </div>

    <div v-else class="tasks-list">
      <div v-for="task in tasks" :key="task.taskId" class="task-item" @click="viewTaskDetail(task)">
        <div class="task-info">
          <p class="task-title">{{ task.title }}</p>
          <span class="task-deadline" :class="getDeadlineClass(task.endTime)">
            截止日期: {{ formatDateTime(task.endTime) }}
          </span>
        </div>
        <el-tag :type="getStatusType(task.status)" size="small" effect="light">
          {{ getStatusText(task.status) }}
        </el-tag>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';
import { getMyTasks } from '@/api/dashboard';
import { formatDate } from '@/utils/date';

interface Task {
  taskId: number;
  title: string;
  endTime: string;
  status: string;
}

const tasks = ref<Task[]>([]);
const loading = ref(true);
const router = useRouter();

const fetchTasks = async () => {
  try {
    const res: any = await getMyTasks();
    if (res.code === 200) {
      tasks.value = res.data;
    } else {
      ElMessage.error('获取待办任务失败');
    }
  } catch (error) {
    ElMessage.error('获取待办任务时出错');
  } finally {
    loading.value = false;
  }
};

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '无截止日期';
  return formatDate(new Date(dateTime), 'YYYY-MM-DD HH:mm');
};

const getStatusType = (status: string) => {
  const types: { [key: string]: string } = {
    'URGENT': 'warning',
    'PENDING_REVIEW': 'info',
    'REJECTED': 'danger',
    'IN_PROGRESS': 'primary',
    'OVERDUE': 'danger',
  };
  return types[status] || 'info';
};

const getStatusText = (status: string) => {
  const texts: { [key: string]: string } = {
    'URGENT': '紧急',
    'PENDING_REVIEW': '待审核',
    'REJECTED': '被退回',
    'IN_PROGRESS': '进行中',
    'OVERDUE': '已逾期',
  };
  return texts[status] || '未知';
};

const getDeadlineClass = (endTime: string) => {
  if (!endTime) return '';
  const deadline = new Date(endTime).getTime();
  const now = new Date().getTime();
  const oneDay = 24 * 60 * 60 * 1000;

  if (deadline < now) {
    return 'deadline-overdue';
  } else if (deadline - now < oneDay) {
    return 'deadline-urgent';
  }
  return '';
};

const goToTaskManagement = () => {
  router.push('/task/management');
};

const viewTaskDetail = (task: Task) => {
  // 假设TaskManagementView可以接收一个查询参数来打开特定任务的详情
  router.push(`/task/management?taskId=${task.taskId}`);
};

onMounted(() => {
  fetchTasks();
});
</script>

<style lang="scss" scoped>
.my-tasks-card {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-card__header) {
    flex-shrink: 0;
  }

  :deep(.el-card__body) {
    flex-grow: 1;
    overflow-y: auto;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;

  .view-all-link {
    font-size: 14px;
    font-weight: normal;
    color: var(--el-color-primary);
    text-decoration: none;
    background-color: rgba(64, 158, 255, 0.1);
    padding: 4px 10px;
    border-radius: 15px;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;

    .el-icon {
      margin-left: 4px;
    }

    &:hover {
      background-color: rgba(64, 158, 255, 0.2);
      transform: translateX(2px);
    }
  }
}

.loading-container, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background-color: #f9fafb;
  cursor: pointer;
  transition: background-color 0.3s, box-shadow 0.3s;

  &:hover {
    background-color: #f0f2f5;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }
}

.task-info {
  display: flex;
  flex-direction: column;
}

.task-title {
  font-weight: 500;
  color: #303133;
  margin: 0 0 4px 0;
}

.task-deadline {
  font-size: 13px;
  color: #909399;

  &.deadline-urgent {
    color: #e6a23c;
    font-weight: 500;
  }

  &.deadline-overdue {
    color: #f56c6c;
    font-weight: 500;
  }
}
</style> 