# 评估测试系统 LLM 增强 — 实现文档

> 版本 1.0 | 2026-06-16 | 将固定题目改为大模型动态出题 + 大模型评测

---

## 目录

1. [功能概述](#1-功能概述)
2. [使用方法](#2-使用方法)
3. [配置说明](#3-配置说明)
4. [文件结构](#4-文件结构)
5. [数据流](#5-数据流)
6. [降级与容错](#6-降级与容错)

---

## 1. 功能概述

### 1.1 改造前后对比

| 维度 | 改造前 | 改造后 |
|------|--------|--------|
| 题目来源 | 5 道硬编码在 `test-scenarios.ts` | **LLM 动态生成**，可配置数量、分类、难度、题型 |
| 判分方式 | 前端字符串匹配 / verify-step API | **LLM 语义评测**，理解等价表达，生成个性化反馈 |
| 题目复用 | 5 题答完无法再练 | 每次生成全新题目，可无限次练习 |
| 配置能力 | 无 | 题数(1-15)、分类/算法、难度(入门/中等/困难/混合)、题型(填空/选择/简答) |
| 降级方案 | 无 | LLM 不可用时自动切换为固定 5 题模式 |

### 1.2 两种模式

| 模式 | 入口 | 题目来源 | 评测方式 | API 依赖 |
|------|------|---------|---------|---------|
| **AI 动态出题** | 设置面板 → 选择"AI 动态出题" | 后端 AssessmentService 调用 LLM | 每题提交后 LLM 评测 | 需要配置 API Key |
| **经典固定题** | 设置面板 → 选择"经典固定题" | 原有 test-scenarios.ts（5 题） | 原有字符串匹配 / verify-step | 无需 API |

---

## 2. 使用方法

### 2.1 前置条件：配置 API Key

Spring Boot 原生读取操作系统环境变量。设置 `AI_ASSESSMENT_API_KEY` 即可：

**IntelliJ IDEA**：Run → Edit Configurations → Environment variables → 添加：
```
AI_ASSESSMENT_API_KEY=你的密钥
```

**终端启动**：
```bash
# Windows (PowerShell)
$env:AI_ASSESSMENT_API_KEY="你的密钥"
cd backend && mvn spring-boot:run

# macOS / Linux
export AI_ASSESSMENT_API_KEY="你的密钥"
cd backend && mvn spring-boot:run
```

**Docker 部署**：
```yaml
backend:
  environment:
    AI_ASSESSMENT_API_KEY: 你的密钥
```

> 不配置 API Key 也能正常使用——进入评估页面时会显示黄色提示，AI 出题按钮不可用，但经典固定题模式完全正常。

### 2.2 操作步骤

#### 2.2.1 使用 AI 动态出题模式

```
步骤 1：点击顶部导航栏 "📝 评估测试" Tab
       ↓
步骤 2：进入「评估测试设置」页面
       ↓
步骤 3：配置参数
       ├── 题目数量：点击 [3] [5] [7] [10] 快速选择，或输入 1-15 自定义
       ├── 题目模式：选择 "AI 动态出题"
       ├── 难度等级：入门 / 中等 / 困难 / 混合
       ├── 覆盖范围：勾选分类（排序/搜索/图/DP/回溯/分治）
       └── 题型偏好：填空 / 选择 / 简答（至少选一种）
       ↓
步骤 4：点击 "🤖 开始 AI 出题（N 题）"
       ↓
步骤 5：等待 5-15 秒，AI 生成题目
       ↓
步骤 6：逐题作答
       ├── 阅读题目描述和输入参数
       ├── 在文本框中输入答案
       ├── 点击 "✓ 提交答案"
       ├── 等待 2-5 秒 AI 评测
       └── 查看正确/错误反馈和详细解析
       ↓
步骤 7：完成全部题目后查看总分卡片
       ↓
步骤 8：点击 "返回设置" 可重新配置并测试
```

#### 2.2.2 使用经典固定题模式

```
步骤 1-2：同上
       ↓
步骤 3：题目模式选择 "经典固定题"
       ↓
步骤 4：点击 "开始答题（5 题）"
       ↓
步骤 5：逐题作答（与原流程相同，无需等待 AI）
       ↓
步骤 6：查看总分 → "返回设置"
```

### 2.3 题目类型说明

| 用户选择的题型 | LLM 实际生成的题型 | 示例 |
|-------------|------------------|------|
| **填空题** | state-fill（填入数组状态） | "第一次分区后数组 [8,3,1,6,2,5] 变为？" |
| | value-fill（填入数值） | "二分查找 23 经过几次比较？" |
| | path-fill（填入路径） | "A 到 F 的最短路径是什么？" |
| | table-fill（填入 DP 单元格值） | "dp[2][5] 的值是多少？" |
| **选择题** | choice（四选一） | "下列哪个是 partition 后的结果？A... B... C... D..." |
| **简答题** | 描述推理过程 | "请描述归并排序第一次合并的过程" |

---

## 3. 配置说明

### 3.1 后端配置

`application.properties` 中的评估配置：

```properties
ai.assessment.api-url=https://llmapi.paratera.com/v1/chat/completions
ai.assessment.api-key=${AI_ASSESSMENT_API_KEY:}
ai.assessment.model=DeepSeek-V3.2
```

`${AI_ASSESSMENT_API_KEY:}` 表示从操作系统环境变量读取。Spring Boot 原生支持这种写法，无需额外插件或配置文件。

- `api-url`：LLM API 地址，默认并行智算云，支持任何兼容 OpenAI 格式的 API
- `api-key`：通过环境变量注入，不写入代码仓库
- `model`：模型名称，默认 DeepSeek-V3.2

### 3.2 配置隔离

评估服务的配置（`ai.assessment.*`）与已有 AI 复杂度分析（`ai.complexity.*`）**完全独立**：

- 两个服务可使用**不同的 API 地址、不同的 Key、不同的模型**
- 修改一方的配置不影响另一方
- `AssessmentService` 不依赖 `AlgorithmComplexityService`，小组其他成员删除或修改后者不影响评估功能

### 3.3 LLM 安全调控

出题和评测均内置多层安全防护：

| 层级 | 措施 |
|------|------|
| System Prompt | 行为准则：只输出 JSON，不输出无关内容 |
| User Prompt | 输出格式约束：禁止 Markdown 包裹、代码块 |
| JSON 提取 | 后端自动剥离 LLM 可能输出的 Markdown 标记 |
| 格式校验 | 必填字段完整性检查，不合格题目丢弃 |
| 内容合规 | 校验 category/algorithm/questionType 在合法范围内 |

---

## 4. 文件结构

### 4.1 新增文件

```
backend/src/main/java/com/algorithmviz/
├── dto/
│   ├── AssessmentConfigRequest.java       ← 评估配置请求 DTO
│   └── AnswerEvaluationRequest.java      ← 评测请求 DTO
├── model/
│   ├── AssessmentQuestion.java           ← LLM 生成的题目模型
│   └── AnswerEvaluationResponse.java     ← 评测结果模型
└── service/
    └── AssessmentService.java            ← 核心服务：LLM 调用 + 交叉验证

frontend/src/app/
├── models/
│   └── algorithm.models.ts               ← 新增 4 个 TypeScript 类型
├── services/
│   └── algorithm.service.ts              ← 新增 2 个 API 方法
├── store/
│   └── algorithm.store.ts                ← 新增 3 个评估相关信号
└── components/
    └── assessment-container/
        ├── assessment-container.component.ts   ← 容器组件
        └── assessment-settings/
            ├── assessment-settings.component.ts   ← 设置面板组件
            └── assessment-settings.component.html ← 设置面板模板
```

### 4.2 修改文件

```
backend/src/main/java/com/algorithmviz/
├── controller/AlgorithmController.java   ← 新增 2 个端点，注入 AssessmentService
└── resources/application.properties      ← 新增 ai.assessment.* 配置

frontend/src/app/
├── components/assessment/
│   ├── assessment.component.ts           ← 接受 @Input() 动态题目，AI 评测模式
│   └── assessment.component.html         ← 新增生成中/评测中加载状态
└── app.component.ts / .html              ← 引用改为 AssessmentContainerComponent
```

---

## 5. 数据流

### 5.1 出题流程

```
用户配置参数 → 前端 POST /assessment/generate
    → AssessmentService.generateQuestions()
        → buildGenerationPrompt() 构造 System + User Prompt
        → callLLM() 调用 DeepSeek-V3.2
        → extractJson() 自动剥离 Markdown 标记
        → parseQuestions() 解析为 List<AssessmentQuestion>
        → crossValidate() 对 state-fill/table-fill 题运行实际算法修正答案
    → 返回 N 道验证后的题目
    → 前端展示第一题
```

### 5.2 评测流程

```
用户输入答案 → 前端 POST /assessment/evaluate
    → AssessmentService.evaluateAnswer()
        → buildEvaluationPrompt() 构造题目 JSON + 用户答案
        → callLLM() 调用 DeepSeek-V3.2
        → parseEvaluation() 解析为 AnswerEvaluationResponse
    → 返回 { correct, feedback, correctAnswer, confidence }
    → 前端展示评测结果 + 个性化反馈
```

### 5.3 交叉验证流程

```
仅对 state-fill 和 table-fill 题型执行：
    提取 question.algorithm + question.inputParams
        ↓
    分发到对应 Service：
        sorting → SortingService.generateSteps()
        search  → SearchService.generateSteps()
        dp      → DPService.generateSteps()
        backtracking → BacktrackingService.generateSteps()
        graph   → 跳过（图结构重建复杂）
        ↓
    取 steps[targetStepIndex] 的指定字段（array/dp/board）
        ↓
    与 LLM 生成的 answer 比对
        ├── 一致 → validated=true，保留 LLM 答案
        └── 不一致 → validated=true，用算法执行结果替换
```

---

## 6. 降级与容错

### 6.1 API 不可用

| 场景 | 表现 |
|------|------|
| API Key 未配置 | 点击"AI 出题"后返回 503，提示用户使用固定题模式 |
| API 超时/网络错误 | 自动降级为经典固定 5 题，用户可正常使用 |
| 评测 API 不可用 | 单题评测降级为原有的字符串匹配逻辑 |

### 6.2 LLM 输出异常

| 场景 | 对策 |
|------|------|
| LLM 输出包含 Markdown 标记 | `extractJson()` 自动剥离 ```` ```json ```` 包裹 |
| LLM 返回错误 JSON | `parseQuestions()` 返回空列表，提示用户重试 |
| LLM 生成的答案与算法执行结果不一致 | 交叉验证以算法执行结果覆盖 LLM 答案 |
| 单道题解析失败 | 丢弃该题，不阻塞其他题目 |

### 6.3 边界条件

| 条件 | 行为 |
|------|------|
| 未选择任何分类 | "开始测试"按钮禁用，显示红色提示 |
| 只选了一种题型 | LLM 生成的题目全部使用该题型 |
| 题数设为 1 | 生成 1 道题，提交后直接显示总分 |
| 题数设为 15 | 生成 15 道题，可能耗时较长（15-30 秒） |
| 网络中断后恢复 | 前端保留已提交的题目结果，可继续作答 |

---

> **相关文档**：
> - 改造方案详见 `docs/assessment-llm-enhancement-plan.md`
> - 项目全貌详见 `docs/comprehensive-project-documentation.md`
