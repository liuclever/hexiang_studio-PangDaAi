<template>
  <div 
    v-show="visible" 
    class="floating-chat"
    :class="{ 
      'minimized': isMinimized,
      'dragging': isDragging,
      'resizing': isResizing
    }"
    :style="{ 
      left: position.x + 'px', 
      top: position.y + 'px',
      width: size.width + 'px',
      height: isMinimized ? '50px' : size.height + 'px'
    }"
    @mousedown="startDrag"
  >
    <!-- 头部 -->
    <div class="chat-header-wrapper" @dblclick="handleHeaderDoubleClick">
      <ChatHeader 
        :is-minimized="isMinimized"
        @toggle-minimize="toggleMinimize"
        @close="handleClose"
      />
    </div>
    
    <!-- 消息区域 -->
    <div v-show="!isMinimized" class="chat-body">
      <MessageList 
        :messages="messages"
        :is-typing="isTyping"
      />
      
      <!-- 输入区域 -->
      <ChatInput 
        @send="handleSendMessage"
        @clear="handleClearMessages"
        @stop="handleStopAI"
        :disabled="isTyping"
        :is-typing="isTyping"
        :has-messages="messages.length > 1"
      />
    </div>
    
    <!-- 调整大小的手柄 -->
    <div 
      v-show="!isMinimized"
      class="resize-handle"
      @mousedown="startResize"
    ></div>
  </div>
  
  <!-- 浮动表格窗口 -->
  <FloatingTableWindow
    v-model:visible="showTableWindow"
    :tableData="currentTableData"
    @close="handleTableWindowClose"
  />
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onBeforeUnmount } from 'vue'
import ChatHeader from './ChatHeader.vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'
import FloatingTableWindow from './FloatingTableWindow.vue'
import { sendChatMessage, sendChatMessageStream, getChatHistory, clearChatHistory } from '@/api/ai-bear'

// 本地类型定义
interface ChatMessage {
  id: number
  content: string
  type: 'user' | 'ai'
  timestamp: Date
}

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'close'): void
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 窗口状态
const isMinimized = ref(false)
const isTyping = ref(false)
const messages = ref<ChatMessage[]>([])

// 表格窗口状态
const showTableWindow = ref(false)
const currentTableData = ref<any>(null)
const tableDetected = ref(false) // 防止重复检测标志

// 流式连接控制
let currentCancelStream: (() => void) | null = null

// 拖拽相关
const position = reactive({ x: 0, y: 0 }) // 初始位置将由智能定位确定
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

// 调整大小相关
const size = reactive({ width: 380, height: 520 }) // 增加初始大小，宽度从280->380，高度从400->520
const resizeStart = reactive({ x: 0, y: 0 })
const isResizing = ref(false)

// 智能定位函数
const getSmartPosition = () => {
  const windowWidth = window.innerWidth
  const windowHeight = window.innerHeight
  const chatWidth = size.width
  const chatHeight = size.height
  
  // 默认在右下角，但要确保完全可见
  let x = windowWidth - chatWidth - 20
  let y = windowHeight - chatHeight - 20
  
  // 如果右下角空间不够，尝试其他位置
  if (x < 20) x = 20
  if (y < 20) y = 20
  
  // 避免与小熊位置重叠
  const bearX = 20 // 小熊的位置
  const bearY = windowHeight - 180 // 小熊的大概位置
  
  if (Math.abs(x - bearX) < 300 && Math.abs(y - bearY) < 200) {
    // 如果会重叠，移动到右上角
    x = windowWidth - chatWidth - 20
    y = 20
  }
  
  return { x, y }
}

// 初始化位置
const initPosition = () => {
  const smartPos = getSmartPosition()
  position.x = smartPos.x
  position.y = smartPos.y
}

// 拖拽功能
const startDrag = (e: MouseEvent) => {
  // 只有点击头部时才允许拖拽
  const target = e.target as HTMLElement
  if (!target.closest('.chat-header') || target.closest('.control-btn')) {
    return
  }
  
  e.preventDefault() // 阻止默认行为
  e.stopPropagation() // 阻止事件冒泡
  
  isDragging.value = true
  dragStart.x = e.clientX - position.x
  dragStart.y = e.clientY - position.y
  
  // 添加视觉反馈
  document.body.style.cursor = 'grabbing'
  document.body.style.userSelect = 'none'
  
  document.addEventListener('mousemove', onDrag, { passive: false })
  document.addEventListener('mouseup', stopDrag)
}

// 双击头部重置大小
const handleHeaderDoubleClick = () => {
  if (!isMinimized.value) {
    // 重置到默认大小
    size.width = 380  // 从280改为380
    size.height = 520 // 从400改为520
    // 重新定位到智能位置
    initPosition()
  }
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) return
  
  e.preventDefault() // 阻止默认行为
  
  const newX = e.clientX - dragStart.x
  const newY = e.clientY - dragStart.y
  
  // 更宽松的边界限制，考虑当前尺寸
  const minX = -50 // 允许部分超出左边界
  const maxX = window.innerWidth - Math.min(100, size.width) // 确保至少100px可见或整个宽度可见
  const minY = 0
  const maxY = window.innerHeight - Math.min(100, size.height) // 确保至少100px可见或整个高度可见
  
  position.x = Math.max(minX, Math.min(maxX, newX))
  position.y = Math.max(minY, Math.min(maxY, newY))
}

const stopDrag = () => {
  isDragging.value = false
  
  // 恢复样式
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 调整大小功能
const startResize = (e: MouseEvent) => {
  e.preventDefault() // 阻止默认行为
  e.stopPropagation() // 阻止事件冒泡

  isResizing.value = true
  resizeStart.x = e.clientX - size.width
  resizeStart.y = e.clientY - size.height

  document.addEventListener('mousemove', onResize, { passive: false })
  document.addEventListener('mouseup', stopResize)
}

const onResize = (e: MouseEvent) => {
  if (!isResizing.value) return

  e.preventDefault()

  const newWidth = e.clientX - position.x
  const newHeight = e.clientY - position.y

  // 边界限制 - 增加最小尺寸，让窗口更实用
  const minWidth = 320 // 最小宽度从200增加到320
  const maxWidth = Math.min(600, window.innerWidth - position.x - 20) // 最大宽度从500增加到600
  const minHeight = 400 // 最小高度从200增加到400
  const maxHeight = Math.min(700, window.innerHeight - position.y - 20) // 最大高度从600增加到700

  size.width = Math.max(minWidth, Math.min(maxWidth, newWidth))
  size.height = Math.max(minHeight, Math.min(maxHeight, newHeight))
}

const stopResize = () => {
  isResizing.value = false

  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// 最小化切换
const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
}

// 关闭窗口
const handleClose = () => {
  emit('close')
  emit('update:visible', false)
}

// 停止AI回复
const handleStopAI = () => {
  console.log('用户主动停止AI回复')
  
  // 取消当前的流式连接
  if (currentCancelStream) {
    currentCancelStream()
    currentCancelStream = null
  }
  
  // 重置输入状态
  isTyping.value = false
  
  // 更新最后一条AI消息为停止状态
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.type === 'ai') {
    lastMessage.content = '🛑 已停止回复'
  }
  
  // 滚动到底部
  nextTick(() => {
    const messageList = document.querySelector('.message-list')
    if (messageList) {
      messageList.scrollTop = messageList.scrollHeight
    }
  })
}

// 发送消息（流式版本）
const handleSendMessage = async (content: string) => {
  // 防止重复提交
  if (isTyping.value) {
    console.log('AI正在回复中，请稍候...')
    return
  }

  // 添加用户消息
  const userMessage: ChatMessage = {
    id: Date.now(),
    content,
    type: 'user',
    timestamp: new Date()
  }
  messages.value.push(userMessage)
  
  // 显示AI输入状态
  isTyping.value = true
  
  // 重置表格检测状态
  tableDetected.value = false
  
  // 创建AI消息占位符
  const aiMessage: ChatMessage = {
    id: Date.now() + 1,
    content: '🤔 让我想想...',
    type: 'ai',
    timestamp: new Date()
  }
  messages.value.push(aiMessage)
  
  try {
    // 使用流式API发送消息
    currentCancelStream = sendChatMessageStream(
      content,
      // onMessage: 接收流式数据
      (data: string) => {
        // 更新AI消息内容（逐步追加）
        const lastMessage = messages.value[messages.value.length - 1];
        if (lastMessage && lastMessage.type === 'ai') {
          // 第一次接收到数据时，清空初始的"思考中"消息
          if (isTyping.value) {
            lastMessage.content = '';
            isTyping.value = false; // 标记为非输入状态，这样下次就不会再清空了
          }
          lastMessage.content += data; // 将新接收到的字符追加到末尾
          
          // 实时检测表格JSON数据
          checkTableDataInStream(lastMessage.content)
        }
          
        // 自动滚动到底部
        nextTick(() => {
          const messageList = document.querySelector('.message-list')
          if (messageList) {
            messageList.scrollTop = messageList.scrollHeight
          }
        })
      },
      // onComplete: 流式结束
      () => {
        isTyping.value = false
        currentCancelStream = null // 清除引用
        console.log('流式输出完成')
        
        // 检测并处理表格数据
        checkAndShowTable()
      },
      // onError: 错误处理
      (error: any) => {
        console.error('流式消息发送失败:', error)
        isTyping.value = false
        currentCancelStream = null // 清除引用
        
        // 更新消息为具体的错误提示
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage && lastMessage.type === 'ai') {
          let errorMsg = '抱歉，我暂时无法回复，请稍后再试。'
          if (error.message) {
            errorMsg = `❌ 错误：${error.message}`
          }
          lastMessage.content = errorMsg
        }
        
        // 滚动到底部
        nextTick(() => {
          const messageList = document.querySelector('.message-list')
          if (messageList) {
            messageList.scrollTop = messageList.scrollHeight
          }
        })
      }
    )
    
  } catch (error) {
    console.error('发送消息失败:', error)
    isTyping.value = false
    currentCancelStream = null // 清除引用
    
    // 更新消息为错误提示
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.type === 'ai') {
      lastMessage.content = '抱歉，我暂时无法回复，请稍后再试。'
    }
    
    // 滚动到底部
    nextTick(() => {
      const messageList = document.querySelector('.message-list')
      if (messageList) {
        messageList.scrollTop = messageList.scrollHeight
      }
    })
  }
}

// 加载历史消息
const loadHistory = async () => {
  try {
    const response = await getChatHistory()
    if (response.data && response.data.length > 0) {
      // 处理历史记录，确保类型一致
      messages.value = response.data
        .sort((a: any, b: any) => {
          // 按消息顺序和时间排序
          if (a.sessionId === b.sessionId) {
            return (a.messageOrder || 0) - (b.messageOrder || 0)
          }
          return new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        })
        .map((msg: any) => ({
          id: msg.id || msg.chatId || Date.now(),
          content: msg.content,
          type: msg.type,
          timestamp: typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : new Date(msg.timestamp || Date.now()),
          sessionId: msg.sessionId,
          messageOrder: msg.messageOrder,
          userName: msg.userName,
          realName: msg.realName
        }))
    } else {
      messages.value = []
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    messages.value = []
  }
}

// 清除聊天记录
const handleClearMessages = async () => {
  // 添加确认对话框
  if (messages.value.length <= 1) {
    // 如果只有欢迎消息，不需要清除
    return
  }
  
  if (confirm('确定要清除所有聊天记录吗？此操作不可撤销。')) {
    try {
      await clearChatHistory()
      messages.value = []
      
      // 显示清除成功的临时消息
      messages.value = [{
        id: Date.now(),
        content: '✨ 聊天记录已清除',
        type: 'ai',
        timestamp: new Date()
      }]
      
      // 1秒后恢复欢迎消息
      setTimeout(() => {
        // 重新初始化，只显示欢迎语
        messages.value = [{
          id: 1,
          content: '你好！我是你的AI助理胖达🐻，我不仅知道工作室的事情还知道，生活中的很多事情，有什么可以帮助你的吗？',
          type: 'ai',
          timestamp: new Date()
        }]
      }, 1000)
    } catch (error) {
      console.error('清除聊天记录失败:', error)
      alert('清除聊天记录失败，请稍后再试。')
    }
  }
}

// 流式输出中实时检测表格数据
const checkTableDataInStream = (content: string) => {
  // 如果已经检测到表格，避免重复检测
  if (tableDetected.value || showTableWindow.value) return
  
  try {
    // 检测JSON开始标志
    const jsonStartPattern = /\{[\s\S]*?"title"[\s\S]*?"/
    if (jsonStartPattern.test(content)) {
      // 尝试提取完整的JSON
      const jsonPattern = /\{[\s\S]*?"title"[\s\S]*?"columns"[\s\S]*?"rows"[\s\S]*?"metadata"[\s\S]*?\}/
      const jsonMatch = content.match(jsonPattern)
      
      if (jsonMatch) {
        try {
          const tableData = JSON.parse(jsonMatch[0])
          if (tableData.title && tableData.columns && tableData.rows && 
              Array.isArray(tableData.columns) && Array.isArray(tableData.rows)) {
            
            currentTableData.value = tableData
            showTableWindow.value = true
            tableDetected.value = true
            console.log('🎯 流式检测到完整表格数据:', tableData.title, `列数: ${tableData.columns.length}, 行数: ${tableData.rows.length}`)
            return
          }
        } catch (parseError) {
          // JSON还不完整，继续等待
          console.log('📝 JSON尚未完整，继续等待...')
        }
      }
    }
    
    // 备选检测：基于关键词和结构特征
    if (!tableDetected.value && content.length > 100) { // 等待足够的内容
      const hasTableKeywords = ['师资团队', '学生团队', '工作室', '成员', '名单'].some(keyword => 
        content.includes(keyword)
      )
      const hasStructure = content.includes('【') && content.includes('】') && content.includes('- ')
      
      if (hasTableKeywords && hasStructure) {
        // 延迟检测，等待更多内容
        setTimeout(() => {
          if (!tableDetected.value) {
            checkAndShowTable()
          }
        }, 500)
      }
    }
  } catch (error) {
    console.log('流式表格检测错误:', error)
  }
}

// 检测消息中的表格数据并显示表格窗口
const checkAndShowTable = () => {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.type === 'ai') {
    try {
      // 方法1: 尝试从消息内容中提取JSON
      const jsonMatch = lastMessage.content.match(/\{[\s\S]*?"title"[\s\S]*?"columns"[\s\S]*?"rows"[\s\S]*?\}/);
      if (jsonMatch) {
        const tableData = JSON.parse(jsonMatch[0])
        if (tableData.columns && tableData.rows && Array.isArray(tableData.columns) && Array.isArray(tableData.rows)) {
          currentTableData.value = tableData
          showTableWindow.value = true
          tableDetected.value = true
          console.log('方法1检测到表格数据:', tableData)
          return
        }
      }

      // 方法2: 检测关键表格提示词
      const tableKeywords = ['表格', '名单', '列表', '统计', '数据', '成员', '师资', '学生']
      const hasTableKeyword = tableKeywords.some(keyword => lastMessage.content.includes(keyword))
      
      // 方法3: 检测结构化数据特征
      const hasStructuredData = (
        lastMessage.content.includes('【') && lastMessage.content.includes('】') && // 部门标记
        lastMessage.content.includes('- ') && // 列表项
        (lastMessage.content.includes('人）') || lastMessage.content.includes('团队')) // 统计信息
      )
      
      if (hasTableKeyword && hasStructuredData) {
        // 创建简单的表格数据
        const lines = lastMessage.content.split('\n').filter(line => line.trim())
        const tableData = {
          title: '工作室信息汇总',
          columns: ['序号', '信息'],
          rows: lines.map((line, index) => [index + 1, line.trim()]),
          metadata: {
            totalCount: lines.length,
            generateTime: new Date().toLocaleString('zh-CN'),
            dataSource: '聊天内容解析',
            sortable: true,
            exportable: true
          }
        }
        
        currentTableData.value = tableData
        showTableWindow.value = true
        tableDetected.value = true
        console.log('方法2+3检测到结构化数据，生成表格:', tableData)
        return
      }
      
      console.log('未检测到表格数据')
    } catch (error) {
      console.log('表格数据检测失败:', error)
    }
  }
}

// 处理表格窗口关闭
const handleTableWindowClose = () => {
  showTableWindow.value = false
  currentTableData.value = null
  tableDetected.value = false // 重置检测状态，允许检测新表格
}

// 初始化
const init = async () => {
  try {
    // 先尝试加载历史记录
    const response = await getChatHistory()
    if (response.data && response.data.length > 0) {
      // 如果有历史记录，使用历史记录并按时间排序（从旧到新）
      messages.value = response.data
        .sort((a: any, b: any) => {
          // 按消息顺序和时间排序
          if (a.sessionId === b.sessionId) {
            return (a.messageOrder || 0) - (b.messageOrder || 0)
          }
          return new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        })
        .map((msg: any) => ({
          id: msg.id || msg.chatId || Date.now(),
        content: msg.content,
        type: msg.type,
          timestamp: typeof msg.timestamp === 'string' ? new Date(msg.timestamp) : new Date(msg.timestamp || Date.now()),
          sessionId: msg.sessionId,
          messageOrder: msg.messageOrder,
          userName: msg.userName,
          realName: msg.realName
      }))
    } else {
      // 如果没有历史记录，显示欢迎消息
      messages.value = [{
        id: 1,
        content: '你好！我是你的AI助理胖达🐻，我不仅知道工作室的事情还知道，生活中的很多事情，有什么可以帮助你的吗？',
        type: 'ai',
        timestamp: new Date()
      }]
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    // 加载失败时显示欢迎消息
    messages.value = [{
      id: 1,
      content: '你好！我是你的AI助理胖达🐻，我不仅知道工作室的事情还知道，生活中的很多事情，有什么可以帮助你的吗？',
      type: 'ai',
      timestamp: new Date()
    }]
  }
}

// 组件挂载时初始化
initPosition() // 设置智能位置
init() // 加载历史记录或显示欢迎消息

// 组件销毁前清理
onBeforeUnmount(() => {
  // 清理流式连接
  if (currentCancelStream) {
    currentCancelStream()
    currentCancelStream = null
  }
})
</script>

<style lang="scss" scoped>
.floating-chat {
  position: fixed;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(15px);
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1004;
  transition: all 0.3s ease;
  overflow: hidden;
  
  &.minimized {
    .chat-header {
      border-radius: 12px;
    }
  }
  
  &.dragging {
    // 拖拽时的样式
    transform: rotate(2deg) scale(1.05);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.25);
    transition: none; // 拖拽时移除过渡动画
    z-index: 1005; // 提升层级
    
    .chat-header {
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.2), rgba(173, 216, 230, 0.4));
      cursor: grabbing !important;
    }
  }

  &.resizing {
    // 调整大小时的样式
    transition: none; // 调整大小时移除过渡动画
    z-index: 1005; // 提升层级
  }
}

.chat-body {
  display: flex;
  flex-direction: column;
  height: calc(100% - 50px); // 减去头部高度
}

.resize-handle {
  position: absolute;
  bottom: 5px;
  right: 5px;
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, #007bff, #0056b3);
  border-radius: 50%;
  cursor: nw-resize;
  z-index: 10;
  opacity: 0.7;
  transition: opacity 0.2s ease;
  
  &:hover {
    opacity: 1;
    transform: scale(1.1);
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 6px;
    height: 6px;
    background: white;
    border-radius: 50%;
  }
  
  &::after {
    content: '↗';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 8px;
    color: white;
    font-weight: bold;
  }
}

.chat-header-wrapper {
  cursor: grab;
  
  &:active {
    cursor: grabbing;
  }
}
</style> 