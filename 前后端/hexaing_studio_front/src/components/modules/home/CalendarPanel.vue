<template>
  <el-card shadow="hover" class="calendar-panel-card">
    <template #header>
      <span>日程安排</span>
    </template>
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <el-calendar v-model="selectedDate" class="custom-calendar">
          <template #date-cell="{ data }">
            <p :class="data.isSelected ? 'is-selected' : ''">
              {{ data.day.split('-').slice(2).join('-') }}
              <span v-if="hasEvent(data.day)" class="event-dot"></span>
            </p>
          </template>
        </el-calendar>
      </el-col>
      <el-col :xs="24" :md="8" class="reminders-section">
        <h4>今日提醒 ({{ formattedSelectedDate }})</h4>
        <el-scrollbar height="250px">
          <div v-if="todayEvents.length === 0" class="empty-reminder">今日无安排</div>
          <div v-for="event in todayEvents" :key="event.id" class="reminder-item">
            <strong>{{ event.title }}</strong>
            <p>{{ event.time }}</p>
          </div>
        </el-scrollbar>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

const selectedDate = ref(new Date());

interface Event {
  id: number;
  title: string;
  time: string;
}

interface EventMap {
  [date: string]: Event[];
}

// 示例日程数据
const events = ref<EventMap>({
  [formatDate(new Date())]: [ // 今天的事件
    { id: 1, title: '团队会议', time: '10:00' },
    { id: 2, title: '项目截止日期', time: '17:00' },
  ],
  // 添加更多日期和事件
  // '2025-06-10': [{ id: 3, title: '客户电话', time: '14:00' }]
});

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const hasEvent = (day: string): boolean => {
  return !!events.value[day];
};

const formattedSelectedDate = computed(() => {
  return formatDate(selectedDate.value);
});

const todayEvents = computed(() => {
  return events.value[formatDate(selectedDate.value)] || [];
});

watch(selectedDate, (newDate) => {
  console.log('选择的日期:', formatDate(newDate));
  // 如果需要，可以在这里获取所选日期的事件
});
</script>

<style lang="scss" scoped>
.calendar-panel-card {
  border-radius: var(--el-border-radius-base);
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 12px rgba(0,0,0,0.1);
  }

  .custom-calendar {
    border-radius: var(--el-border-radius-small);
    
    :deep(.el-calendar-day) {
      height: auto;
      min-height: 60px; // 调整单元格高度
      padding: 6px;
    }
    
    .is-selected {
      color: var(--el-color-primary);
      font-weight: bold;
    }
    
    .event-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: var(--el-color-danger);
      margin-left: 4px;
      vertical-align: middle;
    }
  }

  .reminders-section {
    h4 {
      margin-top: 0;
      margin-bottom: 10px;
      font-size: 16px;
      color: var(--el-text-color-primary);
    }
    
    .empty-reminder {
      color: var(--el-text-color-placeholder);
      font-size: 14px;
      text-align: center;
      padding: 20px 0;
    }
    
    .reminder-item {
      padding: 8px 0;
      border-bottom: 1px solid var(--el-border-color-lighter);
      
      &:last-child {
        border-bottom: none;
      }
      
      strong {
        display: block;
        font-size: 14px;
        color: var(--el-text-color-regular);
      }
      
      p {
        font-size: 12px;
        color: var(--el-text-color-secondary);
        margin: 4px 0 0;
      }
    }
  }
}
</style> 