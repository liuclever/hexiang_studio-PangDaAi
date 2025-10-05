import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'
import type { ChatMessage, ChatRequest, ChatResponse, BearMessage, EnhancedChatResponse } from '@/components/ai-bear/types'
import {getToken, getUserId} from "@/utils/auth";

// 🔧 改进的会话管理
interface SessionInfo {
  sessionId: string;
  timestamp: number;
  userId: string;
}

// 30分钟会话超时
const SESSION_TIMEOUT = 30 * 60 * 1000;

/**
 * 获取会话存储key（按用户隔离）
 */
function getSessionStorageKey(userId: string): string {
  return `ai_session_${userId}`;
}

/**
 * 从localStorage获取会话信息
 */
function getStoredSession(userId: string): SessionInfo | null {
  try {
    const key = getSessionStorageKey(userId);
    const stored = localStorage.getItem(key);
    if (stored) {
      const session = JSON.parse(stored) as SessionInfo;
      // 验证是否过期
      if (Date.now() - session.timestamp < SESSION_TIMEOUT) {
        return session;
      } else {
        // 过期则清除
        localStorage.removeItem(key);
      }
    }
  } catch (error) {
    console.warn('⚠️ 读取会话信息失败:', error);
  }
  return null;
}

/**
 * 保存会话信息到localStorage
 */
function storeSession(session: SessionInfo): void {
  try {
    const key = getSessionStorageKey(session.userId);
    localStorage.setItem(key, JSON.stringify(session));
    console.debug('💾 会话信息已保存:', session.sessionId);
  } catch (error) {
    console.warn('⚠️ 保存会话信息失败:', error);
  }
}

/**
 * 获取或生成会话ID（支持用户隔离和持久化）
 */
function getOrCreateSessionId(): string {
  const userId = getUserId()?.toString() || 'anonymous';
  const now = Date.now();
  
  // 先尝试从存储中获取
  const storedSession = getStoredSession(userId);
  if (storedSession && storedSession.userId === userId) {
    console.debug('🔧 使用存储的会话ID:', storedSession.sessionId);
    return storedSession.sessionId;
  }
  
  // 生成新的会话ID
  const newSessionId = `${userId}_web_${now}`;
  const newSession: SessionInfo = {
    sessionId: newSessionId,
    timestamp: now,
    userId: userId
  };
  
  // 保存到存储
  storeSession(newSession);
  
  console.debug('🔧 生成新会话ID:', newSessionId);
  return newSessionId;
}

/**
 * 清除当前用户的会话（用于手动重置对话上下文）
 */
export function clearCurrentSession(): void {
  const userId = getUserId()?.toString();
  if (userId) {
    const key = getSessionStorageKey(userId);
    localStorage.removeItem(key);
    console.debug('🔧 清除用户会话:', userId);
  }
}

/**
 * 清除指定用户的会话（用于用户切换时清理）
 */
export function clearUserSession(userId: string): void {
  const key = getSessionStorageKey(userId);
  localStorage.removeItem(key);
  console.debug('🔧 清除指定用户会话:', userId);
}

/**
 * 清除所有过期会话（用于定期清理）
 */
export function cleanExpiredSessions(): void {
  try {
    const keys = Object.keys(localStorage).filter(key => key.startsWith('ai_session_'));
    let cleanedCount = 0;
    
    keys.forEach(key => {
      try {
        const stored = localStorage.getItem(key);
        if (stored) {
          const session = JSON.parse(stored) as SessionInfo;
          if (Date.now() - session.timestamp >= SESSION_TIMEOUT) {
            localStorage.removeItem(key);
            cleanedCount++;
          }
        }
      } catch (error) {
        // 损坏的数据直接删除
        localStorage.removeItem(key);
        cleanedCount++;
      }
    });
    
    if (cleanedCount > 0) {
      console.debug(`🧹 清理了 ${cleanedCount} 个过期会话`);
    }
  } catch (error) {
    console.warn('⚠️ 清理过期会话失败:', error);
  }
}

/**
 * 清理指定用户的会话缓存
 * @param userId 用户ID，不指定则清理所有AI会话缓存
 */
export function clearAiSessionCache(userId?: string): void {
  try {
    if (userId) {
      // 清理特定用户的会话缓存
      const key = getSessionStorageKey(userId);
      localStorage.removeItem(key);
      console.debug('🧹 已清理用户会话缓存:', userId);
    } else {
      // 清理所有AI会话缓存
      const keysToRemove: string[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith('ai_session_')) {
          keysToRemove.push(key);
        }
      }
      
      keysToRemove.forEach(key => localStorage.removeItem(key));
      console.debug('🧹 已清理所有AI会话缓存，共清理:', keysToRemove.length, '个缓存');
    }
  } catch (error) {
    console.error('清理AI会话缓存失败:', error);
  }
}

/**
 * 重置当前用户的会话（开始新对话）
 */
export function resetCurrentUserSession(): void {
  const userId = getUserId();
  if (userId) {
    clearAiSessionCache(userId.toString());
    console.debug('🔄 已重置当前用户会话，下次对话将开始新的上下文');
  }
}

// 定期清理过期会话（每10分钟）
setInterval(cleanExpiredSessions, 10 * 60 * 1000);

/**
 * 获取小熊要说的话
 */
export function getBearMessage(): Promise<ApiResponse<BearMessage>> {
  return request({
    url: '/ai-bear/message',
    method: 'get'
  })
}

/**
 * 发送聊天消息到AI助手
 */
export function sendChatMessage(message: string, userId?: string): Promise<ApiResponse<ChatResponse>> {
  const currentUserId = getUserId();
  const chatRequest: ChatRequest = {
    message: message,
    userId: userId || (currentUserId ? currentUserId.toString() : 'anonymous'),
    sessionId: getOrCreateSessionId() // 🔧 添加会话ID
  };
  
  return request({
    url: '/ai-assistant/chat',
    method: 'post',
    data: chatRequest
  })
}

/**
 * 发送流式聊天消息到AI助手
 */
export function sendChatMessageStream(
  message: string, 
  onMessage: (data: string) => void,
  onComplete: () => void,
  onError: (error: any) => void,
  userId?: string
): () => void {
  let isCompleted = false;
  let abortController = new AbortController();
  
  try {
    const token = getToken();
    const currentUserId = getUserId();
    const chatRequest: ChatRequest = {
      message: message,
      userId: userId || (currentUserId ? currentUserId.toString() : 'anonymous'),
      sessionId: getOrCreateSessionId() // 🔧 添加会话ID
    };
    
    // 发送POST请求到流式接口
    fetch('/api/ai-assistant/stream/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify(chatRequest),
      signal: abortController.signal
    })
    .then(response => {
      if (!response.ok) {
        // 尝试解析错误响应的详细信息
        return response.text().then(text => {
          let errorMsg = `服务暂时不可用，请稍后重试`;
          try {
            const errorData = JSON.parse(text);
            if (errorData.msg) {
              // 清理错误消息，移除多余的换行符和格式化
              let cleanMsg = errorData.msg.replace(/\\n/g, '').replace(/\n+/g, ' ').trim();
              
              // 根据错误码提供更友好的提示
              if (errorData.code === 4030) {
                errorMsg = '权限不足，请联系管理员开启AI功能权限';
              } else if (errorData.code === 401) {
                errorMsg = '登录已过期，请重新登录';
              } else if (errorData.code === 500) {
                errorMsg = 'AI服务暂时不可用，请稍后重试';
              } else if (cleanMsg.length > 0 && cleanMsg.length < 100) {
                // 只有当清理后的消息不超过100字符时才显示
                errorMsg = cleanMsg;
              }
            }
          } catch (e) {
            if (text && text.length < 100) {
              errorMsg = text;
            }
          }
          throw new Error(errorMsg);
        });
      }
      
      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('流响应不可用');
      }
      
      const decoder = new TextDecoder();
      let buffer = '';
      
      function readStream(): Promise<void> {
        return reader!.read().then(({ done, value }) => {
          if (done) {
            if (!isCompleted) {
      isCompleted = true;
      onComplete();
            }
            return;
          }
          
          // 解码数据块
          const chunk = decoder.decode(value, { stream: true });
          buffer += chunk;
          
          // 处理完整的消息
          // 2025-09-14: 修复流式数据处理逻辑，后端返回的是纯文本流
          // 不需要再按行分割，直接将整个 chunk 传递给 onMessage
          if (chunk) {
            onMessage(chunk);
          }
          
          return readStream();
        });
      }
      
      return readStream();
    })
    .catch(error => {
      if (error.name === 'AbortError') {
        console.log('流式请求被取消');
        return;
      }
      console.error('流式请求失败:', error);
      if (!isCompleted) {
        onError(error);
      }
    });
    
  } catch (error) {
    console.error('创建流式连接失败:', error);
    onError(error);
  }
  
  // 返回取消函数
  return () => {
    isCompleted = true;
    abortController.abort();
  };
}

/**
 * 获取聊天历史
 */
export function getChatHistory(): Promise<ApiResponse<ChatMessage[]>> {
  return request({
    url: '/ai-bear/history',
    method: 'get'
  })
}

/**
 * 清除聊天历史记录
 */
export function clearChatHistory(): Promise<ApiResponse<string>> {
  return request({
    url: '/ai-bear/history',
    method: 'delete'
  })
}

/**
 * 发送增强聊天消息（支持选择项功能）
 * 当用户输入模糊时，AI会返回选择项供用户选择
 */
export function sendEnhancedChatMessage(message: string, userId?: string): Promise<ApiResponse<EnhancedChatResponse>> {
  const currentUserId = getUserId();
  const chatRequest: ChatRequest = {
    message: message,
    userId: userId || (currentUserId ? currentUserId.toString() : 'anonymous'),
    sessionId: getOrCreateSessionId() // 🔧 添加会话ID
  };
  
  return request({
    url: '/ai-assistant/chat/enhanced',
    method: 'post',
    data: chatRequest
  })
} 