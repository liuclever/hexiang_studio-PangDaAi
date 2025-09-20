<template>
  <div class="chat-input">
    <div class="input-container">
      <input
        v-model="inputMessage"
        type="text"
        placeholder="输入消息后按回车发送..."
        class="message-input"
        :disabled="disabled"
        @keyup.enter="handleSend"
        @focus="handleFocus"
        @blur="handleBlur"
        ref="inputRef"
      />
      
      <button
        class="send-btn"
        @click="isTyping ? handleStop() : handleSend()"
        :disabled="!isTyping && (!inputMessage.trim() || disabled)"
        :class="{ 
          stop: isTyping,
          pulse: isTyping,
          sending: !isTyping && inputMessage.trim()
        }"
        :title="isTyping ? '停止AI回复' : '发送消息'"
      >
        {{ isTyping ? '⏹️' : (inputMessage.trim() ? '📤' : '🤖') }}
      </button>
      
      <button
        class="clear-btn"
        :class="{ 'shake': isShaking }"
        @click="handleClear"
        title="清除聊天记录"
      >
        🗑️
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

interface Props {
  disabled?: boolean
  hasMessages?: boolean // 是否有消息记录
  isTyping?: boolean // AI是否正在输入
}

interface Emits {
  (e: 'send', message: string): void
  (e: 'clear'): void
  (e: 'stop'): void // 停止事件
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const inputMessage = ref('')
const inputRef = ref<HTMLInputElement>()
const isFocused = ref(false)
const isShaking = ref(false)

const handleSend = () => {
  const message = inputMessage.value.trim()
  if (!message || props.disabled || props.isTyping) return
  
  emit('send', message)
  inputMessage.value = ''
}

// 停止AI回复
const handleStop = () => {
  emit('stop')
  ElMessage.success('已停止AI回复')
}

const handleClear = () => {
  // 如果没有消息，显示提示动画
  if (!props.hasMessages) {
    isShaking.value = true
    setTimeout(() => {
      isShaking.value = false
    }, 600)
    return
  }
  
  emit('clear')
}

const handleFocus = () => {
  isFocused.value = true
}

const handleBlur = () => {
  isFocused.value = false
}

// 聚焦输入框
const focus = () => {
  inputRef.value?.focus()
}

// 暴露方法供父组件调用
defineExpose({
  focus
})
</script>

<style lang="scss" scoped>
.chat-input {
  padding: 12px 16px;
  border-top: 1px solid rgba(173, 216, 230, 0.2);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 0 0 12px 12px;
}

.input-container {
  display: flex;
  gap: 8px;
  align-items: center;
}

.message-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(173, 216, 230, 0.3);
  border-radius: 20px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
  outline: none;
  
  &:focus {
    border-color: rgba(64, 158, 255, 0.5);
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
  }
  
  &:disabled {
    background: rgba(0, 0, 0, 0.05);
    cursor: not-allowed;
  }
  
  &::placeholder {
    color: rgba(0, 0, 0, 0.4);
  }
}

.clear-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  
  &:hover {
    transform: scale(1.05);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    color: #f56c6c;
  }
  
  &:active {
    transform: scale(0.95);
  }
  
  &.shake {
    animation: shake 0.6s ease-in-out;
  }
}

.send-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  
  &:hover:not(:disabled) {
    transform: scale(1.05);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    color: #409EFF;
  }
  
  &:active:not(:disabled) {
    transform: scale(0.95);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
  
  // 发送状态样式
  &.sending {
    background: rgba(64, 158, 255, 0.9);
    color: white;
    border-color: rgba(64, 158, 255, 0.3);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    
    &:hover:not(:disabled) {
      background: rgba(64, 158, 255, 1);
      transform: scale(1.05);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    }
  }
  
  // 停止按钮样式
  &.stop {
    background: rgba(245, 108, 108, 0.9);
    color: white;
    border-color: rgba(245, 108, 108, 0.3);
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
    
    &:hover {
      background: rgba(245, 108, 108, 1);
      transform: scale(1.05);
      box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
    }
  }
  
  // 脉冲动画
  &.pulse {
    animation: pulse 1.5s infinite;
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
  }
  50% {
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.6), 0 0 0 4px rgba(245, 108, 108, 0.3);
  }
  100% {
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-3px); }
  20%, 40%, 60%, 80% { transform: translateX(3px); }
}
</style> 