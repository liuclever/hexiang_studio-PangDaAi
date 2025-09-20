<template>
  <div class="check-in-component">
    <el-card shadow="hover" class="check-in-card">
      <template #header>
        <div class="check-in-header">
          <h3>{{ attendance.name }}</h3>
          <el-tag :type="getStatusType">{{ getStatusText }}</el-tag>
        </div>
      </template>
      
      <div class="check-in-content">
        <div class="info-row">
          <el-icon><Calendar /></el-icon>
          <span class="label">时间：</span>
          <span class="value">{{ formatDateTime(attendance.startTime) }} 至 {{ formatDateTime(attendance.endTime) }}</span>
        </div>
        
        <div class="info-row">
          <el-icon><Location /></el-icon>
          <span class="label">地点：</span>
          <span class="value">{{ attendance.location }}</span>
        </div>
        
        <div class="info-row">
          <el-icon><Aim /></el-icon>
          <span class="label">签到范围：</span>
          <span class="value">{{ attendance.radius }} 米</span>
        </div>
        
        <div v-if="userSignInInfo" class="sign-in-status">
          <el-result
            :icon="getStatusIcon"
            :title="getStatusTitle"
            :sub-title="getStatusSubTitle"
          >
            <template #extra>
              <div v-if="userSignInInfo.signInTime" class="sign-in-time">
                <el-icon><Timer /></el-icon>
                <span>签到时间: {{ formatDateTime(userSignInInfo.signInTime) }}</span>
              </div>
            </template>
          </el-result>
        </div>
        
        <div v-else class="sign-in-actions">
          <el-button 
            type="primary" 
            size="large" 
            :loading="isProcessingCheckIn" 
            :disabled="!isCheckInEnabled"
            @click="checkIn">
            <el-icon><Position /></el-icon> 立即签到
          </el-button>
          <div class="check-in-status">
            {{ checkInStatusMessage }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Calendar, Location, Aim, Timer, Position, 
         CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue';
import { getCurrentPosition, calculateDistance } from '@/utils/location';
import { studentCheckIn } from '@/utils/api/attendance';

// 定义API响应类型
interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
  timestamp?: number;
}

// 定义考勤与签到信息接口
interface Attendance {
  id: number;
  planId?: number;
  name: string;
  type: 'activity' | 'course' | 'duty';
  startTime: Date | string;
  endTime: Date | string;
  location: string;
  locationLat?: number;
  locationLng?: number;
  locationCoords?: { lat: number; lng: number };
  radius: number;
}

interface SignInInfo {
  signInTime: Date | string;
  signInLocation: string;
  signInCoords: { lat: number; lng: number };
  status: 'present' | 'absent' | 'late' | 'leave';
}

// 接收考勤信息作为 prop
const props = defineProps<{
  attendance: Attendance;
  userId: number;
  existingRecord?: SignInInfo;
}>();

// 发出签到成功或失败事件
const emit = defineEmits(['check-in-success', 'check-in-error']);

// 用户位置信息和签到状态
const userLocation = ref<{ lat: number; lng: number } | null>(null);
const isProcessingCheckIn = ref(false);
const userSignInInfo = ref<SignInInfo | null>(props.existingRecord || null);
const locationError = ref<string | null>(null);
const checkInStatusMessage = ref('准备签到');

// 格式化日期时间
const formatDateTime = (date: Date | string): string => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

// 判断是否在签到范围内
const isWithinRadius = computed(() => {
  if (!userLocation.value) return false;
  
  // 获取位置坐标，优先使用locationCoords，如果不存在则使用locationLat和locationLng
  const locationLat = props.attendance.locationCoords?.lat || props.attendance.locationLat;
  const locationLng = props.attendance.locationCoords?.lng || props.attendance.locationLng;
  
  if (!locationLat || !locationLng) return false;
  
  const distance = calculateDistance(
    userLocation.value.lat,
    userLocation.value.lng,
    locationLat,
    locationLng
  );
  
  return distance <= props.attendance.radius;
});

// 判断是否在考勤时间内
const isWithinTimeRange = computed(() => {
  const now = new Date();
  const startTime = new Date(props.attendance.startTime);
  const endTime = new Date(props.attendance.endTime);
  
  // 考勤开始前15分钟可以签到
  const earlyStart = new Date(startTime);
  earlyStart.setMinutes(earlyStart.getMinutes() - 15);
  
  return now >= earlyStart && now <= endTime;
});

// 判断是否可以签到
const isCheckInEnabled = computed(() => {
  return isWithinRadius.value && isWithinTimeRange.value && !isProcessingCheckIn.value && !userSignInInfo.value;
});

// 签到状态类型
const getStatusType = computed(() => {
  if (userSignInInfo.value) {
    switch (userSignInInfo.value.status) {
      case 'present': return 'success';
      case 'late': return 'warning';
      case 'leave': return 'info';
      case 'absent': return 'danger';
      default: return 'info';
    }
  }
  return 'info';
});

// 签到状态文本
const getStatusText = computed(() => {
  if (userSignInInfo.value) {
    switch (userSignInInfo.value.status) {
      case 'present': return '已签到';
      case 'late': return '迟到';
      case 'leave': return '已请假';
      case 'absent': return '缺勤';
      default: return '未签到';
    }
  }
  return '未签到';
});

// 签到结果图标
const getStatusIcon = computed(() => {
  if (userSignInInfo.value) {
    switch (userSignInInfo.value.status) {
      case 'present': return 'success';
      case 'late': return 'warning';
      case 'leave': return 'info';
      case 'absent': return 'error';
      default: return 'info';
    }
  }
  return 'info';
});

// 签到结果标题
const getStatusTitle = computed(() => {
  if (userSignInInfo.value) {
    switch (userSignInInfo.value.status) {
      case 'present': return '签到成功';
      case 'late': return '已签到（迟到）';
      case 'leave': return '已请假';
      case 'absent': return '缺勤';
      default: return '未签到';
    }
  }
  return '未签到';
});

// 签到结果副标题
const getStatusSubTitle = computed(() => {
  if (userSignInInfo.value) {
    switch (userSignInInfo.value.status) {
      case 'present': return '您已成功签到';
      case 'late': return '您已迟到签到';
      case 'leave': return '已处理请假申请';
      case 'absent': return '未签到，已记为缺勤';
      default: return '请点击签到按钮进行签到';
    }
  }
  return '请点击签到按钮进行签到';
});

// 执行签到逻辑
const checkIn = async () => {
  isProcessingCheckIn.value = true;
  checkInStatusMessage.value = '正在获取位置信息...';
  
  try {
    // 获取当前位置
    userLocation.value = await getCurrentPosition();
    
    // 检查是否在签到范围内
    if (!isWithinRadius.value) {
      checkInStatusMessage.value = '您不在签到范围内，无法签到';
      ElMessage.warning('您不在签到范围内，无法签到');
      isProcessingCheckIn.value = false;
      return;
    }
    
    // 检查是否在考勤时间内
    if (!isWithinTimeRange.value) {
      checkInStatusMessage.value = '不在签到时间内，无法签到';
      ElMessage.warning('不在签到时间内，无法签到');
      isProcessingCheckIn.value = false;
      return;
    }
    
    checkInStatusMessage.value = '正在提交签到...';
    
    // 构建签到数据
    const now = new Date();
    const startTime = new Date(props.attendance.startTime);
    
    // 判断是否迟到
    const isLate = now > startTime;
    
    const signInData = {
      planId: props.attendance.id || props.attendance.planId || 0,
      studentId: props.userId,
      signInTime: now.toISOString(),
      location: props.attendance.location,
      locationLat: userLocation.value.lat,
      locationLng: userLocation.value.lng,
      status: isLate ? 'late' : 'present'
    };
    
    // 调用签到API
    const response = await studentCheckIn(signInData);
    
    // 我们的request拦截器已经处理了响应，直接使用
    if (response && response.code === 1) {
      // 签到成功
      userSignInInfo.value = {
        signInTime: now,
        signInLocation: props.attendance.location,
        signInCoords: userLocation.value,
        status: isLate ? 'late' : 'present'
      };
      
      ElMessage.success(isLate ? '签到成功（迟到）' : '签到成功');
      emit('check-in-success', userSignInInfo.value);
    } else {
      // 签到失败
      throw new Error(response?.msg || '签到失败，请稍后再试');
    }
  } catch (error: any) {
    checkInStatusMessage.value = error.message || '签到失败，请稍后再试';
    locationError.value = error.message;
    ElMessage.error(error.message || '签到失败，请稍后再试');
    emit('check-in-error', error);
  } finally {
    isProcessingCheckIn.value = false;
  }
};

// 组件挂载时尝试获取位置
onMounted(async () => {
  try {
    userLocation.value = await getCurrentPosition();
    
    // 如果不在范围内，提示用户
    if (!isWithinRadius.value) {
      checkInStatusMessage.value = '您不在签到范围内，请移动到指定位置';
    } else if (!isWithinTimeRange.value) {
      const now = new Date();
      const startTime = new Date(props.attendance.startTime);
      const endTime = new Date(props.attendance.endTime);
      
      if (now < startTime) {
        checkInStatusMessage.value = '签到尚未开始';
      } else if (now > endTime) {
        checkInStatusMessage.value = '签到已结束';
      }
    } else {
      checkInStatusMessage.value = '您可以签到了';
    }
  } catch (error: any) {
    locationError.value = error.message;
    checkInStatusMessage.value = error.message;
  }
});
</script>

<style scoped>
.check-in-component {
  width: 100%;
  margin-bottom: 20px;
}

.check-in-card {
  border-radius: 8px;
  overflow: hidden;
}

.check-in-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.check-in-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.check-in-content {
  padding: 10px 0;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
}

.info-row .el-icon {
  font-size: 18px;
  color: #409EFF;
  margin-right: 8px;
}

.info-row .label {
  color: #606266;
  margin-right: 10px;
  font-weight: 500;
}

.info-row .value {
  color: #303133;
}

.sign-in-actions {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.sign-in-actions .el-button {
  width: 180px;
  margin-bottom: 10px;
}

.check-in-status {
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.sign-in-status .sign-in-time {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.sign-in-status .sign-in-time .el-icon {
  margin-right: 5px;
}
</style> 