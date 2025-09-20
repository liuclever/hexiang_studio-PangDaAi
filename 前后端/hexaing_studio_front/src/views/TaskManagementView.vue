<template>
  <div class="task-management-view">
    <!-- 页面标题 -->
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>任务管理</h2>
        <div class="view-controls">
          <el-radio-group v-model="currentView" size="large" class="view-switch">
            <el-radio-button label="list">列表视图</el-radio-button>
            <el-radio-button label="calendar">日历视图</el-radio-button>
          </el-radio-group>
          <el-button type="warning" class="approval-btn" @click="goToApproval">
            <el-icon><DocumentChecked /></el-icon>任务审批
          </el-button>
          <el-button type="primary" class="create-task-btn" @click="showAddTaskDialog">
            <el-icon><Plus /></el-icon>创建任务
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索和筛选 - 仅在列表视图显示 -->
    <el-card v-if="currentView === 'list'" shadow="hover" class="search-card">
      <div class="search-container">
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索任务名称或描述" 
          class="search-input"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>

        <div class="filter-container">
          <el-select v-model="filterStatus" placeholder="任务状态" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="紧急" value="URGENT" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="被退回" value="REJECTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
          
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="截止日期"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </div>
      </div>
    </el-card>

    <!-- 统计概览 - 在两种视图都显示 -->
    <el-row :gutter="16" class="statistics-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="statistic-card">
          <div class="statistic-item">
            <div class="statistic-icon total">
              <el-icon><Document /></el-icon>
            </div>
            <div class="statistic-info">
              <h3>总任务数</h3>
              <p class="statistic-number">{{ statisticsData.totalTasks }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="statistic-card">
          <div class="statistic-item">
            <div class="statistic-icon in-progress">
              <el-icon><Loading /></el-icon>
            </div>
            <div class="statistic-info">
              <h3>进行中</h3>
              <p class="statistic-number">{{ statisticsData.inProgressTasks }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="statistic-card">
          <div class="statistic-item">
            <div class="statistic-icon completed">
              <el-icon><Select /></el-icon>
            </div>
            <div class="statistic-info">
              <h3>已完成</h3>
              <p class="statistic-number">{{ statisticsData.completedTasks }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="statistic-card">
          <div class="statistic-item">
            <div class="statistic-icon overdue">
              <el-icon><AlarmClock /></el-icon>
            </div>
            <div class="statistic-info">
              <h3>已逾期</h3>
              <p class="statistic-number">{{ statisticsData.overdueTasks }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务完成情况图表 - 仅在列表视图显示 -->
    <el-card v-if="currentView === 'list'" shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <h3>任务状态分布</h3>
        </div>
      </template>
      <div class="chart-container">
        <div ref="taskCompletionChartRef" class="chart"></div>
      </div>
    </el-card>

    <!-- 任务列表 - 仅在列表视图显示 -->
    <div v-if="currentView === 'list'" class="tasks-container">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <el-empty v-else-if="tasks.length === 0" description="暂无任务" />
      
      <el-row :gutter="20" v-else>
        <el-col 
          v-for="task in tasks" 
          :key="task.taskId" 
          :xs="24" 
          :sm="24" 
          :md="12" 
          :lg="8" 
          :xl="6"
          class="task-card-col"
        >
          <el-card 
            shadow="hover" 
            class="task-card"
            :class="{ 
              'task-overdue': task.status === 'OVERDUE', 
              'task-completed': task.status === 'COMPLETED' 
            }"
          >
            <template #header>
              <div class="task-card-header">
                <el-tag 
                  :type="getTaskStatusType(task.status)"
                  effect="dark"
                  size="small"
                >
                  {{ getTaskStatusText(task.status) }}
                </el-tag>
                <h3 class="task-title">{{ task.title }}</h3>
              </div>
            </template>
            
            <div class="task-card-body">
              <p class="task-description">{{ task.description || '暂无描述' }}</p>
              
              <div class="task-progress">
                <div class="progress-label">
                  <span>进度</span>
                  <span>{{ task.completedSubTasks }}/{{ task.totalSubTasks }}</span>
                </div>
                <el-progress 
                  :percentage="calculateProgress(task)"
                  :status="getProgressStatus(task)"
                />
              </div>
              
              <div class="task-dates">
                <div class="date-item">
                  <el-icon><Calendar /></el-icon>
                  <span>开始: {{ (task.startTime || '').substring(0, 16) }}</span>
                </div>
                <div class="date-item">
                  <el-icon><Timer /></el-icon>
                  <span>截止: {{ (task.endTime || '').substring(0, 16) }}</span>
                </div>
              </div>
              
              <div class="task-subtasks">
                <el-icon><List /></el-icon>
                <span>子任务: {{ task.totalSubTasks }}个</span>
              </div>
              
              <div class="task-actions">
                <!-- 未开始任务的"开始任务"按钮 -->
                          <el-button
            v-if="task.status === 'PENDING_REVIEW'"
            type="warning"
            size="small"
            @click="updateTaskStatus(task.taskId, 'COMPLETED')"
          >
            审批通过
          </el-button>
                
                <!-- 逾期任务的"延期处理"按钮 -->
                <el-button
                  v-if="task.status === 'OVERDUE'"
                  type="warning"
                  size="small"
                  @click="showExtendDeadlineDialog(task)"
                >
                  延期处理
                </el-button>
                
                <!-- 进行中任务的"标记完成"按钮 -->
                <el-button
                  v-if="task.status === 'IN_PROGRESS'"
                  type="success"
                  size="small"
                  @click="updateTaskStatus(task.taskId, 'COMPLETED')"
                >
                  标记完成
                </el-button>
                
                <el-button size="small" @click="viewTaskDetail(task)" type="primary">
                  查看详情
                </el-button>
                <el-button size="small" @click="editTask(task)">
                  编辑
                </el-button>
                <el-button size="small" type="danger" @click="deleteTask(task)">
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :total="totalTasks"
          :page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :current-page="currentPage"
          :disabled="loading"
          :prev-disabled="!hasPreviousPage"
          :next-disabled="!hasNextPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 任务日历视图 - 仅在日历视图显示 -->
    <el-card v-if="currentView === 'calendar'" shadow="hover" class="calendar-card">
      <div class="calendar-container">
        <FullCalendar
          ref="fullCalendarRef"
          :options="calendarOptions"
          class="task-calendar"
        />
      </div>
    </el-card>


    <!-- 任务表单对话框 -->
    <task-form-dialog
      v-model:visible="taskFormVisible"
      :task="currentTask"
      :is-edit="isEdit"
      @refresh="fetchTasks"
    />
    
    <!-- 任务详情对话框 -->
    <task-detail-dialog
      v-model:visible="taskDetailVisible"
      :task-id="currentTaskId"
      @refresh="fetchTasks"
    />
    
    <!-- 延期处理对话框 -->
    <el-dialog
      v-model="extendDeadlineDialogVisible"
      title="延期处理"
      width="50%"
      destroy-on-close
      class="extend-deadline-dialog"
    >
      <el-form label-width="100px">
        <el-form-item label="当前截止日期">
          <span>{{ currentTaskForDeadline ? (currentTaskForDeadline.endTime || '').substring(0, 16) : '' }}</span>
        </el-form-item>
        <el-form-item label="新截止日期" required>
          <el-date-picker
            v-model="extendDeadlineForm.newEndTime"
            type="datetime"
            placeholder="选择新的截止日期"
            style="width: 100%"
            :disabled-date="(time: Date) => time.getTime() < Date.now() - 8.64e7"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="延期原因" required>
          <el-input
            v-model="extendDeadlineForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入延期原因"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="extendDeadlineDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmExtendDeadline">确认延期</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router'; // 确保 useRouter 被引入
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import * as echarts from 'echarts';
import {
  Plus,
  Search,
  Document,
  Loading,
  Select,
  AlarmClock,
  Calendar,
  Timer,
  List,
  DocumentChecked,
} from '@element-plus/icons-vue';
import { getTasks, getTaskStatistics, deleteTask as deleteTaskApi, updateTaskStatus as updateTaskStatusApi, getTaskById } from '@/utils/api/task';
import TaskFormDialog from '@/components/modules/task/TaskFormDialog.vue';
import TaskDetailDialog from '@/components/modules/task/TaskDetailDialog.vue';

// 引入FullCalendar和插件
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { formatDate } from '@/utils/date';

// 视图切换
const route = useRoute();
const router = useRouter(); // 实例化 router
const currentView = ref(route.query.view === 'calendar' ? 'calendar' : 'list');
const fullCalendarRef = ref<any>(null);
const calendarEvents = ref<any[]>([]);

// 搜索和筛选
const searchQuery = ref('');
const filterStatus = ref('');
const dateRange = ref([]);

// 分页
const currentPage = ref(1);
const pageSize = ref(10);
const totalTasks = ref(0);
const totalPages = ref(1);
const hasNextPage = ref(false);
const hasPreviousPage = ref(false);

// 任务列表
interface Task {
  taskId: string | number;
  title: string;
  description: string;
  startTime: string;   // API现在使用startTime
  endTime: string;     // API现在使用endTime
  status: string;
  completedSubTasks?: number;
  totalSubTasks?: number;
  subTasks?: any[];    // 子任务列表
}

const tasks = ref<Task[]>([]);
const loading = ref(false);

// 统计数据
interface StatisticsData {
  totalTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  overdueTasks: number;
  notStartedTasks: number; // 增加未开始任务统计
  completionRate?: number;
  last7Days?: {
    notStarted: number;
    inProgress: number;
    completed: number;
    overdue: number;
  };
  last30Days?: {
    notStarted: number;
    inProgress: number;
    completed: number;
    overdue: number;
  };
}

const statisticsData = reactive<StatisticsData>({
  totalTasks: 0,
  inProgressTasks: 0,
  completedTasks: 0,
  overdueTasks: 0,
  notStartedTasks: 0,
});

// 对话框控制
const taskFormVisible = ref(false);
const taskDetailVisible = ref(false);
const currentTask = ref<Record<string, any> | undefined>(undefined);
const currentTaskId = ref('');
const isEdit = ref(false);

// 图表相关
const taskCompletionChartRef = ref(null);
let taskCompletionChart: echarts.ECharts | null = null;

// API响应类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface TaskListResponse {
  records: Task[];
  total: number;
  page: number;        // 后端可能使用page而不是current
  pageSize: number;    // 后端可能使用pageSize而不是size
  current?: number;    // 兼容current字段
  size?: number;       // 兼容size字段
  pages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// 获取任务列表
const fetchTasks = async (options: { forCalendar?: boolean, dateInfo?: any } = {}) => {
  loading.value = true;
  try {
    const params: any = {};
    
    if (options.forCalendar) {
        params.page = 1;
        params.size = 999; // 获取大量任务以填充日历
        if (options.dateInfo) {
            params.startTime = formatDate(options.dateInfo.start, 'YYYY-MM-DD');
            params.endTime = formatDate(options.dateInfo.end, 'YYYY-MM-DD');
        } else if (fullCalendarRef.value) {
            const calendarApi = fullCalendarRef.value.getApi();
            if (calendarApi.view) {
                const view = calendarApi.view;
                params.startTime = formatDate(view.activeStart, 'YYYY-MM-DD');
                params.endTime = formatDate(view.activeEnd, 'YYYY-MM-DD');
            }
        }
    } else {
        // 列表视图的逻辑 (分页, 搜索, etc.)
        params.page = currentPage.value;
        params.size = pageSize.value;
        params.keyword = searchQuery.value;
        params.status = filterStatus.value;
        params.startTime = dateRange.value && dateRange.value[0];
        params.endTime = dateRange.value && dateRange.value[1];
    }

    const res = await getTasks(params) as any;
    
    if (res.code === 200) {
      // 总是更新 tasks.value. 日历的点击和样式处理需要它
      tasks.value = res.data.records.map((task: any) => ({
      ...task,
      startTime: task.startTime,
      endTime: task.endTime,
      completedSubTasks: task.completedSubTasks || 0,
      totalSubTasks: task.totalSubTasks || (task.subTasks?.length || 0)
    }));
    
      if (options.forCalendar) {
        transformTasksToEvents(); // 这会使用更新后的 tasks.value
      } else {
        // 更新列表视图的分页状态
    totalTasks.value = res.data.total;
    
        if (res.data.page) currentPage.value = res.data.page;
        else if (res.data.current) currentPage.value = res.data.current;
    
        if (res.data.pageSize) pageSize.value = res.data.pageSize;
        else if (res.data.size) pageSize.value = res.data.size;
    
        if (res.data.pages) totalPages.value = res.data.pages;
        
    hasNextPage.value = res.data.hasNext !== undefined ? res.data.hasNext : currentPage.value < totalPages.value;
    hasPreviousPage.value = res.data.hasPrevious !== undefined ? res.data.hasPrevious : currentPage.value > 1;
      }
    } else {
        ElMessage.error(res.message || '获取任务列表失败');
    }
  } catch (error) {
    ElMessage.error('获取任务列表失败');
  } finally {
    loading.value = false;
  }
};

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getTaskStatistics() as ApiResponse<StatisticsData>;
    Object.assign(statisticsData, res.data);
    
    nextTick(() => {
      initCompletionChart();
    });
  } catch (error) {
    ElMessage.error('获取统计数据失败');
  }
};

  // 初始化完成情况图表
const initCompletionChart = () => {
  if (!taskCompletionChartRef.value) return;
  
  // 销毁旧图表
  if (taskCompletionChart) {
    taskCompletionChart.dispose();
  }
  
  // 创建新图表
  taskCompletionChart = echarts.init(taskCompletionChartRef.value);
  
  const stats = statisticsData;
  
  // 准备数据
  const statusData = [
    { name: '未开始', value: stats.notStartedTasks || 0, color: '#909399' },
    { name: '进行中', value: stats.inProgressTasks || 0, color: '#409EFF' },
    { name: '已完成', value: stats.completedTasks || 0, color: '#67C23A' },
    { name: '已逾期', value: stats.overdueTasks || 0, color: '#F56C6C' }
  ];
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      padding: [10, 15],
      extraCssText: 'box-shadow: 0 4px 15px rgba(0,0,0,0.1); border-radius: 10px;'
    },
    legend: {
      orient: 'horizontal',
      top: 'bottom',
      data: statusData.map(item => item.name),
      icon: 'roundRect',
      textStyle: {
        fontSize: 12,
        color: '#666'
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 20,
      selectedMode: true
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'outside',
          formatter: '{b}: {c}',
          fontSize: 14,
          fontWeight: 'bold'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        labelLine: {
          show: true,
          length: 15,
          length2: 10,
          smooth: true
        },
        data: statusData.map(item => ({
          name: item.name,
          value: item.value,
          itemStyle: {
            color: item.color
          }
        }))
      }
    ],
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicOut' as const,
    animationDelay: function (idx: number) {
      return idx * 200;
    }
  };
  
  taskCompletionChart.setOption(option);
};
  
// 搜索处理
const handleSearch = () => {
  currentPage.value = 1;
  fetchTasks();
};

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1; // 切换每页大小时重置为第一页
  fetchTasks();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  fetchTasks();
};

// 状态相关方法
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

// 计算任务进度
const calculateProgress = (task: any) => {
  if (!task.totalSubTasks) return 0;
  return Math.round((task.completedSubTasks / task.totalSubTasks) * 100);
};

// 获取进度条状态
const getProgressStatus = (task: any) => {
  if (task.status === 'COMPLETED') return 'success';
  if (task.status === 'OVERDUE') return 'exception';
  
  const progress = calculateProgress(task);
  if (progress >= 80) return 'success';
  if (progress >= 40) return '';
  return 'exception';
};

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return formatDate(new Date(dateStr), 'YYYY-MM-DD HH:mm');
};

// 日历配置
const calendarOptions = reactive({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  locale: 'zh-cn', // 使用中文语言
  eventClick: handleCalendarEventClick,
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
  eventDisplay: 'block' as const, // 使用块状显示堆叠事件
  dayMaxEvents: true, // 当事件太多时允许"更多"链接
  height: 'auto',
  aspectRatio: 1.8,
  firstDay: 1, // 周一开始
  dayMaxEventRows: 4, // 每日最大显示事件数，超过显示"更多"
  eventClassNames: handleEventClassNames,
  noEventsContent: '暂无任务',
  datesSet: (dateInfo: any) => {
    fetchTasks({ forCalendar: true, dateInfo: dateInfo });
  }
});

// 处理日历事件点击
function handleCalendarEventClick(info: any) {
  const taskId = info.event.id;
  if (!taskId) return;
  
  // 查找任务数据
  const task = tasks.value.find(t => String(t.taskId) === String(taskId));
  if (task) {
    currentTaskId.value = String(task.taskId);
    taskDetailVisible.value = true;
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
      start: task.endTime ? task.endTime.substring(0, 10) : null, // 使用截止日期作为事件日期
      backgroundColor,
      borderColor,
      textColor,
      extendedProps: {
        description: task.description,
        status: task.status
      },
      allDay: true // 设置为全天事件以便更好显示
    };
  });
  
  // 更新日历事件
  if (fullCalendarRef.value) {
    const calendarApi = fullCalendarRef.value.getApi();
    calendarApi.removeAllEvents();
    calendarApi.addEventSource(calendarEvents.value);
  }
};

// 显示新增任务对话框
const showAddTaskDialog = () => {
  currentTask.value = undefined;
  isEdit.value = false;
  taskFormVisible.value = true;
};

// 显示编辑任务对话框
const editTask = async (task: any) => {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在加载任务数据...',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  
  try {
    const res = await getTaskById(task.taskId) as ApiResponse<any>;
    if (res.code === 200) {
      currentTask.value = res.data;
      isEdit.value = true;
      taskFormVisible.value = true;
    } else {
      ElMessage.error(res.message || '获取任务详情失败');
    }
  } catch (error) {
    ElMessage.error('获取任务详情失败');
  } finally {
    loadingInstance.close();
  }
};

// 显示任务详情
const viewTaskDetail = (task: any) => {
  currentTaskId.value = String(task.taskId);
  taskDetailVisible.value = true;
};

// 删除任务
const deleteTask = (task: Task) => {
  ElMessageBox.confirm('确定要删除该任务吗？此操作将不可恢复！', '提示', {
    confirmButtonText: '确定',
      cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
      try {
      await deleteTaskApi(task.taskId);
      ElMessage.success('删除任务成功');
          fetchTasks();
          fetchStatistics();
      } catch (error) {
        ElMessage.error('删除任务失败');
      }
    });
};

// 更新任务状态
const updateTaskStatus = async (taskId: string | number, status: string) => {
  // 根据不同状态显示不同的确认信息
  let confirmMessage = '确定要更改任务状态吗？';
  let confirmTitle = '提示';
  
  if (status === 'IN_PROGRESS') {
    confirmMessage = '确定要将任务标记为进行中吗？';
    confirmTitle = '开始任务';
  } else if (status === 'COMPLETED') {
    confirmMessage = '确定要将任务标记为已完成吗？';
    confirmTitle = '完成任务';
  }
  
  // 显示确认对话框
  ElMessageBox.confirm(confirmMessage, confirmTitle, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateTaskStatusApi(taskId, status);
      ElMessage.success('任务状态更新成功');
  fetchTasks();
  fetchStatistics();
    } catch (error) {
      ElMessage.error('任务状态更新失败');
    }
  }).catch(() => {
    // 用户取消操作，不做任何处理
  });
};

// 延期对话框相关
const extendDeadlineDialogVisible = ref(false);
const currentTaskForDeadline = ref<Task | null>(null);
const extendDeadlineForm = ref({
  newEndTime: '',
  reason: ''
});

// 显示延期处理对话框
const showExtendDeadlineDialog = (task: Task) => {
  // 先显示确认对话框
  ElMessageBox.confirm('确定要对此逾期任务进行延期处理吗？', '延期处理', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 用户确认后显示延期处理对话框
    currentTaskForDeadline.value = task;
    extendDeadlineForm.value = {
      newEndTime: '',
      reason: ''
    };
    extendDeadlineDialogVisible.value = true;
  }).catch(() => {
    // 用户取消操作，不做任何处理
  });
};

// 确认延期
const confirmExtendDeadline = async () => {
  if (!currentTaskForDeadline.value) return;
  
  if (!extendDeadlineForm.value.newEndTime || !extendDeadlineForm.value.reason) {
    ElMessage.warning('请填写完整的延期信息');
    return;
  }
  
  try {
    // 更新任务状态为进行中，并更新截止时间
    await updateTaskStatusApi(
      currentTaskForDeadline.value.taskId, 
      'IN_PROGRESS', 
      extendDeadlineForm.value.newEndTime
    );
    
    ElMessage.success('任务延期处理成功');
    extendDeadlineDialogVisible.value = false;
    
    // 重新加载任务列表和统计数据
    fetchTasks();
    fetchStatistics();
  } catch (error) {
    ElMessage.error('任务延期处理失败');
  }
};

// 跳转到任务审批页面
const goToApproval = () => {
  router.push('/task/approval');
};

// 监听窗口大小变化以调整图表
const handleResize = () => {
  if (taskCompletionChart) {
    taskCompletionChart.resize();
  }
};

// 生命周期钩子
onMounted(() => {
  fetchTasks();
  fetchStatistics();
  window.addEventListener('resize', handleResize);
  
  // 添加自定义事件监听器
  window.addEventListener('open-task-detail', ((event: CustomEvent) => {
    if (event.detail && event.detail.taskId) {
      currentTaskId.value = String(event.detail.taskId);
      taskDetailVisible.value = true;
    }
  }) as EventListener);

  // 检查路由中是否有 'action=add' 参数
  if (route.query.action === 'add') {
    showAddTaskDialog();
    // 可选: 清理URL中的查询参数，避免刷新页面时再次触发
    router.replace({ query: {} });
  }
});

// 在组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('open-task-detail', ((event: CustomEvent) => {
    // 清理函数
  }) as EventListener);
});

// 监控视图变化
watch(currentView, (newView) => {
    if (newView === 'calendar') {
        nextTick(() => {
            fetchTasks({ forCalendar: true });
        });
    }
});

// 监控路由参数变化
watch(() => route.query.view, (newView) => {
    if (newView === 'calendar') {
        currentView.value = 'calendar';
    }
});
</script>

<style lang="scss" scoped>
.task-management-view {
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
      
      .view-controls {
        display: flex;
        gap: 15px;
        align-items: center;
        
        .view-switch {
          :deep(.el-radio-button__inner) {
            padding: 8px 15px;
            font-size: 14px;
            font-weight: 500;
        }
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
  }
  
  .search-card {
    border: none;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    
    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .search-container {
      display: flex;
      flex-wrap: nowrap;
      gap: 15px;
      align-items: center;
      justify-content: space-between;
      
      .search-input {
        width: 400px;
        flex-shrink: 0;
        
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        }
        
        :deep(.el-input__inner) {
          height: 40px;
        }
        
        :deep(.el-input-group__append) {
          border-top-right-radius: 8px;
          border-bottom-right-radius: 8px;
          background-color: #409eff;
          border-color: #409eff;
          color: white;
        }
      }
      
      .filter-container {
        display: flex;
        flex-wrap: nowrap;
        gap: 15px;
        flex-grow: 1;
        justify-content: flex-end;
        
        :deep(.el-select) {
          width: 140px;
          flex-shrink: 0;
        }
        
        :deep(.el-date-editor) {
          width: 220px;
          flex-shrink: 0;
        }
        
        :deep(.el-select .el-input__wrapper),
        :deep(.el-date-editor.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        }
      }
    }
  }
  
  .statistics-row {
    margin-bottom: 0;
    
    .statistic-card {
      height: 100%;
      border: none;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
      }
      
      .statistic-item {
        display: flex;
        align-items: center;
        padding: 5px;
        
        .statistic-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 64px;
          height: 64px;
          border-radius: 12px;
          margin-right: 15px;
          color: #fff;
          font-size: 26px;
          box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
          
          &.total {
            background: linear-gradient(135deg, #409eff, #64b5f6);
          }
          
          &.in-progress {
            background: linear-gradient(135deg, #e6a23c, #ffb74d);
          }
          
          &.completed {
            background: linear-gradient(135deg, #67c23a, #8bc34a);
          }
          
          &.overdue {
            background: linear-gradient(135deg, #f56c6c, #ff8a80);
          }
        }
        
        .statistic-info {
          h3 {
            margin: 0 0 8px 0;
            font-size: 15px;
            font-weight: normal;
            color: #909399;
          }
          
          .statistic-number {
            font-size: 28px;
            font-weight: 700;
            margin: 0;
            color: #303133;
          }
        }
      }
    }
  }
  
  .chart-card {
    background: linear-gradient(to bottom, #ffffff, #f8fafc);
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    border-radius: 12px;
    overflow: hidden;
    
    :deep(.el-card__header) {
      padding: 15px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }
    
    :deep(.el-card__body) {
      padding: 20px;
    }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
    
    .chart-container {
      height: 300px;
      
      .chart {
        width: 100%;
        height: 100%;
      }
    }
  }
  
  .tasks-container {
    .loading-container {
      padding: 20px;
    }

    .task-card-col {
      margin-bottom: 20px;
    }
    
    .task-card {
      height: 100%;
      transition: all 0.3s;
      border: none;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 12px 20px rgba(0, 0, 0, 0.1);
      }
      
      &.task-overdue {
        border-top: 3px solid #f56c6c;
      }
      
      &.task-completed {
        border-top: 3px solid #67c23a;
      }
      
      :deep(.el-card__header) {
        padding: 15px;
        background-color: #fafafa;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      }
      
      :deep(.el-card__body) {
        padding: 15px;
      }
      
      .task-card-header {
        .task-title {
          margin: 8px 0 0 0;
          font-size: 16px;
          font-weight: 600;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          color: #303133;
        }
      }
      
      .task-card-body {
        .task-description {
          color: #606266;
          font-size: 14px;
          margin-bottom: 15px;
          height: 40px;
          overflow: hidden;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
        
        .task-progress {
          margin-bottom: 15px;
          
          .progress-label {
            display: flex;
            justify-content: space-between;
            margin-bottom: 5px;
            font-size: 14px;
            color: #606266;
          }
          
          :deep(.el-progress-bar__outer) {
            border-radius: 10px;
            background-color: #f0f2f5;
          }
          
          :deep(.el-progress-bar__inner) {
            border-radius: 10px;
          }
        }
        
        .task-dates {
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-bottom: 15px;
          padding: 10px;
          background-color: #f8fafc;
          border-radius: 8px;
          
          .date-item {
            display: flex;
            align-items: center;
            font-size: 13px;
            color: #606266;
            
            .el-icon {
              margin-right: 5px;
              color: #909399;
            }
          }
        }
        
        .task-subtasks {
          display: flex;
          align-items: center;
          font-size: 13px;
          color: #606266;
          margin-bottom: 15px;
          padding: 10px;
          background-color: #f8fafc;
          border-radius: 8px;
          
          .el-icon {
            margin-right: 5px;
            color: #909399;
          }
        }
        
        .task-actions {
          display: flex;
          gap: 10px;
          justify-content: flex-end;
          
          .el-button {
            border-radius: 20px;
            padding: 8px 15px;
            font-size: 13px;
            
            &.el-button--primary {
              background: linear-gradient(90deg, #409eff, #64b5f6);
              border: none;
              box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
            }
            
            &.el-button--danger {
              background: linear-gradient(90deg, #f56c6c, #ff8a80);
              border: none;
              box-shadow: 0 2px 6px rgba(245, 108, 108, 0.3);
            }
            
            &:not(.el-button--primary):not(.el-button--danger) {
              border: 1px solid #dcdfe6;
              background: white;
              color: #606266;
            }
          }
        }
      }
    }
    
    .pagination-container {
      display: flex;
      justify-content: center;
      margin-top: 20px;
    }
  }

  .calendar-card {
    background: linear-gradient(to bottom, #ffffff, #f8fafc);
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    border-radius: 12px;
    overflow: hidden;
    
    :deep(.el-card__header) {
      padding: 15px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }
    
    :deep(.el-card__body) {
      padding: 20px;
    }

    .calendar-container {
      min-height: calc(100vh - 380px);
      
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

@media (max-width: 768px) {
  .task-management-view {
    .page-header-card {
      .page-title {
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
      }
      .view-controls {
        width: 100%;
        justify-content: space-between;
      }
    }
    .search-card {
    .search-container {
      flex-direction: column;
        align-items: stretch;
      
      .search-input {
        width: 100%;
      }
      
      .filter-container {
        width: 100%;
          justify-content: space-between;
          
          :deep(.el-select) {
            width: 35%;
          }
          
          :deep(.el-date-editor) {
            width: 60%;
          }
        }
      }
    }
    
    .chart-card {
      .card-header {
          flex-direction: column;
        align-items: flex-start;
      }
    }
  }
}
</style> 