<template>
  <div class="duty-table-edit">
    <el-card shadow="hover">
      <template #header>
        <div class="edit-header">
          <div class="header-left">
            <span class="title">编辑值班表</span>
            <el-tag type="warning" size="small">编辑模式</el-tag>
          </div>
          <div class="header-actions">
            <el-button type="success" @click="saveAll" :loading="saving">
              <el-icon><Check /></el-icon> 保存全部
            </el-button>
            <el-button @click="$emit('cancel-edit')">
              <el-icon><Close /></el-icon> 取消
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="table-wrapper">
        <!-- 编辑提示 -->
        <el-alert
          title="编辑模式已激活"
          type="info"
          :closable="false"
          show-icon
          description="点击单元格可以编辑该时段的值班人员。未保存的修改会以橙色边框显示。"
          class="edit-alert"
        />

        <table class="duty-table edit-table" v-if="weekDays.length > 0">
          <thead>
            <tr>
              <th class="time-column">时间段</th>
              <th v-for="day in weekDays" :key="day.date" class="day-header">
                {{ getDayText(day.dayOfWeek) }}<br>
                {{ formatDate(day.date) }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="timeSlot in timeSlots" :key="timeSlot">
              <td class="time-cell">{{ timeSlot }}</td>
              <td v-for="day in weekDays" :key="`${day.date}-${timeSlot}`" 
                  class="duty-cell edit-cell"
                  :class="{ 'has-changes': hasChanges(day.date, timeSlot) }"
                  @click="openEditDialog(day.date, timeSlot)">
                <div class="edit-cell-content">
                  <!-- 学生标签 -->
                  <div class="students-container">
                    <div v-for="student in getStudentsForCell(day.date, timeSlot)" 
                         :key="student.studentId"
                         class="student-tag editable"
                         :class="getStatusClass(student.status)">
                      {{ student.studentName }}
                    </div>
                    <span v-if="getStudentsForCell(day.date, timeSlot).length === 0" 
                          class="no-students">
                      点击添加值班人员
                    </span>
                  </div>
                  
                  <!-- 编辑按钮 -->
                  <div class="edit-overlay">
                    <el-icon class="edit-icon"><Edit /></el-icon>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div v-else class="empty-state">
          <el-empty description="暂无值班数据" />
        </div>
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="`编辑值班人员 - ${formatEditTitle()}`"
      width="800px"
      :close-on-click-modal="false"
    >
      <StudentSelector
        v-if="editDialogVisible"
        :selected-students="currentStudents"
        @update:selected-students="handleStudentsChange"
      />
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCurrentEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Edit } from '@element-plus/icons-vue'
import StudentSelector from './StudentSelector.vue'

// Props
const props = defineProps({
  weekDays: {
    type: Array,
    default: () => []
  },
  timeSlots: {
    type: Array,
    default: () => []
  },
  dutyData: {
    type: Array,
    default: () => []
  },
  attendanceData: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['save-all', 'cancel-edit'])

// 响应式数据
const saving = ref(false)
const editDialogVisible = ref(false)
const editChanges = ref(new Map()) // 存储编辑变更

// 当前编辑的信息
const currentEditDate = ref('')
const currentEditTimeSlot = ref('')
const currentStudents = ref([])

// 工具方法
const getDayText = (dayOfWeek) => {
  const days = ['周一', '周二', '周三', '周四', '周五']
  return days[dayOfWeek - 1] || ''
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatEditTitle = () => {
  if (!currentEditDate.value || !currentEditTimeSlot.value) return ''
  const date = new Date(currentEditDate.value)
  const dayText = getDayText(date.getDay() || 7)
  return `${formatDate(currentEditDate.value)} ${dayText} ${currentEditTimeSlot.value}`
}

// 获取指定单元格的学生数据 - 适配简化版后端数据格式
const getStudentsForCell = (date, timeSlot) => {
  const changeKey = `${date}_${timeSlot}`
  
  // 如果有编辑变更，返回变更后的数据
  if (editChanges.value.has(changeKey)) {
    return editChanges.value.get(changeKey)
  }
  
  // 🔧 适配后端扁平化数据格式（和DutyTableView保持一致）
  if (!props.dutyData || props.dutyData.length === 0) {
    return []
  }
  
  const matchingDuties = props.dutyData.filter(duty => {
    const dutyDate = duty.duty_date || duty.date || duty.dutyDate
    const dutyTimeSlot = duty.time_slot || duty.timeSlot || duty.time || `${duty.start_time_str}-${duty.end_time_str}`
    return dutyDate === date && dutyTimeSlot === timeSlot
  })
  
  if (matchingDuties.length === 0) return []
  
  // 收集所有学生
  const allStudents = []
  matchingDuties.forEach(duty => {
    if (duty.students && Array.isArray(duty.students)) {
      // 统一字段名称，适配后端数据格式
      const formattedStudents = duty.students.map(student => ({
        studentId: student.student_id || student.studentId,
        studentName: student.student_name || student.studentName,
        studentNumber: student.student_number || student.studentNumber,
        ...student
      }))
      allStudents.push(...formattedStudents)
    }
  })
  
  console.log(`🔧 [编辑模式] [${date} ${timeSlot}] 找到学生:`, allStudents.length, '名')
  
  return allStudents
}

// 检查是否有未保存的变更
const hasChanges = (date, timeSlot) => {
  const changeKey = `${date}_${timeSlot}`
  return editChanges.value.has(changeKey)
}

// 获取状态对应的CSS类
const getStatusClass = (status) => {
  const statusMap = {
    'present': 'status-present',
    'late': 'status-late',
    'absent': 'status-absent', 
    'leave': 'status-leave',
    'pending': 'status-pending',
    'pending_activation': 'status-pending-activation'
  }
  return statusMap[status] || 'status-normal'
}

// 打开编辑对话框
const openEditDialog = (date, timeSlot) => {
  currentEditDate.value = date
  currentEditTimeSlot.value = timeSlot
  currentStudents.value = [...getStudentsForCell(date, timeSlot)]
  editDialogVisible.value = true
}

// 处理学生变更
const handleStudentsChange = (students) => {
  currentStudents.value = students
}

// 保存当前编辑
const saveCurrentEdit = () => {
  const changeKey = `${currentEditDate.value}_${currentEditTimeSlot.value}`
  editChanges.value.set(changeKey, [...currentStudents.value])
  editDialogVisible.value = false
  ElMessage.success('修改已暂存，点击"保存全部"提交到服务器')
}

// 保存所有变更
const saveAll = async () => {
  if (editChanges.value.size === 0) {
    ElMessage.warning('没有需要保存的修改')
    return
  }
  
  saving.value = true
  try {
    // 构建保存数据
    const saveData = []
    editChanges.value.forEach((students, key) => {
      const [date, timeSlot] = key.split('_')
      saveData.push({
        dutyDate: date,
        timeSlot: timeSlot,
        studentIds: students.map(s => s.studentId),
        location: '工作室', // 默认位置
        dutyName: `${timeSlot} 值班` // 🔧 添加值班名称
      })
    })
    
    emit('save-all', saveData)
    editChanges.value.clear()
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

// 导出方法
defineExpose({
  hasUnsavedChanges: () => editChanges.value.size > 0,
  clearChanges: () => editChanges.value.clear()
})
</script>

<style scoped>
.duty-table-edit {
  width: 100%;
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.table-wrapper {
  margin: 16px 0;
}

.edit-alert {
  margin-bottom: 16px;
}

.edit-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.edit-table th,
.edit-table td {
  border: 1px solid var(--el-border-color-light);
  padding: 12px 8px;
  text-align: center;
}

.edit-table th {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 14px;
}

.time-column {
  width: 100px;
}

.day-header {
  min-width: 180px;
}

.time-cell {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
  white-space: nowrap;
}

.edit-cell {
  vertical-align: top;
  background-color: #fff;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
}

.edit-cell:hover {
  background-color: var(--el-color-primary-light-9);
  transform: scale(1.02);
}

.edit-cell.has-changes {
  border: 2px solid var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.edit-cell-content {
  position: relative;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px;
}

.students-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  align-items: center;
  min-height: 60px;
}

.student-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  transition: all 0.2s;
}

.student-tag.editable:hover {
  transform: scale(1.05);
}

.no-students {
  color: var(--el-text-color-secondary);
  font-style: italic;
  font-size: 12px;
}

.edit-overlay {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.edit-cell:hover .edit-overlay {
  opacity: 1;
}

.edit-icon {
  font-size: 14px;
  color: var(--el-color-primary);
}

/* 状态样式 */
.status-present {
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success);
  border: 1px solid var(--el-color-success-light-5);
}

.status-pending {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border: 1px solid var(--el-color-primary-light-5);
}

.status-late {
  background-color: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  border: 1px solid var(--el-color-warning-light-5);
}

.status-absent {
  background-color: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  border: 1px solid var(--el-color-danger-light-5);
}

.status-leave {
  background-color: #e0f2f1;
  color: #00796b;
  border: 1px solid #b2dfdb;
}

.status-pending-activation {
  background-color: #f4f4f5;
  color: #909399;
  border: 1px dashed #dcdfe6;
  font-style: italic;
}

.status-normal {
  background-color: var(--el-color-info-light-9);
  color: var(--el-color-info);
  border: 1px solid var(--el-color-info-light-5);
}

.empty-state {
  padding: 40px 0;
}
</style> 