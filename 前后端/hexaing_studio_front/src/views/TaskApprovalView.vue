<template>
  <div class="task-approval-view">
    <!-- 页面标题 -->
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <h2>
          <el-icon><DocumentChecked /></el-icon>
          任务审批
        </h2>
        <div class="header-actions">
          <el-button @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索和筛选 -->
    <el-card shadow="hover" class="search-card">
      <div class="search-container">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索任务标题或描述"
          class="search-input"
          @change="handleSearch"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleSearch"
          clearable
        />
        
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </el-card>

    <!-- 待审批任务列表 -->
    <el-card shadow="hover" class="task-list-card">
      <template #header>
        <div class="card-header">
          <span>待审批任务列表</span>
          <el-tag type="warning">{{ taskList.length }} 个待审批</el-tag>
        </div>
      </template>

      <el-table 
        v-loading="loading"
        :data="taskList" 
        style="width: 100%"
        empty-text="暂无待审批任务"
      >
        
        <el-table-column prop="title" label="任务标题" min-width="200">
          <template #default="scope">
            <div class="task-title">
              <el-link @click="viewTaskDetail(scope.row)" type="primary">
                {{ scope.row.title }}
              </el-link>
              <el-tag v-if="scope.row.urgent" type="warning" size="small" class="urgent-tag">
                紧急
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="任务描述" min-width="250" show-overflow-tooltip />
        
        <el-table-column prop="creatUserName" label="创建人" width="100" />
        
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="endTime" label="截止时间" width="180">
          <template #default="scope">
            <span :class="{ 'text-danger': isNearDeadline(scope.row.endTime) }">
              {{ formatDateTime(scope.row.endTime) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column label="进度" width="120">
          <template #default="scope">
            <div class="progress-info">
              <el-progress 
                :percentage="calculateProgress(scope.row)" 
                :status="getProgressStatus(scope.row)"
                :stroke-width="8"
              />
              <small>{{ scope.row.completedSubTasks || 0 }}/{{ scope.row.totalSubTasks || 0 }}</small>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="small"
                @click="viewTaskDetail(scope.row)"
              >
                <el-icon><View /></el-icon>
                查看详情
              </el-button>
              <el-button 
                type="info" 
                size="small"
                @click="reviewTaskSubmissions(scope.row)"
              >
                <el-icon><EditPen /></el-icon>
                审核提交
              </el-button>

            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>



    <!-- 任务详情对话框 -->
    <task-detail-dialog
      v-model:visible="taskDetailVisible"
      :task-id="currentTaskId"
    />

    <!-- 任务提交审核对话框 -->
    <task-submission-review-dialog
      v-model:visible="submissionReviewVisible"
      :task-id="currentTaskId"
      @refresh="refreshData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  DocumentChecked, 
  Refresh, 
  Search,
  View,
  EditPen
} from '@element-plus/icons-vue';
import TaskDetailDialog from '@/components/modules/task/TaskDetailDialog.vue';
import TaskSubmissionReviewDialog from '@/components/modules/task/TaskSubmissionReviewDialog.vue';
import { 
  getPendingApprovalTasks
} from '@/utils/api/task';

// 响应式数据
const loading = ref(false);
const submitting = ref(false);
const taskList = ref<any[]>([]);

const currentTask = ref<any>(null);
const currentTaskId = ref('');

// 分页
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 搜索
const searchKeyword = ref('');
const dateRange = ref<[Date, Date] | null>(null);

// 对话框状态
const taskDetailVisible = ref(false);
const submissionReviewVisible = ref(false);

// 计算属性
const isNearDeadline = (endTime: string) => {
  if (!endTime) return false;
  const deadline = new Date(endTime);
  const now = new Date();
  const diff = deadline.getTime() - now.getTime();
  return diff < 24 * 60 * 60 * 1000; // 24小时内
};

const calculateProgress = (task: any) => {
  if (!task || !task.totalSubTasks) return 0;
  return Math.round((task.completedSubTasks / task.totalSubTasks) * 100);
};

const getProgressStatus = (task: any) => {
  if (!task) return '';
  const progress = calculateProgress(task);
  if (progress === 100) return 'success';
  if (progress > 60) return '';
  return 'exception';
};

// 格式化时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
};

// 获取待审批任务列表
const fetchPendingTasks = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      title: searchKeyword.value,
      startTime: dateRange.value?.[0]?.toISOString(),
      endTime: dateRange.value?.[1]?.toISOString()
    };
    
    const response = await getPendingApprovalTasks(params) as any;
    if (response.code === 200) {
      // 后端返回的数据结构是 PageResult，直接使用 data 字段
      const pageResult = response.data;
      taskList.value = pageResult.records || [];
      total.value = pageResult.total || 0;
    } else {
      ElMessage.error(response.message || '获取待审批任务失败');
    }
  } catch (error) {
    console.error('获取待审批任务失败:', error);
    ElMessage.error('获取待审批任务失败');
  } finally {
    loading.value = false;
  }
};

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1;
  fetchPendingTasks();
};

// 重置搜索
const resetSearch = () => {
  searchKeyword.value = '';
  dateRange.value = null;
  handleSearch();
};

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  fetchPendingTasks();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  fetchPendingTasks();
};





// 查看任务详情
const viewTaskDetail = (task: any) => {
  currentTaskId.value = String(task.taskId);
  taskDetailVisible.value = true;
};

// 审核任务提交
const reviewTaskSubmissions = (task: any) => {
  currentTaskId.value = String(task.taskId);
  submissionReviewVisible.value = true;
};

// 刷新数据
const refreshData = () => {
  fetchPendingTasks();
};

// 组件挂载
onMounted(() => {
  fetchPendingTasks();
});
</script>

<style scoped lang="scss">
.task-approval-view {
  padding: 20px;
  background-color: #f5f7fa;
}

.page-header-card {
  margin-bottom: 20px;
  
  .page-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h2 {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 8px;
      color: #303133;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.search-card {
  margin-bottom: 20px;
  
  .search-container {
    display: flex;
    gap: 15px;
    align-items: center;
    flex-wrap: wrap;
    
    .search-input {
      width: 300px;
    }
  }
}

.task-list-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .urgent-tag {
    animation: pulse 2s infinite;
  }
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.6; }
  100% { opacity: 1; }
}

.progress-info {
  text-align: center;
  
  small {
    color: #666;
    margin-top: 4px;
    display: block;
  }
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.approval-form {
  .task-info {
    margin-bottom: 20px;
    padding: 15px;
    background-color: #f8f9fa;
    border-radius: 8px;
    
    h4 {
      margin: 0 0 8px 0;
      color: #303133;
    }
    
    .task-desc {
      margin: 0;
      color: #606266;
      font-size: 14px;
    }
  }
}

.batch-approval-form {
  .selected-tasks {
    margin-bottom: 20px;
    
    h4 {
      margin-bottom: 10px;
      color: #303133;
    }
    
    .task-list {
      max-height: 200px;
      overflow-y: auto;
      background-color: #f8f9fa;
      border-radius: 8px;
      padding: 15px;
      margin: 0;
      
      .task-item {
        padding: 5px 0;
        border-bottom: 1px solid #ebeef5;
        color: #606266;
        
        &:last-child {
          border-bottom: none;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 