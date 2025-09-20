<template>
  <div class="student-selector">
    <div class="selector-layout">
      <!-- 左侧：已选学生 -->
      <div class="selected-panel">
        <div class="panel-header">
          <span>已选学生 ({{ selectedStudents.length }})</span>
          <el-button size="small" type="danger" @click="clearAll" :disabled="selectedStudents.length === 0">
            清空
          </el-button>
        </div>
        <div class="student-list">
          <el-scrollbar height="300px">
            <div 
              v-for="student in selectedStudents" 
              :key="student.studentId"
              class="student-item selected-item"
              @click="removeStudent(student)"
            >
              <div class="student-info">
                <span class="student-name">{{ student.studentName }}</span>
                <span class="student-number">{{ student.studentNumber }}</span>
              </div>
              <el-icon class="remove-icon"><Close /></el-icon>
            </div>
            
            <div v-if="selectedStudents.length === 0" class="empty-tip">
              暂未选择学生
            </div>
          </el-scrollbar>
        </div>
      </div>

      <!-- 右侧：可选学生 -->
      <div class="available-panel">
        <div class="panel-header">
          <span>可选学生</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索学生姓名或学号"
            clearable
            size="small"
            style="width: 200px;"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="student-list">
          <el-scrollbar height="300px" v-loading="loading">
            <div 
              v-for="student in filteredAvailableStudents" 
              :key="student.studentId"
              class="student-item available-item"
              @click="addStudent(student)"
            >
              <div class="student-info">
                <span class="student-name">{{ student.studentName }}</span>
                <span class="student-number">{{ student.studentNumber }}</span>
              </div>
              <el-icon class="add-icon"><Plus /></el-icon>
            </div>
            
            <div v-if="filteredAvailableStudents.length === 0 && !loading" class="empty-tip">
              {{ searchKeyword ? '未找到匹配的学生' : '暂无可选学生' }}
            </div>
          </el-scrollbar>
        </div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="quick-actions">
      <el-button size="small" @click="selectAll">全选</el-button>
      <el-button size="small" @click="clearAll">清空</el-button>
      <span class="selection-summary">
        已选择 <strong>{{ selectedStudents.length }}</strong> 名学生
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Close } from '@element-plus/icons-vue'
import { getAllStudents } from '@/api/attendance'

// Props
const props = defineProps({
  selectedStudents: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits(['update:selected-students'])

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const allStudents = ref([])
const internalSelected = ref([...props.selectedStudents])

// 计算属性
const selectedStudentIds = computed(() => 
  new Set(internalSelected.value.map(s => s.studentId))
)

const filteredAvailableStudents = computed(() => {
  const keyword = searchKeyword.value.toLowerCase().trim()
  
  return allStudents.value.filter(student => {
    // 排除已选择的学生
    if (selectedStudentIds.value.has(student.studentId)) {
      return false
    }
    
    // 如果没有搜索关键词，显示所有未选择的学生
    if (!keyword) return true
    
    // 根据关键词过滤
    const name = (student.studentName || '').toLowerCase()
    const number = (student.studentNumber || '').toLowerCase()
    return name.includes(keyword) || number.includes(keyword)
  })
})

// 获取所有学生
const fetchAllStudents = async () => {
  loading.value = true
  try {
    const res = await getAllStudents()
    if (res && res.code === 200 && Array.isArray(res.data)) {
      allStudents.value = res.data.map(student => ({
        studentId: student.id || student.studentId,
        studentName: student.name || student.studentName,
        studentNumber: student.studentNumber || '',
        ...student
      }))
    } else {
      ElMessage.error('获取学生列表失败')
    }
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 方法
const addStudent = (student) => {
  if (!selectedStudentIds.value.has(student.studentId)) {
    internalSelected.value.push(student)
    emit('update:selected-students', [...internalSelected.value])
  }
}

const removeStudent = (student) => {
  const index = internalSelected.value.findIndex(s => s.studentId === student.studentId)
  if (index !== -1) {
    internalSelected.value.splice(index, 1)
    emit('update:selected-students', [...internalSelected.value])
  }
}

const selectAll = () => {
  // 添加所有未选择的可见学生
  const toAdd = filteredAvailableStudents.value.filter(student => 
    !selectedStudentIds.value.has(student.studentId)
  )
  
  if (toAdd.length > 0) {
    internalSelected.value.push(...toAdd)
    emit('update:selected-students', [...internalSelected.value])
    ElMessage.success(`已添加 ${toAdd.length} 名学生`)
  }
}

const clearAll = () => {
  internalSelected.value = []
  emit('update:selected-students', [])
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

// 监听外部props变化
watch(() => props.selectedStudents, (newVal) => {
  internalSelected.value = [...newVal]
}, { deep: true })

// 组件挂载时获取数据
onMounted(() => {
  fetchAllStudents()
})
</script>

<style scoped>
.student-selector {
  width: 100%;
}

.selector-layout {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.selected-panel,
.available-panel {
  flex: 1;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--el-color-primary-light-9);
  border-bottom: 1px solid var(--el-border-color-light);
  font-weight: 600;
  color: var(--el-color-primary);
}

.student-list {
  padding: 8px;
}

.student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.student-item:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.selected-item {
  background-color: var(--el-color-success-light-9);
  border-color: var(--el-color-success-light-5);
}

.selected-item:hover {
  background-color: var(--el-color-success-light-8);
  border-color: var(--el-color-success-light-3);
}

.available-item:hover {
  background-color: var(--el-color-primary-light-9);
}

.student-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.student-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
}

.student-number {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.add-icon,
.remove-icon {
  font-size: 16px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.student-item:hover .add-icon,
.student-item:hover .remove-icon {
  opacity: 1;
}

.add-icon {
  color: var(--el-color-primary);
}

.remove-icon {
  color: var(--el-color-danger);
}

.empty-tip {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 40px 20px;
  font-style: italic;
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: var(--el-color-info-light-9);
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
}

.selection-summary {
  margin-left: auto;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.selection-summary strong {
  color: var(--el-color-primary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .selector-layout {
    flex-direction: column;
  }
  
  .panel-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
}
</style> 