# 算法与复杂度可视化学习平台 — 项目综合文档
小组分工占比：徐润杰100%、邹佳旭100%、陈润100%、张宁100%，即四人分工均等
> 版本 1.2.0 | 2026-06-19 | Angular 17 + Spring Boot 3.2 + WebRTC + MySQL + Docker + AWS ECS

---

## 目录

1. [项目概述](#1-项目概述)
2. [需求分析](#2-需求分析)
3. [系统设计](#3-系统设计)
4. [实现细节](#4-实现细节)
5. [部署与运行](#5-部署与运行)
6. [使用指南](#6-使用指南)
7. [API 参考](#7-api-参考)
8. [数据库设计](#8-数据库设计)
9. [文件清单](#9-文件清单)

---

## 1. 项目概述

### 1.1 项目定位

算法与复杂度可视化学习平台（Algorithm Visualization Learning Platform）是一个交互式 Web 应用，通过**分步可视化**的方式展示经典算法的执行过程。平台覆盖排序、搜索、图遍历、动态规划、回溯、分治六大类共 17 种算法，并提供**教学阶段引导**、**对比模式**、**评估测试**、**AI 复杂度分析**和 **WebRTC 算法 1v1 竞赛**等教学辅助功能，将传统"看见结果"的可视化工具升级为"看见过程、即时练习、实时对战"的学习平台。

### 1.2 核心能力

| 能力 | 说明 |
|------|------|
| 算法执行过程可视化 | 17 种算法的每一步状态变化通过柱状图、SVG 图形、DP 表格、棋盘等直观展示 |
| 教学过程分解（Phase） | 每种算法的执行过程被划分为离散的教学阶段，以进度条形式向学习者展示"算法进行到哪一步了" |
| 双算法对比模式 | 同时运行两个同分类算法，左右并排展示，实时对比步数、比较次数、操作次数 |
| 评估测试系统 | LLM 动态生成题目（支持配置题数/分类/难度/题型）+ 经典固定 5 题降级方案，语义评测与个性化反馈 |
| 算法 1v1 竞赛 | 每种算法提供对应竞赛题库；用户创建或加入 6 位房间，双方准备后通过 WebRTC DataChannel 实时同步进度和聊天，由后端统一判分 |
| AI 复杂度分析 | 接入大语言模型，对用户自定义算法代码进行时间/空间复杂度分析 |
| 3D 数据结构可视化 | 基于 Three.js 的 3D 渲染引擎，可视化数组、栈、队列、链表、二叉树、B+ 树 |
| 用户认证系统 | 注册/登录功能，SHA-256 加盐哈希存储密码 |
| 运行历史记录 | 自动保存每次算法运行的参数、结果和性能数据，可按分类筛选 |

### 1.3 技术栈

```
前端：Angular 17 (Standalone Components) + TypeScript 5.4 + Tailwind CSS 3.4 + Three.js 0.184
后端：Spring Boot 3.2.3 + Java 17 + Spring WebSocket + Spring Data JPA + MySQL Connector
数据库：MySQL 8.0
AI 服务：并行智算云 DeepSeek-V3.2 (兼容 OpenAI 格式)
实时通信：WebRTC DataChannel + WebSocket Signaling + STUN（Cloudflare / Google）
```

### 1.4 AI 辅助开发方法

本项目在开发过程中广泛使用 AI 编程助手（Claude Code 等）来加速框架搭建和代码生成。以下是 AI 辅助开发的关键环节和工作模式。

#### 1.4.1 项目框架搭建

**Spring Boot 后端脚手架**：通过自然语言描述需求（"搭建一个 Spring Boot 3.2 项目，包含 JPA、MySQL、CORS 配置"），AI 生成了完整的项目骨架——包括 `pom.xml` 依赖声明、`application.properties` 数据源配置、`CorsConfig` 跨域配置类、以及 `AlgorithmVizApplication` 主入口。人工只需补充数据库密码等环境变量即可运行。

**Angular 前端脚手架**：通过描述组件树和路由需求，AI 一次性生成了 Standalone Components 体系——`AppComponent`、`SidebarComponent`、`ControlPanelComponent` 及 7 个 Visualizer 组件的骨架代码（含 TypeScript 逻辑、HTML 模板和 Tailwind 样式），省去了逐个 `ng generate` 和样板代码填写的时间。AI 还自动配置了 `angular.json` 中的生产环境文件替换策略（`environment.ts` → `environment.prod.ts`），使首次部署时即避免了端口硬编码问题。

**Docker 部署体系**：`Dockerfile`（前后端各一份）、`docker-compose.yml`（MySQL + Backend + Frontend + Nginx 四容器编排）、`nginx.conf`（反向代理规则）均由 AI 根据项目结构生成初版，人工仅调整了环境变量和服务名以适配 AWS ECS 实际网络拓扑。

#### 1.4.2 算法实现与步骤生成

17 种算法的 Service 层代码（如 `SortingService` 中的快速排序、`GraphService` 中的 Dijkstra）遵循统一的"执行 + 记录"模式——在标准算法逻辑的每个关键操作点插入 `Step` 对象的构建和收集。这类代码具有高度模式化特征：不同算法的差异在于具体逻辑，但 Step 构建、列表管理、计数器维护的结构完全一致。AI 在理解一种算法的实现模式后，能快速复用到其余 16 种算法，人工仅需验证步骤划分的合理性和教学语义的准确性。

#### 1.4.3 前后端类型同步

前端 TypeScript 接口（`algorithm.models.ts` 中的 `SortStep`、`GraphStep` 等）与后端 Java 模型类（`model/SortStep.java`、`model/GraphStep.java` 等）之间存在严格的字段对应关系。当后端 Step 模型新增字段（如 `phase`、`codeLine`）时，AI 可同步更新前端接口定义和 Visualizer 中的渲染逻辑，避免人工逐文件查找和修改导致的遗漏。

#### 1.4.4 AI 辅助的工作模式总结

| 阶段 | AI 的角色 | 人工的职责 |
|------|----------|-----------|
| 项目初始化 | 生成完整项目骨架、配置文件、依赖声明 | 确认技术选型，填充环境变量（数据库密码、API Key） |
| 框架搭建 | 生成组件/服务/路由的样板代码 | 审查架构合理性，调整组件拆分粒度 |
| 算法实现 | 按统一模式批量生成 Service 层代码 | 验证步骤划分的教学语义，确认 phase 标注正确 |
| 前后端对接 | 同步更新 DTO/接口/渲染逻辑 | 确认字段映射无误，端到端测试 |
| 部署配置 | 生成 Dockerfile、docker-compose、nginx.conf | 适配实际服务器环境，安全审计 |
| Bug 修复 | 定位问题代码，提供修复方案 | 验证修复的正确性，回归测试 |

**关键经验**：
- **模式复用 > 逐段生成**：让 AI 先理解一种算法的完整实现模式，再批量应用到同类算法，效率远高于逐个算法从零生成
- **类型系统是合约**：后端 Step 模型和前端接口定义构成前后端的数据合约，AI 能有效维护这份合约的一致性
- **生成 + 审查 > 纯手工 > 纯自动**：AI 生成的代码经过人工审查后提交，既保持了开发速度，又保证了代码质量。
- **环境配置一次性生成**：Docker、Nginx、CORS 等基础设施配置由 AI 根据最佳实践生成初版，人工只需适配实际部署环境

---

## 2. 需求分析

### 2.1 功能需求

#### FR1: 算法运行与可视化
- **FR1.1** 支持 6 大分类 17+ 种算法的运行，包括：快速排序、归并排序、冒泡排序、堆排序、插入排序、二分查找、Dijkstra 最短路、BFS 宽度优先搜索、DFS 深度优先搜索、Prim 最小生成树、Kruskal 最小生成树、A\* 寻路、0/1 背包、N 皇后问题、Karatsuba 大整数乘法、3D 数据结构可视化
- **FR1.2** 每种算法生成完整的分步执行序列，每步包含描述文本、关联的伪代码行、指标计数
- **FR1.3** 支持逐步前进/后退、自动播放（可调速度）、进度跳转
- **FR1.4** 每个分类配有专属的可视化渲染器：排序→柱状图、图→SVG 节点/边、搜索→数组高亮、DP→二维表格、回溯→棋盘网格、分治→递归树

#### FR2: 教学阶段引导（Learning Guidance）
- **FR2.1** 为每种算法定义离散的教学阶段（phase），如快速排序的 `select_pivot → compare → swap → pivot_placed → done`
- **FR2.2** 阶段进度条实时显示当前所处阶段、已完成阶段、未到达阶段
- **FR2.3** 每个阶段配有中文标签，降低理解门槛

#### FR3: 对比模式
- **FR3.1** 允许用户选择同一分类内的两个算法进行并排对比
- **FR3.2** 一个播放控制器同时推进两个算法的步骤
- **FR3.3** 对比指标面板实时显示步数、比较次数、操作次数，绿色高亮表现更优的一方

#### FR4: 评估测试
- **FR4.1** 支持两种模式：AI 动态出题（LLM 生成题目 + 语义评测）和经典固定题（5 道预设题）
- **FR4.2** AI 模式下支持配置：题目数量（1-15）、覆盖分类/算法、难度等级、题型偏好
- **FR4.3** LLM 语义评测：理解等价表达（格式差异、不同表述），生成个性化反馈
- **FR4.4** 答案交叉验证：对 state-fill/table-fill 题型运行实际算法获取 ground truth 并修正 LLM 答案
- **FR4.5** 完成后总分卡片展示，支持返回设置重新测试

#### FR5: AI 复杂度分析
- **FR5.1** 用户输入任意语言的算法代码，选择关注的分析场景（最坏/平均/最好情况）
- **FR5.2** 调用大语言模型分析时间和空间复杂度，返回结构化的 JSON 结果
- **FR5.3** 结果包含推理步骤、假设前提、优化建议、置信度

#### FR6: 3D 数据结构可视化
- **FR6.1** 基于 Three.js 渲染数组、栈、队列、链表、二叉树、B+ 树六种数据结构的 3D 模型
- **FR6.2** 每个结构有独立的 3D 渲染器和动画系统
- **FR6.3** 支持随机生成数据

#### FR7: 用户认证
- **FR7.1** 用户注册（用户名、显示名、密码）
- **FR7.2** 用户登录
- **FR7.3** 密码使用 SHA-256(salt + ":" + password) 加盐哈希存储

#### FR8: 运行历史
- **FR8.1** 自动记录每次算法运行的类别、算法名、输入参数、步数、比较次数、操作次数、耗时
- **FR8.2** 按分类筛选历史
- **FR8.3** 删除历史记录

#### FR9: WebRTC 算法 1v1 竞赛
- **FR9.1** 顶部提供统一的 1v1 竞赛入口，进入后可选择算法，并支持创建房间和输入 6 位房间号加入
- **FR9.2** 房间最多容纳两名玩家；双方均点击“准备”后，后端同时开放题目并开始计时
- **FR9.3** 每场包含 3 道算法相关选择题，后端维护答案、校验答题顺序并执行权威判分
- **FR9.4** WebSocket 仅用于 WebRTC Offer、Answer、ICE Candidate 等信令交换；答题进度和聊天优先通过 WebRTC DataChannel 点对点传输
- **FR9.5** REST 轮询作为状态同步降级方案，即使 WebRTC 失败，创建、加入、准备、答题和结算仍可正常完成
- **FR9.6** 支持刷新恢复比赛、房间号复制、离开确认、断线 15 秒重连宽限、超时判负和赛后“再来一局”
- **FR9.7** 开赛前接口不返回题目内容，防止用户提前查看题目；计时和速度奖励均以后端时间为准

### 2.2 非功能需求

| 类别 | 要求 |
|------|------|
| 性能 | 排序算法支持最多 100 个元素的实时可视化；图算法支持 20 节点以内的流畅渲染 |
| 可用性 | 响应式布局，支持现代浏览器（Chrome 90+, Edge 90+, Firefox 90+） |
| 可靠性 | 后端错误友好提示；前端 loading/empty/error 三态覆盖；竞赛采用 WebRTC + REST 轮询双通道并支持信令自动重连 |
| 安全性 | 密码加盐哈希存储，CORS/WebSocket Origin 限制，竞赛答案仅保存在服务端，N 皇后 N 上限 12 防止资源耗尽 |
| 可维护性 | 单体架构，分层清晰（Controller → Service → Repository），前后端分离 |

### 2.3 用户角色

- **学习者**：核心用户。通过可视化理解算法原理，使用评估测试自检，并通过 1v1 竞赛进行即时练习
- **教师/助教**：可借助对比模式在课堂上演示不同算法的效率差异
- **开发者**：可使用 AI 复杂度分析快速估算自定义算法的复杂度

---

## 3. 系统设计

### 3.1 整体架构

```
┌────────────────────────────────────────────────────────────────────┐
│                     浏览器 (localhost:4200)                         │
│                                                                    │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌──────────────────┐  │
│  │  Sidebar │  │  Control │  │  Phase    │  │  Visualizers     │  │
│  │  算法选择 │  │  Panel   │  │  Guide    │  │  6 个可视化组件   │  │
│  │  对比开关 │  │  播放控制 │  │  阶段引导  │  │  + 3D VR 组件   │  │
│  └──────────┘  └──────────┘  └───────────┘  └──────────────────┘  │
│                                                                    │
│                 AlgorithmStore (Signal-based State)                 │
│                                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐   │
│  │  Assessment  │  │  History     │  │  AI Complexity         │   │
│  │  Component   │  │  Panel       │  │  Dialog                │   │
│  └──────────────┘  └──────────────┘  └────────────────────────┘   │
└──────────────────────────────┬─────────────────────────────────────┘
                               │ HTTP POST/GET (JSON)
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│                Spring Boot 3.2 (localhost:8080)                     │
│                                                                    │
│  ┌──────────────────┐  ┌──────────────────────────────────────┐   │
│  │  AuthController  │  │  AlgorithmController                  │   │
│  │  /api/auth       │  │  /api/algorithms                      │   │
│  │  register/login  │  │  sort|search|graph|dp|backtracking    │   │
│  │                  │  │  divide-conquer|verify-step|history   │   │
│  └──────────────────┘  └───────────────┬──────────────────────┘   │
│                                        │                           │
│  ┌─────────────────────────────────────┼──────────────────────┐   │
│  │                    Service Layer     │                       │   │
│  │  AuthService  SortingService  SearchService  GraphService  │   │
│  │  DPService  BacktrackingService  DivideConquerService      │   │
│  │  AlgorithmComplexityService                                │   │
│  └─────────────────────────────────────┼──────────────────────┘   │
│                                        │                           │
│  ┌─────────────────────────────────────┼──────────────────────┐   │
│  │              Repository / Entity     │                       │   │
│  │  AppUserRepository  RunHistoryRepository                    │   │
│  │  AppUser (JPA Entity)  RunHistory (JPA Entity)             │   │
│  └─────────────────────────────────────┼──────────────────────┘   │
│                                        │                           │
└────────────────────────────────────────┼───────────────────────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │   MySQL 8.0          │
                              │   algorithm_viz 库   │
                              │   - app_user         │
                              │   - run_history      │
                              └─────────────────────┘
```

### 3.2 架构决策记录（ADR）

#### ADR-1: 单 Controller 多 endpoint 模式

**决策**：所有算法相关端点集中在 `AlgorithmController`，按分类使用不同 endpoint（`/sort`, `/search`, `/graph`, `/dp`, `/backtracking`, `/divide-conquer`），而非每种算法一个端点。

**原因**：
- 同一分类的算法共享相同的输入/输出结构（如所有排序算法都接收 `int[]`，返回 `List<SortStep>`）
- 减少路由注册数量，降低 Controller 层维护成本
- 算法选择通过请求体中的 `algorithm` 字段区分，前端侧边栏直接绑定

#### ADR-2: 信号驱动的状态管理

**决策**：前端使用 Angular Signals 构建自定义 `AlgorithmStore`，不使用 NgRx 等第三方状态管理库。

**原因**：
- Angular 17+ 的 Signals API 已足够成熟，提供细粒度的响应式更新
- 减少依赖，降低打包体积
- `computed()` 天然适合派生状态（如 `currentStepData`、`phaseConfig`、`compareMetrics`）

#### ADR-3: Step 模型使用手动 Builder 模式

**决策**：后端所有 Step 模型类（SortStep、GraphStep 等）使用手动实现 Builder，不引入 Lombok `@Builder`。

**原因**：
- 避免与 Lombok 的过渡耦合（项目初期未引入 Lombok，后续添加但仅用于 `@Data` 等注解）
- 手动 Builder 对外部依赖零要求，Java 17 编译即用
- 每个 Step 字段约 10-15 个，手动维护 Builder 的工作量可控

#### ADR-4: Phase 使用字符串而非枚举

**决策**：教学阶段（phase）在后端 Step 模型和前端接口中统一使用 `String` 类型。

**原因**：
- 不同算法有不同的阶段集合，枚举不适合表达开放集合
- 字符串在 JSON 序列化中零转换成本
- 前端 `PHASE_CONFIG` 常量表负责阶段的中文翻译和顺序定义

#### ADR-5: 对比模式限制同分类

**决策**：对比模式仅允许选择与主算法同一 category 的算法。

**原因**：
- 不同分类共享不同的可视化器（柱状图 vs SVG vs 表格），跨分类对比需要同时渲染两种不同的可视化器
- 同分类限制保持了布局的简洁性（左右对半分割）
- 教学上也有意义：快速排序 vs 归并排序 是排序算法内部效率对比

#### ADR-6: 竞赛采用“服务端权威状态 + WebRTC 实时通道”

**决策**：房间、准备状态、题目、计时、判分和排名由 Spring Boot 服务端统一管理；WebSocket 仅承担 WebRTC 信令交换，DataChannel 用于玩家间的进度通知和聊天。前端同时以 750ms 周期轮询房间状态作为降级机制。

**原因**：
- 判分不能信任点对点客户端，答案和服务器计时必须保留在后端
- WebRTC 适合低延迟的临时消息，但在 NAT、防火墙或 STUN 不可用时可能连接失败
- REST 轮询保证 WebRTC 失败时核心比赛流程仍可使用
- 信令层向双方发送对手存在事件，并以较小的用户 ID 作为唯一 Offer 发起方，避免漏协商和双 Offer 冲突

### 3.3 数据流

```
用户操作                    Store                         HTTP                     Backend
─────────                  ──────                        ──────                   ───────
点击"快速排序"
  ─────────────────►  selectedAlgo.set('quick-sort')
                      category = computed → 'sorting'

点击"运行"
  ─────────────────►  runAlgorithm()
                      isLoading.set(true)
                            ───────────────────────────►  POST /sort
                                                         {algorithm, array}
                                                                              SortingService
                                                                              .generateSteps()
                                                                                 │
                                                                                 ▼
                                                                              返回 200
                                                                     { steps: [...], stepCount,
                                                                       comparisons, extra,
                                                                       executionTimeMs }
                         ◄─────────────────────────────
                      steps.set(response.steps)
                      currentStep.set(0)
                      isLoading.set(false)

点击"▶ 播放"
  ─────────────────►  togglePlay()
                      setInterval → stepForward()
                      currentStep.update(s → s+1)

currentStep 变化
  ─────────────────►  currentStepData (computed)
                          │
                          ▼
                      VisualizerComponent
                      读取 step 数据，更新渲染
                      (柱状图颜色、图节点状态等)

phase 变化检测
  ─────────────────►  currentPhase (computed)
                      phaseConfig (computed)
                          │
                          ▼
                      PhaseGuideComponent
                      更新阶段进度条
```

**1v1 竞赛数据流：**

```text
玩家 A                     Spring Boot / WebSocket                    玩家 B
  │ POST /rooms 创建房间              │                                 │
  │◄──── 房间号 + waiting ────────────│                                 │
  │                                    │◄──── POST /rooms/{id}/join ─────│
  │◄──── WebSocket: join-room ─────────│──── peer-present ───────────────►│
  │                                    │                                 │
  │──── WebRTC Offer ─────────────────►│────────────────────────────────►│
  │◄───────────────────────────────────│◄──────────── WebRTC Answer ─────│
  │◄════════════════ WebRTC DataChannel（进度 / 聊天）══════════════════►│
  │                                    │                                 │
  │──── ready / submit（REST）────────►│◄──────── ready / submit（REST）─│
  │        后端按服务器时间判分、更新玩家进度和最终排名                   │
  │◄──── GET room / result ────────────│──────── GET room / result ─────►│
```

### 3.4 组件树

```
AppComponent
├── AuthComponent                          ← 登录/注册表单
├── Header                                 ← Logo + Tab导航 + 用户头像 + 退出
│   └── Tabs: [可视化 | 1v1竞赛 | 评估测试 | 历史记录 | AI复杂度分析]
├── ErrorBanner                            ← 错误提示横幅
├── SidebarComponent                       ← 算法分类列表
│   ├── 分类标题（排序/搜索/图/DP/回溯/分治/3D数据结构）
│   ├── 算法按钮列表
│   └── ⚖ 对比模式开关 + 对比算法选择器
├── Main Content
│   ├── ControlPanelComponent              ← 播放/暂停/上一步/下一步/速度/进度条
│   ├── InputConfigComponent               ← 输入编辑（数组/图/背包参数等）
│   ├── PhaseGuideComponent                ← 教学阶段进度条
│   ├── CompareMetricsPanel                ← 对比模式指标（条件显示）
│   ├── ComplexityPanelComponent           ← 复杂度信息卡片（非对比模式）
│   ├── Visualizer (7种, 按 category 条件渲染)
│   │   ├── SortingVisualizerComponent     ← 柱状图
│   │   ├── GraphVisualizerComponent       ← SVG 图
│   │   ├── SearchVisualizerComponent      ← 数组 + 搜索指示器
│   │   ├── DpVisualizerComponent          ← 二维 DP 表格
│   │   ├── NQueensVisualizerComponent     ← 棋盘 + 皇后
│   │   ├── DivideConquerVisualizerComponent ← 递归树
│   │   └── Vr3dVisualizerComponent        ← Three.js 3D 渲染
│   ├── HistoryPanelComponent              ← 运行历史列表（Tab 切换）
│   ├── AssessmentComponent                ← 评估测试（Tab 切换）
│   └── CompetitionComponent               ← 1v1 房间、答题、聊天和排名
└── AiComplexityDialogComponent            ← AI 复杂度分析弹窗（条件显示）
```

### 3.5 后端分层设计

```
Controller 层
├── AlgorithmController
│   ├── POST /api/algorithms/sort           → SortingService
│   ├── POST /api/algorithms/search         → SearchService
│   ├── POST /api/algorithms/graph          → GraphService
│   ├── POST /api/algorithms/dp             → DPService
│   ├── POST /api/algorithms/backtracking   → BacktrackingService
│   ├── POST /api/algorithms/divide-conquer → DivideConquerService
│   ├── POST /api/algorithms/algorithm-complexity → AlgorithmComplexityService
│   ├── POST /api/algorithms/verify-step    → 各 Service (按需路由)
│   ├── GET  /api/algorithms/history        → RunHistoryRepository
│   ├── DELETE /api/algorithms/history/{id} → RunHistoryRepository
│   └── GET  /api/algorithms/health
├── AuthController
│   ├── POST /api/auth/register             → AuthService
│   └── POST /api/auth/login                → AuthService
├── CompetitionController
│   ├── POST /api/competition/rooms         → 创建房间
│   ├── POST /rooms/{id}/join|ready|leave   → 房间生命周期
│   ├── POST /rooms/{id}/submit             → 权威判分
│   └── GET  /rooms/{id}|result             → 状态与排名
└── SignalingHandler
    └── WS /ws/signaling                    → WebRTC 信令中继与断线宽限

Service 层
├── SortingService        → 5 种排序算法的步骤生成
├── SearchService         → 二分查找步骤生成
├── GraphService          → 6 种图算法步骤生成 (Dijkstra/BFS/DFS/Prim/Kruskal/A*)
├── DPService             → 0/1 背包 DP 步骤生成
├── BacktrackingService   → N 皇后回溯步骤生成
├── DivideConquerService  → Karatsuba 大整数乘法步骤生成
├── AlgorithmComplexityService → AI 大模型复杂度分析
├── AuthService           → 用户注册/登录 + SHA-256 加盐哈希
└── CompetitionRoomService → 内存房间、题库、准备、计时、判分、排名和过期清理

Model 层
├── SortStep.java         → 排序步骤（array, comparing, swapping, sorted, pivot, phase...）
├── SearchStep.java       → 搜索步骤（array, left, right, mid, found, eliminated, phase...）
├── GraphStep.java        → 图步骤（nodeStates, edgeStates, distances, queue, path, phase...）
├── DPStep.java           → DP 步骤（dp[][], currentItem, currentWeight, decision, phase...）
├── NQueensStep.java      → 回溯步骤（board[], currentRow, conflicts, solutions, phase...）
├── DivideConquerStep.java → 分治步骤（tree, currentNodeId, a/b/c/d, z2/z1/z0, phase...）
├── AlgorithmComplexityAnalysis.java → AI 复杂度分析结果
├── CompetitionRoom.java   → 房间状态、玩家、题目和时间戳
├── CompetitionPlayer.java → 玩家准备、得分、进度、耗时和弃权状态
└── CompetitionQuestion.java → 竞赛题目（不包含答案）

Entity 层
├── AppUser.java          → JPA 实体（id, username, displayName, passwordHash, passwordSalt）
└── RunHistory.java       → JPA 实体（id, category, algorithm, inputData, stepCount, comparisons...）

DTO 层
├── SortRequest.java
├── SearchRequest.java
├── GraphRequest.java     → 含内嵌 GraphData/GraphNodeDto/GraphEdgeDto
├── DPRequest.java        → 含内嵌 KnapsackItemDto
├── BacktrackingRequest.java
├── DivideConquerRequest.java
├── AlgorithmComplexityRequest.java
├── AuthRequest.java
├── AuthResponse.java
├── RegisterRequest.java
├── CreateCompetitionRoomRequest.java
├── JoinCompetitionRoomRequest.java
├── CompetitionSubmitRequest.java
└── CompetitionSubmitResponse.java

Repository 层
├── AppUserRepository.java     → findByUsername, existsByUsername
└── RunHistoryRepository.java  → findByCategoryOrderByCreatedAtDesc, findTop20ByOrderByCreatedAtDesc
```

---

## 4. 实现细节

### 4.1 算法步骤生成机制

每种算法的 Service 遵循统一的"步骤生成"模式：

1. **初始化步骤**：创建初始状态，定义空的计数器（comparisons, swaps, accesses 等），生成第一条描述步骤
2. **算法执行 + 步骤记录**：在算法的每次关键操作后，使用 Builder 构建一个 Step 对象并加入 steps 列表
3. **完成步骤**：生成最终状态步骤，通常包含 `"done"` phase
4. **返回步骤列表**：Controller 将 steps 包装为统一响应格式返回

以快速排序为例：

```
quickSort(input) {
  steps = []
  初始化计数器
  调用 quickSortHelper(arr, 0, n-1, sorted, cmp, swp, acc, steps)
    其中 partition() 在每个关键点构建 SortStep:
      - 选择基准值 (select_pivot)
      - 比较元素 (compare)
      - 交换元素 (swap)
      - 基准值归位 (pivot_placed)
  生成 done 步骤
  返回 steps
}
```

### 4.2 Phase 教学阶段系统

每种算法的 phase 在两端各定义一次：

- **后端**：在 Service 中构建 Step 时标注 `phase("xxx")`，确保每个步骤携带阶段标记
- **前端**：`AlgorithmStore` 中 `PHASE_CONFIG` 常量表定义每种算法的阶段序列和中文标签

```typescript
// 前端配置示例
'quick-sort': {
  sequence: ['select_pivot', 'compare', 'swap', 'pivot_placed', 'done'],
  labels: {
    select_pivot: '选择基准值', compare: '比较分区',
    swap: '交换元素', pivot_placed: '基准值归位', done: '排序完成',
  },
},
```

`PhaseGuideComponent` 读取 `store.currentPhase()` 确定当前阶段索引，渲染阶段进度条。

### 4.3 对比模式实现

对比模式的核心组件：

1. **第二套状态信号**（`compareSteps`, `compareCurrentStep`, `compareIsPlaying` 等）在 `AlgorithmStore` 中独立管理
2. **播放同步**：`togglePlay()` 同时启动/停止两个定时器
3. **双请求**：`runBothAlgorithms()` 同时向后端发送两个请求，前端用 `forkJoin` 或独立的 subscribe 处理
4. **可视化器复用**：每个 Visualizer 新增 `@Input() source: 'primary' | 'compare' = 'primary'`，通过 `source` 决定读取 `store.currentStepData()` 还是 `store.compareCurrentStepData()`
5. **布局切换**：AppComponent 在对比模式下将主内容区从全宽切换为左右 50% 分栏

### 4.4 评估测试系统

评估测试提供**两种模式**，通过设置面板切换。

#### 4.4.1 AI 动态出题模式（默认）

LLM 根据用户配置动态生成题目并语义评测答案。

**出题流程**：
1. 用户在设置面板配置参数（题数 1-15、分类/算法、难度、题型偏好）
2. 前端 `POST /api/algorithms/assessment/generate` → 后端 `AssessmentService.generateQuestions()`
3. `AssessmentService` 构造 Prompt 调用 LLM（DeepSeek-V3.2）生成题目 JSON
4. 对 `state-fill`/`table-fill` 题型进行**交叉验证**：运行实际算法获取 ground truth，与 LLM 答案比对，不一致则修正
5. 返回验证后的题目列表，前端逐题展示

**评测流程**：
1. 用户提交答案 → 前端 `POST /api/algorithms/assessment/evaluate`
2. 后端 `AssessmentService.evaluateAnswer()` 将题目信息+用户答案发送给 LLM
3. LLM 执行**语义等价判断**（理解 `[3,1,2]` 与 `3, 1, 2` 等价），生成个性化反馈
4. 返回 `{ correct, feedback, correctAnswer, confidence }`

**LLM 安全调控**：5 层防护（System Prompt 行为准则 → User Prompt 输出约束 → JSON 提取剥离 Markdown → 字段格式校验 → 内容合规检查），确保不输出无关内容。

**配置方式**：通过操作系统环境变量 `AI_ASSESSMENT_API_KEY` 注入。Spring Boot 原生读取环境变量，无需额外文件或插件。在 IDE 的 Run Configuration 中设置，或启动时 `export AI_ASSESSMENT_API_KEY=你的密钥`。

#### 4.4.2 经典固定题模式（降级方案）

保留原有的 5 道固定题目，在 LLM API 不可用时自动切换。

**题目数据**：定义在 `frontend/src/app/data/test-scenarios.ts` 的 `TEST_SCENARIOS` 数组中。

**判分方式**：
- `value-fill` / `choice` → 前端直接比对，字符串标准化后比较
- `state-fill` / `table-fill` → 调用 `POST /api/algorithms/verify-step` 获取标准答案后比对
- `path-fill` → 标准化空白后比较路径字符串

> **详细实现**：参见 `docs/assessment-llm-implementation.md`

### 4.5 AI 复杂度分析
#### 4.5.1 初版
`AlgorithmComplexityService` 通过 RestClient 调用并行智算云提供的 LLM API（兼容 OpenAI 格式）：

1. 构造 System Prompt："你是算法复杂度分析助手..."
2. 构造 User Prompt：包含用户代码、语言类型、分析场景（最坏/平均/最好）
3. 请求 `response_format: { type: "json_object" }` 确保返回 JSON
4. 解析响应，提取 `timeComplexityWorst/Average/Best`, `spaceComplexity`, `reasoningSteps`, `assumptions`, `optimizationSuggestions`, `confidence`
5. 异常处理：API 不可用或解析失败时返回友好错误信息

#### 4.5.2 第二版

本项目集成了一个基于大语言模型（LLM）的 AI 助手，旨在帮助用户理解算法复杂度、解释代码逻辑并提供优化建议。

#### 1. 设计架构

采用了经典的前后端分离架构，并结合了持久化存储以支持对话历史记录。

- 1.1 核心组件

*   **前端 (Angular)**:
  *   `AiComplexityDialogComponent`: 提供对话框界面，负责用户交互、消息显示及滚动控制。
  *   `ChatService`: 负责与后端对话 API 通讯（会话创建、获取历史、删除会话）。
  *   `AlgorithmService`: 负责触发 AI 分析请求。
*   **后端 (Spring Boot)**:
  *   `AlgorithmComplexityService`: 核心业务逻辑类。负责构建系统级 Prompt、管理上下文、调用外部 LLM API 并解析返回结果。
  *   `ChatService`: 管理对话会话和消息的持久化。
  *   `ChatController`: 暴露 RESTful 接口供前端调用。
*   **存储层 (MySQL/JPA)**:
  *   `ChatSession`: 存储对话会话信息（如标题、创建时间、所属用户）。
  *   `ChatMessage`: 存储每一条具体的对话内容（角色：user/assistant，内容，时间）。

- 1.2 设计流程

1.  **用户输入**: 用户在前端界面输入算法代码或提问。
2.  **上下文加载**: 后端根据 `sessionId` 从数据库加载最近的 10 条历史消息。
3.  **Prompt 构建**: 将系统预设（System Prompt，提示词）、历史上下文和当前用户输入拼接成完整的 Prompt。
4.  **模型调用**: 通过 `RestClient` 调用配置好的 LLM API（DeepSeek）。
5.  **结果保存**: AI 的回答和用户的提问均被保存至 `ChatMessage` 表中，实现持久化。
6.  **结果展示**: 前端接收到 AI 的自然语言回答，并在对话框中以 Markdown 风格渲染（支持代码块）。

####2. 功能

- 算法复杂度分析
> AI 能够识别用户输入的代码段（支持伪代码、C++、Java、Python 等），并从时间复杂度和空间复杂度两个维度进行深度剖析，给出最坏、平均和最好情况的分析。

- 2.2 代码解释与推理
> 除了简单的复杂度计算，AI 还可以解释代码的运行逻辑，进行逐步推理（Step-by-step reasoning），帮助学习者理解算法底层原理。

- 2.3 算法优化建议
> AI 会针对当前代码的性能瓶颈提供改进方案，甚至提供重构后的代码示例。

-  2.4 对话持久化
>**多会话支持**: 用户可以创建多个独立的对话会话，方便分类管理不同的算法问题。  
>**历史回溯**: 即使关闭页面或重新登录，之前的对话记录也会被保存并随时可以查看。  
>**上下文关联**: AI 能够记住同一会话内的前文内容，支持追问和连续对话。 

#### 3.AI助手的功能变化
- 3.1 before
- 仅支持最基本的复杂度分析功能（预设了prompt和数据显示格式，故只会显示固定数据）
- 自由度低  


- 3.2 after
- 提示词更改，不再限制回答格式（为保证相关性，提示词仍然内置了算法助手等信息。） 
- 自由度更高，响应速度更快
- 对话历史持久化，用户可以随时创建新的对话和切换历史对话

### 4.6 用户认证系统

- **密码安全**：SHA-256(salt + ":" + password)，salt 为 `SecureRandom` 生成的 16 字节随机数
- **密码比较**：使用 `MessageDigest.isEqual()` 进行常量时间比较，防止时序攻击
- **会话管理**：前端 `AuthStore` 基于 Signals 管理状态，含三个核心信号：`currentUser`（当前用户）、`error`（错误消息）、`success`（成功提示）。登录状态持久化至 `localStorage` 中（键名 `algorithm-viz-session`），不依赖后端 Token
- **简单设计**：无 JWT、无 Session Cookie，适合教学/学习场景
- **注册后行为**：注册成功后**不自动登录**——而是通过 `onSuccess` 回调切换回登录页面，自动填入已注册用户名，并显示 "注册成功，请登录。" 绿色提示。这避免了注册即登录可能带来的会话混淆问题
- **反馈管理**：`clearFeedback()` 方法同时清除 `error` 和 `success` 两个信号，在模式切换（登录↔注册）和登出时调用
- **登录页 UI**：采用全屏背景图 + 渐变遮罩层设计。左侧展示平台标题，右侧为毛玻璃效果（`backdrop-blur`）的登录/注册卡片。背景图位于 `frontend/src/assets/login-bg.png`（约 1.7MB），通过 CSS `absolute inset-0` + `object-cover` 实现全屏覆盖，叠加多层半透明渐变（`bg-slate-950/55` + `bg-gradient-to-r`）保证表单区域的可读性

### 4.7 3D 数据结构可视化

采用分层架构，将**静态渲染**和**交互动画**解耦为独立层：

```
Vr3dVisualizerComponent (Angular 容器 — 管理 Three.js 场景生命周期)
  │
  ├── 渲染层 (Renderers/)               ├── 动画层 (Animators/)
  │   ├── structure-renderer.types.ts   │   ├── structure-animator.interface.ts
  │   ├── three-object-factory.ts       │   ├── basic-structure.animator.ts
  │   ├── array.renderer.ts             │   └── b-plus-tree.animator.ts
  │   ├── stack.renderer.ts             │
  │   ├── queue.renderer.ts             ├── 数据层 (Data/)
  │   ├── linked-list.renderer.ts       │   └── structure-info.ts
  │   ├── binary-tree.renderer.ts       │
  │   └── b-plus-tree.renderer.ts       │
  └─────────────────────────────────────┘
```

**渲染层**：每个 Renderer 是静态类，暴露 `render(ctx: StructureRendererContext): void` 方法。调用 `ThreeObjectFactory`（统一工厂，生产立方体、球体、连接线、箭头等 3D 基元）将数据结构的逻辑状态转化为 Three.js `Object3D` 节点树，挂载到场景中。

**动画层**：通过 `StructureAnimator` 接口与渲染层解耦。`BasicStructureAnimator` 覆盖数组、栈、队列、链表、二叉树五种线性/树形结构的操作动画（访问、插入、删除、搜索、遍历），`BPlusTreeAnimator` 独立处理 B+ 树的查找路径高亮、叶子分裂、借位合并和范围扫描动画。两者均通过 `AnimationContext` 获取场景引用、当前数据和状态更新回调，不直接依赖任何 Renderer。

**鼠标交互**：通过 Three.js `OrbitControls` 统一处理旋转、缩放，不在单个 Renderer 或 Animator 中重复实现。

**最近改进**（commit `37dc01c` + `c5dffb7`）：
- **B+ 树动画器大幅扩展**：`b-plus-tree.animator.ts` 经历 820 行重构，支持查找（高亮路径）、插入（叶子分裂动画）、删除（借位/合并）、范围查询（链表顺序扫描）等完整操作演示
- **二叉树生成修复**：修复了随机生成时可能产生非法 BST 的问题。`AlgorithmStore.randomVr3dData()` 改为先生成不重复的数值集合（`new Set`），再通过 `toBinarySearchTreeValues()` 转换为符合 BST 插入顺序的值序列，确保渲染的二叉树结构正确
- **VR-3D 可视化器重构**：`vr-3d-visualizer.component.ts` 经历 577 行重构，改善了组件结构、操作卡片交互和 Three.js 场景生命周期管理

### 4.8 WebRTC 算法 1v1 竞赛

#### 4.8.1 房间状态机

```text
waiting ──第二名玩家加入──► ready-check ──双方准备──► playing ──双方完成/一方退出──► finished
   ▲                                │
   └──── 等待阶段玩家退出后重置 ─────┘
```

- 房间号为随机生成的 6 位大写字符串，最多两名玩家
- `waiting` 和 `ready-check` 阶段退出会释放席位；`playing` 阶段退出会被标记为 `forfeited`
- 等待房间空闲 30 分钟、比赛中房间空闲 2 小时、已结束房间空闲 1 小时后自动清理
- 浏览器刷新时，前端通过 `sessionStorage` 保存的房间号恢复比赛；WebSocket 断线提供 15 秒重连宽限

#### 4.8.2 题目与公平性

每种算法在服务端生成 3 道题：复杂度判断、状态推演和应用场景选择。题目对象不保存答案，答案表由 `CompetitionRoomService` 单独维护。

- 开赛前公开房间视图只返回 `questionCount`，`questions` 为空，避免提前泄题
- 后端强制按 `q1 → q2 → q3` 顺序提交，并对重复提交返回原判定但不重复加分
- 客户端不提交计时字段；真实耗时由服务端根据每题开始时间计算
- 最终排名依次比较：是否弃权、总分、答对数、总耗时

#### 4.8.3 得分规则

| 题目 | 基础分 |
|------|-------:|
| 第 1 题：复杂度判断 | 100 |
| 第 2 题：状态推演 | 120 |
| 第 3 题：应用场景 | 100 |

正确答案获得基础分，并附加速度奖励：

```text
速度奖励 = max(0, 20 - floor(服务端耗时毫秒 / 5000))
单题得分 = 回答正确 ? 基础分 + 速度奖励 : 0
```

基础满分为 320 分，每题速度奖励最高 20 分，因此理论最高分为 **380 分**。超过 300 分属于正常结果，并非重复计分。

#### 4.8.4 WebRTC 与降级策略

1. 两名玩家通过 `/ws/signaling` 交换 Offer、Answer 和 ICE Candidate
2. 信令服务同时向房主发送 `join-room`、向加入者发送 `peer-present`
3. 用户 ID 较小的一方作为唯一 Offer 发起者，建立 `competition` DataChannel
4. DataChannel 传输 `ready`、`progress`、`submitted` 和 `chat` 消息
5. 前端每 750ms 获取一次权威房间状态；WebRTC 失败仅影响即时聊天，不影响答题、判分和结算
6. 默认 STUN 为 Cloudflare 与 Google；跨复杂 NAT 的生产环境应配置 TURN 服务

### 4.9 统一响应格式

所有算法执行类 POST 端点返回统一 JSON 结构；认证、评估和竞赛端点使用各自的业务响应 DTO：

```json
{
  "steps": [...],           // 步骤列表（类型随分类不同）
  "stepCount": 45,          // 步骤总数
  "comparisons": 120,       // 比较次数（核心指标）
  "extra": 15,              // 附加操作次数（排序→swaps, 回溯→backtracks, 分治→additions）
  "executionTimeMs": 12     // 后端计算耗时（毫秒）
}
```

### 4.10 错误处理与用户反馈

| 层级 | 策略 |
|------|------|
| 前端 HTTP | `AlgorithmService` 每个方法的 `.subscribe()` 均提供 error handler，设置 `store.error` 为中文提示 |
| 前端校验 | 输入参数（N 皇后 1-12、背包容量>0 等）在 InputConfig 组件中限制范围；认证表单在 `AuthStore.register()` 中校验密码一致性 |
| 前端正向反馈 | `AuthStore` 的 `success` 信号用于注册成功后的绿色提示条，与红色的 `error` 信号并列，形成完整的双通道用户反馈 |
| 后端校验 | DTO 使用 `jakarta.validation` 注解（`@NotNull`, `@NotEmpty`, `@Positive`, `@Min/@Max`） |
| 后端异常 | Service 层参数错误返回 400；状态冲突返回 409；竞赛与认证接口返回中文错误信息 |
| CORS | REST 与 WebSocket 共享可配置的 `app.cors.allowed-origin` 来源限制 |
| 竞赛连接 | WebSocket 信令自动重连；忽略旧 Socket/Peer/DataChannel 的迟到关闭事件；12 秒未建立 DataChannel 时显示明确降级提示 |

---

### 4.11 遇到的问题与改进

#### 问题 1：注册后自动登录的会话安全问题

**现象**（commit `165e821` 之前）：
用户注册成功后，后端直接返回用户信息，前端直接调用 `setSession(user)` 完成自动登录。用户无需输入密码即可进入主界面。

**问题分析**：
1. **绕过显式认证**：注册和登录是两个不同的安全边界。自动登录意味着注册端点实际上充当了"免密码登录"的入口。如果注册和登录的密码校验逻辑不一致（例如未来引入邮箱验证），自动登录将成为安全漏洞
2. **密码记忆确认缺失**：用户可能在注册时误输入密码（即使有 `confirmPassword` 校验，两次输入可能犯同样的错误）。强制重新登录可以让用户确认"我记得我设置的密码"
3. **注册反馈不明确**：旧流程下用户注册完直接进入主界面，无法区分"注册成功"和"登录成功"两个事件。用户可能不知道自己已经处于已登录状态还是仍在注册流程中
4. **与业界实践的偏差**：主流 Web 应用（GitHub、Google、大部分 SaaS 产品）在注册后通常要求验证邮箱或重新登录，而非直接创建会话

**改进方案**：
- 前端 `AuthStore.register()` 不再在注册成功时调用 `setSession()`，而是通过 `onSuccess` 回调通知 `AuthComponent` 执行以下切换：
  ```typescript
  // auth.component.ts
  submitRegister(): void {
    this.auth.register(..., () => {
      this.loginUsername = this.registerUsername.trim();  // 预填用户名
      this.loginPassword = '';                              // 清空密码，要求手动输入
      this.registerPassword = '';
      this.registerConfirmPassword = '';
      this.mode = 'login';                                  // 切换到登录页
    });
  }
  ```
- 新增 `AuthStore.success` 信号，独立于 `error`，用于正向反馈消息（绿色提示条）
- `clearError()` 重命名为 `clearFeedback()`，语义上覆盖清除错误和成功两种状态
- 登录页模板顶部新增条件渲染的成功提示区域：
  ```html
  <div *ngIf="auth.success()" class="...text-emerald-300...">
    {{ auth.success() }}
  </div>
  ```

**效果**：注册 → 成功提示 → 手动输入密码登录，形成清晰的 "注册 → 认证" 两步流程。用户知道自己的账号已创建，且密码是自己记住的。

#### 问题 2：API Key 和数据库密码硬编码

**现象**（commit `477cffb` 之前）：
`application.properties` 中直接写入了并行智算云的 API Key 和明文数据库密码。

**问题分析**：
1. 代码提交到公开 GitHub 仓库时，API Key 和密码会泄露
2. 环境切换（开发/测试/生产）时需要手动修改配置文件，容易遗漏
3. 不符合 [12-Factor App](https://12factor.net/config) 的配置管理原则

**改进方案**：
- 数据库密码：已有 `DB_PASSWORD` 环境变量支持（commit `eff8979`），配置文件中设为空字符串，运行时从环境变量读取
- API Key：同样置空，部署时通过环境变量注入
- 在 `.gitignore` 中确认 `application.properties` 不包含敏感信息

#### 问题 3：前后端端口硬编码导致部署兼容性问题

**现象**：
在 Docker 容器化部署到 AWS ECS 时，前端开发环境中的 `apiUrl: 'http://localhost:8080/api'` 和后端 `application.properties` 中的 `server.port=8080` 均为硬编码值，与容器内部网络和 Nginx 反向代理配置不一致。

**问题分析**：
1. **前端**：开发环境 `environment.ts` 将 API 地址硬编码为 `http://localhost:8080/api`。Docker 部署时前端静态资源由 Nginx 容器承载，API 请求应通过 Nginx 反向代理转发（如 `/api` → `backend:8080`），而非直接请求 `localhost:8080`
2. **后端**：`server.port=8080` 在容器内仍然有效（容器内部端口），但开发环境和生产环境使用相同配置缺乏区分度
3. **Nginx 代理**：需要 `nginx.conf` 正确配置 `proxy_pass http://backend:8080/api`（容器间通过 Docker 网络通信），如果后端端口配置与 Nginx 配置不一致，将导致 502 错误

**改进方案**：
- **前端**：通过 Angular 的 `file replacement` 机制在 `angular.json` 中配置环境切换：
  ```json
  // angular.json 中的配置
  "production": {
    "fileReplacements": [
      { "replace": "src/environments/environment.ts", "with": "src/environments/environment.prod.ts" }
    ]
  }
  ```
  生产环境 `environment.prod.ts` 使用相对路径 `apiUrl: '/api'`，由 Nginx 同源代理，消除跨域和端口依赖
- **后端**：`server.port` 保持默认 8080（容器内足够），通过 Docker Compose 的 `ports` 映射或 Nginx 的 `proxy_pass` 暴露服务
- **Docker 网络**：容器间通过服务名通信（`backend:8080`），不依赖宿主机 IP 或端口

**效果**：生产构建 `npm run build --configuration production` 自动使用相对路径，部署到任何域名/端口均可正常工作，不再需要修改代码。

---

## 5. 部署与运行

### 5.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 编译和运行 Spring Boot 3.2 |
| Maven | 3.8+ | 后端构建和依赖管理 |
| Node.js | 18+ | 前端构建和 Angular CLI |
| npm | 9+ | 前端依赖管理 |
| MySQL | 8.0+ | 持久化用户数据和运行历史 |
| Angular CLI | 17.x | 前端开发服务器和构建 |

### 5.2 数据库初始化

```bash
# 1. 启动 MySQL 服务
# 2. 创建数据库
mysql -u root -e "CREATE DATABASE IF NOT EXISTS algorithm_viz CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

JPA 的 `ddl-auto=update` 会在应用启动时自动创建/更新数据表（`app_user`, `run_history`），无需手动执行 SQL 脚本。

### 5.3 后端配置

保留 `backend/src/main/resources/application.properties` 中的环境变量占位符，并在运行环境中设置：

```bash
# 必填：数据库密码
export DB_PASSWORD=你的MySQL密码

# 可选：AI API Key（不配置则 AI 复杂度分析功能不可用）
ai.complexity.api-key=你的API密钥

### 5.4 后端启动

```bash
cd backend
mvn spring-boot:run
# 启动日志中看到：Tomcat started on port 8080
# 验证：curl http://localhost:8080/api/algorithms/health
```

### 5.5 前端配置

`frontend/src/environments/environment.ts`（开发环境）：

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
};
```

Angular CLI 开发服务器默认通过 `proxy.conf.json` 或将 `/api` 请求代理到 `localhost:8080`。注意：当前配置中前端直接请求完整 URL，不依赖代理。

### 5.6 前端启动

```bash
cd frontend
npm install        # 首次运行需安装依赖
npm start          # 启动于 http://localhost:4200
```

### 5.7 生产构建

```bash
# 后端
cd backend
mvn clean package -DskipTests
# 产物：backend/target/algorithm-viz-backend-1.0.0.jar
java -jar target/algorithm-viz-backend-1.0.0.jar &

# 前端
cd frontend
npm run build
# 产物：frontend/dist/algorithm-viz-frontend/
# 部署到 Nginx 或任何静态文件服务器
```

### 5.8 Docker 部署（推荐）
#### 5.8.1 本地源代码部署
```bash
# 在项目根目录创建 docker-compose.yml:
```

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      # 需要改为对应mysql密码
      MYSQL_ROOT_PASSWORD: 
      MYSQL_DATABASE: algorithm_viz
    ports:
      - "3306:3306"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/algorithm_viz?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      # 对应密码
      SPRING_DATASOURCE_PASSWORD: 
    depends_on:
      - mysql

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```
#### 5.8.2 部署到远程服务器（AWS ECS / 公有云）

**目标环境**：AWS ECS 服务器 (Ubuntu) + Docker 容器 + Nginx 反向代理

**总体架构**：
```text
用户浏览器 → http://3.212.58.111 → 宿主机（云服务器）
                                      ├── Nginx 容器（:80）→ 反向代理 + 承载前端静态资源
                                      ├── 后端 API 容器（:8080，仅容器内网可达）
                                      └── MySQL 数据库容器（:3306，仅容器内网可达）
```

**部署的 `deploy/` 目录文件结构**：
```bash
deploy/
├── backend/
│   ├── Dockerfile          # 后端镜像构建文件
│   └── app.jar             # Maven 构建产物
├── docker-compose.yml      # 三容器编排
└── nginx/
    ├── dist/               # Angular ng build 产物
    │   └── browser/
    │       ├── index.html
    │       ├── main-*.js
    │       ├── polyfills-*.js
    │       └── styles-*.css
    └── nginx.conf           # Nginx 反向代理配置
```

**部署步骤**：

**第一步：前后端打包**
```shell
# 前端 — 必须使用 production 配置（apiUrl='/api'，Nginx 同源代理）
cd frontend/
npm install
ng build --configuration production

# 后端 — 跳过测试，打包为可执行 JAR
cd backend/
mvn clean package -DskipTests

# 将 deploy 目录上传至云服务器
scp -r deploy ubuntu@3.212.58.111:/home/ubuntu/
```

**第二步：服务器环境准备**
- 安装 Docker Engine 和 Docker Compose
- 安装 MySQL（或使用 Docker 容器化 MySQL）
- 为非 root 用户授予 Docker 操作权限：`sudo usermod -aG docker $USER`

**第三步：Docker 构建与启动**
```shell
cd /home/ubuntu/deploy/

# 首次构建并启动（--build 强制重新构建镜像）
docker compose up -d --build

# 查看容器状态（所有容器应为 Up）
docker compose ps

# 日常运维命令
docker compose stop              # 停止所有容器
docker compose start             # 启动已停止的容器
docker compose restart           # 重启所有容器
docker compose restart 容器名    # 重启单个容器
docker compose restart backend   # 例如：仅重启后端容器

# 配置较大变动时（如 nginx.conf 或环境变量修改）
docker compose up -d --force-recreate

# 完全移除所有容器（不删除数据卷）
docker compose down
```

**第四步：验证部署**
```bash
# 公网访问
curl http://3.212.58.111
# 应返回 Angular 应用的 index.html

# 后端健康检查（通过 Nginx 代理）
curl http://3.212.58.111/api/algorithms/health
# 返回: {“status”:”ok”,”service”:”Algorithm Viz Backend”}
```

**当前部署状态**：已完成，运行在 **http://3.212.58.111** 。
### 5.9 健康检查

```bash
# 后端
curl http://localhost:8080/api/algorithms/health
# 返回: {"status":"ok","service":"Algorithm Viz Backend"}

# 前端
curl http://localhost:4200
# 返回: HTML 页面内容

# 端到端测试
curl -X POST http://localhost:8080/api/algorithms/sort \
  -H "Content-Type: application/json" \
  -d '{"algorithm":"quick-sort","array":[64,34,25,12,22,11,90]}'
# 应返回包含 steps、stepCount 等字段的 JSON
```

### 5.10 WebRTC 生产部署注意事项

Nginx 除了代理 `/api`，还必须为信令端点启用 WebSocket Upgrade：

```nginx
location /ws/signaling {
    proxy_pass http://backend:8080/ws/signaling;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
}
```

后端通过 `app.cors.allowed-origin` 同时限制 REST 和 WebSocket 来源。生产环境应设置为真实前端域名，例如：

```properties
app.cors.allowed-origin=https://example.com
```

前端默认配置 Cloudflare 和 Google STUN。STUN 不能覆盖所有 NAT、防火墙和企业网络场景；公网部署若要求稳定的点对点连接，应在 `environment.prod.ts` 的 `rtcIceServers` 中加入自建或托管 TURN：

```typescript
rtcIceServers: [
  { urls: 'stun:stun.cloudflare.com:3478' },
  { urls: 'turn:turn.example.com:3478', username: 'user', credential: 'secret' },
]
```

TURN 凭据不应长期硬编码在公开前端仓库中，正式系统宜通过短期凭据服务动态下发。

---

## 6. 使用指南

### 6.1 首次使用

1. 浏览器打开 `http://localhost:4200`
2. 在认证页面点击"没有账号？立即注册"，注册账号（用户名 3-50 字符，显示名可选，密码至少 6 字符）
3. 注册成功后，页面自动切换回登录界面，用户名已预填，顶部显示绿色提示"注册成功，请登录。"
4. 输入密码登录后进入主界面

### 6.2 基本操作：运行算法

1. 在左侧边栏选择算法分类（排序/搜索/图/DP/回溯/分治/3D数据结构）
2. 点击具体算法名称（如"快速排序"）
3. 在 InputConfig 区域查看/编辑输入数据
4. 点击"▶ 运行"按钮
5. 观察可视化区域的变化

### 6.3 播放控制

| 控件 | 功能 |
|------|------|
| ◀◀ | 回到第一步 |
| ▶ | 前进一步 |
| ◀ | 后退一步 |
| ▶▶ | 自动播放（连续前进） |
| 速度选择器 | 调整播放速度（200ms / 500ms / 1000ms / 2000ms） |
| 进度条 | 点击跳转到任意步骤 |

### 6.4 教学阶段引导

算法运行后，ControlPanel 下方会显示教学阶段进度条：
- **已完成阶段**：绿色圆点 + ✓ 标记
- **当前阶段**：蓝色圆点 + 光晕高亮
- **未到达阶段**：灰色圆点 + 序号

底部显示当前阶段的中文名称，帮助理解"算法现在在做什么"。

### 6.5 对比模式

1. 选择一个算法（如"快速排序"）
2. 在侧边栏底部点击"⚖ 对比模式"开关
3. 在展开的对比算法列表中选择要对比的算法（如"归并排序"）
4. 点击 ControlPanel 中的"运行"按钮（此时按钮变为"运行对比"）
5. 界面分为左右两栏，同时展示两个算法的执行过程
6. 点击播放，两个可视化器同步推进
7. PhaseGuide 下方显示对比指标面板，绿色/红色标记表示优劣

**注意**：只能对比同一分类的算法（如排序类之间、图算法之间）。

### 6.6 输入数据编辑

点击 InputConfig 区域可编辑算法输入：

| 分类 | 可编辑内容 |
|------|-----------|
| 排序 | 数组元素（逗号分隔，如 `64,34,25,12,22,11,90`） |
| 搜索 | 有序数组 + 目标值 |
| 图 | 节点位置/标签 + 边和权重（文本编辑） |
| DP | 物品名称/重量/价值 + 背包容量 |
| 回溯 | 皇后数量 N（1-12） |
| 分治 | 两个乘数（大整数字符串，如 `12345678` × `87654321`） |
| 3D 结构 | 数据结构类型 + 元素值 |

### 6.7 评估测试

评估测试提供**两种模式**：

#### AI 动态出题模式（需配置 API Key）

1. 点击顶部导航栏"📝 评估测试" Tab，进入设置页面
2. 配置参数：题目数量（1-15）、难度等级、覆盖分类、题型偏好
3. 选择"AI 动态出题"模式，点击"开始 AI 出题"
4. 等待 5-15 秒题目生成
5. 逐题作答，每题点击"✓ 提交答案"后由 AI 评测（2-5 秒）
6. 查看 AI 生成的个性化反馈和解析
7. 完成全部题目后显示总分卡片，点击"返回设置"可重新测试

#### 经典固定题模式（无需 API）

1. 点击顶部导航栏"📝 评估测试" Tab，进入设置页面
2. 选择"经典固定题"模式，点击"开始答题"
3. 逐题作答（共 5 道预设题目），系统自动判分
4. 完成全部 5 题后显示总分卡片，点击"返回设置"

**预设 5 道测试题概览**：

| # | 标题 | 分类 | 核心考察点 |
|---|------|------|-----------|
| 1 | 快速排序分区过程 | sorting | partition 操作的基准值归位后数组状态 |
| 2 | 归并排序合并过程 | sorting | 分治策略中第一次合并后的数组状态 |
| 3 | 二分查找过程分析 | search | mid 计算和边界收缩逻辑 |
| 4 | Dijkstra 最短路径 | graph | 贪心策略下的最短路径手动计算 |
| 5 | 0/1 背包 DP 计算 | dp | DP 状态转移和 dp[i][w] 值推算 |

> **注意**：AI 动态出题需要配置 `AI_ASSESSMENT_API_KEY` 环境变量，详见 `docs/assessment-llm-implementation.md`。

### 6.8 AI 复杂度分析

1. 点击顶部导航栏"🤖 AI复杂度分析"按钮
2. 在弹出的对话框中粘贴算法代码
3. 选择编程语言和分析场景（最坏/平均/最好情况）
4. 点击"分析"
5. 等待 AI 返回结果（约 5-15 秒）
6. 查看分析结果：时间/空间复杂度、推理步骤、假设前提、优化建议、置信度

**注意**：需要配置 `ai.complexity.api-key`（并行智算云 API Key）才能使用此功能。

### 6.9 3D 数据结构可视化

1. 在侧边栏选择"3D数据结构"分类
2. 选择具体结构类型：数组、栈、队列、链表、二叉树、B+ 树
3. 点击"随机生成数据"可生成新的测试数据
4. 3D 渲染区域支持鼠标旋转和缩放

### 6.10 历史记录

1. 点击顶部导航栏"📋 历史记录" Tab
2. 查看所有历史运行记录（默认最近 20 条）
3. 使用分类下拉框筛选特定分类
4. 点击删除按钮移除单条记录

### 6.11 算法 1v1 竞赛

1. 点击顶部“1v1 竞赛”进入竞赛大厅
2. 从“竞赛算法”下拉框按分类选择算法
3. 一名玩家点击“创建房间”，复制 6 位房间号并发送给另一名已登录用户
4. 对手输入房间号加入；房主通常在 750ms 内看到玩家列表更新
5. 双方点击“准备”，比赛自动进入 `playing` 状态并同时下发 3 道题
6. 每题选择答案并提交；提交后显示正确答案、本题得分和累计进度
7. 双方完成后显示排名、得分、答对数和服务端统计耗时，可点击“再来一局”

**连接状态说明：**

| 显示状态 | 含义 |
|---------|------|
| 信令已连接，等待对手 | WebSocket 正常，尚无第二名玩家 |
| 已发现对手，正在协商连接 | 双方正在交换 Offer/Answer/ICE Candidate |
| 实时连接正常 | WebRTC DataChannel 已打开，可使用即时进度和聊天 |
| 点对点连接失败（答题不受影响） | STUN/TURN 或网络限制导致 P2P 失败，系统自动使用 REST 轮询维持核心比赛 |

**使用限制：**
- 两个参赛窗口必须登录不同账号；同一账号不会占用两个玩家席位
- 比赛中离开会被判负；页面刷新可在 15 秒重连宽限内恢复
- 当前房间保存在后端内存中，后端重启后未结束房间会失效

---

## 7. API 参考

### 7.1 算法执行端点

所有算法执行端点共享统一的**请求头**和**响应格式**。

#### POST /api/algorithms/sort

运行排序算法。

**请求体**：
```json
{
  "algorithm": "quick-sort",
  "array": [64, 34, 25, 12, 22, 11, 90]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | quick-sort / merge-sort / bubble-sort / heap-sort / insertion-sort |
| array | number[] | 是 | 待排序数组 |

**响应**：
```json
{
  "steps": [
    {
      "array": [64, 34, 25, 12, 22, 11, 90],
      "comparing": [0, 6],
      "swapping": [],
      "sorted": [],
      "pivot": 6,
      "rangeLeft": 0,
      "rangeRight": 6,
      "description": "选择基准值 arr[6] = 90，开始分区 [0, 6]",
      "codeLine": 1,
      "comparisons": 0,
      "swaps": 0,
      "accesses": 1,
      "phase": "select_pivot"
    }
    // ... 更多步骤
  ],
  "stepCount": 45,
  "comparisons": 24,
  "extra": 12,
  "executionTimeMs": 8
}
```

#### POST /api/algorithms/search

运行搜索算法。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | binary-search |
| array | number[] | 是 | 有序数组 |
| target | number | 是 | 目标值 |

#### POST /api/algorithms/graph

运行图算法。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | dijkstra / bfs / dfs / prim / kruskal / astar |
| graph | GraphData | 是 | 图结构（节点列表 + 边列表 + 方向/权重标志） |
| startId | string | 否 | 起点节点 ID（Dijkstra/BFS/DFS/A\* 必填） |
| endId | string | 否 | 终点节点 ID（Dijkstra/BFS/DFS/A\* 必填） |

**GraphData 结构**：
```json
{
  "directed": false,
  "weighted": true,
  "nodes": [
    { "id": "A", "x": 150, "y": 80, "label": "A" },
    { "id": "B", "x": 300, "y": 50, "label": "B" }
  ],
  "edges": [
    { "from": "A", "to": "B", "weight": 4 }
  ]
}
```

#### POST /api/algorithms/dp

运行动态规划算法。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | knapsack |
| items | KnapsackItem[] | 是 | 物品列表（name, weight, value） |
| capacity | number | 是 | 背包容量（>0） |

#### POST /api/algorithms/backtracking

运行回溯算法。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | n-queens |
| n | number | 是 | 皇后数量（1-12） |

#### POST /api/algorithms/divide-conquer

运行分治算法。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algorithm | string | 是 | karatsuba |
| x | string | 是 | 第一个乘数（数字字符串） |
| y | string | 是 | 第二个乘数（数字字符串） |

### 7.2 验证端点

#### POST /api/algorithms/verify-step

获取算法在指定步骤的状态数据（用于评估测试的自动判分）。

**请求体**：
```json
{
  "algorithm": "quick-sort",
  "params": { "array": [8, 3, 1, 6, 2, 5] },
  "targetStepIndex": 10
}
```

**响应**：
```json
{
  "algorithm": "quick-sort",
  "category": "sorting",
  "targetStepIndex": 10,
  "stepData": {
    "array": [3, 1, 2, 5, 8, 6],
    "comparing": [],
    "swapping": [],
    "sorted": [2],
    "pivot": 2,
    "phase": "pivot_placed"
  }
}
```

### 7.3 评估测试端点（LLM 增强）

#### POST /api/algorithms/assessment/generate

使用 LLM 动态生成算法测试题。

**请求体**：
```json
{
  "questionCount": 5,
  "categories": ["sorting", "search"],
  "algorithms": ["quick-sort", "binary-search"],
  "difficulty": "medium",
  "mode": "ai",
  "questionTypes": ["fill", "choice"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| questionCount | int | 否 | 题目数量（1-15），默认 5 |
| categories | string[] | 否 | 覆盖分类，空=全部 |
| algorithms | string[] | 否 | 指定算法列表，空=由分类决定 |
| difficulty | string | 否 | easy / medium / hard / mixed，默认 medium |
| mode | string | 否 | "ai"（LLM出题）或 "fixed"（固定题） |
| questionTypes | string[] | 否 | fill / choice / short-answer，空=全部 |

**响应**（200 OK）：
```json
[
  {
    "id": 1,
    "title": "快速排序分区过程",
    "category": "sorting",
    "algorithm": "quick-sort",
    "questionType": "state-fill",
    "description": "对数组 [8, 3, 1, 6, 2, 5] 执行快速排序...",
    "inputParams": { "array": [8, 3, 1, 6, 2, 5] },
    "answer": { "array": [3, 1, 2, 5, 8, 6] },
    "options": null,
    "explanation": "以 5 为基准，比 5 小的移到左边...",
    "targetStepIndex": 10,
    "verifyField": "array",
    "validated": true
  }
]
```

**错误**（503）：`{"error": "AI 评估服务未配置 API Key，请使用固定题目模式"}`

#### POST /api/algorithms/assessment/evaluate

使用 LLM 评测用户答案。

**请求体**：
```json
{
  "question": { ... },
  "userAnswer": "[3, 1, 2, 5, 8, 6]"
}
```

**响应**（200 OK）：
```json
{
  "correct": true,
  "feedback": "回答正确！数组状态 [3,1,2,5,8,6] 与标准答案一致...",
  "correctAnswer": "第一次分区后数组为 [3, 1, 2, 5, 8, 6]",
  "confidence": 0.95
}
```

### 7.4 AI 分析端点

#### POST /api/algorithms/algorithm-complexity

使用 AI 分析算法复杂度。

**请求体**：
```json
{
  "code": "for (int i = 0; i < n; i++) { ... }",
  "language": "java",
  "caseType": "worst"
}
```

**响应**：
```json
{
  "timeComplexityWorst": "O(n²)",
  "timeComplexityAverage": "O(n log n)",
  "timeComplexityBest": "O(n)",
  "spaceComplexity": "O(1)",
  "reasoningSteps": ["外层循环 n 次...", "内层循环 n-1, n-2, ..."],
  "assumptions": ["假设输入数据随机分布"],
  "optimizationSuggestions": ["考虑使用堆排序优化..."],
  "confidence": 0.85
}
```

### 7.5 历史记录端点

#### GET /api/algorithms/history

获取运行历史。可选 `?category=sorting` 筛选。

**响应**：
```json
[
  {
    "id": 1,
    "category": "sorting",
    "algorithm": "quick-sort",
    "inputData": "{\"algorithm\":\"quick-sort\",\"array\":[64,34,...]}",
    "stepCount": 45,
    "comparisons": 24,
    "swaps": 12,
    "executionTimeMs": 8,
    "createdAt": "2026-06-06T10:30:00"
  }
]
```

#### DELETE /api/algorithms/history/{id}

删除单条历史记录。返回 204 No Content。

### 7.6 健康检查

#### GET /api/algorithms/health

```json
{ "status": "ok", "service": "Algorithm Viz Backend" }
```

### 7.7 认证端点

#### POST /api/auth/register

```json
// 请求
{ "username": "alice", "displayName": "Alice", "password": "123456" }

// 响应 (201 Created)
{ "id": 1, "username": "alice", "displayName": "Alice" }
```

> **注意**：注册成功后前端**不会自动登录**。`AuthStore.register()` 收到 201 响应后，通过 `onSuccess` 回调将页面切换回登录模式，用户名自动填入，并显示绿色提示 "注册成功，请登录。"。用户需要手动输入密码完成登录。

#### POST /api/auth/login

```json
// 请求
{ "username": "alice", "password": "123456" }

// 响应 (200 OK)
{ "id": 1, "username": "alice", "displayName": "Alice" }
```

### 7.8 1v1 竞赛端点

#### 房间生命周期

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/competition/rooms` | 创建房间 |
| POST | `/api/competition/rooms/{roomId}/join` | 加入房间 |
| POST | `/api/competition/rooms/{roomId}/ready` | 标记玩家准备 |
| GET | `/api/competition/rooms/{roomId}` | 获取公开房间状态 |
| POST | `/api/competition/rooms/{roomId}/leave` | 离开房间；比赛中离开将判负 |
| POST | `/api/competition/rooms/{roomId}/submit` | 提交当前题答案 |
| GET | `/api/competition/rooms/{roomId}/result` | 获取已结束比赛的排名 |

**创建房间：**

```json
// POST /api/competition/rooms
{
  "algorithm": "quick-sort",
  "userId": 1,
  "displayName": "Alice"
}
```

等待阶段响应中的 `questions` 为空，仅公开题目数量：

```json
{
  "roomId": "A1B2C3",
  "algorithm": "quick-sort",
  "status": "waiting",
  "questionCount": 3,
  "players": [
    { "userId": 1, "displayName": "Alice", "ready": false, "score": 0 }
  ],
  "questions": []
}
```

**提交答案：**

```json
// POST /api/competition/rooms/A1B2C3/submit
{
  "userId": 1,
  "questionId": "quick-sort-q1",
  "answer": "O(n log n)"
}

// 响应；实际计时以后端为准
{
  "correct": true,
  "correctAnswer": "O(n log n)",
  "awardedPoints": 120,
  "score": 120,
  "duplicate": false
}
```

#### WebSocket 信令

连接地址：`ws://localhost:8080/ws/signaling`，生产环境使用 `wss://<domain>/ws/signaling`。

| 消息类型 | 方向 | 用途 |
|---------|------|------|
| `join-room` | 客户端 → 服务端 → 已在线对手 | 注册信令会话并通知旧玩家 |
| `peer-present` | 服务端 → 新加入玩家 | 告知新玩家房间中已有对手 |
| `offer` | 点对点信令中继 | SDP Offer |
| `answer` | 点对点信令中继 | SDP Answer |
| `ice-candidate` | 点对点信令中继 | ICE Candidate |
| `peer-left` | 服务端 → 对手 | 对手信令连接已断开 |

加入信令前，服务端会校验 `userId` 是否已通过 REST 加入该房间。信令断开后若 15 秒内没有同一用户的新会话，服务端执行离开/判负逻辑。

### 7.9 错误响应

所有端点遵循统一的错误格式：

| HTTP 状态码 | 场景 | 响应体示例 |
|------------|------|-----------|
| 400 Bad Request | 参数校验失败 | `{"error": "..."}` 或 Spring Validation 默认格式 |
| 401 Unauthorized | 登录失败 | `{"message": "用户名或密码不正确。"}` |
| 409 Conflict | 房间已满、比赛已开始、答题顺序错误等状态冲突 | `{"message": "请按顺序提交题目"}` |
| 500 Internal Server Error | 服务端异常 | Spring Boot 默认错误格式 |

---

## 8. 数据库设计

### 8.1 数据库信息

```
数据库名：algorithm_viz
字符集：utf8mb4
排序规则：utf8mb4_unicode_ci
```

### 8.2 表结构

#### app_user — 用户表

```sql
CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| username | VARCHAR(50) | 登录用户名（唯一） |
| display_name | VARCHAR(80) | 显示名称 |
| password_hash | VARCHAR(128) | SHA-256(salt + ":" + password) 的 Hex 编码（64 字符） |
| password_salt | VARCHAR(64) | 随机盐值（16 字节 Hex 编码 = 32 字符） |
| created_at | DATETIME | 注册时间 |

#### run_history — 运行历史表

```sql
CREATE TABLE run_history (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    category          VARCHAR(50)   NOT NULL,
    algorithm         VARCHAR(50)   NOT NULL,
    input_data        TEXT,
    step_count        INT,
    comparisons       INT,
    swaps             INT,
    execution_time_ms BIGINT,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| category | VARCHAR(50) | 算法分类（sorting/search/graph/dp/backtracking/divide-conquer） |
| algorithm | VARCHAR(50) | 具体算法名 |
| input_data | TEXT | 输入参数（JSON 格式） |
| step_count | INT | 步骤总数 |
| comparisons | INT | 比较次数 |
| swaps | INT | 操作次数（排序→交换，回溯→回溯次数等） |
| execution_time_ms | BIGINT | 后端计算耗时（毫秒） |
| created_at | DATETIME | 自动记录时间戳 |

### 8.3 索引

- `app_user.uk_app_user_username` — 唯一索引，加速登录查询
- `run_history` 表的 `category` 和 `created_at` 字段应建立复合索引以优化按分类查询历史（当前 JPA 默认无显式索引，建议生产环境添加）

### 8.4 竞赛数据存储说明

当前版本的竞赛房间、答案表和提交记录存放在 `CompetitionRoomService` 的并发内存 Map 中，不写入 MySQL。因此后端重启会清空未结束房间，且多实例部署时不同实例之间不能直接共享房间状态。

若后续需要竞赛历史、排行榜或后端水平扩展，建议：
- 使用 Redis 保存活动房间、玩家心跳和短期提交状态
- 使用 MySQL 持久化已结束比赛、每题结果和最终排名
- 通过 Redis Pub/Sub 或专用消息系统同步不同后端实例上的 WebSocket 信令事件

---

## 9. 文件清单

### 9.1 后端文件（backend/src/main/java/com/algorithmviz/）

| 文件路径 | 职责 |
|---------|------|
| `AlgorithmVizApplication.java` | Spring Boot 应用入口 |
| `config/CorsConfig.java` | REST CORS 来源配置 |
| `config/WebSocketConfig.java` | 注册 `/ws/signaling` 并配置 WebSocket Origin |
| `controller/AlgorithmController.java` | 核心控制器：6 个算法执行端点 + verify-step + history CRUD + health |
| `controller/AuthController.java` | 认证控制器：register + login |
| `controller/CompetitionController.java` | 竞赛房间、准备、提交和排名 REST API |
| `websocket/SignalingHandler.java` | WebRTC 双向信令中继、会话绑定和 15 秒断线宽限 |
| `service/SortingService.java` | 5 种排序算法步骤生成（quick/merge/bubble/heap/insertion） |
| `service/SearchService.java` | 二分查找步骤生成 |
| `service/GraphService.java` | 6 种图算法步骤生成（Dijkstra/BFS/DFS/Prim/Kruskal/A\*） |
| `service/DPService.java` | 0/1 背包 DP 步骤生成 |
| `service/BacktrackingService.java` | N 皇后回溯步骤生成 |
| `service/DivideConquerService.java` | Karatsuba 大整数乘法步骤生成 |
| `service/AlgorithmComplexityService.java` | AI 大模型复杂度分析服务 |
| `service/AssessmentService.java` | LLM 评估服务：动态出题 + 语义评测 + 交叉验证（独立于 AlgorithmComplexityService） |
| `service/AuthService.java` | 用户认证服务（注册/登录 + SHA-256 加盐哈希） |
| `service/CompetitionRoomService.java` | 竞赛题库、内存房间状态机、服务端计时判分、排名和过期清理 |
| `model/SortStep.java` | 排序步骤模型（含 Builder、phase） |
| `model/SearchStep.java` | 搜索步骤模型（含 Builder、phase） |
| `model/GraphStep.java` | 图步骤模型（含 Builder、phase） |
| `model/DPStep.java` | DP 步骤模型（含 Builder、phase） |
| `model/NQueensStep.java` | N 皇后步骤模型（含 Builder、phase） |
| `model/DivideConquerStep.java` | 分治步骤模型（含 Builder、phase、内嵌 TreeNode） |
| `model/AlgorithmComplexityAnalysis.java` | AI 分析结果模型（含 Builder） |
| `model/AssessmentQuestion.java` | LLM 生成的评估题目模型（含 Builder） |
| `model/AnswerEvaluationResponse.java` | LLM 评测结果模型（含 Builder） |
| `model/CompetitionRoom.java` | 竞赛房间模型 |
| `model/CompetitionPlayer.java` | 竞赛玩家状态模型 |
| `model/CompetitionQuestion.java` | 不含答案的公开竞赛题目模型 |
| `entity/RunHistory.java` | 运行历史 JPA 实体 |
| `entity/AppUser.java` | 用户 JPA 实体 |
| `dto/SortRequest.java` | 排序请求 DTO |
| `dto/SearchRequest.java` | 搜索请求 DTO |
| `dto/GraphRequest.java` | 图请求 DTO（内嵌 GraphData/GraphNodeDto/GraphEdgeDto） |
| `dto/DPRequest.java` | DP 请求 DTO（内嵌 KnapsackItemDto） |
| `dto/BacktrackingRequest.java` | 回溯请求 DTO |
| `dto/DivideConquerRequest.java` | 分治请求 DTO |
| `dto/AlgorithmComplexityRequest.java` | AI 分析请求 DTO |
| `dto/AssessmentConfigRequest.java` | 评估配置请求 DTO（题数/分类/难度/题型） |
| `dto/AnswerEvaluationRequest.java` | 评测请求 DTO |
| `dto/AuthRequest.java` | 登录请求 DTO |
| `dto/AuthResponse.java` | 认证响应 DTO |
| `dto/RegisterRequest.java` | 注册请求 DTO |
| `dto/CreateCompetitionRoomRequest.java` | 创建竞赛房间请求 DTO |
| `dto/JoinCompetitionRoomRequest.java` | 加入竞赛房间请求 DTO |
| `dto/CompetitionSubmitRequest.java` | 竞赛答案提交 DTO |
| `dto/CompetitionSubmitResponse.java` | 判分结果 DTO |
| `repository/RunHistoryRepository.java` | 运行历史 JPA Repository |
| `repository/AppUserRepository.java` | 用户 JPA Repository |

### 9.2 前端文件（frontend/src/app/）

| 文件路径 | 职责 |
|---------|------|
| `app.component.ts` | 根组件：Tab 导航、对比指标 computed、布局切换 |
| `app.component.html` | 根模板：header/main/sidebar 布局、三 Tab 内容区域 |
| `app.config.ts` | Angular 应用配置（HttpClient、路由） |
| `app.routes.ts` | 路由定义 |
| `store/algorithm.store.ts` | 核心状态管理：信号定义、算法运行、Phase 系统、对比模式、播放控制 |
| `store/auth.store.ts` | 认证状态管理 |
| `store/competition.store.ts` | 竞赛状态管理：房间恢复、轮询、答题、排名、连接状态和聊天 |
| `services/algorithm.service.ts` | 后端 API HTTP 客户端 |
| `services/auth.service.ts` | 认证 API HTTP 客户端 |
| `services/competition.service.ts` | 竞赛 REST API 客户端 |
| `services/webrtc-peer.service.ts` | WebSocket 信令、WebRTC DataChannel、ICE 重连和旧连接事件隔离 |
| `models/algorithm.models.ts` | TypeScript 类型定义（16 个接口 + 类型别名） |
| `models/competition.models.ts` | 竞赛房间、玩家、题目和实时消息类型 |
| `data/test-scenarios.ts` | 5 道经典固定评估测试题数据 |
| `data/algorithm-catalog.ts` | 侧边栏与竞赛大厅共享的算法分类、名称和复杂度目录 |
| `components/sidebar/sidebar.component.ts` | 侧边栏：算法选择列表、对比模式开关 |
| `components/control-panel/control-panel.component.ts` | 播放控制栏 |
| `components/input-config/input-config.component.ts` | 输入数据编辑面板 |
| `components/phase-guide/phase-guide.component.ts` | 教学阶段进度条 |
| `components/complexity-panel/complexity-panel.component.ts` | 复杂度信息卡片 |
| `components/history-panel/history-panel.component.ts` | 运行历史面板 |
| `components/assessment-container/assessment-container.component.ts` | 评估测试容器：管理设置/答题阶段 |
| `components/assessment-container/assessment-settings/assessment-settings.component.ts` | 评估配置面板：题数/分类/难度/题型 |
| `components/assessment/assessment.component.ts` | 评估答题组件：动态题目+AI评测+固定题降级 |
| `components/auth/auth.component.ts` | 登录/注册表单 |
| `components/competition/competition.component.ts` | 1v1 竞赛房间、答题、聊天和结果界面 |
| `components/ai-complexity-dialog/ai-complexity-dialog.component.ts` | AI 复杂度分析弹窗 |
| `visualizers/sorting/sorting-visualizer.component.ts` | 排序可视化器（柱状图/Canvas） |
| `visualizers/graph/graph-visualizer.component.ts` | 图可视化器（SVG 节点/边） |
| `visualizers/search/search-visualizer.component.ts` | 搜索可视化器（数组高亮） |
| `visualizers/dp/dp-visualizer.component.ts` | DP 可视化器（二维表格） |
| `visualizers/n-queens/n-queens-visualizer.component.ts` | N 皇后可视化器（棋盘） |
| `visualizers/divide-conquer/divide-conquer-visualizer.component.ts` | 分治可视化器（递归树） |
| `visualizers/vr-3d/vr-3d-visualizer.component.ts` | 3D 可视化器容器组件 |
| `visualizers/vr-3d/renderers/three-object-factory.ts` | Three.js 3D 对象工厂 |
| `visualizers/vr-3d/renderers/array.renderer.ts` | 数组 3D 渲染器 |
| `visualizers/vr-3d/renderers/stack.renderer.ts` | 栈 3D 渲染器 |
| `visualizers/vr-3d/renderers/queue.renderer.ts` | 队列 3D 渲染器 |
| `visualizers/vr-3d/renderers/linked-list.renderer.ts` | 链表 3D 渲染器 |
| `visualizers/vr-3d/renderers/binary-tree.renderer.ts` | 二叉树 3D 渲染器 |
| `visualizers/vr-3d/renderers/b-plus-tree.renderer.ts` | B+ 树 3D 渲染器 |
| `visualizers/vr-3d/renderers/structure-renderer.types.ts` | 3D 渲染器类型定义 |
| `visualizers/vr-3d/animators/structure-animator.interface.ts` | 3D 动画器接口 |
| `visualizers/vr-3d/animators/basic-structure.animator.ts` | 基础结构动画器（数组/栈/队列/链表/二叉树） |
| `visualizers/vr-3d/animators/b-plus-tree.animator.ts` | B+ 树动画器 |
| `visualizers/vr-3d/data/structure-info.ts` | 数据结构信息配置 |

### 9.3 配置文件

| 文件 | 说明 |
|------|------|
| `backend/pom.xml` | Maven 项目配置（Spring Boot 3.2.3、WebSocket、Java 17） |
| `backend/src/main/resources/application.properties` | Spring Boot 配置（数据源/JPA/Jackson/AI） |
| `frontend/package.json` | npm 依赖和脚本 |
| `frontend/angular.json` | Angular CLI 配置 |
| `frontend/tailwind.config.js` | Tailwind CSS 配置 |
| `frontend/src/environments/environment.ts` | 开发环境变量（API 地址 + WebRTC ICE Servers） |
| `frontend/src/environments/environment.prod.ts` | 生产环境变量（同源 API + WebRTC ICE Servers） |
| `frontend/src/styles.css` | 全局 Tailwind 样式入口 |
| `frontend/src/assets/login-bg.png` | 登录页背景图（约 1.7MB，全屏覆盖 + 渐变遮罩） |
| `docs/learning-guidance-system.md` | 学习引导系统独立设计文档 |
| `docs/comprehensive-project-documentation.md` | 本文档（项目分析、设计、实现、部署、使用全貌） |
| `docs/assessment-llm-enhancement-plan.md` | 评估测试 LLM 增强改造方案 |
| `docs/assessment-llm-implementation.md` | 评估测试 LLM 增强实现文档（含使用方法） |
| `docs/部署文档.md` | AWS ECS Docker 部署专项文档 |
| `CLAUDE.md` | Claude Code 项目指引文件 |

---

> **文档维护说明**：本文档描述项目截至 2026-06-19 的全貌。代码变更后请同步更新相关章节。此文档整合了 `docs/部署文档.md` 的部署内容，可作为 PPT 汇报的基础材料。
