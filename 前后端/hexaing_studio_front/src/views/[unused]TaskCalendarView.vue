<template>
  <div class="task-calendar-view">
    <!-- Page header -->
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>任务日历</h2>
        <div class="view-options">
          <el-button type="primary" class="create-task-btn" @click="showAddTaskDialog">
            <el-icon><Plus /></el-icon>创建任务
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Calendar card -->
    <el-card shadow="hover" class="calendar-card">
      <div class="calendar-container">
        <FullCalendar
          ref="fullCalendarRef"
          :options="calendarOptions"
          class="task-calendar"
        />
      </div>
    </el-card>

    <!-- Task Detail Dialog -->
    <task-detail-dialog
      v-model:visible="taskDetailVisible"
      :task-id="currentTaskId"
      @refresh="fetchTasks"
    />

    <!-- Task Form Dialog -->
    <task-form-dialog
      v-model:visible="taskFormVisible"
      :task="currentTask"
      :is-edit="isEdit"
      @refresh="fetchTasks"
    />

    <!-- Task Event Preview Popover -->
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getTasks, getTaskById } from '@/utils/api/task';
import { formatDate } from '@/utils/date';

// Import FullCalendar and plugins
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

// Import components
import TaskDetailDialog from '@/components/modules/task/TaskDetailDialog.vue';
import TaskFormDialog from '@/components/modules/task/TaskFormDialog.vue';

// State
const tasks = ref<any[]>([]);
const loading = ref(false);
const calendarEvents = ref<any[]>([]);

// Dialog state
const taskDetailVisible = ref(false);
const taskFormVisible = ref(false);
const taskPreviewVisible = ref(false);
const currentTaskId = ref('');
const currentTask = ref<Record<string, any> | undefined>(undefined);
const isEdit = ref(false);
const selectedTask = ref<any>(null);

// Calendar reference
const fullCalendarRef = ref(null);

// Format date time
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return formatDate(new Date(dateStr), 'YYYY-MM-DD HH:mm');
};

// Calculate task progress
const calculateProgress = (task: any) => {
  if (!task || !task.totalSubTasks) return 0;
  return Math.round((task.completedSubTasks / task.totalSubTasks) * 100);
};

// Get progress status
const getProgressStatus = (task: any) => {
  if (!task) return '';
  if (task.status === 'COMPLETED') return 'success';
  if (task.status === 'OVERDUE') return 'exception';
  
  const progress = calculateProgress(task);
  if (progress >= 80) return 'success';
  if (progress >= 40) return '';
  return 'exception';
};

// Task status methods
const getTaskStatusType = (status: string) => {
  const types: Record<string, string> = {
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    OVERDUE: 'danger',
    URGENT: 'warning',
    PENDING_REVIEW: 'info',
    REJECTED: 'danger',
  };
  return types[status] || 'info';
};

const getTaskStatusText = (status: string) => {
  const texts: Record<string, string> = {
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    OVERDUE: '已逾期',
    URGENT: '紧急',
    PENDING_REVIEW: '待审核',
    REJECTED: '被退回',
  };
  return texts[status] || '未知状态';
};

// Calendar options
const calendarOptions = reactive({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  locale: 'zh-cn', // Use Chinese locale
  eventClick: handleEventClick,
  eventTimeFormat: {
    hour: '2-digit' as const,
    minute: '2-digit' as const,
    hour12: false
  },
  events: calendarEvents.value,
  eventColor: '#409EFF',
  eventBackgroundColor: '#409EFF',
  eventBorderColor: '#1890ff',
  eventTextColor: '#ffffff',
  eventDisplay: 'block', // Use block display to show stacked events
  dayMaxEvents: true, // Allow "more" link when too many events
  height: 'auto',
  aspectRatio: 1.8,
  firstDay: 1, // Monday
  dayMaxEventRows: 4, // Maximum number of events before "more" link
  eventClassNames: handleEventClassNames,
  noEventsContent: '暂无任务'
});

// Handle event click
function handleEventClick(info: any) {
  const taskId = info.event.id;
  if (!taskId) return;
  
  // Find the task data
  const task = tasks.value.find(t => String(t.taskId) === String(taskId));
  if (task) {
    selectedTask.value = task;
    taskPreviewVisible.value = true;
  }
}

// Generate event class names based on task status
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
      } else if (task.status === 'IN_PROGRESS') {
      classNames.push('task-in-progress');
  }
  
  return classNames;
}

// Fetch tasks from API
const fetchTasks = async () => {
  loading.value = true;
  try {
    // Fetch all tasks without pagination for calendar view
    const params = {
      page: 1,
      size: 1000, // Large number to get all tasks
    };

    const res = await getTasks(params) as any;
    
    if (res.code === 200) {
      tasks.value = res.data.records.map((task: any) => ({
        ...task,
        completedSubTasks: task.completedSubTasks || 0,
        totalSubTasks: task.totalSubTasks || (task.subTasks?.length || 0)
      }));
      
      // Transform tasks to calendar events
      transformTasksToEvents();
    } else {
      ElMessage.error(res.message || '获取任务列表失败');
    }
  } catch (error) {
    ElMessage.error('获取任务列表失败');
  } finally {
    loading.value = false;
  }
};

// Transform tasks to calendar events
const transformTasksToEvents = () => {
  calendarEvents.value = tasks.value.map(task => {
    // Determine color based on status
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
      case 'URGENT':
        backgroundColor = '#E6A23C';
        borderColor = '#f39c12';
        break;
      case 'PENDING_REVIEW':
        backgroundColor = '#409EFF';
        borderColor = '#2196f3';
        break;
      case 'REJECTED':
        backgroundColor = '#F56C6C';
        borderColor = '#e53935';
        break;
      default:
        backgroundColor = '#409EFF';
        borderColor = '#1890ff';
    }
    
    return {
      id: String(task.taskId),
      title: task.title,
      start: task.endTime ? task.endTime.substring(0, 10) : null, // Use end time as the event date
      backgroundColor,
      borderColor,
      textColor,
      extendedProps: {
        description: task.description,
        status: task.status
      },
      allDay: true // Set to true for cleaner display
    };
  });
  
  // Update calendar events
  if (fullCalendarRef.value) {
    const calendarApi = (fullCalendarRef.value as any).getApi();
    calendarApi.removeAllEvents();
    calendarApi.addEventSource(calendarEvents.value);
  }
};

// View task details
const viewTaskDetail = (task: any) => {
  if (!task) return;
  
  currentTaskId.value = String(task.taskId);
  taskPreviewVisible.value = false;
  taskDetailVisible.value = true;
};

// Show add task dialog
const showAddTaskDialog = () => {
  currentTask.value = undefined;
  isEdit.value = false;
  taskFormVisible.value = true;
};

// Lifecycle hooks
onMounted(() => {
  fetchTasks();
});
</script>

<style lang="scss" scoped>
.task-calendar-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  .page-header-card {
    border: none;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    background: linear-gradient(135deg, #f0f5ff, #ffffff);
    
    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .page-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        color: #303133;
        position: relative;
        
        &:after {
          content: '';
          position: absolute;
          bottom: -8px;
          left: 0;
          width: 40px;
          height: 3px;
          background: linear-gradient(90deg, #409eff, #64b5f6);
          border-radius: 3px;
        }
      }
      
      .view-options {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      
      .create-task-btn {
        border-radius: 30px;
        padding: 10px 24px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 5px;
        background: linear-gradient(90deg, #409eff, #64b5f6);
        border: none;
        box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
        transition: all 0.3s;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 12px rgba(64, 158, 255, 0.4);
        }
        
        .el-icon {
          margin-right: 2px;
        }
      }
    }
  }
  
  .calendar-card {
    border: none;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    
    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .calendar-container {
      width: 100%;
      min-height: calc(100vh - 250px);
      
      .task-calendar {
        height: 100%;
        width: 100%;
        
        :deep(.fc) {
          .fc-toolbar-title {
            font-size: 20px;
            font-weight: 600;
            color: #303133;
          }
          
          .fc-button {
            background-color: #f5f7fa;
            border-color: #dcdfe6;
            color: #606266;
            font-weight: 500;
            text-transform: none;
            padding: 8px 16px;
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
            padding: 4px 8px;
          }
          
          .fc-daygrid-day-number {
            font-weight: 500;
          }
          
          .fc-daygrid-day.fc-day-today {
            background-color: rgba(64, 158, 255, 0.1);
          }
          
          .fc-event {
            cursor: pointer;
            border-radius: 4px;
            padding: 2px 4px;
            margin-bottom: 2px;
            font-size: 13px;
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
          }
          
          .fc-col-header-cell {
            background-color: #f5f7fa;
            
            .fc-col-header-cell-cushion {
              padding: 10px;
              font-weight: 600;
              color: #606266;
            }
          }
          
          .fc-day-other .fc-daygrid-day-top {
            opacity: 0.6;
          }
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