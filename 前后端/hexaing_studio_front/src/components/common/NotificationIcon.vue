<template>
  <div class="notification-icon">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
      <el-button 
        icon="Bell" 
        circle 
        @click="showNotifications"
        class="notification-button"
        :type="unreadCount > 0 ? 'danger' : 'info'"
      ></el-button>
    </el-badge>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getUnreadCount } from '@/api/notification';
import { ElMessage } from 'element-plus';

export default defineComponent({
  name: 'NotificationIcon',
  setup() {
    const unreadCount = ref(0);
    const router = useRouter();
    let timer: number | null = null;

    // 获取未读通知数量
    const fetchUnreadCount = async () => {
      try {
        const res = await getUnreadCount();
        const response = res as any; // 使用类型断言
        if (response.code === 200 || response.code === 1) {
          unreadCount.value = response.data;
        }
      } catch (error) {
        console.error('获取未读通知数失败', error);
      }
    };

    // 显示通知列表
    const showNotifications = () => {
      router.push('/notifications');
    };

    onMounted(() => {
      // 首次加载
      fetchUnreadCount();
      
      // 定时刷新未读数
      timer = window.setInterval(fetchUnreadCount, 60000); // 每分钟刷新一次
    });

    onBeforeUnmount(() => {
      // 清除定时器
      if (timer !== null) {
        clearInterval(timer);
      }
    });

    return {
      unreadCount,
      showNotifications
    };
  }
});
</script>

<style scoped>
.notification-icon {
  margin-right: 10px;
}

.notification-badge {
  margin-top: 10px;
}

.notification-button {
  font-size: 18px;
}
</style> 