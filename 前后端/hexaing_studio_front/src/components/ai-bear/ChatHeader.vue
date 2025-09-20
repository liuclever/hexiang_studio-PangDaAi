<template>
  <div class="chat-header">
    <div class="header-content">
      <div class="bear-info">
        <span class="bear-icon">🐻</span>
        <span class="bear-name">胖达助理</span>
      </div>
      
      <div class="header-controls">
        <button 
          class="control-btn new-session-btn"
          @click="handleNewSession"
          title="新建会话"
        >
          🔄
        </button>
        
        <button 
          class="control-btn minimize-btn"
          @click="handleToggleMinimize"
          :title="isMinimized ? '展开' : '最小化'"
        >
          {{ isMinimized ? '⬜' : '➖' }}
        </button>
        
        <button 
          class="control-btn close-btn"
          @click="handleClose"
          title="关闭"
        >
          ✖️
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { resetCurrentUserSession } from '@/api/ai-bear'

interface Props {
  isMinimized: boolean
}

interface Emits {
  (e: 'toggle-minimize'): void
  (e: 'close'): void
  (e: 'new-session'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const handleToggleMinimize = () => {
  emit('toggle-minimize')
}

const handleClose = () => {
  emit('close')
}

const handleNewSession = () => {
  // 重置会话缓存
  resetCurrentUserSession()
  // 通知父组件清空聊天记录
  emit('new-session')
}
</script>

<style lang="scss" scoped>
.chat-header {
  background: linear-gradient(135deg, rgba(173, 216, 230, 0.3), rgba(255, 255, 255, 0.5));
  border-bottom: 1px solid rgba(173, 216, 230, 0.2);
  padding: 12px 16px;
  cursor: grab;
  user-select: none;
  border-radius: 12px 12px 0 0;
  position: relative;
  
  // 拖拽指示器
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 30px;
    height: 4px;
    background: repeating-linear-gradient(
      90deg,
      rgba(0, 0, 0, 0.1) 0px,
      rgba(0, 0, 0, 0.1) 4px,
      transparent 4px,
      transparent 8px
    );
    border-radius: 2px;
    pointer-events: none;
  }
  
  &:hover {
    background: linear-gradient(135deg, rgba(173, 216, 230, 0.4), rgba(255, 255, 255, 0.6));
  }
  
  &:active {
    cursor: grabbing;
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.2), rgba(173, 216, 230, 0.4));
  }
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bear-info {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .bear-icon {
    font-size: 16px;
  }
  
  .bear-name {
    font-size: 14px;
    font-weight: 500;
    color: #2c3e50;
  }
}

.header-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  background: none;
  border: none;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  
  &:hover {
    background-color: rgba(0, 0, 0, 0.1);
  }
  
  &.close-btn:hover {
    background-color: rgba(220, 53, 69, 0.1);
  }
  
  &.new-session-btn:hover {
    background-color: rgba(40, 167, 69, 0.1);
  }
}
</style> 