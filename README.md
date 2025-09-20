# 何湘工作室管理系统

## 🎯 项目简介

何湘工作室管理系统是一个现代化、功能全面的教育工作室管理平台。项目采用前后端分离架构，包含三个主要客户端：为学员设计的微信小程序、为管理员设计的Web管理后台，以及由Java强力驱动的后端服务。

系统集成了先进的AI技术（RAG），通过 `LangChain4j` 和 `Milvus` 向量数据库，为用户提供智能问答和信息检索功能，旨在提升教学管理效率和学员服务体验。

## ✨ 项目特点

- **🔄 多端覆盖**
  - **微信小程序**: 学员可以方便地查看课程、管理日程、签到、接收通知等
  - **Web管理后台**: 管理员可以进行全面的系统管理，包括用户管理、课程发布、公告管理、出勤跟踪等

- **🤖 AI智能助手**: 基于RAG（检索增强生成）架构，提供智能问答功能，能够精准回答与工作室资料库相关的问题

- **🏗️ 前后端分离**: 清晰的架构设计，前端和后端独立开发、部署，便于维护和扩展

- **📦 模块化后端**: 后端采用多模块Maven项目结构，职责分明，代码结构清晰

- **🐳 容器化部署**: 核心依赖（如Milvus向量数据库）通过Docker Compose进行容器化管理，简化了开发和部署环境的配置

## 🛠️ 技术栈

### 后端 (Backend)
- **核心框架**: Spring Boot 3.2.0
- **编程语言**: Java 17
- **数据访问**: MyBatis, MyBatis-Plus, PageHelper
- **认证授权**: JWT (JSON Web Tokens)
- **AI集成**: LangChain4j 1.4.0
- **大语言模型服务**: qwen3
- **项目构建**: Maven
- **网络通信**: Netty

### Web管理后台 (Frontend)
- **核心框架**: Vue.js 3.5.13
- **项目构建**: Vite 6.3.5
- **UI组件库**: Element Plus 2.9.11
- **图标库**: Ant Design Icons, Element Plus Icons
- **路由**: Vue Router 4.5.1
- **编程语言**: TypeScript 5.8.3
- **CSS预处理器**: Sass 1.89.1
- **图表库**: ECharts 5.6.0
- **日历组件**: FullCalendar 6.1.17
- **地图组件**: Leaflet 1.9.4
- **动画库**: Lottie
- **拖拽组件**: Vue.Draggable 4.1.0
- **HTTP客户端**: Axios 1.9.0

### 微信小程序 (WeChat Mini Program)
- **UI组件库**: TDesign Miniprogram
- **开发框架**: 微信小程序原生框架

### 数据库与中间件
- **关系型数据库**: MySQL/PostgreSQL
- **向量数据库**: Milvus 2.6.0
- **对象存储**: MinIO
- **服务注册/发现**: etcd 3.5.18

## 📂 项目结构

```
hexiang_studio （langchain4j）/
├── hexiang_studio_wx/              # 微信小程序前端
│   ├── pages/                      # 小程序页面
│   ├── components/                 # 自定义组件
│   ├── utils/                      # 工具函数
│   ├── images/                     # 图片资源
│   └── miniprogram_npm/            # npm包（TDesign组件库）
├── Milvus/                         # Milvus Docker Compose 配置
│   └── docker-compose.yml
├── resource/                       # 项目资源文件 (如上传的图片等)
│   └── upload/                     # 文件上传目录
└── 前后端/
    ├── back_hexiang_studio_back/   # Java 后端 (Spring Boot)
    │   ├── studio_common/          # 公共模块
    │   ├── studio_mapper/          # 数据访问层
    │   ├── studio_pojo/            # 实体类模块
    │   ├── studio_service/         # 业务逻辑层
    │   ├── studio_web/             # Web控制层
    │   └── rag/                    # RAG向量存储
    ├── hexaing_studio_front/       # Web管理后台前端 (Vue.js)
    │   ├── src/
    │   │   ├── views/              # 页面组件
    │   │   ├── components/         # 公共组件
    │   │   ├── api/                # API接口
    │   │   ├── router/             # 路由配置
    │   │   ├── store/              # 状态管理
    │   │   └── utils/              # 工具函数
    │   └── public/                 # 静态资源
    └── hexiang_studio.sql          # 数据库初始化脚本
```

## 🚀 快速开始

### 环境准备

确保您的开发环境中已安装以下软件：

- **Java 17** 或更高版本
- **Maven 3.6+**
- **Node.js 18+** 和 **npm**
- **Docker** 和 **Docker Compose**
- **微信开发者工具**
- **MySQL 8.0+** 或 **PostgreSQL 12+**

### 1. 启动依赖服务

首先启动Milvus向量数据库及其依赖服务：

```bash
cd Milvus
docker-compose up -d
```

等待所有服务启动完成后，您可以通过以下地址访问：
- MinIO管理界面: http://localhost:9001 (用户名/密码: minioadmin/minioadmin)
- Milvus服务: localhost:19530

### 2. 数据库初始化

1. 在您的MySQL/PostgreSQL数据库中创建新数据库
2. 执行 `前后端/hexiang_studio.sql` 脚本创建数据库表结构

```sql
-- 创建数据库（以MySQL为例）
CREATE DATABASE hexiang_studio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hexiang_studio;

-- 导入表结构
SOURCE 前后端/hexiang_studio.sql;
```

### 3. 后端服务启动

1. 进入后端项目目录：
   ```bash
   cd 前后端/back_hexiang_studio_back
   ```

2. 配置应用参数，编辑 `studio_web/src/main/resources/application.yml`：
   ```yaml
   # 数据库连接配置
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/hexiang_studio
       username: your_username
       password: your_password
   
   # Milvus配置
   milvus:
     host: localhost
     port: 19530
   
   # AI模型配置
   ai:
     api-key: your_volcengine_api_key
   ```

3. 编译并启动后端服务：
   ```bash
   mvn clean install
   cd studio_web
   mvn spring-boot:run
   ```

后端服务将在 http://localhost:8080 启动

### 4. Web管理后台启动

1. 进入前端项目目录：
   ```bash
   cd 前后端/hexaing_studio_front
   ```

2. 安装依赖：
   ```bash
   npm install
   ```

3. 配置API地址，编辑相应的环境配置文件，确保API请求指向后端服务地址

4. 启动开发服务器：
   ```bash
   npm run dev
   ```

Web管理后台将在 http://localhost:5173 启动

### 5. 微信小程序部署

1. 打开微信开发者工具
2. 选择"导入项目"，导入 `hexiang_studio_wx` 目录
3. 输入小程序AppID: `wx05b54f1e908f192d`
4. 在 `config/index.js` 中配置API请求地址，指向后端服务
5. 点击"编译"并预览

## 📱 功能模块

### 微信小程序功能
- **用户管理**: 登录、个人信息管理
- **课程管理**: 课程查看、报名、进度跟踪
- **日程安排**: 个人日程、课程安排查看
- **签到打卡**: 位置签到、二维码签到
- **消息通知**: 接收工作室公告和个人消息
- **任务管理**: 查看和提交作业任务
- **AI助手**: 智能问答，获取学习建议

### Web管理后台功能
- **数据分析**: 学员统计、课程分析、出勤报表
- **用户管理**: 学员信息管理、权限分配
- **课程管理**: 课程发布、编辑、进度管理
- **公告管理**: 发布和管理工作室公告
- **考勤管理**: 出勤统计、请假审批
- **任务管理**: 作业发布、批改、成绩管理
- **素材管理**: 教学资料上传和管理
- **AI知识库**: RAG知识库管理和优化

## 🔧 开发指南

### 后端开发
- 使用Maven进行依赖管理和项目构建
- 遵循Spring Boot最佳实践
- 数据库访问使用MyBatis-Plus
- API接口遵循RESTful规范
- 集成LangChain4j进行AI功能开发

### 前端开发
- 使用Vue 3 Composition API
- TypeScript提供类型安全
- Element Plus组件库快速构建UI
- Vite提供快速的开发体验
- 遵循Vue.js官方风格指南

### 小程序开发
- 使用TDesign组件库保持UI一致性
- 遵循微信小程序开发规范
- 注意小程序的性能优化

## 🚢 部署指南

### 生产环境部署

1. **后端部署**:
   ```bash
   mvn clean package
   java -jar studio_web/target/studio_web-0.0.1-SNAPSHOT.jar
   ```

2. **前端部署**:
   ```bash
   npm run build
   # 将dist目录部署到nginx或其他web服务器
   ```

3. **数据库部署**: 确保生产环境数据库配置正确

4. **Milvus部署**: 在生产环境中部署Milvus集群

### Docker部署（推荐）

项目支持Docker容器化部署，详细的Docker配置文件请参考各模块的Dockerfile。

## 🤝 贡献指南

我们欢迎任何形式的贡献！请阅读以下指南：

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系我们

- 项目维护者: 何湘工作室开发团队
- 邮箱: [3486687886@qq.com]
- 项目地址: [https://github.com/liuclever/hexiang_studio-PangDaAi.git]

---

## 🙏 致谢

感谢以下开源项目和技术：
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Milvus](https://milvus.io/)
- [Element Plus](https://element-plus.org/)
- [TDesign](https://tdesign.tencent.com/)

---

*最后更新时间: 2025年9月* 