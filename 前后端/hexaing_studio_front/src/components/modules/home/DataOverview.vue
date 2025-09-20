<template>
  <el-row :gutter="20" class="data-overview">
    <el-col :xs="24" :sm="12" :md="6" v-for="item in stats" :key="item.title">
      <el-card shadow="hover" class="stat-card" :style="{ background: item.bgColor }">
        <div class="stat-card-content">
          <el-icon :size="40" class="stat-icon" :color="item.iconColor || 'var(--el-color-primary)'">
            <component :is="item.icon" />
          </el-icon>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-title">{{ item.title }}</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { User, Finished, TrendCharts, School } from '@element-plus/icons-vue';
import { getDashboardOverview } from '@/utils/api/dashboard';
import { ElMessage } from 'element-plus';

const stats = ref([
  { 
    title: '学生总数', 
    value: 0, 
    icon: 'User', 
    bgColor: 'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)', 
    iconColor: '#fff',
    key: 'studentCount'
  },
  { 
    title: '资料总数', 
    value: 0, 
    icon: 'Finished', 
    bgColor: 'linear-gradient(135deg, #f6d365 0%, #fda085 100%)', 
    iconColor: '#fff',
    key: 'materialCount'
  },
  { 
    title: '当前在线人数', 
    value: 0, 
    icon: 'TrendCharts', 
    bgColor: 'linear-gradient(135deg, #c2e59c 0%, #64b3f4 100%)', 
    iconColor: '#fff',
    key: 'onlineCount'
  },
  { 
    title: '课程总数', 
    value: 0, 
    icon: 'School', 
    bgColor: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)', 
    iconColor: '#fff',
    key: 'courseCount'
  },
]);

const loading = ref(false);

const fetchOverviewData = async () => {
  loading.value = true;
  try {
    const res = await getDashboardOverview();
    if (res && res.code === 200 && res.data) {
      // 更新统计数据
      stats.value.forEach(stat => {
        if (res.data[stat.key] !== undefined) {
          stat.value = res.data[stat.key];
        }
      });
    } else {
      ElMessage.error(res?.msg || '获取概览数据失败');
    }
  } catch (error) {
    console.error('获取概览数据失败:', error);
    ElMessage.error('获取概览数据失败，请检查网络连接');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchOverviewData();
});
</script>

<style lang="scss" scoped>
.data-overview {
  .stat-card {
    border-radius: var(--el-border-radius-base);
    color: #fff; // White text for gradient backgrounds
    margin-bottom: 20px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 6px 12px rgba(0,0,0,0.1);
    }
    
    .stat-card-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 15px;
    }
    
    .stat-icon {
      opacity: 0.8;
    }
    
    .stat-info {
      text-align: right;
      
      .stat-value {
        font-size: 24px;
        font-weight: bold;
      }
      
      .stat-title {
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }
}
</style> 