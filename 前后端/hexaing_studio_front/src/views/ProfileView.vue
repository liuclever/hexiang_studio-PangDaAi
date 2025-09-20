<template>
  <div class="profile-container">
    <el-card class="page-header-card" shadow="hover">
      <div class="page-title">
        <h2>个人中心</h2>
      </div>
    </el-card>

    <div class="main-content">
      <div class="profile-content">
        <!-- 基本信息 -->
        <el-card shadow="hover" class="profile-card">
          <template #header>
            <div class="card-header">
              <h3>基本信息</h3>
            </div>
          </template>
          
          <div class="user-avatar-container">
            <el-avatar
              :size="100"
              :src="avatarUrl"
              class="avatar"
            >
              <img src="/images/default-avatar.svg" alt="默认头像" />
            </el-avatar>
          </div>
          
          <el-descriptions :column="1" border class="info-descriptions">
            <el-descriptions-item label="用户名">
              {{ userInfo.name || '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="用户ID">
              {{ userInfo.userId || '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="电子邮箱">
              {{ userInfo.email || '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号码">
              {{ userInfo.phone || '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="用户角色">
              <el-tag :type="getRoleTagType(userInfo.roleId)">
                {{ getRoleName(userInfo.roleId) || '未设置' }}
              </el-tag>
            </el-descriptions-item>
            
            <!-- 学生特有字段 -->
            <template v-if="isStudent">
              <el-descriptions-item label="学号">
                {{ userInfo.studentNumber || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="专业班级">
                {{ userInfo.major || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="宿舍">
                {{ userInfo.dormitory || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="辅导员">
                {{ userInfo.counselor || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="userInfo.gradeYear" label="年级">
                {{ userInfo.gradeYear || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="userInfo.score !== undefined" label="积分">
                {{ userInfo.score || '0' }}
              </el-descriptions-item>
            </template>
            
            <!-- 教师特有字段 -->
            <template v-if="isTeacher">
              <el-descriptions-item label="职称">
                {{ userInfo.title || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="userInfo.officeLocation" label="办公室">
                {{ userInfo.officeLocation || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="userInfo.positionName" label="职位">
                {{ userInfo.positionName || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="userInfo.directionIdNames && userInfo.directionIdNames.length > 0" label="培训方向">
                <div class="direction-tags">
                  <el-tag 
                    v-for="direction in userInfo.directionIdNames" 
                    :key="direction" 
                    class="direction-tag"
                    size="small"
                  >
                    {{ direction }}
                  </el-tag>
                </div>
              </el-descriptions-item>
            </template>
            
            <el-descriptions-item label="注册时间">
              {{ formatDate(userInfo.createTime) || '未记录' }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ lastLoginTime || '未记录' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" @click="goToAccountSettings">
            <el-icon><Setting /></el-icon>账号设置
          </el-button>
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>返回
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Back, Setting } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { getUserId } from '@/utils/auth';
import { resolveAvatarUrl } from '@/utils/fileUtils';
import { format } from 'date-fns';

interface ApiResponse {
  code: number;
  msg: string;
  data: any;
}

const router = useRouter();
const lastLoginTime = ref(localStorage.getItem('last_login_time') || new Date().toLocaleString());
const avatarUrl = ref('');
const loading = ref(false);

// 角色映射
const roleMap: { [key: number]: string } = {
  0: '访客',
  1: '学生',
  2: '教师',
  3: '管理员',
  4: '超级管理员'
};

// 用户信息
interface UserInfo {
  userId: string;
  name: string;
  email: string;
  phone: string;
  roleId: number;
  role: string;
  avatar: string;
  studentNumber: string;
  major: string;
  dormitory: string;
  counselor: string;
  title: string;
  gradeYear: string;
  score: number;
  positionId: number;
  positionName: string;
  officeLocation: string;
  createTime: null | string | number | Date;
  directionIdNames: string[];
  [key: string]: any; // 添加索引签名以支持动态属性访问
}

const userInfo = reactive<UserInfo>({
  userId: '',
  name: '',
  email: '',
  phone: '',
  roleId: 1, // 默认为学生
  role: '',
  avatar: '',
  studentNumber: '',
  major: '',
  dormitory: '',
  counselor: '',
  title: '',
  gradeYear: '',
  score: 0,
  positionId: 0,
  positionName: '',
  officeLocation: '',
  createTime: null,
  directionIdNames: []
});

// 计算属性：是否为学生
const isStudent = computed(() => {
  return userInfo.roleId === 1 || userInfo.role === '学生';
});

// 计算属性：是否为教师
const isTeacher = computed(() => {
  return userInfo.roleId === 2 || userInfo.role === '教师';
});

// 计算属性：是否为管理员
const isAdmin = computed(() => {
  return userInfo.roleId === 3 || userInfo.roleId === 4 || 
         userInfo.role === '管理员' || userInfo.role === '超级管理员';
});

// 获取角色名称
const getRoleName = (roleId: number) => {
  return roleMap[roleId] || '未知角色';
};

// 获取角色标签类型
const getRoleTagType = (roleId: number) => {
  switch (roleId) {
    case 0: return 'info';     // 访客
    case 1: return 'primary';  // 学生
    case 2: return 'success';  // 教师
    case 3: return 'warning';  // 管理员
    case 4: return 'danger';   // 超级管理员
    default: return 'info';
  }
};

// 获取用户信息
const fetchUserInfo = async () => {
  loading.value = true;
  try {
    // 使用新的个人信息接口，无需传递userId，自动获取当前用户信息
    const response: ApiResponse = await request.get('/admin/user/profile');
    
    if (response?.code === 200 && response?.data) {
      const data = response.data;
      
      // 处理roleId字段，支持多种字段名
      const roleId = data.role_id || data.roleId || 1;
      userInfo.roleId = Number(roleId);
      userInfo.role = roleMap[userInfo.roleId] || '未知';
      
      // 映射基本字段
      Object.keys(userInfo).forEach(key => {
        // 处理snake_case到camelCase的转换
        const snakeKey = key.replace(/[A-Z]/g, c => '_' + c.toLowerCase());
        
        if (data[key] !== undefined) {
          userInfo[key] = data[key];
        } else if (data[snakeKey] !== undefined) {
          userInfo[key] = data[snakeKey];
        }
      });
      
      // 特殊处理directionIdNames字段
      if (data.directionIdNames) {
        userInfo.directionIdNames = data.directionIdNames;
      } else if (data.directionNames) {
        userInfo.directionIdNames = data.directionNames;
      } else {
        userInfo.directionIdNames = [];
      }
      
      // 处理头像URL
      avatarUrl.value = resolveAvatarUrl(data.avatar);
    } else {
      ElMessage.error(response?.msg || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败');
  } finally {
    loading.value = false;
  }
};

// 格式化日期
const formatDate = (date: string | number | Date | null) => {
  if (!date) return '';
  try {
    return format(new Date(date), 'yyyy-MM-dd HH:mm:ss');
  } catch (e) {
    return date;
  }
};

// 跳转到账号设置页面
const goToAccountSettings = () => {
  router.push('/account-settings');
};

// 返回上一页
const goBack = () => {
  router.back();
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background-color: #f5f7fa;
  
  .page-header-card {
    margin-bottom: 20px;
    
    .page-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
      }
    }
  }
  
  .main-content {
    display: flex;
    justify-content: center;
    
    .profile-content {
      width: 100%;
      max-width: 650px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      
      .profile-card {
        width: 100%;
      }
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        h3 {
          margin: 0;
          font-size: 18px;
          font-weight: 600;
        }
      }
      
      .user-avatar-container {
        display: flex;
        justify-content: center;
        margin: 20px 0;
        
        .avatar {
          border: 3px solid #fff;
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        }
      }
      
      .info-descriptions {
        margin-top: 20px;
      }
      
      .action-buttons {
        display: flex;
        justify-content: center;
        gap: 20px;
        margin-top: 20px;
      }
      
      .direction-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        
        .direction-tag {
          margin-right: 6px;
          margin-bottom: 6px;
        }
      }
    }
  }
}

.el-button {
  .el-icon {
    margin-right: 8px;
  }
}
</style> 