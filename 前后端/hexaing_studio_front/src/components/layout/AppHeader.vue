<template>
  <el-header class="app-header">
    <div class="header-left">
      <el-icon class="sidebar-toggle" @click="toggleSidebar"><Expand v-if="isSidebarCollapsed" /><Fold v-else /></el-icon>
      <img src="/images/logo.svg" alt="Logo" class="header-logo" />
      <h2 class="header-title">何湘技能大师工作室</h2>
    </div>
    <div class="header-right">
      <!-- 使用新的通知图标组件 -->
      <notification-icon />
      
      <el-dropdown @command="handleUserCommand" trigger="click">
        <span class="el-dropdown-link user-avatar-container">
          <el-avatar 
            :size="32" 
            :src="userAvatar || defaultAvatar" 
            :alt="userName"
          />
          <span class="user-name">{{ userName }}</span>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>账号设置
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { logout, getUserId, getUserName, getUserAvatar } from '@/utils/auth';
import request, { ApiResponse } from '@/utils/request';
import {
  Search, Bell, Setting, ArrowDown, Fold, Expand, User, SwitchButton
} from '@element-plus/icons-vue';
import { resolveAvatarUrl } from '@/utils/fileUtils';
import NotificationIcon from '@/components/common/NotificationIcon.vue';

const emit = defineEmits(['toggle-sidebar']);
const router = useRouter();

const props = defineProps({
  isSidebarCollapsed: {
    type: Boolean,
    default: false
  }
});

// 用户数据
const userId = ref<number | null>(getUserId());
const userName = ref<string>(getUserName() || '用户');
const userAvatar = ref<string | null>(null);
const defaultAvatar = '/images/default-avatar.svg'; // 与fileUtils保持一致

// 初始化时立即从localStorage加载头像
const storedAvatarPath = getUserAvatar();
userAvatar.value = resolveAvatarUrl(storedAvatarPath);

// 初始化加载用户信息
onMounted(() => {
  loadUserInfo();
});

// 获取用户信息
const loadUserInfo = async () => {
  try {
    // 使用新的个人信息接口，自动获取当前用户信息
    const response: ApiResponse = await request.get('/admin/user/profile');
    
    if (response?.code === 200 && response?.data) {
      const userInfo = response.data;
      // 修改这里：使用name而不是username
      userName.value = userInfo.name || userInfo.username || userName.value;
      
      // 使用工具函数处理头像URL
      userAvatar.value = resolveAvatarUrl(userInfo.avatar);
      
      // 更新localStorage中的头像信息
      if (userInfo.avatar) {
        localStorage.setItem('user_avatar', userInfo.avatar);
      } else {
        localStorage.removeItem('user_avatar');
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

const toggleSidebar = () => {
  emit('toggle-sidebar');
};

// 用户下拉菜单操作
const handleUserCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await logout();
      ElMessage.success('退出登录成功');
      router.push('/login');
    } catch (error) {
      console.error('退出登录失败:', error);
      ElMessage.error('退出登录失败，请重试');
      router.push('/login');
    }
  } else if (command === 'profile') {
    router.push('/profile');
  } else if (command === 'settings') {
    router.push('/account-settings');
  }
};
</script>

<style lang="scss" scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  background-color: var(--header-bg-color);
  backdrop-filter: blur(8px);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 1000;

  .header-left {
    display: flex;
    align-items: center;
    .sidebar-toggle {
      font-size: 20px;
      cursor: pointer;
      margin-right: 15px;
      color: var(--el-text-color-regular);
       &:hover {
        color: var(--el-color-primary);
      }
    }
    .header-logo {
      width: 32px;
      height: 32px;
      margin-right: 12px;
    }
    .header-title {
      font-size: 18px;
      color: var(--el-text-color-primary);
      margin: 0;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .el-icon {
      cursor: pointer;
      color: var(--el-text-color-regular);
      &:hover {
        color: var(--el-color-primary);
      }
    }

    .user-avatar-container {
      display: flex;
      align-items: center;
      cursor: pointer;
      .user-name {
        margin-left: 8px;
        font-size: 14px;
        color: var(--el-text-color-primary);
      }
    }
  }
}

// 下拉菜单中的图标
:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
}
</style> 