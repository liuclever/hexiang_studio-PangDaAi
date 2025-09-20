<template>
  <div class="week-navigation">
    <div class="week-selector">
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        format="YYYY-MM-DD"
        @change="handleDateChange"
      />
      
      <el-button-group class="week-nav">
        <el-button @click="previousWeek">
          <el-icon><ArrowLeft /></el-icon> 上周
        </el-button>
        <el-button @click="currentWeek">本周</el-button>
        <el-button @click="nextWeek">
          下周 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-button-group>
    </div>
    
    <div class="week-info">
      <span class="week-range">{{ weekRangeText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  modelValue: {
    type: Date,
    default: () => new Date()
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'week-change'])

// 响应式数据
const selectedDate = ref(props.modelValue)

// 计算工作日的起止日期文本（周一到周五）
const weekRangeText = computed(() => {
  const date = new Date(selectedDate.value)
  const dayOfWeek = date.getDay() || 7 // 0表示周日，转为7
  
  // 计算本周一
  const monday = new Date(date)
  monday.setDate(date.getDate() - dayOfWeek + 1)
  
  // 计算本周五
  const friday = new Date(monday)
  friday.setDate(monday.getDate() + 4)
  
  const formatDate = (d) => {
    return `${d.getMonth() + 1}月${d.getDate()}日`
  }
  
  return `${formatDate(monday)} - ${formatDate(friday)} (工作日)`
})

// 监听选中日期变化
watch(() => props.modelValue, (newVal) => {
  selectedDate.value = newVal
})

watch(selectedDate, (newVal) => {
  emit('update:modelValue', newVal)
  emit('week-change', newVal)
})

// 方法
const handleDateChange = (date) => {
  selectedDate.value = date
}

const previousWeek = () => {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() - 7)
  selectedDate.value = date
}

const nextWeek = () => {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() + 7)
  selectedDate.value = date
}

const currentWeek = () => {
  selectedDate.value = new Date()
}
</script>

<style scoped>
.week-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.week-selector {
  display: flex;
  align-items: center;
  gap: 16px;
}

.week-nav {
  margin-left: 16px;
}

.week-info {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.week-range {
  font-weight: 500;
}
</style> 