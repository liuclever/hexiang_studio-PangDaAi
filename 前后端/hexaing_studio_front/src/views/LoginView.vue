<template>
  <div class="login-container">
    <!-- 登录卡片 -->
    <div class="login-card-container" :class="{ 'show-form': showLoginForm }">
      <!-- 登录卡片 -->
      <div class="login-card" ref="loginCard">
        <!-- 卡片内容 -->
        <div class="card-content">
          <!-- Logo和标题 -->
          <div class="brand-container">
            <div class="logo-container">
              <img src="/images/logo.svg" alt="Logo" class="logo" />
              <div class="logo-ring"></div>
            </div>
            <h1 class="app-title">何湘技能大师工作室</h1>
            <div class="title-separator">
              <span></span>
            </div>
          </div>
          
          <!-- 欢迎信息 -->
          <div class="welcome-message">
            <h2>欢迎回来</h2>
            <p>请登录您的账号以继续访问</p>
      </div>
      
          <!-- 登录表单 -->
          <form class="login-form" @submit.prevent="handleLogin">
            <!-- 用户名输入框 -->
            <div class="form-group">
              <label for="username" class="input-label">用户名</label>
              <div class="input-container" :class="{ 'input-focus': usernameFocused, 'input-filled': !!loginForm.username }">
                <el-icon class="input-icon"><User /></el-icon>
                <input 
                  id="username"
                    v-model="loginForm.username"
                  type="text"
                  placeholder="请输入用户名"
                  @focus="usernameFocused = true"
                  @blur="usernameFocused = false"
                  autocomplete="username"
                />
              </div>
                </div>
        
            <!-- 密码输入框 -->
            <div class="form-group">
              <label for="password" class="input-label">密码</label>
              <div class="input-container" :class="{ 'input-focus': passwordFocused, 'input-filled': !!loginForm.password }">
                <el-icon class="input-icon"><Lock /></el-icon>
                <input 
                  id="password"
            v-model="loginForm.password" 
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入密码"
                  @focus="passwordFocused = true"
                  @blur="passwordFocused = false"
                  autocomplete="current-password"
                />
                <div class="password-toggle" @click="showPassword = !showPassword">
                  <el-icon v-if="showPassword"><View /></el-icon>
                  <el-icon v-else><Hide /></el-icon>
                </div>
              </div>
        </div>

                 <!-- 验证码输入框（智能显示） -->
         <transition name="captcha-fade">
           <div v-if="showCaptcha" class="form-group captcha-group">
             <label for="captcha" class="input-label">
               <span>验证码</span>
               <span class="required-mark">*</span>
             </label>
          <div class="captcha-input-wrapper">
            <div class="input-container captcha-input-container" 
                 :class="{ 'input-focus': captchaFocused, 'input-filled': !!loginForm.captchaCode }">
              <el-icon class="input-icon"><Key /></el-icon>
              <input 
                id="captcha"
                v-model="loginForm.captchaCode"
                type="text"
                placeholder="请输入验证码"
                maxlength="4"
                @focus="captchaFocused = true"
                @blur="captchaFocused = false"
                @keyup.enter="handleLogin"
                autocomplete="off"
              />
            </div>
            <!-- 验证码图片 -->
            <div class="captcha-image-wrapper" @click="refreshCaptcha">
              <img 
                v-if="captchaData?.imageBase64" 
                :src="captchaData.imageBase64" 
                alt="验证码"
                class="captcha-image"
              />
                             <div v-else class="captcha-placeholder">
                 <div class="captcha-loading">
                   <el-icon class="loading"><Loading /></el-icon>
                 </div>
                 <div class="captcha-refresh-hint">
                   <el-icon><Refresh /></el-icon>
                   <span>点击刷新</span>
                 </div>
               </div>
            </div>
          </div>
                       <!-- 验证码提示 -->
             <div class="captcha-hint">
               <el-icon class="info-icon"><InfoFilled /></el-icon>
               <span>{{ captchaData?.hint || '请输入图中4位数字' }}</span>
             </div>
           </div>
         </transition>
        
            <!-- 记住我和忘记密码 -->
            <div class="form-options">
              <div class="remember-me">
                <el-checkbox v-model="loginForm.rememberMe">记住我 </el-checkbox>
              </div>
              <a href="#" class="forgot-password" @click.prevent="showResetPassword">忘记密码?</a>
                </div>
                
            <!-- 登录按钮 -->
            <button 
              type="submit" 
              class="login-button"
              :class="{ 'button-loading': loading }"
              :disabled="loading"
            >
              <span v-if="!loading">登录</span>
              <div v-else class="button-loader"></div>
                </button>
              </form>
        </div>
      </div>
    </div>

    <!-- 重置密码抽屉 -->
    <el-drawer
      v-model="resetPasswordVisible"
      title="重置密码"
      direction="rtl"
      size="400px"
      :with-header="true"
      custom-class="reset-password-drawer"
    >
      <div class="reset-password-form">
        <p class="reset-description">请输入您的注册邮箱，我们将发送重置密码链接给您。</p>
        <div class="form-group">
          <label for="reset-email" class="input-label">电子邮箱</label>
          <div class="input-container" :class="{ 'input-focus': emailFocused, 'input-filled': !!resetForm.email }">
            <el-icon class="input-icon"><Message /></el-icon>
            <input 
              id="reset-email"
            v-model="resetForm.email"
              type="email"
              placeholder="请输入邮箱地址"
              @focus="emailFocused = true"
              @blur="emailFocused = false"
            />
    </div>
        </div>
        <button 
          class="reset-button"
          @click="handleResetPassword"
          :disabled="resetLoading"
          :class="{ 'button-loading': resetLoading }"
        >
          <span v-if="!resetLoading">发送重置链接</span>
          <div v-else class="button-loader"></div>
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, View, Hide, Message, Key, Loading, Refresh, InfoFilled } from '@element-plus/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const loading = ref(false);
const resetLoading = ref(false);
const showLoginForm = ref(false);
const loginCard = ref<HTMLElement | null>(null);

// 输入框状态
const usernameFocused = ref(false);
const passwordFocused = ref(false);
const emailFocused = ref(false);
const showPassword = ref(false);
const captchaFocused = ref(false); // 新增验证码输入框焦点状态

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false, // 改为 rememberMe
  captchaCode: '' // 新增验证码
});

// 重置密码表单
const resetForm = reactive({
  email: ''
});

// 重置密码抽屉
const resetPasswordVisible = ref(false);

// 验证码数据和状态
const captchaData = ref<{ 
  sessionId: string; 
  imageBase64: string; 
  hint: string; 
  expireSeconds: number;
} | null>(null);
const showCaptcha = ref(false); // 控制验证码显示
const loginFailCount = ref(0); // 登录失败次数
const needsCaptcha = ref(false); // 是否需要验证码

// 粒子效果样式
const getParticleStyle = (index: number) => {
  const size = Math.random() * 20 + 10;
  const speed = Math.random() * 50 + 20;
  const delay = Math.random() * 5;
  const posX = Math.random() * 100;
  const posY = Math.random() * 100;
  const opacity = Math.random() * 0.6 + 0.2;
    
    return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${posX}%`,
    top: `${posY}%`,
    opacity: opacity.toString(),
    animationDuration: `${speed}s`,
    animationDelay: `${delay}s`
    };
};

// 3D卡片效果
let mouseX = 0;
let mouseY = 0;
let windowWidth = window.innerWidth;
let windowHeight = window.innerHeight;

const handleMouseMove = (e: MouseEvent) => {
  if (!loginCard.value) return;
  
  mouseX = e.clientX;
  mouseY = e.clientY;
  
  const centerX = windowWidth / 2;
  const centerY = windowHeight / 2;
  
  const rotateY = ((mouseX - centerX) / centerX) * 5; // 最大旋转角度为5度
  const rotateX = ((centerY - mouseY) / centerY) * 5;
  
  loginCard.value.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
};

const handleMouseLeave = () => {
  if (!loginCard.value) return;
  loginCard.value.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg)';
};

const handleResize = () => {
  windowWidth = window.innerWidth;
  windowHeight = window.innerHeight;
};

// 显示重置密码抽屉
const showResetPassword = () => {
  resetPasswordVisible.value = true;
};

// 处理重置密码
const handleResetPassword = async () => {
  if (!resetForm.email) {
    ElMessage.warning('请输入邮箱地址');
    return;
  }
  
  resetLoading.value = true;
  try {
    // 这里添加发送重置密码邮件的逻辑
    await new Promise(resolve => setTimeout(resolve, 1500)); // 模拟API请求
    ElMessage.success('重置链接已发送到您的邮箱');
    resetPasswordVisible.value = false;
    resetForm.email = '';
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试');
  } finally {
    resetLoading.value = false;
  }
};

// 生成验证码
const generateCaptcha = async () => {
  try {
    const resp: any = await request.post('/admin/captcha/generate');

    // 拦截器已返回解包后的 { code, msg, data }
    const res: any = resp && typeof resp === 'object' && 'code' in resp ? resp : resp?.data;

    // 兼容多种成功标识
    const isSuccess = (
      res && (
        String(res.code) === '200' ||
        String(res.code) === '1' ||
        res.success === true ||
        // 如果直接就是数据对象（没有 code），也视为成功
        (!('code' in res) && (res.imageBase64 || res.sessionId))
      )
    );

    if (isSuccess) {
      captchaData.value = res.data ? res.data : res;
      showCaptcha.value = true;
    } else {
      throw new Error(res?.msg || '生成验证码失败');
    }
  } catch (error) {
    console.error('生成验证码失败:', error);
    ElMessage.error('生成验证码失败，请稍后重试');
  }
};

// 刷新验证码
const refreshCaptcha = async () => {
  if (!captchaData.value?.sessionId) {
    // 如果没有sessionId，重新生成
    await generateCaptcha();
    return;
  }
  
  try {
    const resp: any = await request.post('/admin/captcha/refresh', {
      sessionId: captchaData.value.sessionId
    });
    
    const res: any = resp && typeof resp === 'object' && 'code' in resp ? resp : resp?.data;
    const isSuccess = (
      res && (
        String(res.code) === '200' ||
        String(res.code) === '1' ||
        res.success === true ||
        (!('code' in res) && (res.imageBase64 || res.sessionId))
      )
    );

    if (isSuccess) {
      captchaData.value = res.data ? res.data : res;
    } else {
      throw new Error(res?.msg || '刷新验证码失败');
    }
  } catch (error) {
    console.error('刷新验证码失败:', error);
    // 刷新失败时重新生成
    await generateCaptcha();
  }
};



// 处理登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  // 智能验证码检查
  if (showCaptcha.value && !loginForm.captchaCode) {
    ElMessage.warning('请输入验证码');
    return;
  }
  
  loading.value = true;
    
  try {
    // 构建登录请求参数
    const loginParams: any = {
      userName: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    };
    
    // 如果显示验证码，添加验证码参数
    if (showCaptcha.value && captchaData.value) {
      loginParams.captchaSessionId = captchaData.value.sessionId;
      loginParams.captchaCode = loginForm.captchaCode;
    }
    const response: any = await request.post('/admin/user/login', loginParams);
    
          console.log("登录响应:", response);
          
          // 🔒 安全检查：验证响应的业务状态码
          // 拦截器已解包响应，直接检查业务状态
          const codeStr = String(response?.code || '');
          if (!response || (codeStr !== '200' && codeStr !== '1' && !response.success)) {
            const errorMsg = response?.msg || '登录失败';
            console.error('登录业务逻辑失败:', errorMsg);
            ElMessage.error(errorMsg);
            return;
          }
          
          const responseData = response.data || {};
          const data = responseData.data || responseData;
          
          const token = data.token;
          const userId = data.userId; 
          const userName = data.userName;
          
          console.log("解析出的登录信息:", { token, userId, userName });

    // 进行严格的类型和值检查
          if (!token || typeof userId !== 'number' || userId <= 0) {
            ElMessage.error('登录失败：服务器返回信息无效，请联系管理员');
            // 清理可能已存入的错误信息
            localStorage.removeItem('token');
            localStorage.removeItem('user_id');
            localStorage.removeItem('user_name');
            return;
          }
          
          // 存储token和用户信息
          localStorage.setItem('token', token);
          localStorage.setItem('user_id', String(userId));
          localStorage.setItem('user_name', data.name || userName || '新用户');
          if (data.roleId !== undefined) {
            localStorage.setItem('user_role_id', String(data.roleId));
          }
          if (data.avatar) {
            localStorage.setItem('user_avatar', data.avatar);
          }
          
          // 记录登录时间
          localStorage.setItem('last_login_time', new Date().toLocaleString());
          
          if (loginForm.rememberMe) {
        localStorage.setItem('remember', 'true');
        localStorage.setItem('saved_username', loginForm.username);
          } else {
            localStorage.removeItem('remember');
            localStorage.removeItem('saved_username');
          }
          
          ElMessage.success('登录成功');
          
          // 登录成功后清理状态
          loginFailCount.value = 0;
          showCaptcha.value = false;
          captchaData.value = null;
          loginForm.captchaCode = '';
          
      // 动画结束后跳转
      setTimeout(() => {
          router.push('/');
      }, 500);
  } catch (error: any) {
    console.error('登录失败:', error);
    
    // 登录失败处理
    loginFailCount.value++;
    
    // 检查是否需要显示验证码（失败3次后显示）
    if (loginFailCount.value >= 3 && !showCaptcha.value) {
      showCaptcha.value = true;
      await generateCaptcha();
      ElMessage.warning('登录失败次数较多，请输入验证码');
    } else if (showCaptcha.value) {
      // 如果已显示验证码，刷新验证码
      await refreshCaptcha();
      loginForm.captchaCode = ''; // 清空验证码输入
    }
    
    // 处理特定错误信息
    const errorMsg = error?.response?.data?.msg || error?.message || '登录失败';
    
    // 检查是否包含验证码错误
    if (errorMsg.includes('验证码') && !showCaptcha.value) {
      showCaptcha.value = true;
      await generateCaptcha();
    }
    
    // 统一显示错误信息
    if (errorMsg.includes('网络') || error?.code === 'NETWORK_ERROR') {
      ElMessage.error('网络连接失败，请检查网络设置');
    } else {
      ElMessage.error(errorMsg);
    }
    
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  // 检查是否记住了用户名
  const remember = localStorage.getItem('remember');
  const savedUsername = localStorage.getItem('saved_username');
  
  if (remember && savedUsername) {
    loginForm.username = savedUsername;
    loginForm.rememberMe = true;
  }

  // 初始化时不显示验证码，根据登录失败情况智能显示
  showCaptcha.value = false;
  
  // 添加3D卡片效果的事件监听
  window.addEventListener('mousemove', handleMouseMove);
  window.addEventListener('resize', handleResize);
  document.addEventListener('mouseleave', handleMouseLeave);
  
  // 显示登录表单的动画
  setTimeout(() => {
    showLoginForm.value = true;
  }, 300);
});

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('resize', handleResize);
  document.removeEventListener('mouseleave', handleMouseLeave);
});
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background-image: url('/images/back_group.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 登录卡片容器 */
.login-card-container {
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.8s ease, transform 0.8s ease;
  
  &.show-form {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 登录卡片 */
.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(15px);
  border-radius: 20px;
  box-shadow: 
    0 10px 40px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  padding: 40px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  transform-style: preserve-3d;
  will-change: transform;
  
  &:hover {
    box-shadow: 
      0 15px 50px rgba(0, 0, 0, 0.2),
      0 0 0 1px rgba(255, 255, 255, 0.3);
  }
}

/* 卡片内容 */
.card-content {
  display: flex;
  flex-direction: column;
  gap: 22px;
    }
    
/* Logo和标题 */
.brand-container {
    display: flex;
  flex-direction: column;
    align-items: center;
  margin-bottom: 10px;
  
  .logo-container {
    position: relative;
      width: 80px;
      height: 80px;
    margin-bottom: 16px;
    
    .logo {
    width: 100%;
    height: 100%;
      object-fit: contain;
      z-index: 2;
      position: relative;
      border-radius: 50%;
    }
    
    .logo-ring {
      position: absolute;
      top: -7px;
      left: -7px;
      width: calc(100% + 10px);
      height: calc(100% + 10px);
      border-radius: 50%;
      border: 2px solid var(--el-color-primary-light-5);
      animation: rotate 10s linear infinite;
    
      &::before, &::after {
        content: '';
        position: absolute;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background-color: var(--el-color-primary);
      }
      
      &::before {
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%) translateY(-45px);
      }
      
      &::after {
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%) translateY(45px);
      }
    }
    }
    
  .app-title {
    font-size: 24px;
    font-weight: 600;
    color: var(--el-color-primary-dark-2);
    margin: 0;
    text-align: center;
  }
  
  .title-separator {
    width: 100%;
    margin: 16px 0;
        display: flex;
        align-items: center;
        justify-content: center;
    
    span {
      width: 60px;
      height: 3px;
      background: linear-gradient(90deg, 
        var(--el-color-primary-light-5),
        var(--el-color-primary),
        var(--el-color-primary-light-5)
      );
      border-radius: 3px;
        }
      }
}

/* 欢迎信息 */
.welcome-message {
  text-align: center;
  
  h2 {
    font-size: 22px;
      font-weight: 600;
    color: #303133;
      margin: 0 0 8px;
  }
  
  p {
    font-size: 14px;
    color: #606266;
    margin: 0;
      }
    }
    
/* 登录表单 */
    .login-form {
    display: flex;
    flex-direction: column;
  gap: 20px;
}
    
/* 表单组 */
    .form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  .input-label {
    font-size: 14px;
    font-weight: 500;
    color: #606266;
  }
  
  .input-container {
    position: relative;
    display: flex;
    align-items: center;
    height: 50px;
    border: 1px solid #dcdfe6;
          border-radius: 12px;
    padding: 0 16px;
    background-color: #f5f7fa;
          transition: all 0.3s ease;
          
          &:hover {
      border-color: #c0c4cc;
          }
          
    &.input-focus {
      border-color: var(--el-color-primary);
      background-color: #fff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
          }
          
    &.input-filled {
      background-color: #fff;
    }
    
    .input-icon {
      color: #909399;
      font-size: 18px;
          margin-right: 10px;
          }
    
    input {
            flex: 1;
      height: 100%;
      border: none;
      outline: none;
      background: transparent;
      font-size: 16px;
      color: #303133;
      
      &::placeholder {
        color: #c0c4cc;
      }
    }
    
    .password-toggle {
      cursor: pointer;
      color: #909399;
      font-size: 18px;
      transition: color 0.3s;
      
      &:hover {
        color: var(--el-color-primary);
        }
        }
      }
 }

/* 验证码过渡动画 */
.captcha-fade-enter-active,
.captcha-fade-leave-active {
  transition: all 0.3s ease;
}

.captcha-fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.captcha-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 验证码输入框 */
.captcha-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  .input-label {
    .required-mark {
      color: #f56c6c;
      margin-left: 4px;
    }
  }

  .captcha-input-wrapper {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .captcha-input-container {
    flex: 1;
  }

  .captcha-image-wrapper {
    width: 100px;
    height: 50px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    overflow: hidden;
    background-color: #f5f7fa;
    transition: border-color 0.3s ease;

    &:hover {
      border-color: #c0c4cc;
    }

    .captcha-image {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

         .captcha-placeholder {
       display: flex;
       flex-direction: column;
       align-items: center;
       justify-content: center;
       width: 100%;
       height: 100%;
       gap: 4px;
     }

     .captcha-loading {
       display: flex;
       align-items: center;
       justify-content: center;
       color: #909399;
       
       .loading {
         animation: spin 1s linear infinite;
       }
     }

     .captcha-refresh-hint {
       display: flex;
       align-items: center;
       gap: 4px;
       font-size: 12px;
       color: #909399;
       cursor: pointer;
       transition: color 0.3s ease;

       &:hover {
         color: var(--el-color-primary);
       }
       
       span {
         white-space: nowrap;
       }
     }
  }

  .captcha-hint {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    color: #909399;

    .info-icon {
      color: #909399;
      font-size: 16px;
    }
  }
}

/* 表单选项 */
.form-options {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
  .remember-me {
        :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
      background-color: var(--el-color-primary);
      border-color: var(--el-color-primary);
    }
    
    :deep(.el-checkbox__label) {
      font-size: 14px;
      color: #606266;
    }
      }
      
      .forgot-password {
        font-size: 14px;
    color: var(--el-color-primary);
    text-decoration: none;
    transition: color 0.3s;
        
        &:hover {
      color: var(--el-color-primary-dark-2);
          text-decoration: underline;
        }
        }
      }
      
/* 登录按钮 */
      .login-button {
  height: 50px;
  background: linear-gradient(135deg, 
    var(--el-color-primary-light-3), 
    var(--el-color-primary),
    var(--el-color-primary-dark-2)
  );
  background-size: 200% 100%;
      border: none;
      border-radius: 12px;
  color: white;
  font-size: 16px;
  font-weight: 600;
      cursor: pointer;
        transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
      display: flex;
      justify-content: center;
      align-items: center;
        
        &:hover {
    background-position: 100% 0;
          transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
        }
        
        &:active {
    transform: translateY(0);
    box-shadow: 0 4px 8px rgba(64, 158, 255, 0.2);
      }
  
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
  
  &::before {
    content: '';
          position: absolute;
    top: 0;
    left: -100%;
  width: 100%;
    height: 100%;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.2),
      transparent
    );
    transition: left 0.7s ease;
  }
  
  &:hover::before {
    left: 100%;
  }
  
  &.button-loading::before {
    display: none;
  }
}

/* 按钮加载动画 */
.button-loader {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 重置密码抽屉 */
:deep(.reset-password-drawer) {
  .el-drawer__header {
    margin-bottom: 0;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .el-drawer__title {
  font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .el-drawer__body {
    padding: 0;
  }
}

/* 重置密码表单 */
.reset-password-form {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  .reset-description {
    font-size: 14px;
    color: #606266;
    margin: 0;
    line-height: 1.6;
  }
  
  .reset-button {
    height: 50px;
    background: linear-gradient(135deg, 
      var(--el-color-primary-light-3), 
      var(--el-color-primary)
    );
    border: none;
    border-radius: 12px;
    color: white;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    justify-content: center;
    align-items: center;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 15px rgba(64, 158, 255, 0.2);
    }
    
    &:active {
      transform: translateY(0);
    }
    
    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }
  }
}

/* 动画 */
@keyframes float {
  0% {
    transform: translateY(0) translateX(0);
  }
  25% {
    transform: translateY(-100px) translateX(100px);
  }
  50% {
    transform: translateY(-200px) translateX(0);
  }
  75% {
    transform: translateY(-100px) translateX(-100px);
  }
  100% {
    transform: translateY(0) translateX(0);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 验证码加载动画 */
.loading {
  animation: spin 1s linear infinite;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }
  
  .brand-container {
    .logo-container {
      width: 60px;
      height: 60px;
  }
    
    .app-title {
      font-size: 20px;
  }
}

  .welcome-message {
    h2 {
      font-size: 18px;
  }
    
    p {
      font-size: 13px;
    }
  }
  
  .form-group {
    .input-container {
      height: 45px;
    }
  }
  
  .login-button {
    height: 45px;
  }
}
</style> 
