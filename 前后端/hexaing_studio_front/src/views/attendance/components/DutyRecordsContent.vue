<template>
  <div class="duty-records-content">
    <!-- 值班考勤计划列表 -->
    <el-card shadow="hover" class="attendance-list-card">
      <template #header>
        <div class="card-header">
          <div class="search-form">
            <el-form :inline="true" :model="searchForm">
              <el-form-item label="考勤名称">
                <el-input v-model="searchForm.keyword" placeholder="输入名称关键词" clearable></el-input>
              </el-form-item>
              <el-form-item label="签到状态">
                <el-select v-model="searchForm.status" clearable placeholder="选择状态">
                  <el-option value="present" label="已签到"></el-option>
                  <el-option value="late" label="迟到"></el-option>
                  <el-option value="absent" label="缺席"></el-option>
                  <el-option value="leave" label="请假"></el-option>
                  <el-option value="pending" label="待签到"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  @change="handleDateRangeChange"
                ></el-date-picker>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearch">
                  <el-icon><Search /></el-icon> 搜索
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>

      <!-- 值班考勤记录表格 -->
      <el-table v-loading="tableLoading" :data="attendanceRecords" stripe style="width: 100%">
        <el-table-column label="名字" min-width="120">
          <template #default="scope">
            {{ scope.row.studentName }}
          </template>
        </el-table-column>
        <el-table-column label="考勤任务" min-width="180">
          <template #default="scope">
            {{ scope.row.planName }}
          </template>
        </el-table-column>
        <el-table-column label="考勤时间" min-width="180">
          <template #default="scope">
            <div>起：{{ formatDateTime(scope.row.startTime) }}</div>
            <div>止：{{ formatDateTime(scope.row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="考勤地点" min-width="150">
          <template #default="scope">
            {{ scope.row.planLocation }}
          </template>
        </el-table-column>
        <el-table-column label="签到状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" min-width="180">
          <template #default="scope">
            {{ scope.row.signInTime ? formatDateTime(scope.row.signInTime) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              @click="updateRecordStatus(scope.row)"
            >
              修改状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="searchForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="searchForm.page"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 修改记录状态对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      title="修改考勤状态"
      width="500px"
    >
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="学生">
          <span>{{ selectedRecord?.studentName }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(selectedRecord?.status)">
            {{ getStatusText(selectedRecord?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status">
            <el-option value="present" label="已签到"></el-option>
            <el-option value="late" label="迟到"></el-option>
            <el-option value="absent" label="缺席"></el-option>
            <el-option value="leave" label="请假"></el-option>
            <el-option value="pending" label="待签到"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="statusForm.remark" type="textarea" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="statusDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStatusChange" :loading="statusUpdateLoading">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { getAttendanceRecords, updateAttendanceRecordStatus as apiUpdateStatus } from '@/api/attendance';
import { formatDateTime } from '@/utils/date';

const tableLoading = ref(false);
const statusUpdateLoading = ref(false);
const attendanceRecords = ref([]);
const total = ref(0);

const searchForm = reactive({
  page: 1,
  pageSize: 10,
  type: 'duty', // 固定查询值班类型
  keyword: '', // 对应plan_name
  status: '', // 对应record.status
  startDate: '',
  endDate: ''
});
const dateRange = ref([]);

const statusDialogVisible = ref(false);
const selectedRecord = ref(null);
const statusForm = reactive({
  status: '',
  remark: ''
});

// 获取值班考勤记录列表
const fetchRecords = async () => {
  tableLoading.value = true;
  try {
    // 准备查询参数，直接使用 searchForm 的值
    const queryParams = {
      page: searchForm.page,
      pageSize: searchForm.pageSize,
      type: 'duty', // 值班记录页面，类型固定为 'duty'
      keyword: searchForm.keyword,
      status: searchForm.status,
      startDate: searchForm.startDate,
      endDate: searchForm.endDate
    };
    
    console.log('发送查询参数:', queryParams); // 调试日志
    
    // 调用API
    const res = await getAttendanceRecords(queryParams);

    console.log('API响应:', res); // 调试日志

    if (res && res.code === 200) {
      attendanceRecords.value = res.data.records || [];
      total.value = res.data.total || 0;
      console.log('解析后的记录:', attendanceRecords.value); // 调试日志
    } else {
      ElMessage.error(res?.msg || '获取值班记录失败');
    }
  } catch (error) {
    console.error('获取值班记录失败:', error);
    ElMessage.error('获取值班记录失败，请检查网络或联系管理员');
  } finally {
    tableLoading.value = false;
  }
};

onMounted(() => {
  fetchRecords();
});

const handleSearch = () => {
  searchForm.page = 1;
  fetchRecords();
};

const handleDateRangeChange = (val) => {
  searchForm.startDate = val ? val[0] : '';
  searchForm.endDate = val ? val[1] : '';
};

const handleSizeChange = (size) => {
  searchForm.pageSize = size;
  fetchRecords();
};

const handleCurrentChange = (page) => {
  searchForm.page = page;
  fetchRecords();
};

const getStatusType = (status) => {
  const statusMap = {
    'present': 'success', 
    'late': 'warning', 
    'absent': 'danger', 
    'leave': 'info', 
    'pending': 'primary'
  };
  return statusMap[status] || 'default';
};

const getStatusText = (status) => {
  const statusMap = {
    'present': '已签到', 
    'late': '迟到', 
    'absent': '缺勤', 
    'leave': '请假', 
    'pending': '待签到'
  };
  return statusMap[status] || '未知';
};

const updateRecordStatus = (record) => {
  selectedRecord.value = record;
  statusForm.status = record.status;
  statusForm.remark = record.remark || '';
  statusDialogVisible.value = true;
};

const submitStatusChange = async () => {
  if (!selectedRecord.value?.recordId) return;
  
  statusUpdateLoading.value = true;
  try {
    const res = await apiUpdateStatus(
      selectedRecord.value.recordId,
      statusForm.status,
      statusForm.remark
    );
    if (res && res.code === 200) {
      ElMessage.success('更新状态成功');
      statusDialogVisible.value = false;
      fetchRecords(); // 刷新列表
    } else {
      ElMessage.error(res?.msg || '更新状态失败');
    }
  } catch (error) {
    console.error('更新状态失败:', error);
    ElMessage.error('更新状态失败');
  } finally {
    statusUpdateLoading.value = false;
  }
};

// 导出刷新方法，供父组件调用
defineExpose({
  fetchRecords
});
</script>

<style scoped>
.duty-records-content {
  width: 100%;
}

.attendance-list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  width: 100%;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.plan-info h3 {
  margin: 0 0 10px;
}

.plan-info p {
  margin: 5px 0;
  color: #606266;
}

.records-filter {
  display: flex;
  gap: 10px;
  align-items: center;
}

.ml-5 {
  margin-left: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style> 