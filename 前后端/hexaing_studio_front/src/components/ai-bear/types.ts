/**
 * 聊天消息类型
 */
export interface ChatMessage {
  id: number
  content: string
  type: 'user' | 'ai'
  timestamp: Date
  sessionId?: string
  messageOrder?: number
  userName?: string
  realName?: string
}

/**
 * 聊天窗口位置
 */
export interface Position {
  x: number
  y: number
}

/**
 * 聊天请求参数（与后端Java ChatRequest保持一致）
 */
export interface ChatRequest {
  userId: string          // 用户ID（必填）
  message: string         // 用户消息（必填）
  sessionId?: string      // 会话ID（可选）
}

/**
 * 聊天响应
 */
export interface ChatResponse {
  content: string
  conversationId?: string
  timestamp?: string
}

/**
 * AI选择项（与后端ChoiceOption保持一致）
 */
export interface ChoiceOption {
  id: string
  displayText: string
  description?: string
  queryIntent: string
  icon?: string
  category?: string
}

/**
 * 增强的聊天响应（与后端ChatResponse保持一致）
 */
export interface EnhancedChatResponse {
  type: 'text' | 'choices'
  content?: string
  prompt?: string
  choices?: ChoiceOption[]
  sessionId?: string
  isStreaming?: boolean
}

/**
 * 小熊消息类型
 */
export interface BearMessage {
  content: string
  type: string
  timestamp?: string
} 