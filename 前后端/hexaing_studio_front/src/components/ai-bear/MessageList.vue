<template>
  <div class="message-list" ref="messageListRef">
    <div 
      v-for="message in messages" 
      :key="message.id"
      class="message-item"
      :class="{ 'user-message': message.type === 'user', 'ai-message': message.type === 'ai' }"
    >
      <div class="message-avatar">
        <span v-if="message.type === 'ai'">🐻</span>
        <span v-else>👤</span>
      </div>
      
      <div class="message-content">
        <div class="message-text" v-html="formatMessageContent(message.content)"></div>
        <div class="message-time">
          {{ formatTime(message.timestamp) }}
        </div>
      </div>
    </div>
    
    <!-- 输入状态指示 -->
    <div v-if="isTyping" class="message-item ai-message typing-indicator">
      <div class="message-avatar">
        <span>🐻</span>
      </div>
      <div class="message-content">
        <div class="typing-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ChatMessage } from './types'

interface Props {
  messages: ChatMessage[]
  isTyping: boolean
}

const props = defineProps<Props>()
const messageListRef = ref<HTMLElement>()

// 格式化时间
const formatTime = (timestamp: Date) => {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(timestamp))
}

// 格式化消息内容，支持Markdown格式
const formatMessageContent = (content: string) => {
  if (!content) return ''
  
  console.log('🎨 开始格式化内容:', JSON.stringify(content))
  
  // 检查是否是JSON错误响应，如果是则只提取msg字段
  if (typeof content === 'string' && content.trim().startsWith('{') && content.includes('"code"')) {
    try {
      const errorObj = JSON.parse(content);
      if (errorObj.msg) {
        content = errorObj.msg;
      } else if (errorObj.message) {
        content = errorObj.message;
      } else {
        content = '服务暂时不可用，请稍后重试';
      }
      console.log('🔧 检测到JSON错误响应，提取消息:', content);
    } catch (e) {
      console.log('❌ JSON解析失败，使用原内容');
    }
  }
  
  // 简化格式化，只做基本的Markdown处理
  // 1. HTML转义防止XSS
  let formattedContent = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
  
  console.log('✅ 1.HTML转义后:', JSON.stringify(formattedContent))
  
  // 2. 处理Markdown标题 (修复正则)
  formattedContent = formattedContent
    .replace(/^##\s*(.*?)$/gm, '<h2 class="section-title">$1</h2>')
    .replace(/^#\s*(.*?)$/gm, '<h1 class="section-title">$1</h1>')
  
  console.log('✅ 2.标题处理后:', JSON.stringify(formattedContent))
  
  // 3. 处理无序列表 (修复正则，适配AI的输出格式)
  formattedContent = formattedContent
    .replace(/^-(.+)$/gm, '<div class="list-item">• $1</div>') // 匹配 -内容 (不管有没有空格)
  
  console.log('✅ 3.列表处理后:', JSON.stringify(formattedContent))
  
  // 4. 处理加粗
  formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  
  console.log('✅ 4.加粗处理后:', JSON.stringify(formattedContent))
  
  // 5. 新增：处理时间格式
  formattedContent = formattedContent.replace(/(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/g, 
    '<span class="formatted-date">$1年$2月$3日 $4:$5</span>'
  );
  
  console.log('✅ 5.时间处理后:', JSON.stringify(formattedContent))

  // 6. 处理换行
  formattedContent = formattedContent.replace(/\n/g, '<br>')
  
  console.log('✅ 6.换行处理后:', JSON.stringify(formattedContent))
  console.log('🎨 最终格式化结果:', formattedContent)
  
  return formattedContent
}
</script>

<style lang="scss" scoped>
.message-list {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  &::-webkit-scrollbar {
    width: 4px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 2px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(173, 216, 230, 0.5);
    border-radius: 2px;
    
    &:hover {
      background: rgba(173, 216, 230, 0.7);
    }
  }
}

.message-item {
  display: flex;
  gap: 8px;
  animation: messageSlideIn 0.3s ease-out;
  
  &.user-message {
    flex-direction: row-reverse;
    
    .message-content {
      background: linear-gradient(135deg, #409EFF, #66B3FF);
      color: white;
      border-radius: 16px 16px 4px 16px;
    }
  }
  
  &.ai-message {
    .message-content {
      background: rgba(248, 249, 250, 0.8);
      color: #2c3e50;
      border-radius: 16px 16px 16px 4px;
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
}

.message-content {
  max-width: 300px;
  padding: 8px 12px;
  border-radius: 16px;
  position: relative;
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: normal;
  
  // 添加格式化样式
  ::v-deep(strong) {
    font-weight: bold;
    color: #2c5aa0;
  }
  
  ::v-deep(em) {
    font-style: italic;
    color: #555;
  }
  
  ::v-deep(del) {
    text-decoration: line-through;
    opacity: 0.7;
  }
  
  ::v-deep(.emoji) {
    font-size: 16px;
    margin: 0 2px;
    display: inline-block;
  }
  
  ::v-deep(.bullet) {
    color: #666;
    font-weight: bold;
    margin-right: 4px;
  }
  
  ::v-deep(.section-title) {
    font-weight: bold;
    margin: 12px 0 8px 0;
    padding: 4px 0;
    border-bottom: 1px solid rgba(173, 216, 230, 0.3);
    color: #1e40af;
    font-size: 16px;
  }
  
  ::v-deep(.list-item) {
    margin: 2px 0; // 间距更窄
    line-height: 1.4;
    color: #2c3e50;
    
    &.ordered-item {
      color: #1e40af;
      font-weight: 500;
    }
    
    &.unordered-item {
      position: relative;
    }
  }
  
  // 代码块样式
  ::v-deep(.code-block) {
    background: rgba(248, 248, 248, 0.8);
    border: 1px solid rgba(200, 200, 200, 0.3);
    border-radius: 6px;
    padding: 12px;
    margin: 8px 0;
    overflow-x: auto;
    font-family: 'Courier New', Consolas, Monaco, monospace;
    
    code {
      background: none;
      padding: 0;
      font-size: 13px;
      line-height: 1.4;
      color: #2c3e50;
      white-space: pre;
    }
  }
  
  // 行内代码样式
  ::v-deep(.inline-code) {
    background: rgba(240, 240, 240, 0.8);
    border: 1px solid rgba(200, 200, 200, 0.3);
    border-radius: 3px;
    padding: 2px 6px;
    font-family: 'Courier New', Consolas, Monaco, monospace;
    font-size: 13px;
    color: #d73a49;
  }
  
  // 链接样式
  ::v-deep(.message-link) {
    color: #409EFF;
    text-decoration: none;
    border-bottom: 1px dotted #409EFF;
    transition: all 0.2s ease;
    
    &:hover {
      color: #66B3FF;
      border-bottom-style: solid;
    }
  }
  
  // 引用块样式
  ::v-deep(.quote-block) {
    border-left: 4px solid #409EFF;
    background: rgba(64, 158, 255, 0.05);
    margin: 8px 0;
    padding: 8px 12px;
    font-style: italic;
    color: #666;
  }
  
  // 分隔线样式
  ::v-deep(.separator) {
    border: none;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(173, 216, 230, 0.5), transparent);
    margin: 16px 0;
  }
  
  ::v-deep(h1.section-title) {
    font-size: 18px;
    color: #2c5aa0;
  }
  
  ::v-deep(h2.section-title) {
    font-size: 16px;
    color: #2c5aa0;
  }
  
  ::v-deep(h3.section-title) {
    font-size: 14px;
    color: #2c5aa0;
  }

  ::v-deep(.formatted-date) {
    background-color: #e8f0fe;
    color: #1e40af;
    font-weight: 500;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
    white-space: nowrap;
  }
}

.message-time {
  font-size: 10px;
  opacity: 0.6;
  margin-top: 4px;
}

// 输入指示器
.typing-indicator {
  .message-content {
    padding: 12px 16px;
  }
}

.typing-dots {
  display: flex;
  gap: 4px;
  
  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #409EFF;
    animation: typingDot 1.4s infinite ease-in-out;
    
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typingDot {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style> 