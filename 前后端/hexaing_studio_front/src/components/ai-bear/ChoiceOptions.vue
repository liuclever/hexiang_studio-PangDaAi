<template>
  <div class="choice-options-container">
    <!-- 提示语 -->
    <div class="choice-prompt">
      <div class="prompt-icon">🤔</div>
      <div class="prompt-text">{{ prompt }}</div>
    </div>
    
    <!-- 选择项列表 -->
    <div class="choices-grid">
      <div
        v-for="choice in choices"
        :key="choice.id"
        class="choice-item"
        :class="{ 'choice-item--selected': selectedChoiceId === choice.id }"
        @click="handleChoiceClick(choice)"
        @mouseenter="handleChoiceHover(choice.id)"
        @mouseleave="handleChoiceLeave()"
      >
        <!-- 图标 -->
        <div class="choice-icon">
          {{ choice.icon || '📋' }}
        </div>
        
        <!-- 内容 -->
        <div class="choice-content">
          <div class="choice-title">{{ choice.displayText }}</div>
          <div v-if="choice.description" class="choice-description">
            {{ choice.description }}
          </div>
          <div v-if="choice.category" class="choice-category">
            {{ choice.category }}
          </div>
        </div>
        
        <!-- 选择指示器 -->
        <div class="choice-indicator">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path
              d="M6 12L10 8L6 4"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </div>
      </div>
    </div>
    
    <!-- 操作按钮 -->
    <div class="choice-actions">
      <button 
        class="btn-secondary" 
        @click="$emit('cancel')"
        :disabled="isLoading"
      >
        取消
      </button>
      <button 
        class="btn-primary" 
        @click="handleConfirm"
        :disabled="!selectedChoiceId || isLoading"
      >
        <span v-if="isLoading">处理中...</span>
        <span v-else>确认选择</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ChoiceOption } from './types'

// Props
interface Props {
  prompt: string
  choices: ChoiceOption[]
  isLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false
})

// Events
interface Emits {
  (e: 'choice-selected', choice: ChoiceOption): void
  (e: 'cancel'): void
}

const emit = defineEmits<Emits>()

// State
const selectedChoiceId = ref<string | null>(null)
const hoveredChoiceId = ref<string | null>(null)

// Methods
const handleChoiceClick = (choice: ChoiceOption) => {
  if (props.isLoading) return
  
  selectedChoiceId.value = choice.id
  
  // 可以立即发送选择，或者等用户点击确认按钮
  // 这里选择立即发送，提供更流畅的体验
  setTimeout(() => {
    emit('choice-selected', choice)
  }, 150) // 小延迟让用户看到选择效果
}

const handleChoiceHover = (choiceId: string) => {
  hoveredChoiceId.value = choiceId
}

const handleChoiceLeave = () => {
  hoveredChoiceId.value = null
}

const handleConfirm = () => {
  if (!selectedChoiceId.value || props.isLoading) return
  
  const selectedChoice = props.choices.find(c => c.id === selectedChoiceId.value)
  if (selectedChoice) {
    emit('choice-selected', selectedChoice)
  }
}

// Computed
const groupedChoices = computed(() => {
  const groups: { [key: string]: ChoiceOption[] } = {}
  
  props.choices.forEach(choice => {
    const category = choice.category || '其他'
    if (!groups[category]) {
      groups[category] = []
    }
    groups[category].push(choice)
  })
  
  return groups
})
</script>

<style scoped>
.choice-options-container {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: 16px;
  padding: 20px;
  margin: 16px 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.choice-prompt {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.prompt-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.prompt-text {
  font-size: 16px;
  font-weight: 500;
  color: #334155;
  line-height: 1.5;
}

.choices-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.choice-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-1px);
}

.choice-item--selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

.choice-item--selected::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: #3b82f6;
}

.choice-icon {
  font-size: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f1f5f9;
}

.choice-content {
  flex: 1;
  min-width: 0;
}

.choice-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.choice-description {
  font-size: 14px;
  color: #64748b;
  line-height: 1.4;
  margin-bottom: 4px;
}

.choice-category {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
  padding: 2px 8px;
  background: #eff6ff;
  border-radius: 4px;
  display: inline-block;
}

.choice-indicator {
  color: #94a3b8;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.choice-item:hover .choice-indicator,
.choice-item--selected .choice-indicator {
  color: #3b82f6;
  transform: translateX(4px);
}

.choice-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.btn-primary,
.btn-secondary {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  outline: none;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.btn-secondary {
  background: white;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .choice-options-container {
    padding: 16px;
    margin: 12px 0;
  }
  
  .choice-item {
    padding: 12px;
    gap: 12px;
  }
  
  .choice-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .choice-title {
    font-size: 15px;
  }
  
  .choice-description {
    font-size: 13px;
  }
  
  .choice-actions {
    flex-direction: column;
  }
  
  .btn-primary,
  .btn-secondary {
    width: 100%;
  }
}

/* 动画效果 */
@keyframes choice-appear {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.choice-item {
  animation: choice-appear 0.3s ease forwards;
}

.choice-item:nth-child(1) { animation-delay: 0.1s; }
.choice-item:nth-child(2) { animation-delay: 0.2s; }
.choice-item:nth-child(3) { animation-delay: 0.3s; }
.choice-item:nth-child(4) { animation-delay: 0.4s; }
</style> 