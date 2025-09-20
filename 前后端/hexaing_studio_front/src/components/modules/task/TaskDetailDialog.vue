<template>
  <el-dialog
    title="任务详情"
    v-model="dialogVisible"
    width="75%"
    destroy-on-close
    :close-on-click-modal="false"
    class="task-detail-dialog"
  >
    <div v-loading="loading">
      <!-- 任务基本信息 -->
      <el-card class="detail-card">
        <template #header>
          <div class="card-header">
            <h3>基本信息</h3>
            <el-tag :type="getStatusType(task.status)" effect="dark" class="status-tag">
              {{ getStatusText(task.status) }}
            </el-tag>
          </div>
        </template>

        <div class="task-info">
          <div class="info-item">
            <span class="label">任务名称：</span>
            <span class="value">{{ task.title }}</span>
          </div>

          <div class="info-item">
            <span class="label">任务描述：</span>
            <span class="value description">{{ task.description || '暂无描述' }}</span>
          </div>

          <div class="info-row">
            <div class="info-item">
              <span class="label">开始时间：</span>
              <span class="value">{{ formatDateTime(task.startTime) }}</span>
            </div>

            <div class="info-item">
              <span class="label">截止时间：</span>
              <span class="value">{{ formatDateTime(task.endTime) }}</span>
            </div>
          </div>

          <div class="info-row">
            <div class="info-item">
              <span class="label">创建人：</span>
              <span class="value">{{ task.creatUserName || '未知' }}</span>
            </div>

            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDateTime(task.createTime) }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 任务附件 -->
      <el-card class="detail-section-card" v-if="task.attachments && task.attachments.length > 0">
        <template #header>
          <div class="card-header">
            <h3>任务附件</h3>
          </div>
        </template>
        <div class="attachments-list">
          <el-table :data="task.attachments" style="width: 100%" border>
            <el-table-column label="文件名" prop="fileName" min-width="200"></el-table-column>
            <el-table-column label="大小" width="120">
              <template #default="scope">
                {{ formatFileSize(scope.row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.uploadTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  @click="downloadFile(scope.row)"
                  class="rounded-btn"
                >
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>

      <!-- 任务进度 -->
      <el-card class="detail-card progress-card">
        <template #header>
          <div class="card-header">
            <h3>任务进度</h3>
            <div class="progress-summary">
              <el-tag type="info" size="large">
                {{ completedCount }}/{{ totalCount }} 子任务完成
              </el-tag>
            </div>
          </div>
        </template>

        <div class="progress-overview">
          <div class="subtask-progress">
            <div
              v-for="(subTask, index) in task.subTasks"
              :key="index"
              class="subtask-progress-item"
            >
              <div class="subtask-title">
                <span>{{ subTask.title }}</span>
                <el-tag 
                  :type="getSubTaskStatusType(subTask.status)" 
                  size="small" 
                  effect="dark" 
                  class="status-tag"
                >
                  {{ getSubTaskStatusText(subTask.status) }}
                </el-tag>
              </div>
              <el-progress
                :percentage="calculateSubTaskProgress(subTask)"
                :status="getSubTaskProgressStatus(subTask)"
                :stroke-width="8"
              />
            </div>
          </div>
        </div>
      </el-card>

      <!-- 子任务详情 -->
      <el-card class="detail-card main-content-card">
        <template #header>
          <div class="card-header">
            <h3>子任务详情</h3>
          </div>
        </template>

        <div v-if="task.subTasks && task.subTasks.length > 0" class="subtask-details">
          <el-row :gutter="20" class="subtask-cards">
            <el-col 
              v-for="(subTask, index) in task.subTasks" 
              :key="subTask.subTaskId" 
              :xs="24" 
              :sm="24" 
              :md="12" 
              :lg="12" 
              :xl="8"
              class="subtask-col"
            >
              <el-card class="subtask-card" shadow="hover" @click="showSubTaskDetail(subTask)">
                <template #header>
                  <div class="subtask-card-header">
                    <div class="header-left">
                      <h4>{{ index + 1 }}. {{ subTask.title }}</h4>
                    </div>
                                          <div class="header-right">
                      <el-tag 
                        :type="getSubTaskStatusType(subTask.status)" 
                        size="small" 
                        effect="dark" 
                        class="status-tag"
                      >
                        {{ getSubTaskStatusText(subTask.status) }}
                        </el-tag>
                      </div>
                  </div>
                </template>
                
                <div class="subtask-content">
                  <p class="subtask-description">{{ subTask.description || '暂无描述' }}</p>

                  <div class="member-submissions">
                    <h5>相关成员</h5>

                    <el-table
                      :data="subTask.members"
                      style="width: 100%"
                      border
                      class="member-table"
                      size="small"
                    >
                      <el-table-column label="成员" min-width="180">
                        <template #default="scope">
                          <div class="member-info">
                            <el-avatar :size="24" :src="scope.row.avatar ? `/api/admin/file/view/${scope.row.avatar}` : ''" />
                            <span>{{ scope.row.name }}</span>
                          </div>
                        </template>
                      </el-table-column>

                      <el-table-column label="角色" prop="role" width="100" />
 
                      <el-table-column label="备注" width="200">
                        <template #default="scope">
                          <span v-if="scope.row.note" class="member-note">{{ scope.row.note }}</span>
                          <span v-else class="no-note">暂无备注</span>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <el-empty v-else description="暂无子任务" />
      </el-card>
    </div>

    <!-- Sub-task Detail Dialog -->
    <el-dialog
      v-if="selectedSubTask"
      :title="'子任务详情: ' + selectedSubTask.title"
      v-model="subTaskDetailDialogVisible"
      width="60%"
      append-to-body
      destroy-on-close
      class="sub-task-detail-view-dialog"
    >
      <div v-if="selectedSubTask" class="sub-task-detail-content">
        <div class="info-item">
          <span class="label">子任务状态：</span>
          <el-tag 
            :type="getSubTaskStatusType(selectedSubTask.status)" 
            effect="dark" 
            class="status-tag"
          >
            {{ getSubTaskStatusText(selectedSubTask.status) }}
          </el-tag>
        </div>
        
        <div class="info-item">
          <span class="label">子任务描述：</span>
          <span class="value description">{{ selectedSubTask.description || '暂无描述' }}</span>
        </div>
        
        <el-divider />

        <h5>相关成员</h5>
        <el-table
          :data="selectedSubTask.members"
          style="width: 100%"
          border
          class="member-table"
          size="medium"
        >
          <el-table-column label="成员" min-width="180">
            <template #default="scope">
              <div class="member-info">
                <el-avatar :size="32" :src="scope.row.avatar ? `/api/admin/file/view/${scope.row.avatar}` : ''" />
                <span>{{ scope.row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" prop="role" width="120" />
          <el-table-column label="备注" min-width="200">
            <template #default="scope">
              <span v-if="scope.row.note" class="member-note">{{ scope.row.note }}</span>
              <span v-else class="no-note">暂无备注</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="subTaskDetailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { formatDate } from '@/utils/date';
import { getTaskById } from '@/utils/api/task';
import { Download } from '@element-plus/icons-vue';

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

const emit = defineEmits(['update:visible']);

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

// 监听taskId变化
watch(() => props.taskId, (val) => {
  if (dialogVisible.value && val) {
    fetchTaskDetail(val);
  }
});

// 数据和状态
const loading = ref(false);
const task = ref<any>({
  subTasks: [],
});
const completedCount = ref(0);
const totalCount = ref(0);

// State for Sub-task Detail Dialog
const subTaskDetailDialogVisible = ref(false);
const selectedSubTask = ref<any>(null);

const showSubTaskDetail = (subTask: any) => {
  selectedSubTask.value = subTask;
  subTaskDetailDialogVisible.value = true;
};

// 获取任务详情
const fetchTaskDetail = async (taskId: string | number) => {
  loading.value = true;
  try {
    const res: any = await getTaskById(taskId);
    task.value = res.data;
    
    // 计算完成统计数据
    calculateStatistics();
  } catch (error) {
    ElMessage.error('获取任务详情失败');
  } finally {
    loading.value = false;
  }
};

// 计算统计数据
const calculateStatistics = () => {
  let completed = 0;
  let total = task.value.subTasks.length;
  
  task.value.subTasks.forEach((subTask: any) => {
    // 根据子任务状态判断是否完成：1表示已完成
    const isSubTaskCompleted = subTask.status === 1;
    
    if (isSubTaskCompleted) {
      completed++;
    }
    
    // 统计成员数量
    subTask.memberCount = subTask.members.length;
    
    // 为每个成员添加子任务ID，方便显示详情卡片
    subTask.members.forEach((member: any) => {
      member.subTaskId = subTask.subTaskId;
    });
  });
  
  completedCount.value = completed;
  totalCount.value = total;
};

// 计算子任务进度百分比
const calculateSubTaskProgress = (subTask: any) => {
  // 根据子任务状态直接返回进度
  switch (subTask.status) {
    case 1: return 100; // 已完成
    case 2: return 80;  // 待审核 
    case 3: return 50;  // 已退回
    default: return 30; // 进行中
  }
};

// 获取子任务进度状态
const getSubTaskProgressStatus = (subTask: any) => {
  switch (subTask.status) {
    case 1: return 'success';   // 已完成 - 绿色
    case 2: return 'success';   // 待审核 - 绿色
    case 3: return 'exception'; // 已退回 - 红色  
    default: return '';         // 进行中 - 默认蓝色
  }
};

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-';
  return formatDate(new Date(dateStr), 'YYYY-MM-DD HH:mm');
};

// 获取任务状态类型
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

// 获取任务状态文本
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



// 获取子任务状态类型（数字状态转换）
const getSubTaskStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'success',    // 已完成
    2: 'warning',    // 待审核
    3: 'danger',     // 已退回
    0: 'primary',    // 进行中
  };
  return types[status] || 'primary';
};

// 获取子任务状态文本（数字状态转换）
const getSubTaskStatusText = (status: number) => {
  const texts: Record<number, string> = {
    1: '已完成',
    2: '待审核',
    3: '已退回',
    0: '进行中',
  };
  return texts[status] || '进行中';
};

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (size < 1024) {
    return size + ' B';
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB';
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }
};

// 下载文件
const downloadFile = (file: any) => {
  // 使用正确的文件下载接口
  const link = document.createElement('a');
  link.href = `/api/admin/file/view/${file.filePath}?download=true&originalName=${encodeURIComponent(file.fileName)}`;
  link.download = file.fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
</script>

<style lang="scss" scoped>
.task-detail-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
  
  :deep(.el-dialog__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    margin-right: 0;
    background-color: #f8f9fa;
    
    .el-dialog__title {
      font-weight: 600;
      font-size: 18px;
    }
  }
  
  .detail-card {
    margin-bottom: 20px;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
    border: 1px solid #ebeef5;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    // 基本信息卡片样式调整
    &:first-child {
      margin-bottom: 15px;
      
      :deep(.el-card__body) {
        padding: 15px;
      }
    }
    
    // 任务进度卡片样式调整
    &.progress-card {
      margin-bottom: 15px;
      
      :deep(.el-card__body) {
        padding: 15px;
      }
    }
    
    // 主要内容卡片（子任务详情）
    &.main-content-card {
      flex: 1;
      margin-bottom: 0;
      
      :deep(.el-card__body) {
        padding: 25px;
      }
    }
    
    :deep(.el-card__header) {
      padding: 12px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      background-color: #f8f9fa;
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
      
      .status-tag {
        padding: 6px 16px;
        font-size: 13px;
        border-radius: 20px;
      }
      
      .progress-summary {
        .el-tag {
          font-weight: 600;
        }
      }
    }
  }
  
  .rounded-btn {
    border-radius: 20px !important;
  }
  
  .submit-btn {
    background: linear-gradient(90deg, #409eff, #64b5f6);
    border: none;
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
    
    &:hover, &:focus {
      background: linear-gradient(90deg, #66b1ff, #90caf9);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    }
  }
  
  .cancel-btn {
    border: 1px solid #dcdfe6;
    
    &:hover, &:focus {
      background: #f5f7fa;
    }
  }
  
  .action-button {
    border-radius: 20px !important;
    padding: 6px 16px;
    font-size: 12px;
    margin: 0 3px;
    min-width: 70px;
    
    &.el-button--primary {
      background: linear-gradient(90deg, #409eff, #64b5f6);
      border: none;
      box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
      
      &:hover, &:focus {
        background: linear-gradient(90deg, #66b1ff, #90caf9);
        box-shadow: 0 4px 8px rgba(64, 158, 255, 0.4);
      }
    }
    
    &.el-button--success {
      background: linear-gradient(90deg, #67c23a, #8bc34a);
      border: none;
      box-shadow: 0 2px 6px rgba(103, 194, 58, 0.3);
      
      &:hover, &:focus {
        background: linear-gradient(90deg, #85ce61, #9ccc65);
        box-shadow: 0 4px 8px rgba(103, 194, 58, 0.4);
      }
    }
    
    &.el-button--danger {
      background: linear-gradient(90deg, #f56c6c, #ff8a80);
      border: none;
      box-shadow: 0 2px 6px rgba(245, 108, 108, 0.3);
      
      &:hover, &:focus {
        background: linear-gradient(90deg, #f78989, #ffab91);
        box-shadow: 0 4px 8px rgba(245, 108, 108, 0.4);
      }
    }
  }
  
  .button-group {
    display: flex;
    justify-content: space-around;
    flex-wrap: wrap;
  }
  
  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    
    .el-table__header {
      background-color: #f8f9fa;
      
      th {
        background-color: #f8f9fa;
        color: #606266;
        font-weight: 600;
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 15px;
    padding-top: 10px;
  }
  
  .task-info {
    .info-item {
      margin-bottom: 10px;
      padding: 8px 12px;
      background-color: #f8f9fa;
      border-radius: 6px;
      font-size: 14px;
      
      .label {
        font-weight: 600;
        margin-right: 8px;
        color: #606266;
        font-size: 13px;
      }
      
      .description {
        white-space: pre-line;
      }
    }
    
    .info-row {
      display: flex;
      margin-bottom: 10px;
      gap: 12px;
      
      .info-item {
        flex: 1;
        margin-bottom: 0;
      }
      
      @media (max-width: 768px) {
        flex-direction: column;
        
        .info-item {
          margin-bottom: 10px;
          
          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }
  }
  
  .progress-overview {
    .subtask-progress {
      .subtask-progress-item {
        margin-bottom: 12px;
        padding: 12px 15px;
        background-color: #f8f9fa;
        border-radius: 8px;
        border: 1px solid #ebeef5;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .subtask-title {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          
          span {
            font-weight: 600;
            color: #303133;
            font-size: 14px;
          }
          

        }
      }
    }
  }
  
  .subtask-details {
    .subtask-section {
      margin-bottom: 20px;
      padding: 0;
      background-color: #fff;
      border-radius: 8px;
      border: 1px solid #ebeef5;
      overflow: hidden;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .subtask-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px;
        background-color: #f8f9fa;
        border-bottom: 1px solid #ebeef5;
        cursor: pointer;
        transition: background-color 0.3s;
        
        &:hover {
          background-color: #ecf5ff;
        }
        
        .header-left {
          display: flex;
          align-items: center;
          
          .collapse-icon {
            margin-right: 10px;
            transition: transform 0.3s;
            font-size: 16px;
            
            &.is-active {
              transform: rotate(90deg);
            }
          }
          
          h4 {
            margin: 0;
            font-size: 16px;
            color: #303133;
          }
        }
        
        .header-right {
          display: flex;
          align-items: center;
        }
        
        .subtask-tag {
          margin-left: 10px;
        }
      }
      
      .subtask-content {
        padding: 20px;
        
        .subtask-description {
          margin: 0 0 20px 0;
          color: #606266;
          padding: 15px;
          background-color: #f8f9fa;
          border-radius: 8px;
          border-left: 3px solid #409eff;
        }
        
        .member-submissions {
          margin-top: 20px;
          
          h5 {
            margin: 0 0 15px 0;
            font-size: 16px;
            padding-bottom: 10px;
            border-bottom: 1px solid #ebeef5;
            color: #303133;
            font-weight: 600;
          }
          
          .member-table {
            margin-bottom: 10px;
            box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
          }
          
          .member-info {
            display: flex;
            align-items: center;
              gap: 12px;
              min-width: 140px;
              
              span {
                font-weight: 500;
                color: #303133;
                white-space: nowrap;
              }
              
              .el-avatar {
                flex-shrink: 0;
              }
            }
            
            .member-note {
              color: #303133;
              font-size: 14px;
              line-height: 1.4;
            }
            
            .no-note {
              color: #909399;
              font-size: 13px;
              font-style: italic;
            }
        }
      }
    }
    
    .subtask-cards {
      margin: 0 -12px;
      
      .subtask-col {
        margin-bottom: 30px;
        padding: 0 12px;
      }
      
      .subtask-card {
        height: 100%;
        border-radius: 15px;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
        transition: transform 0.3s, box-shadow 0.3s;
        border: 1px solid #e0e0e0;
        background-color: #ffffff;
        
        &:hover {
          transform: translateY(-8px);
          box-shadow: 0 12px 25px rgba(0, 0, 0, 0.15);
          border-color: #409eff;
        }
        
        :deep(.el-card__header) {
          padding: 18px 20px;
          background-color: #f0f2f5;
          border-bottom: 2px solid #e6e6e6;
          
          .el-card__header-wrapper {
            width: 100%;
          }
        }
        
        :deep(.el-card__body) {
          padding: 25px;
        }
        
                  .subtask-card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
          
                      .header-left {
              flex: 1;
              min-width: 150px;
              h4 {
                margin: 0;
                font-size: 18px;
                font-weight: 700;
                color: #303133;
                position: relative;
                padding-left: 12px;
              
              &:before {
                content: '';
                position: absolute;
                left: 0;
                top: 50%;
                transform: translateY(-50%);
                width: 5px;
                height: 20px;
                background: linear-gradient(to bottom, #409eff, #64b5f6);
                border-radius: 3px;
              }
            }
          }
          
                     .header-right {
             display: flex;
             align-items: center;
             flex-wrap: nowrap;
             gap: 8px;
          }
          

          
          .status-tag {
            font-weight: 600;
            white-space: nowrap;
          }
        }
        
        .subtask-content {
          padding: 18px 0 0;
          
          .subtask-description {
            margin: 0 0 25px 0;
            color: #606266;
            padding: 18px;
            background-color: #f8f9fa;
            border-radius: 10px;
            border-left: 4px solid #409eff;
            box-shadow: 0 3px 8px rgba(0, 0, 0, 0.06);
            font-size: 15px;
            line-height: 1.6;
          }
          
          .member-submissions {
            h5 {
              margin: 0 0 18px 0;
              font-size: 17px;
              padding-bottom: 12px;
              border-bottom: 2px solid #ebeef5;
              color: #303133;
              font-weight: 700;
              position: relative;
              padding-left: 14px;
              
              &:before {
                content: '';
                position: absolute;
                left: 0;
                top: 6px;
                width: 5px;
                height: 18px;
                background: linear-gradient(to bottom, #67c23a, #8bc34a);
                border-radius: 3px;
              }
            }
            
            .member-table {
              margin-bottom: 15px;
              box-shadow: 0 3px 15px 0 rgba(0, 0, 0, 0.08);
              border: 1px solid #ebeef5;
              border-radius: 8px;
              overflow: hidden;
              
              :deep(th) {
                background-color: #f0f2f5;
                font-weight: 700;
                font-size: 14px;
              }
              
              :deep(td) {
                padding: 12px;
                font-size: 14px;
              }
            }
            
            .member-info {
              display: flex;
              align-items: center;
              gap: 12px;
              min-width: 140px;
              
              span {
                font-weight: 500;
                color: #303133;
              }
            }
          }
        }
      }
    }
  }
  
  .icon-button {
    padding: 6px !important;
    min-width: auto !important;
  }
  
  .button-group {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  
  .note-icon {
    margin-left: 5px;
    font-size: 14px;
    color: #e6a23c;
    cursor: pointer;
  }
  
  .submission-detail-card-container {
    margin-top: 20px;
    border-top: 1px dashed #ebeef5;
    padding-top: 20px;
  }
  
  .submission-detail-card {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border-radius: 8px;
    border: 1px solid #e0e0e0;
    
    :deep(.el-card__header) {
      padding: 15px;
      background-color: #f0f2f5;
      border-bottom: 2px solid #e6e6e6;
    }
    
    .detail-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h4 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
      }
      
      .close-btn {
        font-size: 18px;
        color: #909399;
        
        &:hover {
          color: #409EFF;
        }
      }
    }
    
    .submission-content {
      padding: 10px 0;
      
      :deep(.el-descriptions) {
        margin-bottom: 20px;
        
        .el-descriptions__label {
          width: 80px;
          font-weight: 600;
        }
      }
      
      .detail-card-actions {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-top: 20px;
        padding-top: 15px;
        border-top: 1px dashed #ebeef5;
      }
    }
    
    .no-submission {
      padding: 20px 0;
      display: flex;
      justify-content: center;
    }
  }
}

.review-dialog, .submission-detail-dialog {
  :deep(.el-dialog__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    margin-right: 0;
    background-color: #f8f9fa;
    
    .el-dialog__title {
      font-weight: 600;
      font-size: 18px;
    }
  }
  
  :deep(.el-dialog__body) {
    padding: 20px;
  }
}

.submission-detail {
  :deep(.el-descriptions) {
    padding: 20px;
    border-radius: 8px;
    background-color: #f8f9fa;
    
    .el-descriptions__label {
      width: 120px;
      font-weight: 600;
    }
    
    .el-descriptions__content {
      padding: 12px 15px;
    }
  }
}

:deep(.el-divider) {
  margin: 24px 0;
}

.sub-task-detail-view-dialog {
  .sub-task-detail-content {
    .info-item {
      margin-bottom: 18px;
      display: flex;
      align-items: center;
      gap: 12px;
      
      .label {
        font-weight: 600;
        color: #606266;
        min-width: 80px;
      }
      .value.title {
        font-size: 20px;
        font-weight: bold;
      }
      .value.description {
        font-size: 16px;
        line-height: 1.6;
        color: #303133;
        flex: 1;
      }
      
      .status-tag {
        font-weight: 600;
      }
    }
    
    h5 {
      font-size: 18px;
      margin-bottom: 15px;
      margin-top: 10px;
    }

    .member-table {
      .member-info {
        font-size: 14px;
        gap: 12px;
        min-width: 160px;
        
        span {
          font-weight: 500;
          color: #303133;
        }
      }
      
      .member-note {
        color: #303133;
        font-size: 14px;
        line-height: 1.4;
        word-break: break-word;
      }
      
      .no-note {
        color: #909399;
        font-size: 13px;
        font-style: italic;
      }
    }
  }
}

.subtask-card {
  cursor: pointer;
  transition: all 0.3s ease-in-out;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 25px rgba(0, 0, 0, 0.15);
  }
}

.attachments-list {
  margin-top: 10px;
  
  .el-table {
    border-radius: 8px;
    overflow: hidden;
  }
  
  .rounded-btn {
    border-radius: 20px;
    padding: 6px 16px;
  }
}
</style>
