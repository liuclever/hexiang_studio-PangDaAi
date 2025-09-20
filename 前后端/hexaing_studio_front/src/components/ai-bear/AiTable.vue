<template>
  <div class="ai-table-container">
    <div class="table-header">
      <h3 class="table-title">{{ tableData.title }}</h3>
      <div class="table-actions">
        <el-button v-if="tableData.metadata?.sortable" @click="toggleSort" size="small" type="primary">
          排序
        </el-button>
        <el-button v-if="tableData.metadata?.exportable" @click="exportTable" size="small" type="success">
          导出
        </el-button>
      </div>
    </div>
    
    <div class="table-meta" v-if="tableData.metadata">
      <span>数据来源: {{ tableData.metadata.dataSource }}</span>
      <span>总计: {{ tableData.metadata.totalCount }} 条</span>
      <span>生成时间: {{ tableData.metadata.generateTime }}</span>
    </div>
    
    <el-table 
      :data="formattedRows" 
      style="width: 100%" 
      stripe
      :default-sort="sortConfig"
      @sort-change="handleSortChange"
      class="ai-table"
    >
      <el-table-column
        v-for="(col, index) in tableData.columns"
        :key="index"
        :label="col"
        :prop="'col' + index"
        :sortable="tableData.metadata?.sortable"
        :width="getColumnWidth(col, index)"
      >
        <template #default="scope">
          <span :class="getCellClass(scope.row['col' + index], col)">
            {{ formatCellValue(scope.row['col' + index], col) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  tableData: {
    type: Object,
    required: true,
    validator(value) {
      return value.columns && value.rows && value.title
    }
  }
})

const sortConfig = ref({})

// 把 rows 转换为对象数组，ElementPlus 需要 key:value 格式
const formattedRows = computed(() => {
  return props.tableData.rows.map(row => {
    const obj = {}
    row.forEach((val, i) => {
      obj['col' + i] = val
    })
    return obj
  })
})

// 获取列宽度
const getColumnWidth = (colName, index) => {
  if (colName === '序号') return '80px'
  if (colName.includes('时间') || colName.includes('日期')) return '120px'
  if (colName.includes('电话') || colName.includes('邮箱')) return '140px'
  if (index === 0) return '100px'
  return 'auto'
}

// 格式化单元格值
const formatCellValue = (value, colName) => {
  if (value === null || value === undefined) return '-'
  if (colName.includes('率') && typeof value === 'string' && value.includes('%')) {
    return value
  }
  if (typeof value === 'number' && colName.includes('数')) {
    return value.toLocaleString()
  }
  return value
}

// 获取单元格样式类
const getCellClass = (value, colName) => {
  if (colName.includes('率') && typeof value === 'string') {
    const rate = parseFloat(value.replace('%', ''))
    if (rate >= 95) return 'high-rate'
    if (rate >= 80) return 'medium-rate'
    if (rate < 80) return 'low-rate'
  }
  if (colName === '状态') {
    if (value === '已发布') return 'status-active'
    if (value === '已下架') return 'status-inactive'
    if (value === '草稿') return 'status-draft'
  }
  return ''
}

// 排序切换
const toggleSort = () => {
  ElMessage.info('排序功能')
}

// 处理排序变化
const handleSortChange = (sortInfo) => {
  console.log('排序变化:', sortInfo)
}

// 导出表格
const exportTable = () => {
  // 简单的导出为CSV
  const csvContent = generateCSV()
  downloadCSV(csvContent, `${props.tableData.title}.csv`)
  ElMessage.success('表格已导出')
}

// 生成CSV内容
const generateCSV = () => {
  const headers = props.tableData.columns.join(',')
  const rows = props.tableData.rows.map(row => 
    row.map(cell => `"${cell}"`).join(',')
  ).join('\n')
  return headers + '\n' + rows
}

// 下载CSV文件
const downloadCSV = (content, filename) => {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.ai-table-container {
  margin: 20px 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.table-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.table-meta {
  padding: 8px 20px;
  background: #f0f9ff;
  color: #606266;
  font-size: 12px;
  display: flex;
  gap: 20px;
}

.ai-table {
  --el-table-header-bg-color: #f5f7fa;
}

/* 状态样式 */
.status-active {
  color: #67c23a;
  font-weight: 500;
}

.status-inactive {
  color: #f56c6c;
  font-weight: 500;
}

.status-draft {
  color: #909399;
  font-weight: 500;
}

/* 比率样式 */
.high-rate {
  color: #67c23a;
  font-weight: 600;
}

.medium-rate {
  color: #e6a23c;
  font-weight: 500;
}

.low-rate {
  color: #f56c6c;
  font-weight: 600;
}
</style> 