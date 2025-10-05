<template>
  <div class="notifications-container">
    <div class="page-header">
      <el-page-header @back="goBack" :title="'返回'" :content="'通知中心'" />
    </div>
    
    <el-card class="notification-card">
      <template #header>
        <div class="card-header">
          <span>我的通知</span>
          <div class="header-actions">
            <el-button 
              v-if="selectedNotifications.length > 0" 
              type="danger" 
              size="small" 
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon> 删除选中 ({{ selectedNotifications.length }})
            </el-button>
            <el-button type="primary" size="small" @click="refreshNotifications">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all"></el-tab-pane>
        <el-tab-pane label="未读" name="unread"></el-tab-pane>
        <el-tab-pane label="已读" name="read"></el-tab-pane>
      </el-tabs>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
      
      <div v-else-if="notifications.length === 0" class="empty-container">
        <el-empty description="暂无通知" />
      </div>
      
      <el-scrollbar height="calc(100vh - 280px)" v-else>
        <div class="notification-list">
          <!-- 全选控制栏 -->
          <div class="select-all-bar" v-if="notifications.length > 0">
            <el-checkbox 
              v-model="selectAll" 
              @change="handleSelectAll"
              :indeterminate="isIndeterminate"
            >
              全选
            </el-checkbox>
            <span class="selected-count">
              已选择 {{ selectedNotifications.length }} / {{ notifications.length }} 个通知
            </span>
          </div>
          
          <div 
            v-for="notification in notifications" 
            :key="notification.id" 
            class="notification-item"
            :class="{ 'unread': notification.isRead === 0, 'selected': selectedNotifications.includes(notification.id) }"
          >
            <div class="notification-checkbox">
              <el-checkbox 
                v-model="selectedNotifications" 
                :value="notification.id"
                @change="updateSelectAll"
              />
            </div>
            <div class="notification-icon">
              <el-icon v-if="notification.type === 'announcement'"><Bell /></el-icon>
              <el-icon v-else-if="notification.type === 'task'"><Document /></el-icon>
              <el-icon v-else-if="notification.type === 'course'"><School /></el-icon>
              <el-icon v-else><Message /></el-icon>
            </div>
            <div class="notification-content" @click="markAsRead(notification)">
              <div class="notification-header">
                <span class="notification-title">{{ notification.title }}</span>
                <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
              </div>
              <div class="notification-body">{{ notification.content }}</div>
              <div class="notification-footer">
                <span class="notification-type">{{ getTypeName(notification.type) }}</span>
                <span v-if="notification.senderName" class="notification-sender">
                  发送人: {{ notification.senderName }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { Bell, Document, School, Message, Refresh, Delete } from '@element-plus/icons-vue';

// 直接在组件内部定义通知类型，避免导入问题
interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  sourceId?: number;
  senderId?: number;
  senderName?: string;
  isRead: number;
  readTime?: string;
  importance: number;
  createTime: string;
}

const NotificationType = {
  ANNOUNCEMENT: 'announcement',
  TASK: 'task',
  COURSE: 'course',
  SYSTEM: 'system'
};

const NotificationTypeNames = {
  [NotificationType.ANNOUNCEMENT]: '公告',
  [NotificationType.TASK]: '任务',
  [NotificationType.COURSE]: '课程',
  [NotificationType.SYSTEM]: '系统'
};

// 状态变量
const loading = ref(false);
const notifications = ref<Notification[]>([]);
const activeTab = ref('all');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 多选相关状态
const selectedNotifications = ref<number[]>([]);
const selectAll = ref(false);

// 计算是否为半选状态
const isIndeterminate = computed(() => {
  return selectedNotifications.value.length > 0 && selectedNotifications.value.length < notifications.value.length;
});

// 路由
const router = useRouter();
const goBack = () => router.back();

// 生命周期钩子
onMounted(() => {
  fetchNotifications();
  // 每60秒自动刷新一次
  const timer = setInterval(() => {
    fetchNotifications();
  }, 60000);
  
  // 组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(timer);
  });
});

// 导入API
import { getNotificationList, markAsRead as apiMarkAsRead, deleteNotifications } from '@/api/notification';

// 方法
const fetchNotifications = async () => {
  loading.value = true;
  try {
    const readStatus = activeTab.value === 'all' ? undefined : 
                      activeTab.value === 'read' ? 1 : 0;
    
    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
    };
    if (readStatus !== undefined) {
      params.readStatus = readStatus;
    }
    
    const response = await getNotificationList(params);
    
    // 已经由拦截器处理，直接使用数据
    notifications.value = response.data.records || [];
    total.value = response.data.total || 0;
    
    // 清空选择状态
    selectedNotifications.value = [];
    selectAll.value = false;
    
  } catch (error) {
    console.error('获取通知出错:', error);
    // 错误消息也由拦截器统一处理，这里可以留空或只做日志记录
  } finally {
    loading.value = false;
  }
};

const refreshNotifications = () => {
  currentPage.value = 1;
  fetchNotifications();
};

const handleTabChange = () => {
  currentPage.value = 1;
  selectedNotifications.value = [];
  selectAll.value = false;
  fetchNotifications();
};

const handleSizeChange = (val: number) => {
  pageSize.value = val;
  selectedNotifications.value = [];
  selectAll.value = false;
  fetchNotifications();
};

const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  selectedNotifications.value = [];
  selectAll.value = false;
  fetchNotifications();
};

const markAsRead = async (notification: Notification) => {
  if (notification.isRead === 1) return;
  
  try {
    await apiMarkAsRead(notification.id);
    // 成功后直接更新前端状态
    notification.isRead = 1;
    notification.readTime = new Date().toISOString();
    ElMessage.success('已标记为已读');
  } catch (error) {
    console.error('标记已读出错:', error);
    // 错误消息由拦截器统一处理
  }
};

// 全选/反选
const handleSelectAll = (value: boolean) => {
  if (value) {
    selectedNotifications.value = notifications.value.map(n => n.id);
  } else {
    selectedNotifications.value = [];
  }
};

// 更新全选状态
const updateSelectAll = () => {
  selectAll.value = selectedNotifications.value.length === notifications.value.length;
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedNotifications.value.length === 0) {
    ElMessage.warning('请选择要删除的通知');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedNotifications.value.length} 个通知吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await deleteNotifications(selectedNotifications.value);
    ElMessage.success(`成功删除 ${selectedNotifications.value.length} 个通知`);
    
    // 删除成功后刷新列表
    fetchNotifications();
    
  } catch (error: any) {
    if (error === 'cancel') {
      return;
    }
    console.error('删除通知出错:', error);
    ElMessage.error('删除失败');
  }
};

const formatTime = (timeString: string) => {
  const date = new Date(timeString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  // 一小时内
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000);
    return `${minutes}分钟前`;
  }
  
  // 一天内
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000);
    return `${hours}小时前`;
  }
  
  // 一周内
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000);
    return `${days}天前`;
  }
  
  // 超过一周
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const getTypeName = (type: string) => {
  return NotificationTypeNames[type as keyof typeof NotificationTypeNames] || '系统';
};
</script>

<style scoped>
.notifications-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.notification-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-container,
.empty-container {
  padding: 20px 0;
  text-align: center;
}

.notification-list {
  padding: 10px 0;
}

.select-all-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.selected-count {
  color: #909399;
  font-size: 14px;
}

.notification-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.3s;
  align-items: flex-start;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.notification-item.selected {
  background-color: #e1f3d8;
  border-left: 4px solid #67c23a;
}

.notification-checkbox {
  margin-right: 15px;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.notification-icon {
  margin-right: 15px;
  font-size: 24px;
  color: #409eff;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.notification-content {
  flex: 1;
  cursor: pointer;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.notification-title {
  font-weight: bold;
  font-size: 16px;
}

.notification-time {
  color: #909399;
  font-size: 12px;
}

.notification-body {
  margin-bottom: 10px;
  color: #606266;
  line-height: 1.5;
}

.notification-footer {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.notification-type {
  background-color: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 