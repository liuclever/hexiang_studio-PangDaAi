<template>
  <el-card shadow="hover" class="notice-board-card">
    <template #header>
      <div class="card-header">
        <span>通知公告</span>
        <div class="view-all-link" @click="goToAnnouncements">
          <span>查看全部</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </template>
    
    <el-scrollbar class="notice-scrollbar">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="notices.length === 0" class="empty-state">
        <el-empty description="暂无公告" :image-size="80" />
      </div>
      <div v-else class="notice-list">
        <div v-for="notice in notices" :key="notice.id" class="notice-item" @click="viewNoticeDetail(notice)">
          <div class="notice-image-container">
            <el-image :src="notice.imageUrl || defaultImage" fit="cover" class="notice-image" />
          </div>
          <div class="notice-details">
            <p class="notice-title">{{ notice.title }}</p>
            <div class="notice-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon> {{ notice.publisher || '系统发布' }}
              </span>
              <span class="meta-item">
                <el-icon><Clock /></el-icon> {{ formatDateTime(notice.publishTime) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { User, Clock, ArrowRight } from '@element-plus/icons-vue';
import { getRecentNotices } from '@/utils/api/notice';
import { ElMessage } from 'element-plus';
import defaultImage from '/images/default-cover.svg';

interface Notice {
  id: number;
  title: string;
  publisher: string;
  publishTime: string;
  imageUrl?: string;
}

const notices = ref<Notice[]>([]);
const loading = ref(true);
const router = useRouter();

const fetchRecentNotices = async () => {
  try {
    const res: any = await getRecentNotices(); // 不限制数量，使用后端设置的限制
    if (res.code === 200 && res.data) {
      console.log('获取到的公告数据:', res.data); // 添加日志，便于调试
      notices.value = res.data.map((item: any) => {
        // 处理图片路径
        let imageUrl = null;
        if (item.images && item.images.length > 0) {
          const image = item.images[0];
          // 使用 image_path 字段
          imageUrl = `/api/admin/file/view/${image.filePath || image.image_path || ''}`;
        }
        
        return {
          id: item.noticeId,
          title: item.title,
          publisher: item.publisher || item.publisherName || '系统',
          publishTime: item.publishTime,
          imageUrl: imageUrl || defaultImage
        };
      });
    } else {
      ElMessage.error('获取公告列表失败');
    }
  } catch (error) {
    console.error('获取公告列表失败:', error);
    ElMessage.error('获取公告时出错');
  } finally {
    loading.value = false;
  }
};

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '未知时间';
  // 只显示日期部分
  return dateTime.substring(0, 10);
};

const goToAnnouncements = () => {
  router.push('/announcements');
};

const viewNoticeDetail = (notice: Notice) => {
  router.push(`/announcements?noticeId=${notice.id}`);
};

onMounted(() => {
  fetchRecentNotices();
});
</script>

<style lang="scss" scoped>
.notice-board-card {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-card__header) {
    flex-shrink: 0;
  }

  :deep(.el-card__body) {
    flex-grow: 1;
    padding: 0; 
    overflow: hidden; // 确保 el-scrollbar 能正确计算高度
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.view-all-link {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--el-color-primary);
  cursor: pointer;
  font-weight: 500;
  background-color: rgba(64, 158, 255, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;

  &:hover {
    background-color: rgba(64, 158, 255, 0.2);
    span {
      text-decoration: underline;
    }
  }

  .el-icon {
    margin-left: 4px;
  }
}

.notice-scrollbar {
  padding: 15px;
}

.loading-state, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%; // 占满可用空间
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  padding: 10px;
  border-radius: 8px;
  transition: background-color 0.3s, box-shadow 0.3s;

  &:hover {
    background-color: #f9fafb;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }
}

.notice-image-container {
  width: 100px;
  height: 60px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
}

.notice-image {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 修改: 从 cover 改为 contain */
}

.notice-details {
  flex-grow: 1;
  min-width: 0; // 允许flex item收缩
}

.notice-title {
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  .el-icon {
    margin-right: 4px;
  }
}
</style> 