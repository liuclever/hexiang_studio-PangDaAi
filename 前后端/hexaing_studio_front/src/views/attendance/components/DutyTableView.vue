<template>
  <div class="duty-table-view">
    <el-card shadow="hover">
      <template #header>
        <div class="table-header">
          <span class="title">值班表</span>
          <el-button type="primary" @click="$emit('enter-edit')" v-if="canEdit">
            <el-icon><Edit /></el-icon> 编辑值班表
          </el-button>
        </div>
      </template>

      <div v-loading="loading" class="table-wrapper">
        <table class="duty-table" v-if="weekDays.length > 0">
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
              <td v-for="day in weekDays" :key="`${day.date}-${timeSlot}`" class="duty-cell">
                <div class="students-container">
                  <div v-for="student in getStudentsForCell(day.date, timeSlot)" 
                       :key="student.studentId"
                       class="student-tag"
                       :class="getStatusClass(student.status)">
                    {{ student.studentName }}
                  </div>
                  <span v-if="getStudentsForCell(day.date, timeSlot).length === 0" 
                        class="no-students">
                    无值班人员
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div v-else class="empty-state">
          <el-empty description="暂无值班数据" />
        </div>
      </div>

      <!-- 状态图例 -->
      <div class="legend">
        <span class="legend-title">状态图例：</span>
        <span class="legend-item">
          <span class="legend-marker present"></span> 已签到
        </span>
        <span class="legend-item">
          <span class="legend-marker pending"></span> 待签到
        </span>
        <span class="legend-item">
          <span class="legend-marker late"></span> 迟到
        </span>
        <span class="legend-item">
          <span class="legend-marker absent"></span> 缺勤
        </span>
        <span class="legend-item">
          <span class="legend-marker leave"></span> 请假
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Edit } from '@element-plus/icons-vue'

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
  },
  canEdit: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['enter-edit'])

// 工具方法
const getDayText = (dayOfWeek) => {
  const days = ['周一', '周二', '周三', '周四', '周五']
  return days[dayOfWeek - 1] || ''
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 获取指定单元格的学生数据 - 适配简化版后端数据格式  
const getStudentsForCell = (date, timeSlot) => {
  // 基础检查
  if (!props.dutyData || props.dutyData.length === 0) {
    return []
  }
  
  // 🔧 适配后端直接返回的扁平化数据格式
  const matchingDuties = props.dutyData.filter(duty => {
    // 尝试多种可能的字段名
    const dutyDate = duty.duty_date || duty.date || duty.dutyDate
    const dutyTimeSlot = duty.time_slot || duty.timeSlot || duty.time || `${duty.start_time_str}-${duty.end_time_str}`
    
    return dutyDate === date && dutyTimeSlot === timeSlot
  })
  
  if (matchingDuties.length === 0) return []
  
  // 收集所有学生
  const allStudents = []
  matchingDuties.forEach(duty => {
    if (duty.students && Array.isArray(duty.students)) {
      allStudents.push(...duty.students)
    }
  })
  
  // 合并考勤状态
  const studentsWithStatus = allStudents.map(student => {
    const statusRecord = props.attendanceData.find(record => 
      record.studentId === student.student_id && 
      record.dutyDate === date && 
      record.timeSlot === timeSlot
    )
    
    return {
      // 🔧 统一字段名称，适配后端数据格式
      studentId: student.student_id || student.studentId,
      studentName: student.student_name || student.studentName,
      studentNumber: student.student_number || student.studentNumber,
      status: statusRecord?.attendanceStatus || student.status || 'pending',
      ...student  // 保留其他字段
    }
  })
  
  // 简化日志，只在有学生时输出
  if (studentsWithStatus.length > 0) {
    console.log(`[${date} ${timeSlot}] 找到 ${studentsWithStatus.length} 名学生`)
  }
  
  return studentsWithStatus
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
</script>

<style scoped>
.duty-table-view {
  width: 100%;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.table-wrapper {
  margin: 16px 0;
}

.duty-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.duty-table th,
.duty-table td {
  border: 1px solid var(--el-border-color-light);
  padding: 12px 8px;
  text-align: center;
}

.duty-table th {
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

.duty-cell {
  vertical-align: top;
  background-color: #fff;
  transition: background-color 0.2s;
}

.duty-cell:hover {
  background-color: var(--el-color-primary-light-9);
}

.students-container {
  min-height: 60px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  align-items: center;
  padding: 8px;
}

.student-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  transition: all 0.2s;
}

.no-students {
  color: var(--el-text-color-secondary);
  font-style: italic;
  font-size: 12px;
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

/* 图例样式 */
.legend {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-light);
  background-color: var(--el-color-primary-light-9);
  font-size: 12px;
}

.legend-title {
  font-weight: 600;
  color: var(--el-color-primary);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-marker {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-marker.present {
  background-color: var(--el-color-success-light-8);
  border: 1px solid var(--el-color-success-light-5);
}

.legend-marker.pending {
  background-color: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-5);
}

.legend-marker.late {
  background-color: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-5);
}

.legend-marker.absent {
  background-color: var(--el-color-danger-light-9);
  border: 1px solid var(--el-color-danger-light-5);
}

.legend-marker.leave {
  background-color: #e0f2f1;
  border: 1px solid #b2dfdb;
}

.empty-state {
  padding: 40px 0;
}
</style> 