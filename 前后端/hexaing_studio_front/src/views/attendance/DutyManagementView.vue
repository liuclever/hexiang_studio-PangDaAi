<template>
  <div class="duty-management-container">
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" class="duty-tabs">
      <el-tab-pane label="值班表" name="schedule">
        <duty-schedule-content ref="scheduleContentRef" />
      </el-tab-pane>
      <el-tab-pane label="值班记录" name="records">
        <duty-records-content ref="recordsContentRef" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import DutyScheduleContent from './components/DutyScheduleContent.vue';
import DutyRecordsContent from './components/DutyRecordsContent.vue';

// 默认激活值班表标签页
const activeTab = ref('schedule');

// 组件引用
const scheduleContentRef = ref(null);
const recordsContentRef = ref(null);

// 刷新值班数据的方法，供父组件调用
const refreshDutyData = async () => {
  if (activeTab.value === 'schedule') {
    await scheduleContentRef.value?.fetchDutyData();
  } else if (activeTab.value === 'records') {
    await recordsContentRef.value?.fetchRecords();
  }
};

// 导出方法供父组件调用
defineExpose({
  refreshDutyData
});
</script>

<style scoped>
.duty-management-container {
  /* padding: 20px; */ /* Adjusted for better embedding */
}

.duty-tabs {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}
</style> 