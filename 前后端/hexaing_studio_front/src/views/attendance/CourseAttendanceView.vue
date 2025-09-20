<template>
  <div class="course-attendance-container">
    <!-- 搜索区域 -->
    <el-card shadow="hover" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="课程名称">
          <el-input v-model="searchForm.courseName" placeholder="请输入课程名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="考勤名称">
          <el-input v-model="searchForm.name" placeholder="请输入考勤名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="1"></el-option>
            <el-option label="已禁用" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
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
          <el-button type="primary" @click="searchPlans">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 课程考勤列表 -->
    <el-card shadow="hover" class="attendance-list-card">
      <template #header>
        <div class="card-header">
          <span>课程考勤计划列表</span>
          <div>
          </div>
        </div>
      </template>

      <el-table
        :data="attendancePlans"
        style="width: 100%"
        border
        stripe
        v-loading="loading"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold'}"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="name" label="考勤名称" width="200" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column label="考勤时间" width="320">
          <template #default="scope">
            <div>{{ scope.row.startTime && scope.row.endTime ? `${formatDateTime(scope.row.startTime)} ~ ${formatDateTime(scope.row.endTime)}` : '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="考勤地点" width="200" />
        <el-table-column label="签到半径" width="100">
          <template #default="scope">
            {{ scope.row.radius }} 米
          </template>
        </el-table-column>
        <el-table-column prop="createUserName" label="创建人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到情况" width="280">
          <template #default="scope">
            <div class="attendance-stats">
              <el-tooltip content="总人次">
                <el-tag type="info">总计: {{ scope.row.recordStats?.total || 0 }}</el-tag>
              </el-tooltip>
              <el-tooltip content="已签到人次">
                <el-tag type="success">已签到: {{ scope.row.recordStats?.presentCount || 0 }}</el-tag>
              </el-tooltip>
              <el-tooltip content="迟到人次">
                <el-tag type="warning">迟到: {{ scope.row.recordStats?.lateCount || 0 }}</el-tag>
              </el-tooltip>
              <el-tooltip content="缺勤人次">
                <el-tag type="danger">缺勤: {{ scope.row.recordStats?.absentCount || 0 }}</el-tag>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="viewDetails(scope.row.planId)">
              详情
            </el-button>
            <el-button 
              :type="scope.row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="togglePlanStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="deletePlan(scope.row.planId)">
              删除
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAttendancePlans, toggleAttendanceStatus, deleteAttendancePlan, updateAttendancePlanStatus } from '@/api/attendance';

// 搜索表单数据
const searchForm = reactive({
  courseName: '',
  name: '',
  status: '',
  timeRange: []
});

// 表格数据
const attendancePlans = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 获取课程考勤计划列表
const fetchAttendancePlans = async () => {
  loading.value = true;
  try {
    // 准备查询参数
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: 'course', // 指定考勤类型为课程考勤
      keyword: searchForm.name || undefined,
      courseName: searchForm.courseName || undefined,
      status: searchForm.status || undefined,
      startDate: searchForm.timeRange?.[0] || undefined,
      endDate: searchForm.timeRange?.[1] || undefined
    };

    console.log('课程考勤查询参数:', params); // 调试日志

    // 调用API获取数据
    const res = await getAttendancePlans(params);
    
    console.log('课程考勤API原始响应:', res); // 调试日志
    
    if (res && res.code === 200) {
      // 在映射之前打印第一条原始记录，查看状态值
      const firstRecord = res.data.records && res.data.records[0];
      if (firstRecord) {
        console.log('原始记录示例:', firstRecord);
        console.log('原始记录的状态值:', firstRecord.status, 'typeof:', typeof firstRecord.status);
      }
      
      attendancePlans.value = (res.data.records || []).map(item => {
        // Manually convert snake_case to camelCase for all relevant fields
        const camelCaseItem = {};
        for (const key in item) {
          const camelKey = key.replace(/_([a-z])/g, g => g[1].toUpperCase());
          camelCaseItem[camelKey] = item[key];
        }
        // Ensure planId is correctly mapped as it's a primary identifier
        if (item.plan_id) {
          camelCaseItem.planId = item.plan_id;
        }
        // 确保status字段是数字类型，1表示正常，0表示禁用
        if (item.status !== undefined) {
          camelCaseItem.status = Number(item.status);
        }
        return camelCaseItem;
      });
      
      // 打印转换后的第一条记录
      if (attendancePlans.value.length > 0) {
        console.log('转换后的记录示例:', attendancePlans.value[0]);
        console.log('转换后的状态值:', attendancePlans.value[0].status, 'typeof:', typeof attendancePlans.value[0].status);
      }
      
      total.value = res.data.total || 0;
      console.log('格式化后的课程考勤数据:', attendancePlans.value);
    } else {
      ElMessage.error(res?.msg || '获取课程考勤计划失败');
      attendancePlans.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取课程考勤计划失败:', error);
    ElMessage.error('获取课程考勤计划失败');
    attendancePlans.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

// 搜索考勤计划
const searchPlans = () => {
  currentPage.value = 1; // 重置页码
  fetchAttendancePlans();
};

// 重置搜索
const resetSearch = () => {
  searchForm.courseName = '';
  searchForm.name = '';
  searchForm.status = '';
  searchForm.timeRange = [];
  searchPlans();
};

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchAttendancePlans();
};

// 页码变更
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchAttendancePlans();
};

// 查看考勤详情
const viewDetails = (planId) => {
  emit('view-detail', planId);
};

// 查看考勤记录
const viewRecords = (planId) => {
  // 根据考勤计划ID查看相关记录的逻辑
  // 课程考勤记录会包含所有选课学生的记录
  ElMessage.info(`查看课程考勤记录: ${planId}`);
};

// 编辑考勤
const editAttendance = (id) => {
  emit('edit-attendance', id);
};

// 切换考勤计划状态
const togglePlanStatus = async (row) => {
  // 添加调试日志
  console.log('当前行数据:', row);
  console.log('当前状态值:', row.status, '类型:', typeof row.status);
  
  // 当前状态为1（正常）时，发送false表示禁用；当前状态为0（已禁用）时，发送true表示启用
  const newStatus = row.status === 0 || row.status === '0';
  console.log('计算得到的新状态:', newStatus, '类型:', typeof newStatus);
  
  const statusText = newStatus ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}考勤计划 "${row.name}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    console.log('发送请求前的数据:', {planId: row.planId, status: newStatus});
    const res = await updateAttendancePlanStatus(row.planId, newStatus);
    console.log('API响应结果:', res);
    
    if (res.code === 200) {
      ElMessage.success('状态更新成功');
      // 刷新列表前添加一个小延迟
      setTimeout(() => {
      fetchAttendancePlans(); // Refresh list
      }, 200);
    } else {
      ElMessage.error(res.msg || '操作失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新状态失败:', error);
      ElMessage.error('操作失败');
    }
  }
};

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

// 定义事件
const emit = defineEmits(['view-detail', 'edit-attendance']);

// 页面初始化时加载数据
onMounted(() => {
  fetchAttendancePlans();
});

// 导出给父组件调用的方法
defineExpose({
  fetchAttendancePlans
});

// 删除考勤计划
const deletePlan = async (planId) => {
  try {
    // 调用删除API
    const res = await deleteAttendancePlan(planId);
    
    if (res && res.code === 200) {
      ElMessage.success('删除成功');
      fetchAttendancePlans(); // 刷新列表
    } else {
      ElMessage.error(res?.msg || '删除失败');
    }
  } catch (error) {
    console.error('删除考勤计划失败:', error);
    ElMessage.error('删除考勤计划失败');
  }
};
</script>

<style scoped>
.course-attendance-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
}

.attendance-list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.attendance-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 