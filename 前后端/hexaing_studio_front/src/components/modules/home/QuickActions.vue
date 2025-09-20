<template>
  <el-card shadow="hover" class="quick-actions-card">
    <template #header>
      <span>快捷操作</span>
    </template>
    <div class="actions-grid">
      <div v-for="action in actions" :key="action.title" class="action-item" @click="handleActionClick(action.actionType)">
        <el-button :type="action.type" :icon="action.icon" circle size="large" />
        <span>{{ action.title }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Plus, UploadFilled, DocumentAdd, Promotion, List, Calendar } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();

const actions = ref([
  { title: '发布公告', icon: Promotion, type: 'success', actionType: 'announcement' },
  { title: '上传资料', icon: UploadFilled, type: 'primary', actionType: 'material' },
  { title: '新增课程', icon: DocumentAdd, type: 'warning', actionType: 'course' },
  { title: '新增任务', icon: List, type: 'info', actionType: 'task' },
  { title: '发起考勤', icon: Calendar, type: 'danger', actionType: 'attendance' },
]);

// 处理快捷操作点击
const handleActionClick = (actionType: string) => {
  switch(actionType) {
    case 'announcement': 
      router.push('/announcements?action=new');
      break;
    case 'material': 
      router.push('/materials?action=upload');
      break;
    case 'course': 
      router.push('/courses?action=new');
      break;
    case 'task': 
      router.push('/task/management?action=add');
      break;
    case 'attendance': 
      router.push('/attendance?action=add');
      break;
    default:
      console.error('未知的操作类型:', actionType);
  }
};

</script>

<style lang="scss" scoped>
.quick-actions-card {
  border-radius: var(--el-border-radius-base);
  transition: all 0.3s ease;
  height: 100%;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 12px rgba(0,0,0,0.1);
  }

  .actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); // Adjust minmax for more items
    gap: 15px; // Reduce gap slightly
    text-align: center;

    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      span {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
      
      &:hover {
        .el-button {
          transform: scale(1.1);
        }
        
        span {
          color: var(--el-color-primary);
        }
      }
      
      .el-button {
        transition: transform 0.2s ease;
      }
    }
  }
}
</style> 