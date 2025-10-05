# 配置管理说明

## 🎯 统一配置系统

本项目已实现统一配置管理，避免了之前分散配置导致的维护问题。

## 📁 配置文件位置

```
config/index.js - 主配置文件
```

## 🔧 快速切换环境

在 `config/index.js` 第37行，修改 `currentEnv` 值：

```javascript
// 当前使用的环境（可以通过这里快速切换）
const currentEnv = 'development';  // 👈 修改这里
```

### 可选环境：

- **`development`** - 开发环境（当前网络：192.168.11.60）
- **`local`** - 本地环境（localhost）
- **`production`** - 生产环境（需要配置正式域名）

## 📊 配置项说明

| 配置项 | 说明 | 用途 |
|--------|------|------|
| `BASE_URL` | API基础地址 | 小程序请求后端接口 |
| `FILE_URL` | 文件访问地址 | 查看/下载文件 |
| `WS_URL` | WebSocket地址 | 真机调试连接 |
| `TIMEOUT` | 请求超时时间 | 网络请求配置 |
| `PAGE_SIZE` | 分页大小 | 列表分页 |

## 🔄 切换步骤

1. **打开配置文件**：`config/index.js`
2. **修改环境**：更改第37行的 `currentEnv` 值
3. **重新编译**：在微信开发者工具中点击"编译"
4. **测试连接**：验证功能是否正常

## 🏠 网络环境配置

### 开发环境 (development)
```javascript
baseUrl: 'http://192.168.11.60:8044'
```
- **适用场景**：真机测试、团队开发
- **要求**：手机和电脑在同一WiFi网络

### 本地环境 (local)
```javascript
baseUrl: 'http://localhost:8044'
```
- **适用场景**：模拟器测试、本地开发
- **要求**：后端服务在本机运行

### 生产环境 (production)
```javascript
baseUrl: 'https://your-domain.com'
```
- **适用场景**：正式发布
- **要求**：配置正式域名和HTTPS

## 🚀 启动后端服务

确保后端服务已启动：

```bash
# 进入后端目录
cd back_hexiang_studio_back

# 启动服务
mvn spring-boot:run
```

## ❗ 注意事项

1. **Redis 依赖**：确保 Redis 服务已启动（端口 6379）
2. **数据库连接**：确保 MySQL 数据库可用
3. **网络连通性**：测试 IP 地址 ping 连通性
4. **防火墙设置**：确保端口 8044 未被阻挡

## 🔍 故障排查

### 1. 登录失败
- 检查后端服务是否启动（端口 8044）
- 检查 Redis 服务是否运行
- 验证网络连通性：`ping 192.168.11.60`

### 2. 文件无法加载
- 确认 `FILE_URL` 配置正确
- 检查文件路径是否存在

### 3. 真机调试连接失败
- 确保手机和电脑在同一网络
- 检查防火墙设置
- 重启微信开发者工具

## 📝 更新日志

- ✅ 统一配置管理系统
- ✅ 环境快速切换
- ✅ 消除硬编码地址
- ✅ 集中化配置维护 

## 🚀 发布时环境切换指南

### 1. 开发环境（当前）
```javascript
const currentEnv = 'development';
```
- 使用本地IP地址或开发服务器
- 适用于开发调试

### 2. 发布生产环境
**发布前必须修改 `config/index.js` 文件：**

```javascript
// 将这行改为：
const currentEnv = 'production';
```

### 3. 生产环境域名配置
修改 `production` 配置中的域名：

```javascript
production: {
  baseUrl: 'https://api.你的域名.com',     // 你的API服务器域名
  fileUrl: 'https://files.你的域名.com',   // 你的文件服务器域名  
  wsUrl: 'wss://ws.你的域名.com'          // 你的WebSocket域名
}
```

### 4. 微信小程序后台配置
发布时还需要在微信公众平台配置以下域名：

- **request合法域名**：`https://api.你的域名.com`
- **uploadFile合法域名**：`https://files.你的域名.com`
- **downloadFile合法域名**：`https://files.你的域名.com`
- **socket合法域名**：`wss://ws.你的域名.com`

### 5. 发布检查清单
- [ ] 修改 `currentEnv` 为 `'production'`
- [ ] 配置正确的生产域名
- [ ] 在微信后台添加域名白名单
- [ ] 测试所有网络请求功能
- [ ] 上传代码并提交审核

### 6. 快速切换
如果需要快速切换环境，只需修改 `currentEnv` 变量：
- `'development'` - 开发环境
- `'production'` - 生产环境  
- `'local'` - 本地环境 