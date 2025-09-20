<template>
  <div class="leave-approval-container">
    <!-- 搜索与筛选区域 -->
    <el-card shadow="hover" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="学生姓名">
          <el-input v-model="searchForm.studentName" placeholder="请输入学生姓名" clearable></el-input>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="searchForm.status" placeholder="请选择审批状态" clearable>
            <el-option label="待审批" value="pending"></el-option>
            <el-option label="已通过" value="approved"></el-option>
            <el-option label="已驳回" value="rejected"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 请假申请列表 -->
    <el-card shadow="hover">
      <el-table
        :data="leaveRequests"
        style="width: 100%"
        border
        stripe
        v-loading="loading"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold'}"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="studentName" label="申请人" width="120" />
        <el-table-column prop="attendancePlanName" label="考勤计划" width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="请假类型" width="120">
          <template #default="scope">
            {{ getLeaveTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="请假事由" min-width="200" show-overflow-tooltip />
        <el-table-column label="请假时间" width="320">
          <template #default="scope">
            <div>{{ formatDateTime(scope.row.startTime) }}</div>
            <div>至 {{ formatDateTime(scope.row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请提交时间" width="180">
           <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="附件" width="120" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.attachments && scope.row.attachments.length > 0"
              type="primary"
              :icon="Document"
              size="small"
              @click="showAttachments(scope.row)"
            >
              查看附件
            </el-button>
            <span v-else style="color: #909399;">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="scope">
            <div v-if="scope.row.status === 'pending'">
              <el-button type="success" size="small" @click="handleApprove(scope.row)">
                批准
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(scope.row)">
                驳回
              </el-button>
            </div>
             <div v-else>
              <el-button type="info" size="small" @click="viewDetails(scope.row)">
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:currentPage="currentPage"
          v-model:pageSize="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 审批/查看详情 对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false">
      <div v-if="currentRequestDetail" class="detail-container">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ currentRequestDetail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">{{ getLeaveTypeText(currentRequestDetail.type) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(currentRequestDetail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(currentRequestDetail.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">{{ formatDateTime(currentRequestDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="请假事由" :span="2">{{ currentRequestDetail.reason }}</el-descriptions-item>
          <el-descriptions-item label="审批状态">
            <el-tag :type="getStatusTagType(currentRequestDetail.status)">
              {{ getStatusText(currentRequestDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批人">{{ currentRequestDetail.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间" :span="2">{{ formatDateTime(currentRequestDetail.approvedAt) }}</el-descriptions-item>
          <el-descriptions-item label="审批备注" :span="2">{{ currentRequestDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="加载详情失败"></el-empty>
    </el-dialog>
    
    <!-- 附件展示对话框 -->
    <el-dialog v-model="attachmentDialogVisible" title="附件列表" width="600px">
      <ul v-if="currentAttachments && currentAttachments.length > 0" class="attachment-list">
        <li v-for="(filePath, index) in currentAttachments" :key="index" class="attachment-item">
          <el-link 
            :underline="false" 
            type="primary" 
            @click="downloadFile(filePath)"
          >
            <div class="attachment-info">
              <el-icon class="attachment-icon"><Document /></el-icon>
              <div class="attachment-details">
                <span class="attachment-name">{{ getFileName(filePath) }}</span>
              </div>
            </div>
          </el-link>
        </li>
      </ul>
      <el-empty v-else description="暂无附件"></el-empty>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Document } from '@element-plus/icons-vue';
import { getLeaveRequests, approveLeaveRequest, rejectLeaveRequest, getLeaveRequestDetail } from '@/api/attendance';
import { formatDateTime } from '@/utils/date';

// 搜索表单
const searchForm = reactive({
  studentName: '',
  status: '',
  dateRange: []
});

// 表格与分页
const leaveRequests = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 对话框
const dialogVisible = ref(false);
const dialogTitle = ref('');
const attachmentDialogVisible = ref(false);
const currentAttachments = ref([]);
const currentRequestDetail = ref(null); // 新增：用于存储详情数据


// 获取请假列表
const fetchLeaveRequests = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      studentName: searchForm.studentName || undefined,
      status: searchForm.status || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
    };
    const res = await getLeaveRequests(params);

    if (res && res.code === 200) {
      leaveRequests.value = (res.data.records || []).map(req => {
        try {
          if (req.attachments && typeof req.attachments === 'string') {
            req.attachments = JSON.parse(req.attachments);
          } else {
            req.attachments = [];
          }
        } catch (e) {
          console.error('解析附件JSON失败:', e);
          req.attachments = [];
        }
        return req;
      });
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res?.msg || '获取请假列表失败');
    }
  } catch (error) {
    console.error('获取请假列表失败:', error);
    ElMessage.error('获取请假列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchLeaveRequests();
};

const resetSearch = () => {
  searchForm.studentName = '';
  searchForm.status = '';
  searchForm.dateRange = [];
  handleSearch();
};

const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchLeaveRequests();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchLeaveRequests();
};

const handleApprove = async (row) => {
  await ElMessageBox.confirm(`确定要批准 ${row.studentName} 的请假申请吗？`, '确认批准', { type: 'success' });
  try {
    const res = await approveLeaveRequest(row.requestId);
    if (res.code === 200) {
      ElMessage.success('批准成功');
      fetchLeaveRequests();
    } else {
      ElMessage.error(res.msg || '操作失败');
    }
  } catch (error) {
     ElMessage.error('操作失败');
  }
};

const handleReject = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入驳回理由', '驳回申请', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '驳回理由不能为空',
  });

  try {
    const res = await rejectLeaveRequest(row.requestId, { remark: value });
    if (res.code === 200) {
      ElMessage.success('驳回成功');
      fetchLeaveRequests();
    } else {
      ElMessage.error(res.msg || '操作失败');
    }
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const viewDetails = async (row) => {
  dialogTitle.value = '请假申请详情';
  try {
    const res = await getLeaveRequestDetail(row.requestId);
    if (res.code === 200) {
      currentRequestDetail.value = res.data;
      dialogVisible.value = true;
    } else {
      ElMessage.error(res.msg || '获取详情失败');
    }
  } catch (error) {
    ElMessage.error('获取详情失败');
  }
};

const showAttachments = (row) => {
  currentAttachments.value = row.attachments || [];
  attachmentDialogVisible.value = true;
};

const getFileUrl = (filePath, download = false) => {
  if (!filePath) return '';
  const baseUrl = '/api/admin/file/view/';
  return `${baseUrl}${filePath}${download ? '?download=true' : ''}`;
};

const getFileName = (filePath) => {
  if (!filePath) return '未知文件';
  return filePath.split('/').pop();
};

const downloadFile = (filePath) => {
  if (!filePath) return;
  window.open(getFileUrl(filePath, true), '_blank');
};

const getStatusTagType = (status) => {
  switch (status) {
    case 'pending': return 'warning';
    case 'approved': return 'success';
    case 'rejected': return 'danger';
    default: return 'info';
  }
};

const getStatusText = (status) => {
  const map = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已驳回',
  };
  return map[status] || '未知';
};

const getLeaveTypeText = (type) => {
  const map = {
    personal_leave: '事假',
    sick_leave: '病假',
  };
  return map[type] || '其他';
};

onMounted(() => {
  fetchLeaveRequests();
});

// 将刷新方法暴露给父组件
defineExpose({
  fetchLeaveRequests
});
</script>

<style scoped>
.leave-approval-container {
  padding: 0;
}
.search-card {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.attachment-list {
  list-style: none;
  padding: 0;
}
.attachment-item {
  margin-bottom: 10px;
}
.attachment-info {
  display: flex;
  align-items: center;
}
.attachment-icon {
  margin-right: 10px;
  font-size: 20px;
}
.attachment-details {
  display: flex;
  flex-direction: column;
}
.attachment-name {
  font-size: 14px;
}
.detail-container {
  padding: 10px;
}
</style> 