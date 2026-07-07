# 学习引导系统（Learning Guidance System）设计文档

> 算法与复杂度可视化学习平台 — 教学引导 + 对比模式 + 评估测试

---

## 目录

1. [概述](#1-概述)
2. [模块 A：算法过程分解（教学动态化）](#2-模块-a算法过程分解教学动态化)
3. [模块 B：对比模式（并排双算法）](#3-模块-b对比模式并排双算法)
4. [模块 C：标准测试集](#4-模块-c标准测试集)
5. [模块 D：自动判分](#5-模块-d自动判分)
6. [检测机制](#6-检测机制)
7. [文件清单](#7-文件清单)

---

## 1. 概述

### 1.1 背景

当前平台已实现 14 种算法的分步可视化，但缺少**教学引导**（步骤的阶段划分）和**评估功能**（测试与判分）。本系统在现有基础设施之上，新增四大功能模块，将平台从"可视化工具"升级为"教—学—练—测"一体化的学习平台。

### 1.2 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                     前端 (Angular 17)                            │
│                                                                 │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────┐  │
│  │ Sidebar  │  │ PhaseGuide   │  │ Visualizers│ │ Assessment │  │
│  │ (对比模式) │  │ (阶段进度条)   │  │ (双源输入)  │  │ (5题+判分)  │  │
│  └──────────┘  └──────────────┘  └──────────┘  └────────────┘  │
│                          ↕ HTTP POST                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                AlgorithmStore (Signal-based)              │   │
│  │  phaseConfig │ compareMode │ currentPhase │ activePanel   │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot 3.2)                         │
│                                                                 │
│  POST /api/algorithms/sort|search|graph|dp|backtracking        │
│  POST /api/algorithms/verify-step    ← 新增                     │
│  GET  /api/algorithms/history                                    │
│                                                                 │
│  5× Service → 每步标注 phase → 14 种算法 × 教学阶段              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 新增 Tab 导航

| Tab | 图标 | 功能 |
|-----|------|------|
| 可视化 | ⚡ | 算法运行、阶段引导、对比模式 |
| 评估测试 | 📝 | 5 道算法理解题、自动判分 |
| 历史记录 | 📋 | 运行历史查询与删除 |

---

## 2. 模块 A：算法过程分解（教学动态化）

### 2.1 设计思路

为每种算法的执行过程定义一组**离散的教学阶段（phase）**。后端在生成每一步时标注当前所属阶段，前端通过阶段进度条（PhaseGuideComponent）向学习者展示"算法进行到哪一步了"。

**为什么用 phase 字符串而非枚举？**
- phase 值是教学语义层的概念，不同于代码实现细节
- 不同算法的 phase 集合不同（排序有 swap，图算法有 explore_edge）
- 字符串灵活可扩展，JSON 序列化零成本

### 2.2 后端变更

#### 2.2.1 模型层 — 5 个 Step 类新增 `phase` 字段

```
SortStep.java       →  private String phase;
SearchStep.java     →  private String phase;
GraphStep.java      →  private String phase;
DPStep.java         →  private String phase;
NQueensStep.java    →  private String phase;
```

每个模型均使用手动 Builder 模式，新增 `.phase(String)` 方法和 `getPhase()` getter。

#### 2.2.2 服务层 — 14 种算法的 phase 定义

**排序算法：**

| 算法 | Phase 序列 | 中文说明 |
|------|-----------|---------|
| 快速排序 | `select_pivot` → `compare` → `swap` → `pivot_placed` → `done` | 选基准→比较→交换→基准归位→完成 |
| 归并排序 | `divide` → `merge_setup` → `merge_place` → `done` | 分割→准备合并→归并元素→完成 |
| 冒泡排序 | `compare` → `swap` → `bubble_complete` → `done` | 比较→交换→本轮完成→完成 |
| 堆排序 | `build_heap` → `heapify_compare` → `heapify_swap` → `extract_max` → `done` | 建堆→堆比较→堆交换→提取最大值→完成 |
| 插入排序 | `key_select` → `shift` → `insert` → `done` | 选关键字→后移→插入→完成 |

**图算法：**

| 算法 | Phase 序列 |
|------|-----------|
| Dijkstra | `init` → `select_min` → `explore_edge` → `update_dist` → `reconstruct_path` |
| BFS | `init` → `dequeue` → `discover_neighbor` → `reconstruct_path` |
| DFS | `init` → `pop_stack` → `discover_neighbor` → `reconstruct_path` |
| Prim | `init` → `add_to_mst` → `done` |
| Kruskal | `init` → `check_cycle` → `add_to_mst` / `skip_edge` → `done` |
| A\* | `init` → `select_min` → `update_dist` → `reconstruct_path` |

**搜索与 DP：**

| 算法 | Phase 序列 |
|------|-----------|
| 二分搜索 | `init` → `calculate_mid` → `eliminate` → `found` / `not_found` |
| 0/1 背包 | `init` → `skip_weight` → `compare` → `take` / `skip` → `traceback` |

**回溯：**

| 算法 | Phase 序列 |
|------|-----------|
| N 皇后 | `init` → `try_place` → `place` → `solution_found` → `backtrack` → `done` |

#### 2.2.3 关键代码示例

以归并排序为例，merge 函数的 setup 步骤标注：

```java
steps.add(SortStep.builder()
    .array(toList(arr))
    .mergeLeft(leftList).mergeRight(rightList).mergeTarget(left)
    .description("合并子数组 [" + left + ".." + mid + "] 和 [" + (mid+1) + ".." + right + "]")
    .phase("merge_setup")   // ← 新增
    .build());
```

### 2.3 前端变更

#### 2.3.1 PhaseGuideComponent

**位置**：ControlPanel 下方、Visualizer 上方

**UI 组成**：
- 水平阶段进度条：圆点 + 连接线
  - 已完成阶段 → 绿色圆点 + ✓ 标记
  - 当前阶段 → 蓝色圆点 + 光晕高亮
  - 未到阶段 → 灰色圆点 + 序号
- 每个圆点下方标注阶段中文名称
- 底部显示"当前阶段：XXX"文字

**数据来源**：`AlgorithmStore.phaseConfig()` computed signal 从 `PHASE_CONFIG` 常量表读取当前算法的 phase 序列和中文标签。

#### 2.3.2 Store 中的 Phase 信号

```typescript
// PHASE_CONFIG: Record<AlgorithmId, { sequence: string[], labels: Record<string, string> }>
// 为每种算法预定义了 phase 序列和中英文映射

phaseConfig       = computed(() => PHASE_CONFIG[this.selectedAlgo()]);
currentPhase      = computed(() => 从 currentStepData 中提取 phase);
currentPhaseIndex = computed(() => phase 在序列中的位置);
phaseLabel        = computed(() => 当前 phase 的中文标签);
```

---

## 3. 模块 B：对比模式（并排双算法）

### 3.1 设计思路

允许用户同时运行两个**同分类**的算法，左右并排展示两个可视化器，共享同一套播放控制。适合比较"快速排序 vs 归并排序"等场景。

**限制为同分类的原因**：不同分类的算法使用不同的可视化器组件（排序用柱状图、图用 SVG 等），无法在同一视图中并排渲染。

### 3.2 数据模型

在 `AlgorithmStore` 中新增第二套执行状态信号（全部以 `compare` 前缀命名）：

```typescript
compareMode          : signal<boolean>           // 是否启用对比模式
compareAlgo          : signal<AlgorithmId>       // 第二个算法
compareSteps         : signal<AnyStep[]>         // 第二个算法的步骤数据
compareCurrentStep   : signal<number>            // 第二个算法的当前步
compareIsPlaying     : signal<boolean>
compareIsLoading     : signal<boolean>
compareError         : signal<string | null>
compareCurrentStepData : computed                // 第二个算法的当前步骤数据
```

### 3.3 播放同步机制

对比模式的核心是**一个定时器同时推进两个算法的步骤**：

```typescript
togglePlay(): void {
  if (this.isPlaying()) {
    this.stopPlay();
    if (this.compareMode()) this.stopComparePlay();
  } else {
    this.startPlay();                          // 主算法
    if (this.compareMode() && this.compareSteps().length > 0)
      this.startComparePlay();                 // 对比算法
  }
}
```

**边界处理**：任一算法先到达末尾时，其自己的定时器自动停止（由 `stopPlay`/`stopComparePlay` 处理）。速度切换时两个定时器同步重建。

### 3.4 可视化器的双源模式

5 个可视化器组件各新增：

```typescript
@Input() source: 'primary' | 'compare' = 'primary';

step = computed(() => {
  const data = this.source === 'primary'
    ? this.store.currentStepData()
    : this.store.compareCurrentStepData();
  return data as XxxStep | null;
});
```

此模式让可视化器在对比布局中可以不修改内部渲染逻辑直接复用。

### 3.5 对比指标面板

在 PhaseGuide 下方显示一行紧凑的对比数据：

```
[主算法名] 步数: 45 vs 38 [对比名]  比较: 120 vs 95  操作: 15 vs 10
```

- 绿色高亮数值更优的一方
- 红色高亮数值较差的一方
- 数据取自每个算法最后一步的 `comparisons`、`swaps`/`backtracks` 等字段

### 3.6 侧边栏交互

- "⚖ 对比模式"切换按钮位于侧边栏底部
- 激活后下方展开对比算法列表，仅显示与当前算法**同一 category** 的其他算法
- 点击可切换对比算法
- 选择算法后点击控制面板的"▶ 运行对比"按钮，同时向后端发起两个请求

### 3.7 AppComponent 布局切换

**普通模式**：
```
┌──────────┬───────────────────────────────┐
│ Sidebar  │ ControlPanel                  │
│          │ PhaseGuide                    │
│          │ ┌───────────────────────────┐ │
│          │ │    Visualizer (全宽)       │ │
│          │ └───────────────────────────┘ │
└──────────┴───────────────────────────────┘
```

**对比模式**：
```
┌──────────┬─────────────────────────────────────────┐
│ Sidebar  │ ControlPanel                            │
│          │ CompareMetrics Panel                    │
│          │ ┌──────────────┬──────────────────────┐ │
│          │ │ 快速排序(左)   │ 归并排序(右)          │ │
│          │ │              │                      │ │
│          │ │  [柱状图]     │  [柱状图]             │ │
│          │ └──────────────┴──────────────────────┘ │
└──────────┴─────────────────────────────────────────┘
```

---

## 4. 模块 C：标准测试集

### 4.1 设计思路

5 道算法理解题，覆盖 5 个分类。每道题包含输入参数、题目描述、标准答案和解析。题目定义在前端 TypeScript 常量中，不依赖数据库。

### 4.2 题目列表

| # | 标题 | 分类 | 算法 | 题型 | 核心考察点 |
|---|------|------|------|------|-----------|
| 1 | 快速排序分区过程 | sorting | quick-sort | 状态填空 | 理解 partition 操作，知道基准值归位后的数组状态 |
| 2 | 归并排序合并过程 | sorting | merge-sort | 状态填空 | 理解分治策略，知道第一次合并后的数组状态 |
| 3 | 二分查找过程分析 | search | binary-search | 数值填空 | 理解二分查找的 mid 计算和边界收缩 |
| 4 | Dijkstra 最短路径 | graph | dijkstra | 路径填空 | 理解贪心策略，能手动计算最短路径 |
| 5 | 0/1 背包 DP 计算 | dp | knapsack | 表格填空 | 理解 DP 状态转移，能推算 dp[i][w] 值 |

### 4.3 数据定义

`frontend/src/app/data/test-scenarios.ts` 中定义 `TEST_SCENARIOS` 数组：

```typescript
export interface TestScenario {
  id: number;
  title: string;
  category: AlgorithmCategory;
  algorithm: AlgorithmId;
  questionType: 'value-fill' | 'state-fill' | 'path-fill' | 'table-fill' | 'choice';
  description: string;             // 题目描述（中文）
  inputParams: Record<string, unknown>;  // 算法输入参数
  answer: unknown;                 // 标准答案
  options?: string[];              // 选择题选项
  explanation: string;             // 解析（中文）
  targetStepIndex?: number;        // 后端验证时取第几步
  verifyField?: string;            // 后端验证时取哪个字段
}
```

### 4.4 AssessmentComponent 交互流程

```
进入"评估测试" Tab
       │
       ▼
  ┌──────────────────────────────┐
  │  进度指示器 (1/5 圆点)        │
  │  ┌─────────────────────────┐ │
  │  │ 题目卡片                 │ │
  │  │  - 分类标签              │ │
  │  │  - 题目描述              │ │
  │  │  - 输入参数预览           │ │
  │  │  - 答案输入框             │ │
  │  │  - [✓ 提交答案] 按钮     │ │
  │  └─────────────────────────┘ │
  │  ← 上一题    [下一题 →]      │
  └──────────────────────────────┘
       │ 提交答案
       ▼
  ┌──────────────────────────────┐
  │  ✅ 正确 / ❌ 错误 反馈       │
  │  📖 解析展示                 │
  │  （错误时）正确答案展示        │
  └──────────────────────────────┘
       │ 全部答完
       ▼
  ┌──────────────────────────────┐
  │  🎉 总分卡片 (X/5)           │
  │  - 满分: 绿色 + 太棒了！      │
  │  - ≥3分: 黄色 + 继续加油     │
  │  - <3分: 红色 + 多练习       │
  │  - [重新测试] 按钮           │
  └──────────────────────────────┘
```

---

## 5. 模块 D：自动判分

### 5.1 判分策略

按题型分派不同的判分逻辑：

| 题型 | 判分方式 | 说明 |
|------|---------|------|
| `value-fill` (数值填空) | 前端直接比对 | 提取用户输入中的关键数值，与标准答案的 comparisons/mids 等字段比对 |
| `state-fill` (状态填空) | 调用后端 API 验证 | 向后端发送算法参数，获取指定步骤的标准状态，与用户输入比对 |
| `path-fill` (路径填空) | 前端标准化比对 | 去除空白后比较路径字符串和距离值 |
| `table-fill` (表格填空) | 后端 API + 前端兜底 | 先尝试 API 获取 dp 表格值，fallback 到 `dpValue` 直接比对 |
| `choice` (选择) | 前端直接比对 | 字符串标准化后比较 |

### 5.2 后端验证端点

**新增** `POST /api/algorithms/verify-step`

**请求体**：
```json
{
  "algorithm": "quick-sort",
  "params": { "array": [8, 3, 1, 6, 2, 5] },
  "targetStepIndex": 10
}
```

**响应体**：
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
    "phase": "pivot_placed",
    "description": "基准值归位...",
    ...
  }
}
```

**实现原理**：端点接收算法名和参数后，调用对应的 Service 完整运行该算法，然后返回 `targetStepIndex` 处的那一步数据。前端拿到标准步骤后，可提取 `verifyField` 指定的字段与用户答案比对。

**支持的算法类别**：
- 排序（5 种）
- 搜索（二分查找）
- DP（背包）
- 回溯（N 皇后）
- 图算法（返回提示信息，图数据解析由前端处理）

### 5.3 评分规则

- 每题 1 分，满分 5 分
- 已答题目可回看修改（重新提交覆盖之前结果）
- 进度圆点的颜色反映结果：绿色 ✓ = 正确，红色 ✗ = 错误，灰色 = 未作答
- 完成全部 5 题后显示总分卡片，可重新测试

---

## 6. 检测机制

### 6.1 编译检测

**后端**：
```bash
cd backend
mvn clean compile    # 20 个源文件编译通过
```

**前端**：
```bash
cd frontend
ng build             # 无 TS 错误，产物 460 KB（含 lazy chunk）
```

### 6.2 功能检测清单

#### 6.2.1 教学过程分解

| 检测项 | 方法 | 预期结果 |
|--------|------|---------|
| Phase 字段存在于所有步骤 | 运行任一算法，检查 Network 响应中的 `steps[].phase` | 每步都有非空 phase 字符串 |
| 阶段进度条显示 | 运行算法后查看 PhaseGuide 组件 | 显示该算法对应的阶段圆点和中文标签 |
| 阶段正确切换 | 逐步播放算法 | 蓝色高亮圆点随 phase 变化而移动，已完成阶段变绿 |
| 14 种算法全覆盖 | 逐一运行 14 种算法 | 每种算法都有各自独特的阶段序列 |

#### 6.2.2 对比模式

| 检测项 | 方法 | 预期结果 |
|--------|------|---------|
| 开关显示 | 打开侧边栏，查看底部 | 显示"⚖ 对比模式"按钮 |
| 对比算法列表 | 选中快速排序，打开对比模式 | 显示同分类的其它 4 种排序算法 |
| 同时运行 | 选择对比算法后点击"运行对比" | 后端收到 2 个请求，左右各显示一个可视化器 |
| 播放同步 | 点击播放 | 两个可视化器同步推进，步数指针同时移动 |
| 对比指标 | 查看 PhaseGuide 下方的指标面板 | 显示步数、比较次数、操作次数的对比，优者绿色高亮 |
| 同分类限制 | 选择二分查找（search 分类），打开对比 | 无可对比算法提示，因为 search 分类仅一种算法 |

#### 6.2.3 评估测试

| 检测项 | 方法 | 预期结果 |
|--------|------|---------|
| Tab 切换 | 点击"评估测试"Tab | 进入评估界面，显示 5 个进度圆点 |
| 题目内容 | 浏览 5 道题 | 每题显示分类标签、题目描述、输入参数预览 |
| 故意答错 | 第 1 题输入错误数组，提交 | 显示红色"回答错误"，显示解析和正确答案 |
| 全部答对 | 依次输入正确答案 | 每题显示绿色"回答正确"，最终显示 5/5 满分卡片 |
| 重新测试 | 点击"重新测试" | 所有答案清空，进度重置 |
| 题目导航 | 点击进度圆点跳转、使用上/下一题按钮 | 正确跳转到对应题目 |

#### 6.2.4 后端验证端点

| 检测项 | 方法（curl） | 预期结果 |
|--------|-------------|---------|
| 排序验证 | `curl -X POST http://localhost:8080/api/algorithms/verify-step -H "Content-Type: application/json" -d '{"algorithm":"quick-sort","params":{"array":[8,3,1,6,2,5]},"targetStepIndex":10}'` | 返回 `stepData` 包含第一次分区后的数组状态 |
| 搜索验证 | `curl ... -d '{"algorithm":"binary-search","params":{"array":[2,5,8,12,16,23,38,45,56,67,78],"target":23},"targetStepIndex":3}'` | 返回第 3 步的 mid、left、right 等数据 |
| 参数缺失 | 发送缺少 `algorithm` 的请求 | 返回 400 Bad Request |
| 索引越界 | `targetStepIndex` 超出步骤总数 | 返回 `stepData: null` |

### 6.3 端到端检测流程

```
1. 启动后端: cd backend && mvn spring-boot:run   (确保 MySQL 运行)
2. 启动前端: cd frontend && npm start
3. 浏览器打开 http://localhost:4200

4. 教学过程分解检测:
   a. 左侧选择"快速排序" → 点击"运行"
   b. 观察步骤描述下方的阶段进度条
   c. 逐步播放，确认阶段切换
   d. 切换到其他算法，重复验证

5. 对比模式检测:
   a. 左侧选择"快速排序"
   b. 点击底部"⚖ 对比模式"
   c. 选择"归并排序"作为对比算法
   d. 点击"运行对比"
   e. 观察左右两个可视化器 + 对比指标面板
   f. 点击播放，确认同步

6. 评估测试检测:
   a. 点击顶部"📝 评估测试"Tab
   b. 依次回答 5 道题
   c. 第 1 题故意答错 → 确认红色反馈
   d. 重新输入正确答案 → 确认绿色反馈
   e. 全部完成后查看总分卡片
   f. 点击"重新测试"验证重置

7. API 检测:
   a. 使用 curl 或 Postman 测试 verify-step 端点
   b. 验证返回的 stepData 与前端运行结果一致
```

---

## 7. 文件清单

### 7.1 修改的文件

| 文件路径 | 修改内容 |
|---------|---------|
| `backend/src/main/java/.../model/SortStep.java` | 新增 `phase` 字段、Builder 方法、Getter |
| `backend/src/main/java/.../model/SearchStep.java` | 同上 |
| `backend/src/main/java/.../model/GraphStep.java` | 同上 |
| `backend/src/main/java/.../model/DPStep.java` | 同上 |
| `backend/src/main/java/.../model/NQueensStep.java` | 同上 |
| `backend/src/main/java/.../service/SortingService.java` | 所有步骤添加 phase 标注（5 种排序 × phase） |
| `backend/src/main/java/.../service/SearchService.java` | 二分查找步骤添加 phase 标注 |
| `backend/src/main/java/.../service/GraphService.java` | 6 种图算法步骤添加 phase 标注 |
| `backend/src/main/java/.../service/DPService.java` | 背包 DP 步骤添加 phase 标注 |
| `backend/src/main/java/.../service/BacktrackingService.java` | N 皇后步骤添加 phase 标注 |
| `backend/src/main/java/.../controller/AlgorithmController.java` | 新增 `POST /verify-step` 端点 |
| `frontend/src/app/models/algorithm.models.ts` | 所有 TS step 接口新增 `phase: string`，新增 `TestScenario` 等类型 |
| `frontend/src/app/store/algorithm.store.ts` | 新增 PHASE_CONFIG、对比模式信号、phase computed signals |
| `frontend/src/app/services/algorithm.service.ts` | 新增 `verifyStep()` 方法 |
| `frontend/src/app/app.component.ts` | 新增 `compareMetrics`、导入新组件、tabs 新增 assessment |
| `frontend/src/app/app.component.html` | 对比模式双栏布局、PhaseGuide 组件、对比指标面板 |
| `frontend/src/app/components/sidebar/sidebar.component.ts` | 新增 `compareSiblings` computed、`selectCompare` 方法 |
| `frontend/src/app/components/sidebar/sidebar.component.html` | 新增对比模式开关 + 对比算法选择器 |
| `frontend/src/app/components/control-panel/control-panel.component.html` | "运行"按钮适配对比模式（`runBothAlgorithms`） |
| `frontend/src/app/visualizers/sorting/sorting-visualizer.component.ts` | 新增 `@Input() source` |
| `frontend/src/app/visualizers/graph/graph-visualizer.component.ts` | 同上 |
| `frontend/src/app/visualizers/search/search-visualizer.component.ts` | 同上 |
| `frontend/src/app/visualizers/dp/dp-visualizer.component.ts` | 同上 |
| `frontend/src/app/visualizers/n-queens/n-queens-visualizer.component.ts` | 同上 |

### 7.2 新建的文件

| 文件路径 | 用途 |
|---------|------|
| `frontend/src/app/components/phase-guide/phase-guide.component.ts` | PhaseGuide 组件逻辑 |
| `frontend/src/app/components/phase-guide/phase-guide.component.html` | PhaseGuide 组件模板（阶段进度条） |
| `frontend/src/app/data/test-scenarios.ts` | 5 道测试题目数据定义 |
| `frontend/src/app/components/assessment/assessment.component.ts` | 评估组件逻辑（答题、判分、导航） |
| `frontend/src/app/components/assessment/assessment.component.html` | 评估组件模板（题目卡片、反馈、总分） |
| `docs/learning-guidance-system.md` | 本文档 |

---

## 附录：关键技术决策

### A. Phase 使用字符串而非枚举

- **原因**：phase 是教学语义概念，不同算法的 phase 集合完全不同。枚举适合固定集合，但此场景中 phase 是开放集合，字符串灵活且 JSON 友好。

### B. 对比模式限制同分类

- **原因**：不同分类共享不同的可视化器（柱状图 vs SVG vs 表格），跨分类对比需要同时渲染两种不同的可视化器，布局复杂度剧增。同分类限制保持了 v1 实现的简洁性。

### C. Builder 模式手动维护

- **原因**：项目未引入 Lombok（避免额外依赖），采用手动 Builder。新增 `phase` 字段时，手动在 5 个模型的 Builder 内部类和 getter 中各添加一行即可，改动量可控。

### D. 测试题目定义在前端

- **原因**：测试题是静态教学资源，不频繁变更。定义在 TS 常量中避免数据库表设计和后端 CRUD 的额外开销。未来如需动态题库，可迁移至数据库。

### E. 判分的前端-后端协作

- **原因**：简单题型（数值、选择）前端直接比对即可；复杂题型（数组状态、DP 表格）需要运行算法获取标准答案，因此调用后端 `verify-step` 端点。同时设计了 API 不可用时的 fallback 策略（基本字符串匹配）。
