<template>
  <div class="home-view">
    <DataOverview />
    <el-row :gutter="20" class="home-row">
      <el-col :xs="24" :sm="24" :md="16">
        <MyTasks />
      </el-col>
      <el-col :xs="24" :sm="24" :md="8">
        <QuickActions />
      </el-col>
    </el-row>
    <el-row :gutter="20" class="home-row">
      <el-col :xs="24" :sm="24" :md="16">
        <NoticeBoard />
      </el-col>
      <el-col :xs="24" :sm="24" :md="8">
        <TaskCalendarPanel />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import DataOverview from '@/components/modules/home/DataOverview.vue';
import MyTasks from '@/components/modules/home/MyTasks.vue';
import QuickActions from '@/components/modules/home/QuickActions.vue';
import NoticeBoard from '@/components/modules/home/NoticeBoard.vue';
import TaskCalendarPanel from '@/components/modules/home/TaskCalendarPanel.vue';
</script>

<style lang="scss" scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 20px; // Space between major sections

  .home-row {
    margin-bottom: 20px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}
</style> 