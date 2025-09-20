<template>
  <el-card shadow="hover" class="calendar-panel-card">
    <template #header>
      <div class="panel-header">
        <span>任务日历</span>
        <router-link to="/task/management?view=calendar" class="view-all-link">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </template>
    <div class="calendar-container">
      <FullCalendar
        ref="fullCalendarRef"
        :options="calendarOptions"
        class="task-calendar"
      />

      <!-- 任务预览对话框 -->
      <el-dialog
        v-model="taskPreviewVisible"
        :title="selectedTask?.title || '任务详情'"
        width="400px"
        destroy-on-close
        :close-on-click-modal="true"
        class="task-preview-dialog"
      >
        <div v-if="selectedTask" class="task-preview-content">
          <div class="preview-item">
            <span class="label">状态:</span>
            <el-tag :type="getTaskStatusType(selectedTask.status)" size="small">
              {{ getTaskStatusText(selectedTask.status) }}
            </el-tag>
          </div>
          
          <div class="preview-item">
            <span class="label">描述:</span>
            <span class="value">{{ selectedTask.description || '暂无描述' }}</span>
          </div>
          
          <div class="preview-item">
            <span class="label">开始时间:</span>
            <span class="value">{{ formatDateTime(selectedTask.startTime) }}</span>
          </div>
          
          <div class="preview-item">
            <span class="label">截止时间:</span>
            <span class="value">{{ formatDateTime(selectedTask.endTime) }}</span>
          </div>
          
          <div class="preview-item">
            <span class="label">进度:</span>
            <div class="progress-wrapper">
              <el-progress 
                :percentage="calculateProgress(selectedTask)" 
                :status="getProgressStatus(selectedTask)"
              />
              <span class="progress-text">{{ selectedTask.completedSubTasks || 0 }}/{{ selectedTask.totalSubTasks || 0 }}</span>
            </div>
          </div>
        </div>
        
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="taskPreviewVisible = false">关闭</el-button>
            <el-button type="primary" @click="viewTaskDetail(selectedTask)">查看详情</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';
import { getTasks } from '@/utils/api/task';
import { formatDate } from '@/utils/date';

// 引入FullCalendar和插件
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';

// 状态管理
const tasks = ref<any[]>([]);
const loading = ref(false);
const calendarEvents = ref<any[]>([]);
const router = useRouter();

// 预览对话框状态
const taskPreviewVisible = ref(false);
const selectedTask = ref<any>(null);

// 日历引用
const fullCalendarRef = ref(null);

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return formatDate(new Date(dateStr), 'YYYY-MM-DD HH:mm');
};

// 计算任务进度
const calculateProgress = (task: any) => {
  if (!task || !task.totalSubTasks) return 0;
  return Math.round((task.completedSubTasks / task.totalSubTasks) * 100);
};

// 获取进度状态
const getProgressStatus = (task: any) => {
  if (!task) return '';
  if (task.status === 'COMPLETED') return 'success';
  if (task.status === 'OVERDUE') return 'exception';
  
  const progress = calculateProgress(task);
  if (progress >= 80) return 'success';
  if (progress >= 40) return '';
  return 'exception';
};

// 任务状态方法
const getTaskStatusType = (status: string) => {
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

const getTaskStatusText = (status: string) => {
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

// 日历配置
const calendarOptions = reactive({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next',
    center: 'title',
    right: 'today'
  },
  locale: 'zh-cn', // 使用中文语言
  eventClick: handleEventClick,
  eventTimeFormat: { // 使用符合FullCalendar要求的格式
    hour: '2-digit' as const,
    minute: '2-digit' as const,
    hour12: false
  },
  events: calendarEvents.value,
  eventColor: '#409EFF',
  eventBackgroundColor: '#409EFF',
  eventBorderColor: '#1890ff',
  eventTextColor: '#ffffff',
  eventDisplay: 'block' as const, // 使用块状显示堆叠事件
  dayMaxEvents: true, // 当事件太多时允许"更多"链接
  height: 'auto',
  aspectRatio: 1.5,
  firstDay: 1, // 从周一开始
  dayMaxEventRows: 3, // 每日最大显示事件数，超过显示"更多"
  eventClassNames: handleEventClassNames,
  noEventsContent: '暂无任务'
});

// 处理事件点击
function handleEventClick(info: any) {
  const taskId = info.event.id;
  if (!taskId) return;
  
  // 查找任务数据
  const task = tasks.value.find(t => String(t.taskId) === String(taskId));
  if (task) {
    selectedTask.value = task;
    taskPreviewVisible.value = true;
  }
}

// 根据任务状态生成事件样式类
function handleEventClassNames(info: any) {
  const taskId = info.event.id;
  const task = tasks.value.find(t => String(t.taskId) === String(taskId));
  
  if (!task) return [];
  
  const classNames = ['task-event'];
  
  if (task.status === 'COMPLETED') {
    classNames.push('task-completed');
  } else if (task.status === 'OVERDUE') {
    classNames.push('task-overdue');
  } else if (task.status === 'IN_PROGRESS') {
    classNames.push('task-in-progress');
  } else if (task.status === 'NOT_STARTED') {
    classNames.push('task-not-started');
  }
  
  return classNames;
}

// 从API获取任务
const fetchTasks = async () => {
  loading.value = true;
  try {
    // 获取所有任务，不使用分页（首页只显示最近的任务）
    const params = {
      page: 1,
      size: 100, // 获取足够的任务以展示在日历上
    };

    const res = await getTasks(params);
    
    if (res.code === 200) {
      tasks.value = res.data.records.map((task: any) => ({
        ...task,
        completedSubTasks: task.completedSubTasks || 0,
        totalSubTasks: task.totalSubTasks || (task.subTasks?.length || 0)
      }));
      
      // 将任务转换为日历事件
      transformTasksToEvents();
    } else {
      ElMessage.error(res.message || '获取任务列表失败');
    }
  } catch (error) {
    console.error('获取任务失败:', error);
  } finally {
    loading.value = false;
  }
};

// 将任务转换为日历事件
const transformTasksToEvents = () => {
  calendarEvents.value = tasks.value.map(task => {
    // 根据状态确定颜色
    let backgroundColor;
    let borderColor;
    let textColor = '#ffffff';
    
    switch (task.status) {
      case 'COMPLETED':
        backgroundColor = '#67C23A';
        borderColor = '#4caf50';
        break;
      case 'OVERDUE':
        backgroundColor = '#F56C6C';
        borderColor = '#e53935';
        break;
      case 'IN_PROGRESS':
        backgroundColor = '#409EFF';
        borderColor = '#2196f3';
        break;
      case 'NOT_STARTED':
        backgroundColor = '#909399';
        borderColor = '#607d8b';
        break;
      default:
        backgroundColor = '#409EFF';
        borderColor = '#1890ff';
    }
    
    return {
      id: String(task.taskId),
      title: task.title,
      start: task.endTime ? task.endTime.substring(0, 10) : null, // 使用截止时间作为事件日期
      backgroundColor,
      borderColor,
      textColor,
      extendedProps: {
        description: task.description,
        status: task.status
      },
      allDay: true // 设置为全天事件以美化显示
    };
  });
  
  // 更新日历事件
  if (fullCalendarRef.value) {
    const calendarApi = fullCalendarRef.value.getApi();
    calendarApi.removeAllEvents();
    calendarApi.addEventSource(calendarEvents.value);
  }
};

// 查看任务详情
const viewTaskDetail = (task: any) => {
  if (!task) return;
  
  taskPreviewVisible.value = false;
  // 直接打开任务详情对话框，而不是跳转到任务管理页面
  router.push(`/task/management`);
  // 使用一个小延迟确保页面已加载
  setTimeout(() => {
    // 通过自定义事件通知任务管理页面打开指定任务的详情
    window.dispatchEvent(new CustomEvent('open-task-detail', { 
      detail: { taskId: task.taskId }
    }));
  }, 100);
};

// 生命周期钩子
onMounted(() => {
  fetchTasks();
});
</script>

<style lang="scss" scoped>
.calendar-panel-card {
  border-radius: var(--el-border-radius-base);
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  }
  
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    span {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
    
    .view-all-link {
      display: flex;
      align-items: center;
      font-size: 14px;
      color: var(--el-color-primary);
      text-decoration: none;
      background-color: rgba(64, 158, 255, 0.1);
      padding: 4px 10px;
      border-radius: 15px;
      transition: all 0.3s ease;
      
      .el-icon {
        margin-left: 4px;
      }
      
      &:hover {
        background-color: rgba(64, 158, 255, 0.2);
        transform: translateX(2px);
      }
    }
  }
  
  .calendar-container {
    width: 100%;
    
    .task-calendar {
      height: 100%;
      width: 100%;
      
      :deep(.fc) {
        .fc-toolbar-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
        
        .fc-button {
          background-color: #f5f7fa;
          border-color: #dcdfe6;
          color: #606266;
          font-weight: 500;
          text-transform: none;
          padding: 6px 12px;
          height: auto;
          
          &:focus {
            box-shadow: none;
          }
          
          &.fc-button-active {
            background-color: #409eff;
            border-color: #409eff;
            color: #ffffff;
          }
        }
        
        .fc-daygrid-day-top {
          flex-direction: row;
          padding: 3px 5px;
        }
        
        .fc-daygrid-day-number {
          font-weight: 500;
          font-size: 13px;
        }
        
        .fc-daygrid-day.fc-day-today {
          background-color: rgba(64, 158, 255, 0.1);
        }
        
        .fc-event {
          cursor: pointer;
          border-radius: 4px;
          padding: 1px 4px;
          margin-bottom: 2px;
          font-size: 12px;
          font-weight: 500;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          
          &.task-completed {
            background-color: #67C23A;
            border-color: #4caf50;
          }
          
          &.task-overdue {
            background-color: #F56C6C;
            border-color: #e53935;
          }
          
          &.task-in-progress {
            background-color: #409EFF;
            border-color: #2196f3;
          }
          
          &.task-not-started {
            background-color: #909399;
            border-color: #607d8b;
          }
          
          &:hover {
            filter: brightness(1.1);
          }
        }
        
        .fc-daygrid-more-link {
          color: #409eff;
          font-weight: 500;
          font-size: 12px;
        }
        
        .fc-col-header-cell {
          background-color: #f5f7fa;
          
          .fc-col-header-cell-cushion {
            padding: 8px;
            font-weight: 600;
            color: #606266;
            font-size: 13px;
          }
        }
        
        .fc-day-other .fc-daygrid-day-top {
          opacity: 0.6;
        }
      }
    }
  }
}

.task-preview-dialog {
  .task-preview-content {
    padding: 10px 0;
    
    .preview-item {
      margin-bottom: 16px;
      
      .label {
        font-weight: 600;
        margin-right: 8px;
        color: #606266;
        display: block;
        margin-bottom: 5px;
      }
      
      .value {
        color: #303133;
      }
      
      .progress-wrapper {
        margin-top: 5px;
        
        .progress-text {
          display: block;
          text-align: right;
          margin-top: 5px;
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}
</style> 