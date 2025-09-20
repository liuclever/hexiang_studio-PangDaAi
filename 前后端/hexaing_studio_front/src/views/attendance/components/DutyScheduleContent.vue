<template>
  <div class="duty-schedule-simplified">
    <!-- 周导航 -->
    <WeekNavigation 
              v-model="selectedDate"
    />
    
                    <!-- 查看模式 -->
    <DutyTableView
      v-if="!isEditMode"
      :week-days="weekDays"
      :time-slots="timeSlots"
      :duty-data="dutyData"
      :attendance-data="attendanceData"
      :loading="loading"
      :can-edit="canEditCurrentWeek"
      @enter-edit="enterEditMode"
    />
                    
                    <!-- 编辑模式 -->
    <DutyTableEdit
      v-else-if="isEditMode"
      :week-days="weekDays"
      :time-slots="timeSlots"
      :duty-data="dutyData"
      :attendance-data="attendanceData"
      :loading="loading"
      @cancel-edit="handleCancelEdit"
      @save-all="handleSaveChanges"
    />
    
    <!-- 🔧 未来周提示 -->
    <el-alert
      v-if="!canEditCurrentWeek"
      title="未来周只能查看"
      type="info"
      :closable="false"
      show-icon
      description="为避免数据冲突，未来周的值班表只能查看，不能编辑。请返回当前周进行编辑操作。"
      class="future-week-notice"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WeekNavigation from './WeekNavigation.vue'
import DutyTableView from './DutyTableView.vue'
import DutyTableEdit from './DutyTableEdit.vue'
import { batchSyncDutySchedules } from '@/api/attendance'
import request from '@/utils/request'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const isEditMode = ref(false)
const selectedDate = ref(new Date())
const dutyData = ref([])
const attendanceData = ref([])

// 固定的时间段
const timeSlots = ref([
  '08:30-10:00',
  '10:20-11:50', 
  '14:00-15:30',
  '15:50-17:20',
  '18:30-20:00'
])

// 计算工作日（周一到周五）
const weekDays = computed(() => {
  const days = []
  const date = new Date(selectedDate.value)
  const dayOfWeek = date.getDay()
  const startDate = new Date(date)
  startDate.setDate(date.getDate() - dayOfWeek + 1) // 调整到周一

  // 只生成5天工作日（周一到周五）
  for (let i = 0; i < 5; i++) {
    const currentDate = new Date(startDate)
    currentDate.setDate(startDate.getDate() + i)
    days.push({
      date: currentDate.toISOString().split('T')[0],
      dayOfWeek: i + 1  // 1=周一, 2=周二, ..., 5=周五
    })
  }
  return days
})

// 🔧 新增：判断当前查看的周是否允许编辑 (测试阶段注释掉，允许编辑所有周)
const canEditCurrentWeek = computed(() => {
  // 测试阶段：允许编辑所有周
  return true
  
  /*
  const now = new Date()
  const currentDate = new Date(selectedDate.value)
  
  // 计算当前周的周一
  const currentDayOfWeek = currentDate.getDay() || 7
  const currentWeekMonday = new Date(currentDate)
  currentWeekMonday.setDate(currentDate.getDate() - currentDayOfWeek + 1)
  currentWeekMonday.setHours(0, 0, 0, 0)
  
  // 计算今天所在周的周一
  const todayDayOfWeek = now.getDay() || 7
  const todayWeekMonday = new Date(now)
  todayWeekMonday.setDate(now.getDate() - todayDayOfWeek + 1)
  todayWeekMonday.setHours(0, 0, 0, 0)
  
  // 只允许编辑当前周和过去的周，禁止编辑未来的周
  return currentWeekMonday.getTime() <= todayWeekMonday.getTime()
  */
})



// 监听日期变化，自动获取数据
watch(selectedDate, async (newDate) => {
  if (newDate) {
    // 🔧 如果切换到未来周且正在编辑模式，自动退出编辑 (测试阶段注释掉)
    /*
    if (!canEditCurrentWeek.value && isEditMode.value) {
      isEditMode.value = false
      ElMessage.info('已切换到未来周，自动退出编辑模式')
    }
    */
    await fetchDutyData()
  }
}, { immediate: false })

// 获取值班数据
const fetchDutyData = async () => {
  loading.value = true
  try {
    const startDate = weekDays.value[0]?.date  // 周一
    const endDate = weekDays.value[4]?.date    // 周五
    
    if (!startDate || !endDate) return
    
    // 调用后端统一接口获取值班表数据
    const res = await request.get('/admin/duty-schedule/weekly-table', {
      params: { startDate, endDate }
    })
    
    if (res.code === 200 && res.data) {
      // 解析后端返回的综合数据
      dutyData.value = res.data.tableData?.dutyData || []  // 🔧 修复：获取实际的值班数据
      attendanceData.value = res.data.statusData || []
      
      // 可选：获取时间段信息
      if (res.data.structure?.timeSlots) {
        timeSlots.value = res.data.structure.timeSlots
      }
      
      console.log('获取到值班数据:', dutyData.value.length, '条')
    }
    
  } catch (error) {
    console.error('获取值班数据失败:', error)
    ElMessage.error('获取值班数据失败')
  } finally {
    loading.value = false
  }
}

// 进入编辑模式
const enterEditMode = () => {
  // 🔧 检查是否允许编辑 (测试阶段注释掉)
  /*
  if (!canEditCurrentWeek.value) {
    ElMessage.warning('未来周的值班表不能编辑，只能查看')
    return
  }
  */
  isEditMode.value = true
}

// 处理编辑取消
const handleCancelEdit = () => {
  ElMessageBox.confirm('确定要取消编辑吗？未保存的修改将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isEditMode.value = false
  }).catch(() => {})
}

// 处理保存修改
const handleSaveChanges = async (changes) => {
  saving.value = true
  try {
    if (changes && changes.length > 0) {
      const response = await batchSyncDutySchedules(changes)
      
      // 🔧 处理服务器返回的详细信息
      if (response && response.data) {
        const { successCount, skippedCount, message, skippedReasons } = response.data
        
        if (skippedCount > 0) {
          // 有跳过的记录，显示警告信息
          ElMessage.warning({
            message: message,
            duration: 5000
          })
          
          // 如果有具体的跳过原因，在控制台显示
          if (skippedReasons && skippedReasons.length > 0) {
            console.warn('跳过的记录详情:', skippedReasons)
          }
        } else {
          // 全部成功
          ElMessage.success(message || '保存成功')
        }
      } else {
      ElMessage.success('保存成功')
      }
      
      await fetchDutyData()
    }
    isEditMode.value = false
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}
      
// 组件挂载时获取数据
onMounted(() => {
  fetchDutyData()
})
    

    
// 导出方法供父组件调用
defineExpose({
  fetchDutyData
})
</script>

<style scoped>
.duty-schedule-simplified {
  width: 100%;
}

.future-week-notice {
  margin: 16px 0;
}
</style> 