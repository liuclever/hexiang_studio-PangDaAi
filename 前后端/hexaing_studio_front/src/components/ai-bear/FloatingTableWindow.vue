<template>
  <div 
    v-show="visible"
    class="floating-table-window"
    :style="{
      left: position.x + 'px',
      top: position.y + 'px',
      width: size.width + 'px',
      height: size.height + 'px'
    }"
    @mousedown="startDrag"
  >
    <!-- 表格窗口头部 -->
    <div class="table-window-header">
      <div class="header-content">
        <span class="window-title">📊 {{ tableData?.title || '数据表格' }}</span>
        <div class="header-actions">
          <button @click="toggleMaximize" class="action-btn" title="最大化/还原">
            <span v-if="!isMaximized">🔲</span>
            <span v-else>🗗</span>
          </button>
          <button @click="handleClose" class="action-btn close-btn" title="关闭">
            ✕
          </button>
        </div>
      </div>
    </div>

    <!-- 表格内容区域 -->
    <div class="table-content" :class="{ 'maximized': isMaximized }">
      <AiTable v-if="tableData" :tableData="tableData" />
      <div v-else class="no-data">
        <p>暂无表格数据</p>
      </div>
    </div>

    <!-- 调整大小手柄 -->
    <div 
      v-show="!isMaximized"
      class="resize-handle" 
      @mousedown="startResize"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import AiTable from './AiTable.vue'

interface TableData {
  title: string
  columns: string[]
  rows: any[][]
  metadata?: {
    totalCount: number
    generateTime: string
    dataSource: string
    sortable: boolean
    exportable: boolean
  }
}

interface Props {
  visible: boolean
  tableData?: TableData
}

interface Emits {
  (e: 'close'): void
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 窗口状态
const isMaximized = ref(false)
const position = reactive({ x: window.innerWidth - 600, y: 100 })
const size = reactive({ width: 580, height: 400 })
const originalSize = reactive({ width: 580, height: 400 })
const originalPosition = reactive({ x: window.innerWidth - 600, y: 100 })

// 拖拽相关
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

// 调整大小相关
const isResizing = ref(false)
const resizeStart = reactive({ x: 0, y: 0 })

// 拖拽处理
const startDrag = (e: MouseEvent) => {
  if (e.target === e.currentTarget || (e.target as HTMLElement).closest('.table-window-header')) {
    isDragging.value = true
    dragStart.x = e.clientX - position.x
    dragStart.y = e.clientY - position.y
    
    document.addEventListener('mousemove', handleDrag)
    document.addEventListener('mouseup', stopDrag)
    e.preventDefault()
  }
}

const handleDrag = (e: MouseEvent) => {
  if (isDragging.value && !isMaximized.value) {
    position.x = e.clientX - dragStart.x
    position.y = e.clientY - dragStart.y
    
    // 边界约束
    position.x = Math.max(0, Math.min(window.innerWidth - size.width, position.x))
    position.y = Math.max(0, Math.min(window.innerHeight - size.height, position.y))
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 调整大小处理
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  resizeStart.x = e.clientX
  resizeStart.y = e.clientY
  
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
  e.stopPropagation()
}

const handleResize = (e: MouseEvent) => {
  if (isResizing.value) {
    const deltaX = e.clientX - resizeStart.x
    const deltaY = e.clientY - resizeStart.y
    
    size.width = Math.max(400, size.width + deltaX)
    size.height = Math.max(300, size.height + deltaY)
    
    resizeStart.x = e.clientX
    resizeStart.y = e.clientY
  }
}

const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

// 最大化/还原
const toggleMaximize = () => {
  if (isMaximized.value) {
    // 还原
    position.x = originalPosition.x
    position.y = originalPosition.y
    size.width = originalSize.width
    size.height = originalSize.height
    isMaximized.value = false
  } else {
    // 保存当前位置和大小
    originalPosition.x = position.x
    originalPosition.y = position.y
    originalSize.width = size.width
    originalSize.height = size.height
    
    // 最大化
    position.x = 0
    position.y = 0
    size.width = window.innerWidth
    size.height = window.innerHeight
    isMaximized.value = true
  }
}

// 关闭窗口
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}
</script>

<style scoped>
.floating-table-window {
  position: fixed;
  background: white;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
}

.floating-table-window:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.table-window-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 16px;
  cursor: move;
  user-select: none;
  border-bottom: 1px solid #e1e5e9;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.window-title {
  font-weight: 600;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: background-color 0.2s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.close-btn:hover {
  background: #f56c6c;
}

.table-content {
  flex: 1;
  overflow: auto;
  padding: 0;
}

.table-content.maximized {
  height: calc(100vh - 60px);
}

.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  cursor: nw-resize;
  background: linear-gradient(-45deg, transparent 0%, transparent 40%, #ccc 41%, #ccc 43%, transparent 44%, transparent 100%);
}

.resize-handle:hover {
  background: linear-gradient(-45deg, transparent 0%, transparent 40%, #999 41%, #999 43%, transparent 44%, transparent 100%);
}

/* 重写AiTable样式以适应浮动窗口 */
.floating-table-window :deep(.ai-table-container) {
  margin: 0;
  border: none;
  border-radius: 0;
}

.floating-table-window :deep(.table-header) {
  display: none; /* 隐藏AiTable自带的标题，使用窗口标题 */
}

.floating-table-window :deep(.ai-table) {
  font-size: 12px;
}

.floating-table-window :deep(.el-table .el-table__cell) {
  padding: 8px 12px;
}
</style> 