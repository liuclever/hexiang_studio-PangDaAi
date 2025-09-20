<template>
  <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-sidebar">
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :background-color="sidebarStyles.backgroundColor"
        :text-color="sidebarStyles.textColor"
        :active-text-color="sidebarStyles.activeTextColor"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
        router
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/courses">
          <el-icon><Reading /></el-icon>
          <template #title>课程管理</template>
        </el-menu-item>
        <el-menu-item index="/attendance">
          <el-icon><Calendar /></el-icon>
          <template #title>考勤管理</template>
        </el-menu-item>
        <el-menu-item index="/user-management">
          <el-icon><User /></el-icon>
          <template #title>人员管理</template>
        </el-menu-item>
        <el-menu-item index="/task/management">
          <el-icon><List /></el-icon>
          <template #title>任务管理</template>
        </el-menu-item>
        <el-menu-item index="/announcements">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        <el-menu-item index="/materials">
          <el-icon><Folder /></el-icon>
          <template #title>资料管理</template>
        </el-menu-item>
        <el-menu-item index="/info-portal">
          <el-icon><InfoFilled /></el-icon>
          <template #title>信息门户</template>
        </el-menu-item>
        <el-menu-item index="/system-settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
    <div class="sidebar-footer">
       <el-button v-if="!isCollapsed" type="primary" class="support-btn" :icon="Promotion" @click="showSupportDialog">技术支持</el-button>
       <el-tooltip v-else content="技术支持" placement="right">
         <el-button type="primary" circle :icon="Promotion" @click="showSupportDialog" />
       </el-tooltip>
       <div class="logout-link" @click="handleLogout">
         <el-icon><SwitchButton /></el-icon>
         <span v-if="!isCollapsed" style="margin-left: 8px;">退出登录</span>
       </div>
    </div>
  </el-aside>
  
  <!-- 技术支持弹窗 - 使用左侧抽屉 (页面级别) -->
  <el-drawer
    v-model="supportDialogVisible"
    title="技术支持"
    direction="ltr"
    size="400px"
    :with-header="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
  >
    <div class="support-dialog-content">
      <div class="support-dialog-header">
        <h3>技术支持联系方式</h3>
        <p>如遇系统问题，请联系以下技术支持人员获取帮助</p>
      </div>
      
      <div v-if="supportContacts.length > 0" class="support-contacts-container">
        <el-card 
          v-for="contact in supportContacts" 
          :key="contact.id" 
          shadow="hover" 
          class="support-contact-card"
        >
          <div class="support-contact-header">
            <h4>{{ contact.name }}</h4>
            <div class="support-contact-position">{{ contact.position }}</div>
          </div>
          <div class="support-contact-info">
            <div class="support-contact-item">
              <el-icon><Phone /></el-icon>
              <span>{{ contact.phone }}</span>
            </div>
            <div class="support-contact-item">
              <el-icon><Message /></el-icon>
              <span>{{ contact.email }}</span>
            </div>
          </div>
        </el-card>
      </div>
      <div v-else-if="loading" class="support-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else class="support-empty">
        <el-empty description="暂无技术支持联系人" />
      </div>
    </div>
    <template #footer>
      <div style="flex: auto;">
        <el-button type="primary" @click="supportDialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { logout } from '@/utils/auth';
import request from '@/utils/request';
import {
  DataAnalysis, Files, Reading, User, Bell, InfoFilled, Calendar, SwitchButton, Promotion, Folder,
  Document, List, Phone, Message, Loading, WarningFilled, UserFilled, Setting
} from '@element-plus/icons-vue';

const props = defineProps({
  isCollapsed: {
    type: Boolean,
    default: false
  }
});

const route = useRoute();
const router = useRouter();
const activeMenu = computed(() => route.path);

// 技术支持相关变量
const supportDialogVisible = ref(false);
const loading = ref(false);
const supportContacts = ref<any[]>([]); 

// 获取技术支持联系人
const fetchSupportContacts = async () => {
  loading.value = true;
  supportContacts.value = []; // 清空之前的数据
  try {
    // 从后端API获取数据
    const response = await request.get('/admin/support/contacts');
    console.log('技术支持联系人API响应:', response);
    
    // 情况1: 标准响应格式 {code: 200, data: [...]}
    if (response?.data?.code === 200 && Array.isArray(response?.data?.data)) {
      supportContacts.value = response.data.data;
      console.log('解析后的联系人数据:', supportContacts.value);
    } 
    // 情况2: 直接返回数组
    else if (Array.isArray(response?.data)) {
      supportContacts.value = response.data;
      console.log('解析后的联系人数据(直接数组):', supportContacts.value);
    }
    // 情况3: 数据在response.data的顶层
    else if (response?.data && typeof response.data === 'object') {
      const contacts = [];
      // 尝试从响应中提取联系人数据
      if (Array.isArray(response.data.contacts)) {
        contacts.push(...response.data.contacts);
      } else if (Array.isArray(response.data.list)) {
        contacts.push(...response.data.list);
      } else if (response.data.id && response.data.name) {
        // 单个联系人对象
        contacts.push(response.data);
      }
      
      if (contacts.length > 0) {
        supportContacts.value = contacts;
        console.log('从其他字段提取的联系人数据:', supportContacts.value);
      } else {
        console.warn('API返回数据格式未识别:', response.data);
      }
    } else {
      console.warn('API未返回有效的技术支持联系人数据');
    }
  } catch (error) {
    console.error('获取技术支持联系人出错:', error);
    ElMessage.error('获取技术支持联系人失败');
  } finally {
    loading.value = false;
  }
};

// 显示技术支持弹窗
const showSupportDialog = () => {
  supportDialogVisible.value = true;
  fetchSupportContacts();
};

// Styles for the sidebar
const sidebarStyles = ref({
  backgroundColor: 'rgba(220, 235, 255, 0.8)', // Light blue, semi-transparent for frosted glass
  textColor: '#303133',
  activeTextColor: 'var(--el-color-primary)', // Element Plus primary color
});

const handleLogout = async () => {
  console.log('执行退出登录');
  try {
    // 调用退出登录函数（现在是异步的）
    await logout();
    
    // 显示成功消息
    ElMessage.success('退出登录成功');
    
    // 重定向到登录页
    router.push('/login');
  } catch (error) {
    console.error('退出登录出错:', error);
    ElMessage.error('退出登录过程中出现错误，请重试');
    
    // 即使出错也跳转到登录页
    router.push('/login');
  }
};
</script>

<style lang="scss" scoped>
.app-sidebar {
  transition: width 0.28s;
  background-color: v-bind('sidebarStyles.backgroundColor');
  backdrop-filter: blur(10px); // Frosted glass effect
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  height: 100vh; // Ensure full height

  .sidebar-logo-container {
    padding: 15px;
    text-align: center;
    height: 60px; // Match header height
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .sidebar-title {
      color: var(--el-color-primary);
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      white-space: nowrap;
    }
    
    .sidebar-title-collapsed {
      color: var(--el-color-primary);
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }

  .el-scrollbar {
    flex-grow: 1;
    .scrollbar-wrapper {
      overflow-x: hidden !important;
    }
  }

  .el-menu {
    border-right: none;
    &:not(.el-menu--collapse) {
      width: 100%;
    }
  }

  .el-menu-item {
    &:hover {
      background-color: rgba(0, 0, 0, 0.05) !important; // Subtle hover
    }
    &.is-active {
      background-color: var(--el-color-primary-light-9) !important;
      border-right: 3px solid var(--el-color-primary);
      color: var(--el-color-primary);
    }
  }

  .sidebar-footer {
    padding: 15px;
    border-top: 1px solid rgba(0,0,0,0.05);
    
    .support-btn {
      width: 100%;
      margin-bottom: 10px;
    }
    
    .logout-link {
      margin-top: 10px;
      display: flex;
      align-items: center;
      cursor: pointer;
      color: var(--el-text-color-regular);
      &:hover {
        color: var(--el-color-primary);
      }
    }
  }
}

// 技术支持弹窗样式
.support-contacts-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.support-contact-card {
  margin-bottom: 16px;
  width: 100%;

  .support-contact-header {
    margin-bottom: 12px;
    
    h4 {
      margin: 0 0 4px 0;
      font-size: 16px;
      font-weight: 600;
    }
    
    .support-contact-position {
      font-size: 14px;
      color: #606266;
    }
  }
  
  .support-contact-info {
    display: flex;
    flex-direction: column;
    gap: 10px;
    
    .support-contact-item {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .el-icon {
        color: var(--el-color-primary);
      }
    }
  }
}

.support-loading, .support-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
  gap: 16px;
  
  .el-icon {
    font-size: 24px;
    margin-bottom: 10px;
  }
}

.support-empty .el-icon {
  color: #E6A23C;
}

.support-dialog-content {
  padding: 0 20px;
}

.support-dialog-header {
  text-align: center;
  margin-bottom: 24px;
  
  h3 {
    font-size: 18px;
    color: var(--el-color-primary);
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: #606266;
    margin: 0;
  }
}

:deep(.el-drawer__footer) {
  display: flex;
  justify-content: center;
}
</style> 