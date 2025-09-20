<template>
  <div class="attendance-view">
    <!-- 页面头部 -->
    <el-card shadow="hover" class="page-header-card">
      <div class="page-title">
        <div class="title-with-refresh">
          <h2>考勤管理</h2>
          <el-button type="primary" @click="refreshCurrentView" :loading="refreshing" circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
        <div class="action-buttons">
          <el-button type="success" @click="showAddAttendanceDialog">
            <el-icon><Plus /></el-icon> 新增考勤
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 考勤标签页 -->
    <el-tabs v-model="activeTab" class="attendance-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="活动考勤" name="activity">
        <ActivityAttendanceView 
          ref="activityAttendanceRef" 
          v-show="activeTab === 'activity'" 
          @view-detail="viewAttendanceDetail"
          @edit-attendance="editAttendance"
        />
      </el-tab-pane>
      <el-tab-pane label="课程考勤" name="course">
        <CourseAttendanceView 
          ref="courseAttendanceRef" 
          v-show="activeTab === 'course'" 
          @view-detail="viewAttendanceDetail"
          @edit-attendance="editAttendance"
        />
      </el-tab-pane>
      <el-tab-pane label="值班考勤" name="duty">
        <DutyManagementView 
          ref="dutyManagementRef" 
          v-show="activeTab === 'duty'" 
        />
      </el-tab-pane>
      <el-tab-pane label="考勤记录" name="records">
        <AttendanceRecordsView ref="attendanceRecordsRef" v-show="activeTab === 'records'" />
      </el-tab-pane>
      <el-tab-pane label="考勤统计" name="statistics">
        <AttendanceStatisticsView ref="attendanceStatisticsRef" v-show="activeTab === 'statistics'" />
      </el-tab-pane>
      <el-tab-pane label="请假审批" name="approval">
        <LeaveApprovalView ref="leaveApprovalRef" v-show="activeTab === 'approval'" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增考勤对话框 -->
    <el-dialog
      v-model="addAttendanceDialogVisible"
      title="新增考勤计划"
      width="900px"
      :close-on-click-modal="false"
    >
      <AttendancePlanForm 
        v-if="addAttendanceDialogVisible"
        ref="attendancePlanFormRef"
        :initial-data="attendancePlanForm"
        :attendance-type="activeTab"
        :loading="submitLoading"
        @submit="submitAttendancePlan"
        @cancel="addAttendanceDialogVisible = false"
      />
    </el-dialog>

    <!-- 查看考勤详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="考勤详情"
      width="900px"
    >
      <div v-loading="detailLoading">
        <div v-if="currentAttendance" class="attendance-detail">
          <!-- 考勤计划基本信息 -->
          <el-card shadow="hover" class="detail-card">
            <template #header>
              <div class="card-header">
                <span>考勤计划信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="考勤名称">{{ currentAttendance.name }}</el-descriptions-item>
              <el-descriptions-item label="考勤类型">
                <el-tag :type="currentAttendance.type === 'activity' ? 'success' : 'primary'">
                  {{ currentAttendance.type === 'activity' ? '活动考勤' : '课程考勤' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatDateTime(currentAttendance.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatDateTime(currentAttendance.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="考勤地点">{{ currentAttendance.location }}</el-descriptions-item>
              <el-descriptions-item label="签到半径">{{ currentAttendance.radius }} 米</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentAttendance.createUser }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentAttendance.status === 1 ? 'success' : 'info'">
                  {{ currentAttendance.status === 1 ? '正常' : '已取消' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ currentAttendance.note || '无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 签到统计 -->
          <el-card shadow="hover" class="detail-card" v-if="currentAttendance.recordStats">
            <template #header>
              <div class="card-header">
                <span>签到统计</span>
              </div>
            </template>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-number">{{ currentAttendance.recordStats.total || 0 }}</div>
                <div class="stat-label">总人次</div>
              </div>
              <div class="stat-item">
                <div class="stat-number success">{{ currentAttendance.recordStats.presentCount || 0 }}</div>
                <div class="stat-label">已签到</div>
              </div>
              <div class="stat-item">
                <div class="stat-number warning">{{ currentAttendance.recordStats.lateCount || 0 }}</div>
                <div class="stat-label">迟到</div>
              </div>
              <div class="stat-item">
                <div class="stat-number danger">{{ currentAttendance.recordStats.absentCount || 0 }}</div>
                <div class="stat-label">缺勤</div>
              </div>
              <div class="stat-item">
                <div class="stat-number info">{{ currentAttendance.recordStats.leaveCount || 0 }}</div>
                <div class="stat-label">请假</div>
              </div>
            </div>
          </el-card>

          <!-- 预约人员列表（仅活动考勤显示） -->
          <el-card shadow="hover" class="detail-card" v-if="currentAttendance.type === 'activity'">
            <template #header>
              <div class="card-header">
                <span>预约人员</span>
                <el-tag type="info" size="small" v-if="currentAttendance.reservations">
                  共 {{ currentAttendance.reservations.length }} 人
                </el-tag>
              </div>
            </template>
            <div v-if="currentAttendance.reservations && currentAttendance.reservations.length > 0">
              <el-table :data="currentAttendance.reservations" border stripe>
                <el-table-column type="index" label="序号" width="70" />
                <el-table-column prop="studentName" label="学生姓名" width="120" />
                <el-table-column prop="studentNumber" label="学号" width="150" />
                <el-table-column label="预约状态" width="100">
                  <template #default="scope">
                    <el-tag :type="getReservationStatusType(scope.row.status)">
                      {{ getReservationStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="reservationTime" label="预约时间" min-width="180">
                  <template #default="scope">
                    {{ scope.row.reservationTime ? formatDateTime(scope.row.reservationTime) : '-' }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="no-reservations">
              <el-empty description="暂无预约人员" :image-size="100" />
            </div>
          </el-card>

          <!-- 签到记录列表 -->
          <el-card shadow="hover" class="detail-card" v-if="currentAttendance.records && currentAttendance.records.length > 0">
            <template #header>
              <div class="card-header">
                <span>签到记录</span>
              </div>
            </template>
            <el-table :data="currentAttendance.records" border stripe>
              <el-table-column type="index" label="序号" width="70" />
              <el-table-column prop="student_name" label="学生姓名" width="120" />
              <el-table-column label="签到状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sign_in_time" label="签到时间" width="180">
                <template #default="scope">
                  {{ scope.row.sign_in_time ? formatDateTime(scope.row.sign_in_time) : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="location" label="签到地点" min-width="150" />
              <el-table-column prop="remark" label="备注" min-width="120" />
            </el-table>
          </el-card>
        </div>
        <div v-else-if="!detailLoading" class="no-data">
          <el-empty description="暂无考勤详情数据" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { getAttendanceDetail, createAttendancePlan, batchCreateActivityReservation, getActivityReservations } from '@/api/attendance';
// 这些组件需要在后续创建
import ActivityAttendanceView from './attendance/ActivityAttendanceView.vue';
import CourseAttendanceView from './attendance/CourseAttendanceView.vue';
import DutyManagementView from './attendance/DutyManagementView.vue';
import AttendanceRecordsView from './attendance/AttendanceRecordsView.vue';
import AttendanceStatisticsView from './attendance/AttendanceStatisticsView.vue';
import AttendancePlanForm from './attendance/AttendancePlanForm.vue';
import LeaveApprovalView from './attendance/LeaveApprovalView.vue';

// 当前激活的标签页
const activeTab = ref('activity');
const refreshing = ref(false);

// 组件引用
const activityAttendanceRef = ref(null);
const courseAttendanceRef = ref(null);
const dutyManagementRef = ref(null);
const attendanceRecordsRef = ref(null);
const attendanceStatisticsRef = ref(null);
const leaveApprovalRef = ref(null);
const attendancePlanFormRef = ref(null);

// 新增考勤对话框
const addAttendanceDialogVisible = ref(false);
const submitLoading = ref(false);
const attendancePlanForm = reactive({
  name: '',
  type: 'activity',
  startTime: '',
  endTime: '',
  location: '',
  locationLat: 29.552965,
  locationLng: 106.238573,
  radius: 50,
  remark: '',
  courseId: null
});

// 考勤详情对话框
const detailDialogVisible = ref(false);
const detailLoading = ref(false);
const currentAttendance = ref(null);

// 处理标签页切换
const handleTabChange = (tabName) => {
  console.log('Tab changed to:', tabName);
  // 切换到值班考勤标签页时，主动刷新数据
  if (tabName === 'duty') {
    nextTick(() => {
      if (dutyManagementRef.value && typeof dutyManagementRef.value.refreshDutyData === 'function') {
        console.log('正在主动调用值班管理组件的 refreshDutyData 方法...');
        dutyManagementRef.value.refreshDutyData();
      }
    });
  }
};

// 监听activeTab的变化，以便在组件首次显示时也能加载数据
watch(activeTab, (newTab) => {
  if (newTab === 'duty') {
    // 使用nextTick确保组件已经渲染
    nextTick(() => {
      if (dutyManagementRef.value && typeof dutyManagementRef.value.refreshDutyData === 'function') {
        console.log('检测到 duty 标签页激活，调用 refreshDutyData...');
        dutyManagementRef.value.refreshDutyData();
      }
    });
  }
}, { immediate: true }); // immediate: true 保证组件初始化时就会执行一次

// 刷新当前视图
const refreshCurrentView = async () => {
  refreshing.value = true;
  try {
    switch (activeTab.value) {
      case 'activity':
        await activityAttendanceRef.value?.fetchAttendancePlans();
        break;
      case 'course':
        await courseAttendanceRef.value?.fetchAttendancePlans();
        break;
      case 'duty':
        await dutyManagementRef.value?.refreshDutyData();
        break;
      case 'records':
        await attendanceRecordsRef.value?.fetchRecords();
        break;
      case 'statistics':
        await attendanceStatisticsRef.value?.fetchStatistics();
        break;
      case 'approval':
        await leaveApprovalRef.value?.fetchLeaveRequests();
        break;
    }
    ElMessage.success('刷新成功');
  } catch (error) {
    console.error('刷新失败:', error);
    ElMessage.error('刷新失败');
  } finally {
    refreshing.value = false;
  }
};

// 显示新增考勤对话框
const showAddAttendanceDialog = () => {
  // 重置表单数据
  Object.assign(attendancePlanForm, {
    name: '',
    type: activeTab.value,
    startTime: '',
    endTime: '',
    location: '',
    locationLat: 29.552965,
    locationLng: 106.238573,
    radius: 50,
    remark: '',
    courseId: null
  });
  
  addAttendanceDialogVisible.value = true;
};

// 提交考勤计划
const submitAttendancePlan = async (formData) => {
  console.log('提交考勤计划:', formData);
  
  // 表单数据验证
  if (!formData.name || formData.name.trim() === '') {
    ElMessage.error('考勤名称不能为空');
    return;
  }
  
  if (!formData.startTime) {
    ElMessage.error('开始时间不能为空');
    return;
  }
  
  if (!formData.endTime) {
    ElMessage.error('结束时间不能为空');
    return;
  }
  
  // 活动考勤必须选择学生
  if (formData.type === 'activity') {
    const selectedStudents = formData.selectedStudents || [];
    if (selectedStudents.length === 0) {
      ElMessage.error('活动考勤必须选择参与学生');
      return;
    }
  }
  
  submitLoading.value = true;
  try {
    // 准备提交数据
    const submitData = {
      type: formData.type,
      name: formData.name,
      startTime: formData.startTime,
      endTime: formData.endTime,
      location: formData.location || '未指定位置',
      locationLng: formData.locationLng || 0,
      locationLat: formData.locationLat || 0,
      radius: formData.radius || 50,
      courseId: formData.courseId,
      note: formData.note
    };
    
    console.log('调用API提交数据:', submitData);
    
    // 调用API创建考勤计划
    const res = await createAttendancePlan(submitData);
    
    console.log('服务器响应:', res);

    if (res && res.code === 200) {
      const planId = res.data?.planId;
      const selectedStudents = formData.selectedStudents || [];
      
      // 如果是活动考勤，创建预约（因为已强制要求选择学生）
      if (formData.type === 'activity' && selectedStudents.length > 0 && planId) {
        try {
          console.log('创建活动预约，计划ID:', planId, '学生IDs:', selectedStudents);
          const reservationRes = await batchCreateActivityReservation({
            planId: planId,
            studentIds: selectedStudents,
            remark: '系统自动创建预约'
          });
          
          if (reservationRes && reservationRes.code === 200) {
            const result = reservationRes.data;
            console.log('预约创建结果:', result);
            ElMessage.success(`考勤计划创建成功，已为 ${result.successCount} 名学生创建预约`);
            if (result.conflictStudents && result.conflictStudents.length > 0) {
              ElMessage.warning(`有 ${result.conflictStudents.length} 名学生已预约过此活动`);
            }
          } else {
            ElMessage.warning('考勤计划创建成功，但预约学生失败');
          }
        } catch (reservationError) {
          console.error('创建预约失败:', reservationError);
          ElMessage.warning('考勤计划创建成功，但预约学生失败');
        }
      } else {
        ElMessage.success('考勤计划创建成功');
      }
      
      addAttendanceDialogVisible.value = false;
      // 刷新对应类型的考勤列表
      await refreshCurrentView();
    } else {
      console.error('创建失败，响应:', res);
      ElMessage.error(res?.msg || '创建考勤计划失败');
    }
  } catch (error) {
    console.error('创建考勤计划失败:', error);
    ElMessage.error('创建考勤计划失败: ' + (error.message || '未知错误'));
  } finally {
    submitLoading.value = false;
  }
};

// 查看考勤详情
const viewAttendanceDetail = async (attendanceId) => {
  detailDialogVisible.value = true;
  detailLoading.value = true;
  
  try {
    const res = await getAttendanceDetail(attendanceId);
    if (res && res.code === 200) {
      currentAttendance.value = res.data;
      
      // 如果是活动考勤，获取预约人员信息
      if (currentAttendance.value && currentAttendance.value.type === 'activity') {
        try {
          const reservationRes = await getActivityReservations(attendanceId);
          if (reservationRes && reservationRes.code === 200) {
            currentAttendance.value.reservations = reservationRes.data || [];
          } else {
            currentAttendance.value.reservations = [];
          }
        } catch (reservationError) {
          console.error('获取预约信息失败:', reservationError);
          currentAttendance.value.reservations = [];
        }
      }
    } else {
      ElMessage.error(res?.msg || '获取考勤详情失败');
      currentAttendance.value = null;
    }
  } catch (error) {
    console.error('获取考勤详情失败:', error);
    ElMessage.error('获取考勤详情失败');
    currentAttendance.value = null;
  } finally {
    detailLoading.value = false;
  }
};

// 编辑考勤
const editAttendance = (attendanceId) => {
  // 根据考勤类型跳转到对应的编辑页面或打开编辑对话框
  ElMessage.info(`编辑考勤 ID: ${attendanceId}`);
};

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '';
  return new Date(dateTime).toLocaleString();
};

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'present': return 'success';
    case 'late': return 'warning';
    case 'absent': return 'danger';
    case 'leave': return 'info';
    default: return 'info';
  }
};

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'present': return '已签到';
    case 'late': return '迟到';
    case 'absent': return '缺勤';
    case 'leave': return '请假';
    default: return '未知';
  }
};

// 获取预约状态类型
const getReservationStatusType = (status) => {
  switch (status) {
    case 'reserved': return 'warning';
    case 'checked_in': return 'success';
    case 'cancelled': return 'info';
    default: return 'info';
  }
};

// 获取预约状态文本
const getReservationStatusText = (status) => {
  switch (status) {
    case 'reserved': return '已预约';
    case 'checked_in': return '已签到';
    case 'cancelled': return '已取消';
    default: return '未知';
  }
};

const route = useRoute(); // 获取当前路由信息
const router = useRouter(); // 实例化 router

// 页面加载时执行的逻辑
onMounted(() => {
  // 检查路由中是否有 'action=add' 参数
  if (route.query.action === 'add') {
    showAddAttendanceDialog();
    // 可选: 清理URL中的查询参数，避免刷新页面时再次触发
    router.replace({ query: {} });
  }
});
</script>

<style scoped>
.attendance-view {
  padding: 20px;
}

.page-header-card {
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-with-refresh {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-with-refresh h2 {
  margin: 0;
  font-size: 20px;
}

.attendance-tabs {
  margin-top: 20px;
}

.attendance-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-card {
  margin-bottom: 20px;
}

.detail-card:last-child {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  padding: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 20px;
  border-radius: 8px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.stat-number.success {
  color: #67c23a;
}

.stat-number.warning {
  color: #e6a23c;
}

.stat-number.danger {
  color: #f56c6c;
}

.stat-number.info {
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.no-data {
  text-align: center;
  padding: 40px 0;
}

.no-reservations {
  text-align: center;
  padding: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 