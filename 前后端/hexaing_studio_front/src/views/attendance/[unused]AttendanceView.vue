<template>
  <div class="attendance-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="活动考勤" name="activity">
        <div class="table-operations">
          <el-button type="primary" @click="createAttendancePlan('activity')">创建活动考勤</el-button>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索活动名称"
            style="width: 200px; margin-left: 10px"
            clearable
            @clear="handleSearch"
            @input="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        <el-table
          :data="activityPlans"
          style="width: 100%"
          v-loading="loading"
          border
        >
          <el-table-column prop="name" label="活动名称" min-width="150" />
          <el-table-column label="考勤时间" min-width="240">
            <template #default="scope">
              {{ formatDateTime(scope.row.startTime) }} 至 {{ formatDateTime(scope.row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="考勤地点" min-width="150">
            <template #default="scope">
              {{ scope.row.location }}
              <div class="coordinates">
                ({{ scope.row.locationLat }}, {{ scope.row.locationLng }})
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="radius" label="签到半径" min-width="100">
            <template #default="scope">
              {{ scope.row.radius }}米
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="180">
            <template #default="scope">
              <el-button type="primary" link @click="viewAttendanceDetail(scope.row)">
                详情
              </el-button>
              <el-button type="primary" link @click="editAttendancePlan(scope.row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定要删除此考勤计划吗？"
                @confirm="deleteAttendancePlan(scope.row.planId)"
              >
                <template #reference>
                  <el-button type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="课程考勤" name="course">
        <div class="table-operations">
          <el-button type="primary" @click="createAttendancePlan('course')">创建课程考勤</el-button>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索课程名称"
            style="width: 200px; margin-left: 10px"
            clearable
            @clear="handleSearch"
            @input="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        <el-table
          :data="coursePlans"
          style="width: 100%"
          v-loading="loading"
          border
        >
          <el-table-column prop="name" label="考勤名称" min-width="150" />
          <el-table-column label="课程名称" min-width="150">
            <template #default="scope">
              {{ scope.row.course_name || '未关联课程' }}
            </template>
          </el-table-column>
          <el-table-column label="考勤时间" min-width="240">
            <template #default="scope">
              {{ formatDateTime(scope.row.startTime) }} 至 {{ formatDateTime(scope.row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="考勤地点" min-width="150">
            <template #default="scope">
              {{ scope.row.location }}
              <div class="coordinates">
                ({{ scope.row.locationLat }}, {{ scope.row.locationLng }})
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="radius" label="签到半径" min-width="100">
            <template #default="scope">
              {{ scope.row.radius }}米
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="180">
            <template #default="scope">
              <el-button type="primary" link @click="viewAttendanceDetail(scope.row)">
                详情
              </el-button>
              <el-button type="primary" link @click="editAttendancePlan(scope.row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定要删除此考勤计划吗？"
                @confirm="deleteAttendancePlan(scope.row.planId)"
              >
                <template #reference>
                  <el-button type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="值班考勤" name="duty">
        <div class="table-operations">
          <el-button type="primary" @click="createAttendancePlan('duty')">创建值班考勤</el-button>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索值班名称"
            style="width: 200px; margin-left: 10px"
            clearable
            @clear="handleSearch"
            @input="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        <el-table
          :data="dutyPlans"
          style="width: 100%"
          v-loading="loading"
          border
        >
          <el-table-column prop="name" label="值班名称" min-width="150" />
          <el-table-column label="考勤时间" min-width="240">
            <template #default="scope">
              {{ formatDateTime(scope.row.startTime) }} 至 {{ formatDateTime(scope.row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="考勤地点" min-width="150">
            <template #default="scope">
              {{ scope.row.location }}
              <div class="coordinates">
                ({{ scope.row.locationLat }}, {{ scope.row.locationLng }})
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="radius" label="签到半径" min-width="100">
            <template #default="scope">
              {{ scope.row.radius }}米
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="180">
            <template #default="scope">
              <el-button type="primary" link @click="viewAttendanceDetail(scope.row)">
                详情
              </el-button>
              <el-button type="primary" link @click="editAttendancePlan(scope.row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定要删除此考勤计划吗？"
                @confirm="deleteAttendancePlan(scope.row.planId)"
              >
                <template #reference>
                  <el-button type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建/编辑考勤计划对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑考勤计划' : '创建考勤计划'"
      width="70%"
      :before-close="handleDialogClose"
    >
      <attendance-plan-form
        :form-data="formData"
        :is-edit="isEdit"
        :attendance-type="currentType"
        :loading="formLoading"
        @submit="handleFormSubmit"
        @cancel="handleDialogClose"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { 
  getAttendancePlans, 
  createAttendancePlan as createAttendancePlanApi, 
  updateAttendancePlan as updateAttendancePlanApi,
  deleteAttendancePlan as deleteAttendancePlanApi
} from '@/api/attendance';
import { formatDateTime } from '@/utils/date';
import AttendancePlanForm from './AttendancePlanForm.vue';

const router = useRouter();

// 页面状态
const activeTab = ref('activity');
const loading = ref(false);
const formLoading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const currentType = ref('activity');
const searchKeyword = ref('');

// 分页参数
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 表单数据
const formData = reactive({
  planId: null,
  type: 'activity',
  name: '',
  startTime: '',
  endTime: '',
  location: '',
  locationLat: null,
  locationLng: null,
  radius: 50,
  courseId: null,
  scheduleId: null,
  commonLocationId: null,
  note: ''
});

// 考勤计划列表
const attendancePlans = ref([]);
const activityPlans = computed(() => {
  return attendancePlans.value.filter(plan => plan.type === 'activity');
});
const coursePlans = computed(() => {
  return attendancePlans.value.filter(plan => plan.type === 'course');
});
const dutyPlans = computed(() => {
  return attendancePlans.value.filter(plan => plan.type === 'duty');
});

// 监听标签页变化
watch(activeTab, (newValue) => {
  currentType.value = newValue;
  currentPage.value = 1;
  fetchAttendancePlans();
});

// 获取考勤计划列表
const fetchAttendancePlans = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: activeTab.value,
      keyword: searchKeyword.value
    };
    
    const res = await getAttendancePlans(params);
    if (res && res.code ===200) {
      attendancePlans.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res?.msg || '获取考勤计划失败');
    }
  } catch (error) {
    console.error('获取考勤计划失败:', error);
    ElMessage.error('获取考勤计划失败');
  } finally {
    loading.value = false;
  }
};

// 处理标签页点击
const handleTabClick = () => {
  searchKeyword.value = '';
};

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1;
  fetchAttendancePlans();
};

// 处理分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size;
  fetchAttendancePlans();
};

// 处理分页页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page;
  fetchAttendancePlans();
};

// 创建考勤计划
const createAttendancePlan = (type) => {
  isEdit.value = false;
  currentType.value = type;
  
  // 重置表单数据
  Object.assign(formData, {
    planId: null,
    type: type,
    name: '',
    startTime: '',
    endTime: '',
    location: '',
    locationLat: null,
    locationLng: null,
    radius: 50,
    courseId: null,
    scheduleId: null,
    commonLocationId: null,
    note: '',
    status: 1
  });
  
  dialogVisible.value = true;
};

// 编辑考勤计划
const editAttendancePlan = (plan) => {
  isEdit.value = true;
  currentType.value = plan.type;
  
  // 填充表单数据
  Object.assign(formData, {
    planId: plan.planId,
    type: plan.type,
    name: plan.name,
    startTime: plan.startTime,
    endTime: plan.endTime,
    location: plan.location,
    locationLat: plan.locationLat,
    locationLng: plan.locationLng,
    radius: plan.radius,
    courseId: plan.courseId,
    scheduleId: plan.scheduleId,
    commonLocationId: plan.commonLocationId,
    note: plan.note
  });
  
  dialogVisible.value = true;
};

// 查看考勤详情
const viewAttendanceDetail = (plan) => {
  router.push({
    name: 'AttendanceRecordsView',
    params: { planId: plan.planId },
    query: { planType: plan.type }
  });
};

// 删除考勤计划
const deleteAttendancePlan = async (planId) => {
  try {
    const res = await deleteAttendancePlanApi(planId);
    if (res && res.code ===200) {
      ElMessage.success('删除成功');
      fetchAttendancePlans();
    } else {
      ElMessage.error(res?.msg || '删除失败');
    }
  } catch (error) {
    console.error('删除考勤计划失败:', error);
    ElMessage.error('删除考勤计划失败');
  }
};

// 处理表单提交
const handleFormSubmit = async (data) => {
  formLoading.value = true;
  try {
    let res;
    if (isEdit.value) {
      res = await updateAttendancePlanApi(data.planId, data);
    } else {
      res = await createAttendancePlanApi(data);
    }
    
    if (res && res.code ===200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功');
      dialogVisible.value = false;
      fetchAttendancePlans();
    } else {
      ElMessage.error(res?.msg || (isEdit.value ? '更新失败' : '创建失败'));
    }
  } catch (error) {
    console.error(isEdit.value ? '更新考勤计划失败:' : '创建考勤计划失败:', error);
    ElMessage.error(isEdit.value ? '更新考勤计划失败' : '创建考勤计划失败');
  } finally {
    formLoading.value = false;
  }
};

// 处理对话框关闭
const handleDialogClose = () => {
  dialogVisible.value = false;
};

// 获取状态类型
const getStatusType = (status) => {
  return status === 1 ? 'success' : 'danger';
};

// 获取状态文本
const getStatusText = (status) => {
  return status === 1 ? '有效' : '已取消';
};

// 页面加载时获取考勤计划列表
onMounted(() => {
  fetchAttendancePlans();
});
</script>

<style scoped>
.attendance-container {
  padding: 20px;
}

.table-operations {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.coordinates {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

:deep(.el-dialog__body) {
  padding-top: 10px;
}
</style> 