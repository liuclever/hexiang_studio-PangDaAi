<template>
  <div class="notification-page">
    <page-header title="通知中心"></page-header>
    
    <div class="notification-filter">
      <el-radio-group v-model="activeType" @change="handleTypeChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button :label="NotificationType.ANNOUNCEMENT">公告</el-radio-button>
        <el-radio-button :label="NotificationType.TASK">任务</el-radio-button>
        <el-radio-button :label="NotificationType.COURSE">课程</el-radio-button>
        <el-radio-button :label="NotificationType.SYSTEM">系统</el-radio-button>
      </el-radio-group>
      
      <el-button type="primary" @click="markAllAsRead" :disabled="!hasUnread">
        全部标为已读
      </el-button>
      <el-button @click="showTestNotificationDialog" type="success">测试通知</el-button>
    </div>
    
    <el-card class="notification-list" :loading="loading">
      <template v-if="notifications.length > 0">
        <div 
          v-for="notification in notifications" 
          :key="notification.id" 
          class="notification-item"
          :class="{ 'notification-unread': notification.isRead === 0 }"
          @click="viewNotification(notification)"
        >
          <div class="notification-icon">
            <el-icon v-if="notification.type === NotificationType.ANNOUNCEMENT"><Promotion /></el-icon>
            <el-icon v-else-if="notification.type === NotificationType.TASK"><List /></el-icon>
            <el-icon v-else-if="notification.type === NotificationType.COURSE"><Reading /></el-icon>
            <el-icon v-else><Setting /></el-icon>
          </div>
          <div class="notification-content">
            <div class="notification-header">
              <span class="notification-title">{{ notification.title }}</span>
              <el-tag 
                size="small" 
                :type="getTypeTagType(notification.type)"
              >
                {{ getTypeName(notification.type) }}
              </el-tag>
              <el-tag 
                v-if="notification.importance > 0" 
                size="small" 
                type="danger"
              >
                {{ notification.importance === 2 ? '紧急' : '重要' }}
              </el-tag>
            </div>
            <div class="notification-body">{{ notification.content }}</div>
            <div class="notification-footer">
              <span>{{ notification.senderName || '系统' }}</span>
              <span>{{ formatTime(notification.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <el-empty description="暂无通知"></el-empty>
      </template>
    </el-card>
    
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 通知详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="currentNotification.title"
      width="600px"
    >
      <div class="notification-detail">
        <div class="notification-detail-header">
          <el-tag 
            size="small" 
            :type="getTypeTagType(currentNotification.type || '')"
          >
            {{ getTypeName(currentNotification.type || '') }}
          </el-tag>
          <el-tag 
            v-if="currentNotification.importance && currentNotification.importance > 0" 
            size="small" 
            type="danger"
          >
            {{ currentNotification.importance === 2 ? '紧急' : '重要' }}
          </el-tag>
        </div>
        <div class="notification-detail-content">
          {{ currentNotification.content }}
        </div>
        <div class="notification-detail-footer">
          <span>发送人: {{ currentNotification.senderName || '系统' }}</span>
          <span>时间: {{ formatTime(currentNotification.createTime) }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleAction" v-if="hasAction">
            {{ getActionText() }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 测试通知对话框 -->
    <el-dialog
      title="创建测试通知"
      v-model="testNotificationDialogVisible"
      width="500px"
    >
      <el-form :model="testNotificationForm" label-width="120px">
        <el-form-item label="通知类型">
          <el-select v-model="testNotificationForm.type" placeholder="请选择通知类型">
            <el-option label="系统通知" value="system"></el-option>
            <el-option label="公告通知" value="announcement"></el-option>
            <el-option label="任务通知" value="task"></el-option>
            <el-option label="课程通知" value="course"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标用户ID">
          <el-input v-model.number="testNotificationForm.targetUserId" placeholder="留空表示全局通知"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="testNotificationDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createTestNotification">创建</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getNotificationList, markAsRead, markAllAsRead as markAllRead, getNotificationDetail, getUnreadCount } from '@/api/notification';
import { Notification, NotificationType, NotificationTypeNames } from '@/types/notification';
import { ElMessage } from 'element-plus';
import { Promotion, List, Reading, Setting } from '@element-plus/icons-vue';
import PageHeader from '@/components/common/PageHeader.vue';
import { formatDate } from '@/utils/date';


export default defineComponent({
  name: 'NotificationsView',
  components: {
    PageHeader
  },
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const notifications = ref<Notification[]>([]);
    const currentPage = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    const activeType = ref('');
    const dialogVisible = ref(false);
    const currentNotification = ref<Partial<Notification>>({});
    
    // 计算是否有未读通知
    const hasUnread = computed(() => {
      return notifications.value.some(item => item.isRead === 0);
    });
    
    // 计算当前通知是否有相关操作
    const hasAction = computed(() => {
      if (!currentNotification.value.type) return false;
      
      switch (currentNotification.value.type) {
        case NotificationType.ANNOUNCEMENT:
          return true;
        case NotificationType.TASK:
          return currentNotification.value.sourceId !== undefined;
        case NotificationType.COURSE:
          return currentNotification.value.sourceId !== undefined;
        default:
          return false;
      }
    });
    
    // 获取通知列表
    const fetchNotifications = async () => {
      loading.value = true;
      try {
        const res = await getNotificationList({
          type: activeType.value,
          page: currentPage.value,
          size: pageSize.value
        });
        
        const response = res as any;
        if (response.code === 200 || response.code === 1) {
          const data = response.data;
          notifications.value = data.records || [];
          total.value = data.total || 0;
        }
      } catch (error) {
        console.error('获取通知列表失败', error);
        ElMessage.error('获取通知列表失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 查看通知详情
    const viewNotification = async (notification: Notification) => {
      currentNotification.value = notification;
      dialogVisible.value = true;
      
      // 标记为已读
      if (notification.isRead === 0) {
        try {
          await markAsRead(notification.id);
          notification.isRead = 1;
        } catch (error) {
          console.error('标记已读失败', error);
        }
      }
    };
    
    // 标记所有通知为已读
    const markAllAsRead = async () => {
      try {
        await markAllRead(activeType.value);
        ElMessage.success('已全部标为已读');
        fetchNotifications(); // 重新加载列表
      } catch (error) {
        console.error('标记全部已读失败', error);
        ElMessage.error('标记全部已读失败');
      }
    };
    
    // 处理类型变更
    const handleTypeChange = () => {
      currentPage.value = 1;
      fetchNotifications();
    };
    
    // 处理页码变更
    const handleCurrentChange = (page: number) => {
      currentPage.value = page;
      fetchNotifications();
    };
    
    // 获取通知类型名称
    const getTypeName = (type: string) => {
      return NotificationTypeNames[type as NotificationType] || '未知';
    };
    
    // 获取通知类型标签样式
    const getTypeTagType = (type?: string) => {
      if (!type) return '';
      
      switch (type) {
        case NotificationType.ANNOUNCEMENT:
          return 'success';
        case NotificationType.TASK:
          return 'warning';
        case NotificationType.COURSE:
          return 'primary';
        case NotificationType.SYSTEM:
          return 'info';
        default:
          return '';
      }
    };
    
    // 格式化时间
    const formatTime = (time?: string) => {
      if (!time) return '';
      return formatDate(new Date(time), 'YYYY-MM-DD HH:mm');
    };
    
    // 获取操作按钮文本
    const getActionText = () => {
      if (!currentNotification.value.type) return '';
      
      switch (currentNotification.value.type) {
        case NotificationType.ANNOUNCEMENT:
          return '查看公告';
        case NotificationType.TASK:
          return '查看任务';
        case NotificationType.COURSE:
          return '查看课程';
        default:
          return '';
      }
    };
    
    // 处理操作按钮点击
    const handleAction = () => {
      if (!currentNotification.value.type || !currentNotification.value.sourceId) return;
      
      dialogVisible.value = false;
      
      switch (currentNotification.value.type) {
        case NotificationType.ANNOUNCEMENT:
          router.push(`/announcements/${currentNotification.value.sourceId}`);
          break;
        case NotificationType.TASK:
          router.push(`/tasks/${currentNotification.value.sourceId}`);
          break;
        case NotificationType.COURSE:
          router.push(`/courses/${currentNotification.value.sourceId}`);
          break;
      }
    };

    const isAdmin = ref(false);
    const checkAdminStatus = () => {
        const roleId = localStorage.getItem('user_role_id');
        console.log('在通知页面检查权限，从本地存储读取到的 roleId 是:', roleId);
        isAdmin.value = roleId === '3' || roleId === '4';
    };

    const testNotificationDialogVisible = ref(false);
    const testNotificationForm = reactive({
      type: 'system',
      targetUserId: undefined as number | undefined
    });

    function showTestNotificationDialog() {
      testNotificationDialogVisible.value = true;
    }

    // 获取未读通知数量
    const fetchUnreadCount = async () => {
      try {
        const res = await getUnreadCount();
        // 通知图标组件会自动从API获取未读数量，这里不需要处理结果
      } catch (error) {
        console.error('获取未读通知数量失败', error);
      }
    };

    async function createTestNotification() {
      try {
        const params: Record<string, any> = {
          type: testNotificationForm.type
        };
        
        if (testNotificationForm.targetUserId) {
          params.targetUserId = testNotificationForm.targetUserId;
        }
        
        // 使用request工具而不是直接fetch
        const response = await fetch(`/api/admin/notification/test/create?${new URLSearchParams(params).toString()}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}` || ''
          }
        });
        
        const data = await response.json();
        console.log('测试通知响应:', data);
        
        // 支持多种成功状态码
        if (data.code === 1 || data.code === 200) {
          ElMessage.success('测试通知创建成功');
          testNotificationDialogVisible.value = false;
          // 刷新通知列表
          fetchNotifications();
          // 刷新未读数量
          fetchUnreadCount();
        } else {
          ElMessage.error(`测试通知创建失败: ${data.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('测试通知创建异常:', error);
        ElMessage.error(`测试通知创建异常: ${error}`);
      }
    }
    
    onMounted(() => {
      fetchNotifications();
      checkAdminStatus();
    });
    
    return {
      loading,
      notifications,
      currentPage,
      pageSize,
      total,
      activeType,
      dialogVisible,
      currentNotification,
      hasUnread,
      hasAction,
      NotificationType,
      fetchNotifications,
      viewNotification,
      markAllAsRead,
      handleTypeChange,
      handleCurrentChange,
      getTypeName,
      getTypeTagType,
      formatTime,
      getActionText,
      handleAction,
      isAdmin,
      testNotificationDialogVisible,
      testNotificationForm,
      showTestNotificationDialog,
      createTestNotification,
      fetchUnreadCount
    };
  }
});
</script>

<style scoped>
.notification-page {
  padding: 20px;
}

.notification-filter {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
}

.notification-list {
  margin-bottom: 20px;
}

.notification-item {
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  transition: background-color 0.3s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-unread {
  background-color: #f0f9eb;
}

.notification-icon {
  margin-right: 15px;
  font-size: 24px;
  color: #409eff;
}

.notification-content {
  flex: 1;
}

.notification-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.notification-title {
  font-weight: bold;
  margin-right: 10px;
  flex: 1;
}

.notification-body {
  color: #606266;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-footer {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.notification-detail-header {
  margin-bottom: 15px;
}

.notification-detail-content {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 15px;
  min-height: 100px;
  white-space: pre-line;
}

.notification-detail-footer {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}
</style> 