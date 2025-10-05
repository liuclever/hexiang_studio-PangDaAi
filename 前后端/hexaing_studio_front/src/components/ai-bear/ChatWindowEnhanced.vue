<!-- AI熊工具助手聊天窗口 -->
<template>
  <div class="chat-window">
    <!-- 消息显示区域 -->
    <div class="messages-container" ref="messagesContainer">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message', message.type]"
      >
        <!-- 用户消息 -->
        <div v-if="message.type === 'user'" class="user-message">
          {{ message.content }}
        </div>

        <!-- 助手响应 -->
        <div v-else class="assistant-message">
          <div class="message-text">{{ message.content }}</div>
          
          <!-- 工具选项按钮 -->
          <div v-if="message.options && message.options.length" class="tool-options">
            <button
              v-for="option in message.options"
              :key="option.action"
              @click="handleToolAction(option)"
              class="tool-option-btn"
              :title="option.description"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.description }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
      <div class="input-container">
      <input
          v-model="inputMessage"
        @keyup.enter="sendMessage"
        placeholder="问我任何关于工作室的问题..."
        class="message-input"
        :disabled="isLoading"
      />
      <button @click="sendMessage" :disabled="isLoading || !inputMessage.trim()" class="send-btn">
        发送
        </button>
    </div>
  </div>
</template>

<script>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

export default {
  name: 'ChatWindowEnhanced',
  setup() {
    const router = useRouter()
    const messages = ref([
  {
    id: 1,
        type: 'assistant',
        content: '你好！我是何湘工作室的工具助手胖达🐻！我可以帮你快速找到需要的功能和信息。只需告诉我你想要做什么，我会为你提供相关的操作选项！',
        options: []
      }
    ])
    const inputMessage = ref('')
const isLoading = ref(false)
    const messagesContainer = ref(null)

    // 发送消息
    const sendMessage = async () => {
      if (!inputMessage.value.trim() || isLoading.value) return

      const userMessage = inputMessage.value.trim()

  // 添加用户消息
      messages.value.push({
        id: Date.now(),
    type: 'user',
        content: userMessage
      })
      
  inputMessage.value = ''
  isLoading.value = true
  
  try {
        // 调用后端API
        const response = await fetch('/api/ai-bear/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({ message: userMessage })
        })

        const result = await response.json()
        
        if (result.code === 200 && result.data) {
          // 添加助手响应
          messages.value.push({
            id: Date.now() + 1,
            type: 'assistant',
            content: result.data.message,
            options: result.data.options || []
          })
    } else {
          // 只显示msg内容，不显示整个JSON
          let errorMessage = '请求失败，请重试'
          if (result && result.msg) {
            errorMessage = result.msg
          } else if (result && result.message) {
            errorMessage = result.message
          }
          
          // 根据错误码提供更友好的提示
          if (result && result.code === 4030) {
            errorMessage = '权限不足，请联系管理员开启AI功能权限'
          }
          
          throw new Error(errorMessage)
    }
    
  } catch (error) {
    console.error('发送消息失败:', error)
        ElMessage.error('发送失败，请重试')
        
        // 添加错误消息 - 显示具体错误而不是通用消息
        let displayMessage = '抱歉，我暂时无法处理您的请求，请稍后重试。'
        if (error.message && error.message.length < 100) {
          displayMessage = error.message
        }
        
        messages.value.push({
          id: Date.now() + 1,
          type: 'assistant',
          content: displayMessage,
          options: []
        })
  } finally {
    isLoading.value = false
        await nextTick()
    scrollToBottom()
  }
}

    // 🔑 处理工具选项点击 - 核心跳转逻辑
    const handleToolAction = (option) => {
      console.log('工具选项被点击:', option)
      
      // 判断action是否为路由路径
      if (option.action.startsWith('/')) {
        // 执行路由跳转
        router.push(option.action)
        ElMessage.success(`正在跳转到：${option.label}`)
        
        // 记录用户操作
        messages.value.push({
          id: Date.now(),
          type: 'user',
          content: `点击了：${option.label}`
        })
        
        messages.value.push({
          id: Date.now() + 1,
          type: 'assistant',
          content: `✅ 已为您跳转到${option.label}页面`,
          options: []
        })
        
      } else {
        // 处理其他类型的action（如API调用等）
        handleOtherAction(option)
      }
      
      scrollToBottom()
    }

    // 处理其他类型的操作
    const handleOtherAction = (option) => {
      switch (option.action) {
        case 'user-stats':
          // 调用统计API
          ElMessage.info('正在获取用户统计...')
          break
        case 'system-status':
          // 显示系统状态
          ElMessage.info('系统运行正常')
          break
        default:
          ElMessage.warning(`未知操作: ${option.action}`)
      }
    }

    // 滚动到底部
const scrollToBottom = () => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
    }

    return {
      messages,
      inputMessage,
      isLoading,
      messagesContainer,
      sendMessage,
      handleToolAction
    }
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
}

.message {
  margin-bottom: 16px;
}

.user-message {
  background: #007bff;
  color: white;
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 70%;
  margin-left: auto;
  word-wrap: break-word;
}

.assistant-message {
  max-width: 85%;
}

.message-text {
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  margin-bottom: 12px;
}

/* 🎨 工具选项样式 */
.tool-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-option-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px 16px;
  border: 2px solid #007bff;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.tool-option-btn:hover {
  background: #007bff;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,123,255,0.3);
}

.option-label {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.option-desc {
  font-size: 12px;
  opacity: 0.8;
}

.input-container {
  display: flex;
  padding: 16px;
  background: white;
  border-top: 1px solid #e0e0e0;
}

.message-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  margin-right: 12px;
}

.send-btn {
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.send-btn:hover {
  background: #0056b3;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style> 