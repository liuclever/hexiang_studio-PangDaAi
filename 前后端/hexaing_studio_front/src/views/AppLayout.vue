<template>
  <el-container class="app-layout">
    <AppSidebar :is-collapsed="isSidebarCollapsed" />
    <el-container direction="vertical" class="app-layout__main">
      <AppHeader @toggle-sidebar="toggleSidebar" />
      <AppMain>
        <router-view /> <!-- This is where your HomeView will be rendered -->
      </AppMain>
    </el-container>
    
    <!-- AI动画元素 - 左下角 -->
    <transition name="lottie-fade">
      <div v-show="showLottie" class="floating-lottie">
        <!-- 语言气泡 -->
        <transition name="bubble-fade">
          <div v-show="showBubble && currentMessage" class="speech-bubble">
            <div class="bubble-content">
              {{ currentMessage }}
            </div>
            <div class="bubble-arrow"></div>
          </div>
        </transition>
        
        <!-- 小熊动画 -->
        <img 
          src="/images/lottieAIEnter.gif" 
          alt="AI Assistant Animation" 
          class="lottie-animation"
          @click="handleBearClick"
          @dblclick="handleBearDoubleClick"
          style="cursor: pointer;"
        />
      </div>
    </transition>
    
    <!-- 控制按钮 -->
    <div class="lottie-control">
      <el-button 
        circle 
        size="small"
        type="primary"
        @click="toggleLottie"
        :title="showLottie ? '隐藏小熊' : '显示小熊'"
      >
        🐻
      </el-button>
    </div>
    
    <!-- 浮窗聊天 -->
    <FloatingChatWindow 
      v-model:visible="showChat"
      @close="handleChatClose"
    />
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import AppSidebar from '@/components/layout/AppSidebar.vue';
import AppHeader from '@/components/layout/AppHeader.vue';
import AppMain from '@/components/layout/AppMain.vue';
import FloatingChatWindow from '@/components/ai-bear/FloatingChatWindow.vue';
import { getBearMessage, type BearMessage } from '@/api/ai-bear';

const isSidebarCollapsed = ref(false);
const showLottie = ref(true); // 控制小熊显示状态
const showBubble = ref(false); // 控制语言气泡显示状态
const currentMessage = ref(''); // 当前显示的语言气泡内容
const showChat = ref(false); // 控制浮窗聊天显示状态
let bubbleTimer: number | null = null; // 气泡自动隐藏定时器
let clickTimer: number | null = null; // 处理单击/双击的定时器

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

const toggleLottie = () => {
  showLottie.value = !showLottie.value;
};

// 处理小熊点击事件（区分单击和双击）
const handleBearClick = () => {
  // 如果已经有点击定时器，说明是双击，取消单击处理
  if (clickTimer) {
    clearTimeout(clickTimer);
    clickTimer = null;
    return;
  }
  
  // 设置单击延迟，如果在此期间没有双击，则执行单击逻辑
  clickTimer = window.setTimeout(() => {
    handleBearSingleClick();
    clickTimer = null;
  }, 200);
};

// 处理小熊双击事件
const handleBearDoubleClick = () => {
  // 清除单击定时器
  if (clickTimer) {
    clearTimeout(clickTimer);
    clickTimer = null;
  }
  
  // 隐藏气泡，打开聊天窗口
  hideBubble();
  showChat.value = true;
};

// 处理小熊单击事件（快速提示模式）
const handleBearSingleClick = () => {
  fetchBearMessage();
};

// 获取小熊消息
const fetchBearMessage = async () => {
  // 如果气泡已经显示，点击隐藏
  if (showBubble.value) {
    hideBubble();
    return;
  }

  showBubble.value = true;
  currentMessage.value = '🤔 思考中...';
  
  try {
    const response = await getBearMessage();
    // 根据项目的统一响应结构，数据在response.data中
    const bearMessage = response.data;
    
    if (bearMessage && bearMessage.content) {
      currentMessage.value = bearMessage.content;
      
      // 设置自动隐藏定时器
      const duration = bearMessage.duration || 5000; // 默认5秒
      bubbleTimer = window.setTimeout(() => {
        hideBubble();
      }, duration);
    } else {
      throw new Error('Invalid response format');
    }
  } catch (error) {
    console.error('获取小熊消息失败:', error);
    currentMessage.value = '🐻 嗯...我暂时想不出什么要说的，稍后再试试吧！';
    
    // 错误消息也设置自动隐藏
    bubbleTimer = window.setTimeout(() => {
      hideBubble();
    }, 3000);
  }
};

// 隐藏气泡
const hideBubble = () => {
  showBubble.value = false;
  if (bubbleTimer) {
    clearTimeout(bubbleTimer);
    bubbleTimer = null;
  }
};

// 处理聊天窗口关闭
const handleChatClose = () => {
  showChat.value = false;
};

// 从本地存储读取设置
onMounted(() => {
  const saved = localStorage.getItem('showLottie');
  if (saved !== null) {
    showLottie.value = JSON.parse(saved);
  }
});

// 监听设置变化并保存到本地存储
watch(showLottie, (newValue) => {
  localStorage.setItem('showLottie', JSON.stringify(newValue));
  
  // 隐藏小熊时也隐藏气泡和聊天窗口
  if (!newValue) {
    hideBubble();
    showChat.value = false;
  }
});
</script>

<style lang="scss" scoped>
.app-layout {
  height: 100vh;
  background-color: var(--app-bg-color);
  position: relative; // 为固定定位的动画提供定位上下文

  &__main {
    // Transition for sidebar collapse/expand
    transition: margin-left 0.28s;
  }
}

// AI动画样式
.floating-lottie {
  position: fixed;
  bottom: 80px; // 往上移动，从20px改为80px
  left: 20px;
  z-index: 1001; // 确保在其他元素之上
  pointer-events: auto; // 允许点击事件
  
  .lottie-animation {
    width: 100px; // 稍微增大尺寸
    height: 100px;
    transition: all 0.3s ease;
    cursor: pointer; // 确保显示点击光标
    
    &:hover {
      transform: scale(1.1) rotate(5deg); // 增加旋转效果，更有趣
    }
  }
}

// 小熊显示/隐藏动画
.lottie-fade-enter-active,
.lottie-fade-leave-active {
  transition: all 0.5s ease;
}

.lottie-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

.lottie-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.8);
}

// 浮窗聊天进入/退出动画
:deep(.floating-chat) {
  animation: chatWindowSlideIn 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  &.minimized {
    animation: chatWindowMinimize 0.3s ease;
  }
}

@keyframes chatWindowSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes chatWindowMinimize {
  from {
    height: 400px;
  }
  to {
    height: 50px;
  }
}

// 语言气泡动画
.bubble-fade-enter-active,
.bubble-fade-leave-active {
  transition: opacity 0.5s ease;
}

.bubble-fade-enter-from {
  opacity: 0;
}

.bubble-fade-leave-to {
  opacity: 0;
}

// 控制按钮样式
.lottie-control {
  position: fixed;
  bottom: 170px; // 与小熊同一高度
  left: 00px; // 在小熊右侧
  z-index: 1002; // 确保在小熊之上
  
  :deep(.el-button) {
    background: rgba(173, 216, 230, 0.3); // 磨砂淡蓝色背景
    backdrop-filter: blur(10px); // 磨砂效果
    border: 2px solid rgba(135, 206, 235, 0.5); // 淡蓝色边框
    box-shadow: 0 4px 12px rgba(135, 206, 235, 0.2);
    transition: all 0.3s ease;
    font-size: 16px;
    color: #409EFF; // 蓝色图标
    
    &:hover {
      background: rgba(173, 216, 230, 0.5); // hover时稍微增加不透明度
      border-color: rgba(135, 206, 235, 0.7);
      transform: scale(1.1);
      box-shadow: 0 6px 16px rgba(135, 206, 235, 0.3);
    }
  }
}

// 响应式设计 - 在小屏幕上调整动画大小
@media (max-width: 768px) {
  .floating-lottie {
    bottom: 60px; // 移动端也往上移动
    left: 15px;
    
    .lottie-animation {
      width: 80px;
      height: 80px;
    }
    
    // 移动端气泡样式调整
    .speech-bubble {
      bottom: 90px; // 调整移动端气泡位置
      max-width: 160px;
      font-size: 12px;
      padding: 10px 14px;
    }
  }
  
  .lottie-control {
    bottom: 60px; // 与移动端小熊同一高度
    left: 110px; // 在小熊右侧
  }
  
  // 浮窗聊天移动端适配
  :deep(.floating-chat) {
    width: calc(100vw - 20px) !important;
    max-width: 320px;
    left: 10px !important;
    right: 10px;
  }
}

// 语言气泡样式
.speech-bubble {
  position: absolute;
  bottom: 110px; /* 在小熊上方 */
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #fff 0%, #f8f9ff 100%);
  border-radius: 15px;
  padding: 12px 16px;
  box-shadow: 0 8px 25px rgba(220, 53, 69, 0.15);
  border: 2px solid rgba(220, 53, 69, 0.1);
  z-index: 1003;
  max-width: 200px;
  min-width: 120px;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
  color: #2c3e50;
  text-align: center;
  animation: bubble-bounce 0.5s ease-out;
}

.bubble-content {
  position: relative;
  z-index: 1;
}

.bubble-arrow {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #fff;
  filter: drop-shadow(0 2px 4px rgba(220, 53, 69, 0.1));
}

// 气泡弹跳动画
@keyframes bubble-bounce {
  0% {
    opacity: 0;
    transform: translateX(-50%) translateY(10px) scale(0.8);
  }
  50% {
    transform: translateX(-50%) translateY(-5px) scale(1.05);
  }
  100% {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}
</style> 