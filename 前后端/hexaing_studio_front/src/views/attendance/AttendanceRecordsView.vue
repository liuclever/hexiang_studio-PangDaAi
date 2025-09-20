<template>
  <div class="attendance-records-container">
    <el-card shadow="hover" class="search-card">
      <!-- 搜索过滤区 -->
      <el-form :model="searchForm" inline label-width="80px" class="search-form">
        <el-form-item label="考勤类型">
          <el-select v-model="searchForm.type" clearable placeholder="选择考勤类型">
            <el-option label="活动考勤" value="activity"></el-option>
            <el-option label="课程考勤" value="course"></el-option>
            <el-option label="值班考勤" value="duty"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="考勤名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入考勤名称" clearable />
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="searchForm.studentName" placeholder="请输入学生姓名" clearable></el-input>
        </el-form-item>
        <el-form-item label="签到状态">
          <el-select v-model="searchForm.status" clearable placeholder="选择状态">
            <el-option label="待签到" value="pending"></el-option>
            <el-option label="已签到" value="present"></el-option>
            <el-option label="迟到" value="late"></el-option>
            <el-option label="缺勤" value="absent"></el-option>
            <el-option label="请假" value="leave"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchRecords">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 考勤记录列表 -->
    <el-card shadow="hover" class="records-card">
      <template #header>
        <div class="card-header">
          <span>考勤记录列表</span>
        </div>
      </template>

      <el-table :data="records" border stripe v-loading="loading"
        highlight-current-row
        :cell-style="{padding: '8px 12px'}"
        :row-style="{height: '55px'}"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold', padding: '12px'}"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="studentName" label="学生姓名" min-width="100" />
        <el-table-column prop="planName" label="任务名称" min-width="180" />
        <el-table-column prop="planType" label="考勤类型" width="120">
          <template #default="scope">
            {{ getTypeLabel(scope.row.planType) }}
          </template>
        </el-table-column>
        <el-table-column label="任务时间" width="170">
          <template #default="scope">
            {{ formatDateTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="考勤地点" min-width="150">
          <template #default="scope">
            {{ scope.row.location || scope.row.planLocation || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="签到状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="170">
          <template #default="scope">
            {{ scope.row.status !== 'pending' ? formatDateTime(scope.row.updateTime) : '未签到' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button type="primary" size="small" @click="viewRecordDetail(scope.row)">
                详情
              </el-button>
              <el-button type="warning" size="small" @click="editRecord(scope.row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定要删除此考勤记录吗？"
                @confirm="deleteRecord(scope.row.recordId)"
              >
                <template #reference>
                  <el-button type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:currentPage="currentPage"
          v-model:pageSize="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="考勤记录详情"
      width="600px"
    >
      <div v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ currentRecord.studentName }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ currentRecord.planName }}</el-descriptions-item>
          <el-descriptions-item label="考勤类型">
            <el-tag :type="getTypeTagType(currentRecord.planType)">
              {{ getTypeLabel(currentRecord.planType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务时间">{{ formatDateTime(currentRecord.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="签到状态">
            <el-tag :type="getStatusTagType(currentRecord.status)">
              {{ getStatusLabel(currentRecord.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="签到时间" v-if="currentRecord.status !== 'pending'">
            {{ formatDateTime(currentRecord.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="签到位置" v-if="currentRecord.location">
            {{ currentRecord.location }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ currentRecord.remark || '无' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="warning" @click="editRecord(currentRecord)">编辑</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑考勤记录"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
        v-loading="saveLoading"
      >
        <el-form-item label="学生姓名">
          <el-input v-model="editForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input v-model="editForm.planName" disabled />
        </el-form-item>
        <el-form-item label="签到状态" prop="status">
          <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="待签到" value="pending" />
            <el-option label="已签到" value="present" />
            <el-option label="迟到" value="late" />
            <el-option label="缺勤" value="absent" />
            <el-option label="请假" value="leave" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitEditForm" :loading="saveLoading">保存</el-button>
          <el-button @click="editDialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAttendanceRecords, updateAttendanceRecordStatus, deleteAttendanceRecord } from '@/api/attendance';

const route = useRoute();
const planId = ref(route.params.planId ? Number(route.params.planId) : null);
const planType = ref(route.query.planType || '');

// Utility function to convert object keys from snake_case to camelCase, recursive
const keysToCamelCase = (obj) => {
  if (Array.isArray(obj)) {
    return obj.map(v => keysToCamelCase(v));
  } else if (obj !== null && typeof obj === 'object' && obj.constructor === Object) {
    return Object.keys(obj).reduce((result, key) => {
      const camelKey = key.replace(/_([a-z])/g, g => g[1].toUpperCase());
      result[camelKey] = keysToCamelCase(obj[key]);
      return result;
    }, {});
  }
  return obj;
};

// 搜索表单
const searchForm = reactive({
  type: '', // 默认为空，显示所有类型
  keyword: '',
  studentName: '',
  status: '',
  timeRange: []
});

// 表格数据
const records = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 编辑对话框
const editDialogVisible = ref(false);
const editForm = reactive({
  recordId: 0,
  studentName: '',
  planName: '',
  status: '',
  remark: ''
});
const editRules = {
  status: [{ required: true, message: '请选择签到状态', trigger: 'change' }]
};
const editFormRef = ref();
const saveLoading = ref(false);

// 详情对话框
const detailDialogVisible = ref(false);
const currentRecord = ref(null);

// 获取考勤记录列表
const fetchRecords = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword || undefined,
      studentName: searchForm.studentName || undefined,
      status: searchForm.status || undefined,
      startDate: searchForm.timeRange?.[0] || undefined,
      endDate: searchForm.timeRange?.[1] || undefined
    };
    
    // 只有当type有值时才添加到参数中
    if (searchForm.type && searchForm.type.trim() !== '') {
      params.type = searchForm.type;
    }

    console.log('发送查询参数:', params); // 调试日志
    
    const res = await getAttendanceRecords(params);
    
    console.log('API响应:', res); // 调试日志
    
    if (res && res.code === 200) {
      // 确保records是一个数组
      if (Array.isArray(res.data.records)) {
        records.value = keysToCamelCase(res.data.records); // Use the utility function
        total.value = res.data.total || 0;
      } else {
        console.error('API返回的记录数据不是数组:', res.data);
        records.value = [];
        total.value = 0;
        ElMessage.error('获取考勤记录数据格式异常');
      }
      console.log('解析后的记录:', records.value); // 调试日志
    } else {
      ElMessage.error(res?.msg || '获取考勤记录失败');
      records.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取考勤记录失败:', error);
    ElMessage.error('获取考勤记录失败');
    records.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

// 搜索记录
const searchRecords = () => {
  currentPage.value = 1; // 重置页码
  fetchRecords();
};

// 重置搜索
const resetSearch = () => {
  // 重置搜索表单
  searchForm.type = '';
  searchForm.keyword = '';
  searchForm.studentName = '';
  searchForm.status = '';
  searchForm.timeRange = [];
  // 重新加载数据
  currentPage.value = 1;
  fetchRecords();
};

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchRecords();
};

// 页码变更
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchRecords();
};

// 查看记录详情
const viewRecordDetail = (record) => {
  currentRecord.value = { ...record };
  detailDialogVisible.value = true;
};

// 编辑记录
const editRecord = (record) => {
  // 关闭详情对话框
  detailDialogVisible.value = false;
  
  // 设置编辑表单数据
  editForm.recordId = record.recordId;
  editForm.studentName = record.studentName;
  editForm.planName = record.planName;
  editForm.status = record.status;
  editForm.remark = record.remark || '';
  
  // 显示编辑对话框
  editDialogVisible.value = true;
};

// 提交编辑表单
const submitEditForm = async () => {
  if (!editFormRef.value) return;
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true;
      try {
        const res = await updateAttendanceRecordStatus(
          editForm.recordId,
          editForm.status,
          editForm.remark
        );
        
        if (res && res.code === 200) {
          ElMessage.success('考勤记录更新成功');
          editDialogVisible.value = false;
          fetchRecords(); // 刷新列表
        } else {
          ElMessage.error(res?.msg || '更新考勤记录失败');
        }
      } catch (error) {
        console.error('更新考勤记录失败:', error);
        ElMessage.error('更新考勤记录失败');
      } finally {
        saveLoading.value = false;
      }
    }
  });
};

// 删除记录
const deleteRecord = async (recordId) => {
  try {
    const res = await deleteAttendanceRecord(recordId);
    
    if (res && res.code === 200) {
      ElMessage.success('考勤记录删除成功');
      fetchRecords(); // 刷新列表
    } else {
      ElMessage.error(res?.msg || '删除考勤记录失败');
    }
  } catch (error) {
    console.error('删除考勤记录失败:', error);
    ElMessage.error('删除考勤记录失败');
  }
};

// 格式化日期时间
const formatDateTime = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

// 获取考勤类型对应的标签类型
const getTypeTagType = (type) => {
  switch (type) {
    case 'activity': return 'success';
    case 'course': return 'primary';
    case 'duty': return 'warning';
    default: return 'info';
  }
};

// 获取考勤类型对应的标签文本
const getTypeLabel = (type) => {
  switch (type) {
    case 'activity': return '活动考勤';
    case 'course': return '课程考勤';
    case 'duty': return '值班考勤';
    default: return '未知';
  }
};

// 获取考勤状态对应的标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'present': return 'success';
    case 'late': return 'warning';
    case 'absent': return 'danger';
    case 'leave': return 'info';
    case 'pending': return 'primary';
    default: return 'default';
  }
};

// 获取考勤状态对应的标签文本
const getStatusLabel = (status) => {
  switch (status) {
    case 'present': return '已签到';
    case 'late': return '迟到';
    case 'absent': return '缺勤';
    case 'leave': return '请假';
    case 'pending': return '待签到';
    default: return '未知';
  }
};

// 页面加载时获取数据
onMounted(() => {
  fetchRecords();
});

// 导出给父组件调用的方法
defineExpose({
  fetchRecords
});
</script>

<style scoped>
.attendance-records-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.records-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 