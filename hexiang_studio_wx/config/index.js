/**
 * 全局配置文件
 * 统一管理所有环境配置，避免分散配置导致的维护问题
 */

// 获取当前网络环境配置
const getNetworkConfig = () => {
  // 可以根据不同环境自动切换，或手动配置
  const configs = {
    // 开发环境配置
    development: {
      // 后端API地址 - 根据实际网络情况选择
      baseUrl: 'http://172.20.10.2:8044',
      // 文件访问地址
      fileUrl: 'http://172.20.10.2:8044',
      // WebSocket调试地址（由微信开发者工具自动配置）
      wsUrl: 'ws://172.20.10.2:8001'
    },
    
    // 生产环境配置
    production: {
      baseUrl: 'https://your-production-domain.com',
      fileUrl: 'https://your-production-domain.com',
      wsUrl: 'wss://your-production-domain.com'
    },
    
    // 本地环境配置
    local: {
      baseUrl: 'http://localhost:8044',
      fileUrl: 'http://localhost:8044',
      wsUrl: 'ws://localhost:8001'
    }
  };
  
  // 当前使用的环境（可以通过这里快速切换）
  const currentEnv = 'development';
  
  return configs[currentEnv];
};

// 导出配置
const config = getNetworkConfig();

module.exports = {
  // API基础地址
  BASE_URL: config.baseUrl,
  
  // 文件访问地址
  FILE_URL: config.fileUrl,
  
  // WebSocket地址（调试用）
  WS_URL: config.wsUrl,
  
  // API版本
  API_VERSION: 'v1',
  
  // 超时配置
  TIMEOUT: 15000, // 减少超时时间到15秒
  
  // 分页配置
  PAGE_SIZE: 10,
  
  // 文件上传配置
  UPLOAD: {
    maxSize: 10 * 1024 * 1024, // 10MB
    allowedTypes: ['image/jpeg', 'image/png', 'image/gif', 'application/pdf', 'application/msword']
  },
  
  // 调试配置
  DEBUG: true,
  
  // 完整配置对象（供高级用法）
  config
}; 