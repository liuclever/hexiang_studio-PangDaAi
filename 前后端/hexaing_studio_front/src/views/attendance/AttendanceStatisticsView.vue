<template>
  <div class="attendance-statistics-container">
    <el-card shadow="hover" class="search-card">
      <!-- 搜索过滤区 -->
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="考勤类型">
          <el-select v-model="filterForm.type" placeholder="请选择类型" clearable @change="fetchStatisticsData">
            <el-option label="全部" value=""></el-option>
            <el-option label="活动考勤" value="activity"></el-option>
            <el-option label="课程考勤" value="course"></el-option>
            <el-option label="值班考勤" value="duty"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="fetchStatisticsData"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchStatisticsData" :loading="loading">
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片区域 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="stat-icon blue">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-title">总人次</div>
                <div class="stat-value">{{ overviewData.totalCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="stat-icon green">
                <el-icon><Check /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-title">已签到</div>
                <div class="stat-value">{{ overviewData.presentCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="stat-icon orange">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-title">迟到</div>
                <div class="stat-value">{{ overviewData.lateCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="stat-icon red">
                <el-icon><Close /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-title">缺勤</div>
                <div class="stat-value">{{ overviewData.absentCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <div class="chart-header">
            <h3>考勤状态分布</h3>
          </div>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <div class="chart-header">
            <h3>考勤类型分布</h3>
          </div>
          <div ref="typeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="chart-card">
      <div class="chart-header">
        <h3>考勤趋势</h3>
      </div>
      <div ref="trendChartRef" class="chart-container trend-chart"></div>
    </el-card>

    <!-- 学生考勤统计表 -->
    <el-card shadow="hover" class="statistics-table-card">
      <template #header>
        <div class="card-header">
          <span>学生考勤统计</span>
          <div>
            <el-button @click="fetchStudentStatistics" :loading="studentLoading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      

      <el-table 
        :data="studentStatistics" 
        border 
        stripe 
        v-loading="studentLoading"
        :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold'}"
        :key="tableKey"
      >
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="studentName" label="学生姓名" show-overflow-tooltip />
        <el-table-column prop="studentNumber" label="学号" show-overflow-tooltip />
        <el-table-column prop="totalCount" label="总人次" />
        <el-table-column prop="presentCount" label="已签到" />
        <el-table-column prop="lateCount" label="迟到" />
        <el-table-column prop="absentCount" label="缺勤" />
        <el-table-column prop="leaveCount" label="请假" />
        <el-table-column label="出勤率">
          <template #default="scope">
            <el-progress 
              :percentage="calculateAttendanceRate(scope.row)" 
              :color="getAttendanceRateColor(scope.row)"
            ></el-progress>
          </template>
        </el-table-column>
        
        <!-- 空状态 -->
        <template #empty>
          <div style="padding: 20px;">
            <el-empty description="暂无学生考勤统计数据">
              <el-button type="primary" @click="fetchStudentStatistics">重新加载</el-button>
            </el-empty>
          </div>
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:currentPage="currentPage"
          v-model:pageSize="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="studentTotal"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { User, Check, Close, Timer, Refresh } from '@element-plus/icons-vue';
import * as echarts from 'echarts/core';
import { PieChart, BarChart, LineChart } from 'echarts/charts';
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  DatasetComponent, TransformComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';

// 注册必需的组件
echarts.use([
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  DatasetComponent, TransformComponent, PieChart, BarChart, LineChart,
  LabelLayout, UniversalTransition, CanvasRenderer
]);

// 引入API
import { 
  getAttendanceStatistics, 
  getAttendanceTypeStatistics,
  getAttendanceTrendStatistics,
  getStudentAttendanceStatistics
} from '@/api/attendance';

// 图表引用
const pieChartRef = ref(null);
const typeChartRef = ref(null);
const trendChartRef = ref(null);
let pieChart = null;
let typeChart = null;
let trendChart = null;

// 表单数据
const filterForm = reactive({
  type: '',
  timeRange: []
});

// 统计数据
const loading = ref(false);
const overviewData = reactive({
  totalCount: 0,
  presentCount: 0,
  lateCount: 0,
  absentCount: 0,
  leaveCount: 0
});

// 学生统计数据
const studentLoading = ref(false);
const studentStatistics = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const studentTotal = ref(0);
const tableKey = ref(0);



// 获取概览统计数据
const fetchStatisticsData = async () => {
  loading.value = true;
  try {
    // 准备查询参数
    const params = {
      type: filterForm.type || undefined,
      startDate: filterForm.timeRange?.[0] || undefined,
      endDate: filterForm.timeRange?.[1] || undefined
    };

    // 获取总体统计数据
    const overviewRes = await getAttendanceStatistics(params);
    if (overviewRes && overviewRes.code === 200) {
      const data = overviewRes.data;
      
      // 兼容下划线命名
      overviewData.totalCount = data.total || data.total_count || 0;
      overviewData.presentCount = data.present || data.present_count || 0;
      overviewData.lateCount = data.late || data.late_count || 0;
      overviewData.absentCount = data.absent || data.absent_count || 0;
      overviewData.leaveCount = data.leave || data.leave_count || 0;
      
      // 绘制饼图
      drawPieChart(data);
    }

    // 获取考勤类型统计并更新类型图表
    const typeRes = await getAttendanceTypeStatistics(params);
    if (typeRes && typeRes.code === 200) {
      drawTypeChart(typeRes.data);
    }

    // 获取趋势数据并更新趋势图
    const trendRes = await getAttendanceTrendStatistics(params);
    if (trendRes && trendRes.code === 200) {
      drawTrendChart(trendRes.data);
    }

    // 获取学生统计数据
    fetchStudentStatistics();

  } catch (error) {
    console.error('获取统计数据失败:', error);
    ElMessage.error('获取统计数据失败');
  } finally {
    loading.value = false;
  }
};

// 获取学生统计数据
const fetchStudentStatistics = async () => {
  studentLoading.value = true;
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: filterForm.type || undefined,
      startDate: filterForm.timeRange?.[0] || undefined,
      endDate: filterForm.timeRange?.[1] || undefined
    };

    const res = await getStudentAttendanceStatistics(params);
    
    if (res && res.code === 200) {
      const records = res.data?.records || res.data || [];
      
      studentStatistics.value = records;
      studentTotal.value = res.data?.total || 0;
      
      // 强制刷新表格
      tableKey.value += 1;
    } else {
      ElMessage.error(res?.msg || '获取学生统计数据失败');
      studentStatistics.value = [];
      studentTotal.value = 0;
    }
  } catch (error) {
    console.error('获取学生统计数据失败:', error);
    ElMessage.error('获取学生统计数据失败');
    studentStatistics.value = [];
    studentTotal.value = 0;
  } finally {
    studentLoading.value = false;
  }
};



// 重置过滤器
const resetFilter = () => {
  filterForm.type = '';
  filterForm.timeRange = [];
  fetchStatisticsData();
};

// 分页大小变更
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchStudentStatistics();
};

// 页码变更
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchStudentStatistics();
};

// 计算出勤率
const calculateAttendanceRate = (row) => {
  const totalCount = Number(row.totalCount) || 0;
  if (totalCount === 0) return 0;
  
  const presentCount = Number(row.presentCount) || 0;
  const lateCount = Number(row.lateCount) || 0;
  const presentAndLate = presentCount + lateCount;
  
  return Math.round((presentAndLate / totalCount) * 100);
};

// 获取出勤率颜色
const getAttendanceRateColor = (row) => {
  const rate = calculateAttendanceRate(row);
  if (rate >= 90) return '#67C23A';
  if (rate >= 75) return '#E6A23C';
  return '#F56C6C';
};

// 绘制考勤状态饼图
const drawPieChart = (data) => {
  if (!pieChartRef.value) return;

  // 确保图表存在
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value);
  } else {
    pieChart.dispose();
    pieChart = echarts.init(pieChartRef.value);
  }

  console.log('绘制饼图数据:', data);
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['已签到', '迟到', '缺勤', '请假']
    },
    series: [
      {
        name: '考勤状态',
        type: 'pie',
        radius: '65%',
        center: ['50%', '50%'],
        data: [
          { value: data.present || data.present_count || 0, name: '已签到', itemStyle: { color: '#67C23A' } },
          { value: data.late || data.late_count || 0, name: '迟到', itemStyle: { color: '#E6A23C' } },
          { value: data.absent || data.absent_count || 0, name: '缺勤', itemStyle: { color: '#F56C6C' } },
          { value: data.leave || data.leave_count || 0, name: '请假', itemStyle: { color: '#909399' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };
  
  console.log('饼图配置项:', option);

  pieChart.setOption(option);
  window.addEventListener('resize', () => pieChart.resize());
};

// 绘制考勤类型分布图
const drawTypeChart = (data) => {
  if (!typeChartRef.value) return;

  // 确保图表存在
  if (!typeChart) {
    typeChart = echarts.init(typeChartRef.value);
  } else {
    typeChart.dispose();
    typeChart = echarts.init(typeChartRef.value);
  }

  console.log('绘制类型分布图数据:', data);

  // 处理类型分布数据
  const typeData = [];
  if (Array.isArray(data)) {
    data.forEach(item => {
      typeData.push({
        type: item.type,
        count: item.total || item.total_count || 0
      });
    });
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['活动考勤', '课程考勤', '值班考勤']
    },
    series: [
      {
        name: '考勤类型',
        type: 'pie',
        radius: '65%',
        center: ['50%', '50%'],
        data: [
          { value: typeData.find(t => t.type === 'activity')?.count || 0, name: '活动考勤', itemStyle: { color: '#409EFF' } },
          { value: typeData.find(t => t.type === 'course')?.count || 0, name: '课程考勤', itemStyle: { color: '#67C23A' } },
          { value: typeData.find(t => t.type === 'duty')?.count || 0, name: '值班考勤', itemStyle: { color: '#E6A23C' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };

  typeChart.setOption(option);
  window.addEventListener('resize', () => typeChart.resize());
};

// 绘制考勤趋势图
const drawTrendChart = (data) => {
  if (!trendChartRef.value) return;

  // 确保图表存在
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  } else {
    trendChart.dispose();
    trendChart = echarts.init(trendChartRef.value);
  }

  console.log('绘制趋势图数据:', data);

  // 处理趋势数据
  const dates = [];
  const presentData = [];
  const lateData = [];
  const absentData = [];
  const leaveData = [];

  if (Array.isArray(data)) {
    data.forEach(item => {
      dates.push(item.date);
      presentData.push(item.present || item.present_count || 0);
      lateData.push(item.late || item.late_count || 0);
      absentData.push(item.absent || item.absent_count || 0);
      leaveData.push(item.leave || item.leave_count || 0);
    });
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['已签到', '迟到', '缺勤', '请假']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: dates
      }
    ],
    yAxis: [
      {
        type: 'value'
      }
    ],
    series: [
      {
        name: '已签到',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: presentData,
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '迟到',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: lateData,
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '缺勤',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: absentData,
        itemStyle: { color: '#F56C6C' }
      },
      {
        name: '请假',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: leaveData,
        itemStyle: { color: '#909399' }
      }
    ]
  };

  trendChart.setOption(option);
  window.addEventListener('resize', () => trendChart.resize());
};

// 初始化图表
const initCharts = () => {
  nextTick(() => {
    if (pieChartRef.value) {
      pieChart = echarts.init(pieChartRef.value);
    }
    if (typeChartRef.value) {
      typeChart = echarts.init(typeChartRef.value);
    }
    if (trendChartRef.value) {
      trendChart = echarts.init(trendChartRef.value);
    }
  });
};

// 页面挂载时初始化数据
onMounted(() => {
  nextTick(() => {
    initCharts();
    setTimeout(() => {
      fetchStatisticsData();
    }, 500);
  });
});

// 页面卸载时清理图表
onBeforeUnmount(() => {
  if (pieChart) {
    pieChart.dispose();
  }
  if (typeChart) {
    typeChart.dispose();
  }
  if (trendChart) {
    trendChart.dispose();
  }
});

// 导出给父组件调用的方法
defineExpose({
  fetchStatistics: fetchStatisticsData
});
</script>

<style scoped>
.attendance-statistics-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
}

.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.card-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: white;
}

.blue {
  background-color: #409EFF;
}

.green {
  background-color: #67C23A;
}

.orange {
  background-color: #E6A23C;
}

.red {
  background-color: #F56C6C;
}

.stat-info {
  flex-grow: 1;
}

.stat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-header {
  padding: 0 0 20px 0;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.chart-container {
  height: 300px;
}

.trend-chart {
  height: 400px;
}

.statistics-table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}
</style> 