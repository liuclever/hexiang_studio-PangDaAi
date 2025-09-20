<template>
  <div class="settings-container">
    <el-card class="page-header-card" shadow="hover">
      <div class="page-title">
        <h2>账号设置</h2>
      </div>
    </el-card>

    <div class="main-content">
      <div class="settings-content">
        <!-- 密码设置 -->
        <el-card shadow="hover" class="settings-card">
          <template #header>
            <div class="card-header">
              <h3>修改密码</h3>
            </div>
          </template>
          
          <el-form 
            ref="passwordFormRef" 
            :model="passwordForm" 
            :rules="passwordRules" 
            label-width="120px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password" 
                placeholder="请输入原密码" 
                show-password
                :disabled="loading"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password" 
                placeholder="请输入新密码" 
                show-password
                :disabled="loading"
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码" 
                show-password
                :disabled="loading"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleChangePassword" 
                :loading="loading"
              >
                确认修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 安全设置 -->
        <el-card shadow="hover" class="settings-card">
          <template #header>
            <div class="card-header">
              <h3>账号安全</h3>
            </div>
          </template>
          
          <el-descriptions :column="1" border>
            <el-descriptions-item label="登录设备">
              <el-tag>当前设备</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最后登录时间">
              {{ lastLoginTime || '未记录' }}
            </el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag type="success">正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>返回
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { Back } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { getUserId } from '@/utils/auth';
import type { FormInstance, FormRules } from 'element-plus';

const router = useRouter();
const loading = ref(false);
const passwordFormRef = ref<FormInstance>();
const lastLoginTime = ref(localStorage.getItem('last_login_time') || new Date().toLocaleString());

// 密码表单数据
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 验证函数
const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'));
  } else {
    if (passwordForm.confirmPassword !== '') {
      if (!passwordFormRef.value) return;
      passwordFormRef.value.validateField('confirmPassword', () => {});
    }
    callback();
  }
};

const validateConfirmPass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致'));
  } else {
    callback();
  }
};

// 密码表单验证规则
const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, trigger: 'blur', validator: validatePass },
    { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', validator: validateConfirmPass }
  ]
});

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return;
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const userId = getUserId();
        if (userId === null) {
          ElMessage.error('未找到用户ID，请重新登录');
          return;
        }
        
        console.log('发送修改密码请求，用户ID:', userId);
        const response = await request.post('/admin/user/change-password', {
          userId: userId,
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        });
        
        console.log('修改密码响应:', response);
        
        // 处理响应数据，确保类型安全
        const responseData = response as any;
        if (responseData && 
            (responseData.code === 200 || responseData.code === 1 || 
            (responseData.data && (responseData.data.code === 200 || responseData.data.code === 1)))) {
          ElMessage.success('密码修改成功');
          // 重置表单
          passwordForm.oldPassword = '';
          passwordForm.newPassword = '';
          passwordForm.confirmPassword = '';
        } else {
          // 获取错误消息，尝试从多个可能的位置获取
          const errorMsg = 
            (responseData && responseData.msg) || 
            (responseData && responseData.data && responseData.data.msg) || 
            '密码修改失败，请确认旧密码是否正确';
          
          console.error('修改密码失败:', errorMsg);
          ElMessage.error(errorMsg);
        }
      } catch (error) {
        console.error('修改密码请求异常:', error);
        // ElMessage.error('修改密码失败，请稍后重试'); // 由拦截器统一处理
      } finally {
        loading.value = false;
      }
    }
  });
};

// 返回上一页
const goBack = () => {
  router.back();
};
</script>

<style lang="scss" scoped>
.settings-container {
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
    
    .settings-content {
      width: 100%;
      max-width: 650px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      
      .settings-card {
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
      
      .action-buttons {
        display: flex;
        justify-content: center;
        margin-top: 20px;
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