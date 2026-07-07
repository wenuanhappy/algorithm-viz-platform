# 算法可视化学习平台

**第七小组项目** — 交互式算法学习与可视化平台

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Angular | 17.3 |
| 3D 可视化 | Three.js | 0.184 |
| CSS 框架 | Tailwind CSS | 3.4 |
| 后端框架 | Spring Boot | 3.2.3 |
| ORM | Spring Data JPA | - |
| 数据库 | MySQL | 8.0 |
| 实时通信 | WebSocket | - |
| 构建工具 | Maven / npm | - |

## 项目结构

```
algorithm-viz-platform/
├── frontend/                      # Angular 17 前端应用
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/        # 通用组件
│   │   │   │   ├── ai-complexity-dialog/     # AI 复杂度分析对话框
│   │   │   │   ├── assessment-container/     # 评测容器
│   │   │   │   │   └── assessment-settings/  # 评测设置
│   │   │   │   ├── assessment/                # 评测组件
│   │   │   │   ├── auth/                      # 认证组件
│   │   │   │   ├── complexity-panel/          # 复杂度面板
│   │   │   │   ├── competition/               # 竞赛组件
│   │   │   │   ├── control-panel/             # 控制面板
│   │   │   │   ├── history-panel/             # 历史记录面板
│   │   │   │   ├── input-config/              # 输入配置
│   │   │   │   ├── phase-guide/               # 阶段引导
│   │   │   │   └── sidebar/                   # 侧边栏导航
│   │   │   ├── data/               # 静态数据
│   │   │   │   ├── algorithm-catalog.ts       # 算法目录
│   │   │   │   └── test-scenarios.ts          # 测试场景
│   │   │   ├── models/             # 数据模型
│   │   │   ├── services/           # API 服务
│   │   │   │   ├── algorithm.service.ts       # 算法服务
│   │   │   │   ├── auth.service.ts            # 认证服务
│   │   │   │   ├── chat.service.ts            # 聊天服务
│   │   │   │   ├── competition.service.ts     # 竞赛服务
│   │   │   │   └── webrtc-peer.service.ts     # WebRTC 点对点服务
│   │   │   ├── store/              # 状态管理 (NgRx-style)
│   │   │   └── visualizers/        # 算法可视化组件
│   │   │       ├── sorting/        # 排序算法可视化
│   │   │       ├── graph/          # 图搜索可视化
│   │   │       ├── dp/             # 动态规划可视化
│   │   │       ├── search/         # 搜索算法可视化
│   │   │       ├── divide-conquer/ # 分治算法可视化
│   │   │       ├── n-queens/       # N皇后可视化
│   │   │       └── vr-3d/          # VR 3D 数据结构可视化
│   │   │           ├── animators/  # 动画控制器
│   │   │           ├── data/       # 结构数据
│   │   │           └── renderers/  # Three.js 渲染器
│   │   ├── assets/
│   │   │   └── login-bg.png
│   │   └── environments/
│   ├── angular.json
│   ├── package.json
│   ├── tailwind.config.js
│   └── tsconfig.json
│
├── backend/                       # Spring Boot 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/algorithmviz/
│   │   │   │   ├── AlgorithmVizApplication.java  # 启动类
│   │   │   │   ├── config/                        # 配置类
│   │   │   │   │   ├── CorsConfig.java            # CORS 跨域配置
│   │   │   │   │   └── WebSocketConfig.java       # WebSocket 配置
│   │   │   │   ├── controller/                     # REST 控制器
│   │   │   │   │   ├── AlgorithmController.java   # 算法 API
│   │   │   │   │   ├── AuthController.java        # 认证 API
│   │   │   │   │   ├── ChatController.java        # 聊天 API
│   │   │   │   │   └── CompetitionController.java # 竞赛 API
│   │   │   │   ├── dto/                            # 数据传输对象
│   │   │   │   ├── entity/                         # JPA 实体
│   │   │   │   ├── model/                          # 业务模型
│   │   │   │   ├── repository/                     # 数据仓库
│   │   │   │   ├── service/                        # 业务逻辑
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── AlgorithmComplexityService.java
│   │   │   │   │   ├── BacktrackingService.java
│   │   │   │   │   ├── ChatService.java
│   │   │   │   │   ├── CompetitionRoomService.java
│   │   │   │   │   ├── DivideConquerService.java
│   │   │   │   │   ├── DPService.java
│   │   │   │   │   ├── GraphService.java
│   │   │   │   │   ├── SearchService.java
│   │   │   │   │   ├── SortingService.java
│   │   │   │   │   └── AssessmentService.java
│   │   │   │   └── websocket/
│   │   │   │       └── SignalingHandler.java      # WebRTC 信令
│   │   │   └── resources/
│   │   │       └── application.properties         # 应用配置
│   │   └── test/
│   │       └── java/com/algorithmviz/service/
│   │           └── CompetitionRoomServiceTest.java # 单元测试
│   └── pom.xml
│
├── docs/                          # 项目文档
│   ├── ai助手.md                  # AI 助手功能说明
│   ├── assessment-llm-implementation.md  # 评测系统 LLM 实现
│   ├── comprehensive-project-documentation.md  # 综合项目文档
│   ├── learning-guidance-system.md        # 学习指导系统
│   └── 部署文档.md                 # 部署指南
│
├── .gitignore
├── CLAUDE.md                      # 本文件
├── 算法可视化学习平台_第7小组.pptx    # 演示 PPT
├── 算法可视化平台报告文档.pdf         # 项目报告
└── 算法平台演示视频.mov              # 演示视频 (~175MB)
```

## 关键命令

### 前端开发
```bash
cd frontend
npm install              # 安装依赖
npm start                # 启动开发服务器 (http://localhost:4200)
npm run build            # 生产构建到 dist/
```

### 后端开发
```bash
cd backend
./mvnw spring-boot:run   # 启动后端服务 (http://localhost:8080)
./mvnw clean package     # 构建 JAR 包到 target/
```

### Docker 部署
```bash
# 需要创建 docker-compose.yml 和相关 Dockerfile
docker-compose up -d --build
```

## 核心功能

| 功能 | 描述 |
|------|------|
| **算法可视化** | 排序、图搜索、动态规划、回溯、分治、N皇后 |
| **VR 3D 展示** | 二叉树、B+树、栈、队列、数组、链表的 Three.js 3D 可视化 |
| **在线竞赛** | 实时对战竞速，支持 WebRTC 点对点通信 |
| **AI 助手** | 智能答疑，基于 LLM 的算法问题解答 |
| **评测系统** | 自动评测算法实现，LLM 辅助评分 |
| **复杂度分析** | 时间/空间复杂度分析，AI 辅助优化建议 |
| **学习指导** | 个性化学习路径推荐 |

## API 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/auth/*` | POST | 用户认证 (注册/登录) |
| `/api/algorithm/*` | POST | 算法可视化 (排序/图/DP/搜索等) |
| `/api/chat/*` | GET/POST | AI 聊天功能 |
| `/api/competition/*` | - | 竞赛房间管理 |
| `/ws/signal` | WS | WebRTC 信令通道 |

## 数据库实体

- **AppUser** - 用户信息
- **RunHistory** - 运行历史记录
- **ChatSession** - 聊天会话
- **ChatMessage** - 聊天消息

## 注意事项

- 视频文件较大 (~175MB)，建议上传至外链存储或添加至 .gitignore
- 后端需要配置 MySQL 数据库连接
- 前端开发服务器默认端口 4200，后端默认端口 8080
- WebSocket 用于 WebRTC 信令，不是通用的实时聊天
