# AgentForge Studio 架构设计文档

> **版本**: 0.1.0  
> **生成方式**: 基于现有代码逆向分析  
> **日期**: 2026-10-23

---

## 1. 概述

AgentForge Studio 是 AgentForge 框架的 **可视化开发环境 (Visual Development Environment)**，面向 AI Agent 的全生命周期管理提供 Web 端图形化工作台。核心能力包括：Graph 可视化编排、执行监控、Agent/Capability/Skill 工作区、评估、记忆/知识/RAG 管理、MCP 集成、治理审批、可观测性、调试与进化等。

### 1.1 设计目标

| 编号 | 目标 | 实现策略 |
|------|------|----------|
| FE-03 | API 驱动，不直操底层 fetch | 统一 `AgentForgeApiClient` 封装 |
| FE-05 | 消除手动 `useState + useEffect` 数据获取 | TanStack Query 统一缓存层 |
| FE-06 | 无障碍可访问性 | ARIA 属性 + 键盘焦点测试工具 |
| FE-07 | 国际化 | 轻量 i18n 模块，支持 zh-CN / en-US |
| FE-08 | 路由级代码分割 + 供应商分块 | `React.lazy` + Rollup `manualChunks` |
| GRP-UI | 统一设计系统 | `@agentforge/ui` 组件库 + CSS 变量 Token |
| AF-STUDIO-01 | 路由规范 | 25+ 页面路由按功能域分组 |
| CE-02 | E2E 冒烟测试 | Playwright 自动化导航/渲染/可访问性/错误处理 |

---

## 2. 技术栈

| 层次 | 技术 | 版本约束 |
|------|------|----------|
| **运行时** | Node.js | ≥ 22.0.0 |
| **包管理** | pnpm (workspace monorepo) | 10.28.2 |
| **构建工具** | Vite 5 | ESM, HMR |
| **语言** | TypeScript 5.3+ | strict 模式, ES2020 target |
| **UI 框架** | React 18 | JSX react-jsx |
| **路由** | react-router-dom 6.20+ | 嵌套路由 + lazy |
| **服务端状态** | @tanstack/react-query 5 | 缓存/去重/后台刷新 |
| **客户端状态** | Zustand 4.4 | 3 个独立 Store |
| **图形引擎** | React Flow 11 | 节点/边/画布交互 |
| **样式** | Tailwind CSS 3.4 + CSS Variables | dark mode via class |
| **组件基元** | Radix UI (Slot/Checkbox/Radio/Dialog/Dropdown/...) | Headless 无障碍 |
| **图标** | lucide-react | — |
| **代码高亮** | Shiki | — |
| **测试** | Vitest 1 + Testing Library + jsdom | 单元测试 |
| **E2E** | Playwright 1.48+ | 冒烟测试 |
| **组件文档** | Storybook 8 | UI 包独立开发 |

---

## 3. Monorepo 结构

```
agentforge-studio/                    # pnpm workspace root
├── apps/
│   └── studio/                       # @agentforge/studio — 主应用
│       ├── src/
│       │   ├── components/           # 应用级组件 (AuthGuard)
│       │   ├── hooks/                # TanStack Query 数据获取层
│       │   ├── i18n/                 # 国际化翻译表
│       │   ├── layouts/              # 页面壳 (MainLayout)
│       │   ├── lib/                  # 工具函数 (cn)
│       │   ├── pages/                # 25+ 页面组件
│       │   ├── stores/               # Zustand 状态管理
│       │   ├── styles/               # 全局 CSS (设计 Token)
│       │   ├── test/                 # 单元测试
│       │   ├── test-utils/           # 无障碍测试工具
│       │   ├── App.tsx               # 路由配置
│       │   └── main.tsx              # 应用入口
│       ├── e2e/                      # Playwright E2E
│       └── dist/                     # 构建产物
├── packages/
│   ├── model/                        # @agentforge/model — 类型定义层
│   ├── api-client/                   # @agentforge/api-client — REST/SSE 客户端
│   ├── ui/                           # @agentforge/ui — GRP-UI 组件库
│   └── graph-editor/                 # @agentforge/graph-editor — 图编辑器引擎
├── scripts/                          # 工程脚本
├── pnpm-workspace.yaml
├── tsconfig.base.json                # 共享 TS 编译配置
└── package.json                      # workspace 根脚本
```

### 3.1 包依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                    @agentforge/studio (App)                  │
│  depends on: model, api-client, ui, graph-editor            │
└──────────┬──────────────┬──────────────┬────────────────────┘
           │              │              │
     ┌─────▼─────┐  ┌────▼────┐  ┌──────▼──────┐
     │ @agentforge│  │@agentforge│  │@agentforge   │
     │ /ui        │  │ /graph-  │  │ /api-client  │
     │            │  │ editor   │  │              │
     └────────────┘  └────┬─────┘  └──────┬───────┘
                          │               │
                    ┌─────▼───────────────▼─────┐
                    │    @agentforge/model       │
                    │  (所有包的公共类型基础)       │
                    └───────────────────────────┘
```

---

## 4. 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌──────────────┐  │
│  │  Pages   │  │ Layouts  │  │Components │  │  @agentforge/ │  │
│  │  (25+)   │  │(MainLyt) │  │ (AuthGuard)│  │  ui (78+)   │  │
│  └────┬─────┘  └────┬─────┘  └─────┬─────┘  └──────┬───────┘  │
│       │              │              │                │          │
├───────┴──────────────┴──────────────┴────────────────┴──────────┤
│                      Data Fetching Layer                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  TanStack Query Hooks (use-graphs, use-executions, ...)  │   │
│  │  - 自动缓存 / 去重 / 后台刷新 / 乐观更新                    │   │
│  │  - Query Key 策略: [domain, id, sub-resource]             │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                            │                                     │
├────────────────────────────┴─────────────────────────────────────┤
│                       API Client Layer                           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AgentForgeApiClient                                      │   │
│  │  - 60+ 类型化 API 方法 (CRUD/Action)                      │   │
│  │  - OpenAPI 生成类型校验 (ApiSchemas)                       │   │
│  │  - HttpClient: fetch 封装, 自动注入 Auth + Tenant          │   │
│  │  - ExecutionEventSource: SSE 实时事件流 + 断线重连          │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                            │                                     │
├────────────────────────────┴─────────────────────────────────────┤
│                        State Management                          │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │ AuthStore  │  │ TenantStore  │  │   EditorStore        │    │
│  │ (Zustand)  │  │  (Zustand)   │  │    (Zustand)         │    │
│  │ token/user │  │ tenantId     │  │ selectedNode/Edge    │    │
│  │ session    │  │              │  │ editorMode/panel     │   │
│  └────────────┘  └──────────────┘  └──────────────────────┘    │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│                       Domain Model Layer                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  @agentforge/model                                        │   │
│  │  - 31 个类型定义文件，映射后端 GraphDefinition 契约          │   │
│  │  - 覆盖: Graph/Execution/Agent/Capability/Skill/Memory/   │   │
│  │    Knowledge/MCP/Governance/Observability/Evaluation/      │   │
│  │    Checkpoint/Diff/Search/HITL/Service/Deployment/         │   │
│  │    Evolution/Debugger/Workspace/Multimodal                 │   │
│  │  - 生成类型: OpenAPI → ApiSchemas (编译时契约校验)          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│                    Graph Editor Engine Layer                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  @agentforge/graph-editor                                  │   │
│  │  - Nodes: 5 种专用渲染器 + 1 通用渲染器                     │   │
│  │  - Commands: 8 种命令 + CommandHistory (Undo/Redo)         │   │
│  │  - Serialization: GraphDefinition ↔ React Flow 双向转换     │   │
│  │  - Round-trip 保真性验证                                    │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  AgentForge Server    │
                │  (localhost:8080)     │
                │  REST API + SSE       │
                └───────────────────────┘
```

---

## 5. 各包详细设计

### 5.1 @agentforge/model — 类型定义层

**职责**: 与后端 AgentForge Server 的 JSON 契约保持 1:1 映射，前端不得创建并行语义类型。

**模块划分** (31 个文件):

| 模块组 | 文件 | 覆盖领域 |
|--------|------|----------|
| 核心图模型 | `graph-definition.ts`, `node-definition.ts`, `edge-definition.ts`, `node-type.ts`, `edge-type.ts`, `reference.ts`, `binding.ts`, `canvas-state.ts`, `descriptors.ts` | Graph/Node/Edge 定义、类型枚举、引用绑定 |
| 执行模型 | `execution.ts` | Execution 状态/事件/请求/编译结果/验证结果 |
| Agent 工作区 | `agent-view.ts` | Agent 概览/定义/版本/身份/契约/模型/策略 |
| Capability 工作区 | `capability-view.ts` | Capability 详情/安全/超时/重试/策略/健康/调用审计 |
| Skill 工作区 | `skill-view.ts` | Skill 生命周期/契约/依赖/安全/评估/版本/治理/审计/调用 |
| 执行增强 | `execution-view.ts` | Timeline/AgentLoop/Context/Trace/ToolCall/ModelCall/TokenUsage |
| 评估 | `evaluation.ts` | Dataset/TestCase/Criteria/Run/Report/Regression |
| 记忆 | `memory-view.ts` | MemoryRecord/Query/Candidate/Audit/Metrics |
| 知识/RAG | `knowledge-view.ts` | Source/Document/Chunk/Retrieval/Citation |
| MCP | `mcp-view.ts` | Server/Tool/Resource/Prompt/Connection/Health |
| 治理 | `governance-view.ts` | Policy/Decision/Violation/Approval/Audit/Risk |
| 可观测性 | `observability-view.ts` | Metrics/Series/ModelDashboard/CapabilityHealth/Filter/MultiAgentTree |
| 检查点/恢复 | `checkpoint-view.ts` | Checkpoint/Recovery/Conflict/Snapshot |
| 版本/差异 | `diff-view.ts` | Diff/GraphDiff/Version/ReleaseGate |
| 搜索 | `search-view.ts` | SearchRequest/SearchResponse |
| 人工审批 | `hitl-view.ts` | HumanApproval/Decision/Summary |
| 服务通道 | `service-view.ts` | AgentService/Session/Conversation/Channel/Health |
| 部署 | `deployment-view.ts` | Deployment/Runtime/Environment/Release/Quota |
| 进化 | `evolution-view.ts` | EvolutionCandidate/Change/Evidence/Problem/Policy/Lineage |
| 调试 | `debugger-view.ts` | DebugSession/Breakpoint/Cursor/Step/Event |
| 工作区沙箱 | `workspace-sandbox-view.ts` | Workspace/Sandbox/Artifact/ResourceLimits |
| 多模态 | `multimodal-view.ts` | Content/Image/RealtimeSession/Transport/Event |
| 生成类型 | `generated/api-types.ts` | OpenAPI 生成的 paths/components/operations/schemas |

**关键设计约束**:
- 所有类型均为 `export type`（纯类型，无运行时开销）
- 通过 `openapi-typescript` 从 `docs/api/openapi.yaml` 自动生成 API 类型
- 手动视图类型与生成类型共存，通过 `as unknown as` 桥接

---

### 5.2 @agentforge/api-client — REST/SSE 客户端

**职责**: 封装与 AgentForge Server 的所有 HTTP 通信。

**架构**:

```
AgentForgeApiClient (568 行, 60+ 方法)
    │
    ├── HttpClient (91 行)
    │   ├── fetch 封装
    │   ├── 自动注入: Authorization (Bearer) + X-Tenant-Id
    │   ├── 身份切换检测 (请求前/后比对 token/tenantId)
    │   ├── 统一错误处理 → ApiError
    │   └── 超时控制 (AbortSignal.timeout)
    │
    ├── ExecutionEventSource (166 行)
    │   ├── SSE 实时事件流 (ReadableStream)
    │   ├── 断线自动重连 (3s 退避)
    │   ├── 事件序号追踪 (lastSequence) + 重放
    │   ├── 缓冲区大小限制 (1MB)
    │   ├── 事件类型订阅 (按 type / "*" 通配)
    │   └── 身份切换安全校验
    │
    └── ApiError (config.ts)
        ├── HTTP 状态码分类 (4xx/5xx/401/403/404/409)
        └── Toast 消息生成 (toToastMessage)
```

**API 方法分组**:

| 分组 | 方法数 | 端点前缀 |
|------|--------|----------|
| Graph CRUD | 7 | `/api/v1/graphs` |
| 验证/编译 | 2 | `/api/v1/graphs/validate`, `/compile` |
| 执行管理 | 7 | `/api/v1/executions` |
| 执行事件 | 1 | `/api/v1/executions/:id/events` |
| Agent 工作区 | 4 | `/api/v1/agents` |
| Capability 工作区 | 4 | `/api/v1/capabilities` |
| Skill 工作区 | 10 | `/api/v1/skills` |
| 评估 | 6 | `/api/v1/evaluations` |
| 记忆 | 4 | `/api/v1/memories` |
| 知识/RAG | 5 | `/api/v1/knowledge` |
| MCP | 6 | `/api/v1/mcp` |
| 治理 | 6 | `/api/v1/governance` |
| 可观测性 | 4 | `/api/v1/observability` |
| 检查点/恢复 | 5 | `/api/v1/executions/:id/checkpoints` |
| 版本/差异 | 4 | `/api/v1/diff`, `/api/v1/versions` |
| 搜索 | 1 | `/api/v1/search` |
| 人工审批 | 3 | `/api/v1/human-approvals` |
| 执行增强 | 6 | `/api/v1/executions/:id/{timeline,trace,...}` |
| 多Agent | 1 | `/api/v1/executions/:id/agent-tree` |
| 通用 | 1 | `request<T>()` (限 `/api/v1/` 路径) |

**安全设计**:
- 每次请求从 `getAuth()` 读取最新身份快照，避免闭包陈旧
- 请求发出后和响应返回前双重校验身份一致性
- SSE 流持续校验 `current()` 身份，身份切换立即中止
- 路径白名单：`request()` 仅允许 `/api/v1/` 前缀

---

### 5.3 @agentforge/ui — GRP-UI 组件库

**职责**: 基于 macOS 设计语言的组件库，提供统一视觉语言。

**组件清单** (78 个文件, 30+ 组件):

| 分类 | 组件 |
|------|------|
| **布局** | Box, Stack (VStack/HStack), Flex, Card (Header/Title/Description/Content/Footer), Sidebar (Header/Content/Footer/Section/Item/Collapse) |
| **交互** | Button, Input, Checkbox, RadioGroup, Select, Switch, Textarea, Label, Tabs, Separator, ScrollArea, Tooltip |
| **数据展示** | Table (Header/Body/Footer/Head/Row/Cell/Caption), Typography, Badge, Avatar, Timeline, Descriptions, CodeBlock, InlineCode |
| **导航** | Link, Sidebar, Breadcrumb |
| **反馈** | Spinner, Alert, Progress, Skeleton, Empty |
| **覆盖层** | Dialog, Drawer, DropdownMenu |
| **主题** | ThemeProvider (system/light/dark), themeTokens (CSS 变量映射) |

**设计 Token 系统**:

```
CSS Variables (global.css)
    ├── 颜色: --mac-blue-50~700, --color-primary/muted/border/...
    ├── 间距: --spacing-xs~2xl (4px 网格)
    ├── 圆角: --radius-sm~xl
    ├── 阴影: --shadow-sm~xl
    ├── 字体: --font-sans (SF Pro), --font-mono (SF Mono)
    ├── 字号: --text-xs~2xl
    └── 动画: --duration-fast/normal/slow, --ease-mac/mac-bounce
```

**Dark Mode**: 通过 `.dark` class 切换，覆盖 `--color-background/foreground/muted/border` 变量。

---

### 5.4 @agentforge/graph-editor — 图编辑器引擎

**职责**: 基于 React Flow 的可视化 Graph 编辑器，处理画布渲染/交互/序列化，**不处理**语义验证/编译/执行。

**架构**:

```
@agentforge/graph-editor
│
├── nodes/                        # 节点渲染层
│   ├── AgentForgeNode.tsx        # 通用节点 (ACTION/TOOL/LLM/CONDITION/...)
│   ├── StartEndNode.tsx          # START/END 专用渲染
│   ├── AgentNode.tsx             # AGENT 专用渲染
│   ├── CapabilityNode.tsx        # CAPABILITY 专用渲染
│   ├── SubgraphNode.tsx          # SUBGRAPH 专用渲染
│   └── node-data.ts             # NodeData 接口 + 16 种节点类型注册表
│
├── commands/                     # 命令模式 (Undo/Redo)
│   ├── graph-commands.ts         # 8 种命令: Add/Delete/Update Node/Edge, Move, BatchMove
│   └── command-history.ts        # 命令历史栈 + 观察者模式通知
│
└── serialization/                # 序列化层
    └── graph-serialization.ts    # GraphDefinition ↔ React Flow 双向转换
                                  # + CanvasState 提取 + Round-trip 保真验证
```

**节点类型注册表** (16 种):

| 分类 | 类型 | 图标 | 颜色 |
|------|------|------|------|
| flow | START, END | ▶, ⏹ | #22c55e, #ef4444 |
| action | ACTION | ⚡ | #3b82f6 |
| control | CONDITION, SWITCH, PARALLEL, JOIN, LOOP, ITERATOR, WAIT, SUBGRAPH | ◇⊞⫸⫷↻⇄⏳▣ | 各异 |
| integration | AGENT, CAPABILITY, TOOL, LLM, HUMAN | 🤖🔧🛠🧠👤 | 各异 |

**序列化核心原则**:
- `GraphDefinition` 是 **权威模型** (canonical model)
- React Flow 的 nodes/edges 是 **视图投影** (view projection)
- `serialize`: React Flow → GraphDefinition
- `deserialize`: GraphDefinition → React Flow
- `roundTripEquals`: 确保序列化往返不丢失语义信息

---

## 6. 应用层架构

### 6.1 路由系统

**路由规范 (AF-STUDIO-01)**: 25+ 页面，全部使用 `React.lazy` 代码分割。

```
/ ──────────────────→ /dashboard (重定向)
/login ─────────────→ LoginPage (公开路由)

─── 以下均需 AuthGuard 保护 ───

Core:
  /dashboard ───────→ DashboardPage
  /graphs ──────────→ GraphListPage
  /graphs/:id ──────→ GraphEditorPage
  /graphs/:id/:ver/edit → GraphEditorPage
  /executions ──────→ ExecutionListPage
  /executions/:id ──→ ExecutionDetailPage
  /executions/:id/checkpoints → CheckpointPage
  /agents ──────────→ AgentWorkspacePage
  /capabilities ────→ CapabilityWorkspacePage
  /skills ──────────→ SkillWorkspacePage

Intelligence:
  /memory ──────────→ MemoryPage
  /knowledge ───────→ KnowledgePage
  /mcp ─────────────→ McpPage

Quality:
  /evaluation ──────→ EvaluationPage
  /observability ───→ ObservabilityPage

Governance:
  /governance ──────→ GovernancePage
  /human-in-the-loop → HumanInTheLoopPage

Operations:
  /checkpoints ─────→ CheckpointPage
  /versions ────────→ VersionDiffPage
  /search ──────────→ SearchPage

Advanced:
  /services ────────→ ServiceChannelPage
  /deployments ─────→ DeploymentPage
  /evolution ───────→ EvolutionPage
  /debugger ────────→ DebuggerPage
  /workspaces ──────→ WorkspaceSandboxPage
  /multimodal ──────→ MultimodalPage

* ──────────────────→ / (重定向)
```

### 6.2 认证与权限隔离

```
┌─────────────────────────────────────────────────────┐
│                    AuthGuard                         │
│                                                     │
│  1. 检查 restoring 状态 (等待 session 恢复)          │
│  2. 检查 isAuthenticated (token + user 非空)         │
│  3. 未认证 → 重定向 /login?redirect=<path>           │
│  4. 已认证 → 创建身份隔离的 QueryClient              │
│     key = crypto.randomUUID()                       │
│     scope = [token, tenantId, userId]               │
│  5. 身份切换时自动销毁旧 QueryClient 缓存             │
└─────────────────────────────────────────────────────┘
```

**认证流程**:
1. `LoginPage` → `authStore.login(username, password)` → Basic Auth POST `/api/auth/login`
2. 服务端返回 `{ token, user }` → 存入 `localStorage`
3. `AuthGuard` 内创建独立 `QueryClient`，按身份隔离缓存
4. 登出时清除 `localStorage` + 重置 Store

### 6.3 状态管理

采用 **三 Store 分离** 策略：

| Store | 职责 | 持久化 |
|-------|------|--------|
| `AuthStore` | token / user / loading / error / session 恢复 | localStorage |
| `TenantStore` | 当前租户 ID (默认 "default") | 内存 |
| `EditorStore` | 图编辑器局部 UI 状态 (选中节点/边/面板/模式) | 内存 |

**关键设计**: 服务端状态 (graphs, executions 等) **不使用 Zustand**，完全由 TanStack Query 管理。Zustand 仅负责客户端 UI 状态。

### 6.4 数据获取层

```
Pages ──→ TanStack Query Hooks ──→ AgentForgeApiClient ──→ HttpClient ──→ Server
              │
              ├── useQuery: 自动缓存/去重/后台刷新
              │   ├── staleTime: 30s
              │   ├── refetchInterval: 按状态动态调整
              │   │   ├── RUNNING/PENDING → 3s 轮询
              │   │   ├── 事件流 → 2s 轮询
              │   │   └── HITL 审批 → 10s 轮询
              │   └── enabled: 条件查询 (!!id)
              │
              └── useMutation: 乐观更新
                  └── onSuccess → invalidateQueries
```

**Query Key 策略**: `[domain, id?, subResource?, params?]`

示例:
- `["graphs"]` — 图列表
- `["graphs", id, version]` — 特定图
- `["executions", id, "state"]` — 执行状态
- `["executions", id, "events", after]` — 执行事件
- `["observability", "metrics"]` — 可观测性指标

### 6.5 页面布局

```
┌──────────────────────────────────────────────────────────────┐
│  Header: "AgentForge Studio" | Tenant Badge | Theme | User  │
├──────────┬───────────────────────────────────────────────────┤
│ Sidebar  │  Main Content Area                                │
│          │                                                   │
│ Core     │  ┌─────────────────────────────────────────────┐  │
│  Dashboard│  │                                             │  │
│  Graphs   │  │         <Outlet />                          │  │
│  Agents   │  │                                             │  │
│  Capabilities│  │   各页面组件渲染区                         │  │
│  Skills   │  │                                             │  │
│  Executions│  │                                             │  │
│          │  └─────────────────────────────────────────────┘  │
│ Intelligence│                                                 │
│  Memory   │                                                   │
│  Knowledge│                                                   │
│  MCP      │                                                   │
│          │                                                   │
│ Quality  │                                                   │
│  Evaluation│                                                  │
│  Observability│                                               │
│          │                                                   │
│ Governance│                                                  │
│  Governance│                                                  │
│  HITL    │                                                   │
│          │                                                   │
│ Operations│                                                  │
│  Checkpoints│                                                 │
│  Versions │                                                  │
│  Search  │                                                   │
│          │                                                   │
│ Advanced │                                                   │
│  Services│                                                   │
│  Control Plane│                                              │
│  Evolution│                                                  │
│  Debugger│                                                   │
│  Workspace│                                                  │
│  Multimodal│                                                 │
└──────────┴───────────────────────────────────────────────────┘
```

---

## 7. 页面功能及路由详述

> 共 27 个页面组件，27 条路由，按侧边栏 6 大功能域分组。所有页面均通过 `React.lazy` 实现路由级代码分割。

### 7.1 公开路由

| 路由 | 页面 | 行数 | 功能描述 |
|------|------|------|----------|
| `/login` | `LoginPage` | 157 | 认证登录页。Basic Auth 表单 (username/password)，POST `/api/auth/login`，登录成功后跳转 `/dashboard`，支持 `redirect` 参数回跳 |

### 7.2 Core — 核心工作台

| 路由 | 页面 | 行数 | 功能描述 |
|------|------|------|----------|
| `/dashboard` | `DashboardPage` | 199 | **总览仪表盘**。并行请求 8 个 API 端点获取计数 (graphs/executions/agents/capabilities/skills/mcpServers/policies/evalDatasets)，以 Card 网格展示各模块入口及统计 |
| `/graphs` | `GraphListPage` | 168 | **Graph 列表管理**。表格展示 ID/Name/Version/Status/Nodes/Edges/Created，支持创建/删除/编辑/刷新操作，点击行跳转至图编辑器 |
| `/graphs/:graphId` | `GraphEditorPage` | 526 | **图编辑器** (最复杂页面)。三栏布局: Palette (节点面板) / Canvas (React Flow 画布) / Properties (属性面板)。集成 CommandHistory (Undo/Redo) + 5 种专用节点渲染器 + Validate/Compile/Save 操作 + 编译诊断高亮错误节点 |
| `/graphs/:graphId/:version/edit` | `GraphEditorPage` | (同上) | 指定版本的图编辑器入口 |
| `/executions` | `ExecutionListPage` | 123 | **执行列表**。表格展示执行记录，支持 Pause/Resume/Cancel 控制操作 |
| `/executions/:executionId` | `ExecutionDetailPage` | 528 | **执行详情增强页**。9 个 Tab: Overview (概览) / Timeline (时间线) / Trace (调用链) / State (状态快照) / Context (上下文) / AgentLoop (Agent 循环) / Tools (工具调用) / Models (模型调用) / Events (SSE 实时事件流 + 断线重连 + 序号追踪) |
| `/executions/:executionId/checkpoints` | `CheckpointPage` | 212 | 执行级检查点 (详见 Operations 组) |
| `/agents` | `AgentWorkspacePage` | 250 | **Agent 工作区**。4 个 Tab: List (Agent 列表) / Overview (选中 Agent 概览) / Definition (定义详情) / Versions (版本列表) |
| `/capabilities` | `CapabilityWorkspacePage` | 266 | **Capability 工作区**。4 个 Tab: Catalog (目录) / Detail (详情: Schema/Provider/Security/Timeout&Retry/Health 五区块) / Invocation (调用: 必须走真实调用链) / Audit (调用审计) |
| `/skills` | `SkillWorkspacePage` | 465 | **Skill 工作区**。9 个 Tab: Overview / Contract (契约) / Dependencies (依赖) / Security (安全: Secret 永不直接显示) / Evaluation (评估) / Versions (版本) / Governance (治理) / Usage (用量) / Executions (执行记录) |

### 7.3 Intelligence — 智能层

| 路由 | 页面 | 行数 | Tab / 功能描述 |
|------|------|------|----------------|
| `/memory` | `MemoryPage` | 346 | **记忆查看器** (§31)。7 个 Tab: Search (实时查询) / Candidates (候选记忆) / Active (活跃) / Expired (过期) / Archived (归档) / Audit (审计日志) / Metrics (9 项指标: 命中率/延迟/大小/...)) |
| `/knowledge` | `KnowledgePage` | 273 | **知识/RAG 查看器** (§32-33)。5 个 Tab: Sources (知识源) / Documents (文档) / Chunks (分块) / Retrieval (检索流水线: Scope Filter→Permission→Keyword→Vector→Merge→Dedup→Rerank→Policy Filter→TopK 共 9 级) / Citations (Answer→Source 溯源) |
| `/mcp` | `McpPage` | 277 | **MCP 工作区** (§34-35)。6 个 Tab: Servers (MCP 服务器) / Tools (工具列表) / Resources (资源) / Prompts (提示词) / Health (健康检查) / Audit (审计)。核心规则: Discovered ≠ Enabled |

### 7.4 Quality — 质量保障

| 路由 | 页面 | 行数 | Tab / 功能描述 |
|------|------|------|----------------|
| `/evaluation` | `EvaluationPage` | 393 | **评估工作区** (§37-39)。8 个 Tab: Datasets (数据集) / Cases (测试用例) / Runs (执行记录) / Results (结果) / Findings (发现) / Regression (回归检测) / Benchmark (基准测试) / Report (报告)。Datasets 和 Runs 加载真实后端数据 |
| `/observability` | `ObservabilityPage` | 217 | **可观测性仪表盘** (§61-63)。展示 Overview Metrics (6 项指标) + Filters (6 维度过滤) + Model Dashboard (模型面板) + Capability Health (能力健康) |

### 7.5 Governance — 治理域

| 路由 | 页面 | 行数 | Tab / 功能描述 |
|------|------|------|----------------|
| `/governance` | `GovernancePage` | 302 | **治理工作区** (§40-41)。6 个 Tab: Policies (策略列表) / Decisions (决策溯源) / Violations (违规记录) / Approvals (审批) / Audit (审计) / Risk (4 项汇总风险指标) |
| `/human-in-the-loop` | `HumanInTheLoopPage` | 317 | **人工审批** (§42)。3 个 Tab: Pending (待审批: Approve/Reject + 原因输入 + 10s 自动刷新 + 倒计时显示 + 风险等级标记) / History (历史) / Summary (汇总) |

### 7.6 Operations — 运维域

| 路由 | 页面 | 行数 | Tab / 功能描述 |
|------|------|------|----------------|
| `/checkpoints` | `CheckpointPage` | 212 | **检查点与恢复** (§43-44)。3 个 Tab: Checkpoints (列表: ID/Sequence/GraphVersion/PlanVersion/Checksum/Status，支持 Inspect/Compare/Recover) / Recovery (4 种恢复决策: RESUME / RETRY_FROM_CHECKPOINT / RESTART / ABORT) / StateConflict (并行执行冲突查看: Node A/B Delta + Strategy + Resolution) |
| `/versions` | `VersionDiffPage` | 257 | **版本与差异** (§45-48)。3 个 Tab: Versions (版本生命周期 7 阶段: Draft→Validated→Tested→Evaluated→Approved→Released→Deprecated) / Diff (差异对比: 支持 AGENT/GRAPH/CAPABILITY/POLICY/PROMPT/SCHEMA 6 种实体类型，Graph 类型额外展示双面板可视化) / Release (Release Gate 7 步流水线: Definition Validation→Unit Test→Evaluation→Security Test→Governance→Approval→Release) |
| `/search` | `SearchPage` | 96 | **全局搜索** (§49)。搜索框 + Enter 触发，POST `/api/v1/search`，结果表格展示 Type/ID/Name/Score |

### 7.7 Advanced — 高级域

| 路由 | 页面 | 行数 | Tab / 功能描述 |
|------|------|------|----------------|
| `/services` | `ServiceChannelPage` | 187 | **服务通道** (Enhance3 §3)。4 个 Tab: Services (Agent 服务列表: Namespace/Name/Version 身份) / Sessions (会话: create→activate→close 生命周期) / Conversations (对话消息: role-based threading) / Health (5 项指标: Active Services/Sessions/Total Requests/Avg Latency/Error Rate) |
| `/deployments` | `DeploymentPage` | 219 | **控制平面** (Enhance3 §2)。5 个 Tab: Deployments (部署列表: Namespace/Name/Strategy/Rollout) / Instances (运行时实例: Status/Endpoint/Region/CPU/Memory/Threads) / Environments (环境: Development/Staging/Production/Test) / Releases (发布: canary/monitoring 阶段 + Rollout %) / Quotas (资源配额: Instances/CPU/Memory used/max) |
| `/evolution` | `EvolutionPage` | 290 | **进化工作区** (Enhance3 §4)。7 个 Tab: Candidates (进化候选: Agent/Score/Changes/Evidence) / Changes (变更范围: Scope/Target/Description) / Evidence (证据收集: Type/Source/Value) / Problems (问题检测: Category/Severity) / Validation (验证门控: Gate/Passed/Score) / Lineage (进化谱系树: Generation/Parent) / Policy (进化策略: MaxCandidates/MinScore/RequireEvidence/AutoApprove) |
| `/debugger` | `DebuggerPage` | 199 | **调试器工作区** (Enhance4)。4 个 Tab + 底部控制栏: Sessions (调试会话) / Breakpoints (断点: 支持 Node Entry/Exit、Edge Traversal、Tool/Model Call、Policy、Error 等类型) / Cursor (执行游标: EventID/Sequence/CursorType/Node) / Events (调试事件) + Step Controls (Continue/Step Over/Step Into/Step Out/Pause/Stop) |
| `/workspaces` | `WorkspaceSandboxPage` | 158 | **工作区与沙箱** (Enhance7)。3 个 Tab: Workspaces (隔离工作区: Scope=agent/graph/session/tenant/global) / Sandboxes (沙箱: Isolation/CPU Limit/Memory Limit/Timeout/Expires) / Artifacts (产物: Files/Data/Model Outputs/Logs/Checkpoints，展示 Type/Size/Checksum) |
| `/multimodal` | `MultimodalPage` | 183 | **多模态内容** (Enhance3 §3)。4 个 Tab: Content (聚合内容: text/image/audio/video/JSON/binary/file) / Images (图片详情: Width/Height/Format/MIME/Size) / Realtime (实时会话: SSE/WebSocket/Long Poll/gRPC streaming) / Events (实时事件: Type/Sequence/Payload) |

### 7.8 页面复杂度统计

| 复杂度级别 | 页面 | 特征 |
|-----------|------|------|
| **高** (>400行) | GraphEditorPage (526), ExecutionDetailPage (528), SkillWorkspacePage (465), EvaluationPage (393) | 多 Tab + 实时数据 + 复杂交互 |
| **中** (200~400行) | MemoryPage (346), GovernancePage (302), EvolutionPage (290), CapabilityWorkspacePage (266), VersionDiffPage (257), AgentWorkspacePage (250), KnowledgePage (273), McpPage (277), HumanInTheLoopPage (317) | 多 Tab + 数据展示 |
| **中低** (100~200行) | DeploymentPage (219), CheckpointPage (212), ObservabilityPage (217), ServiceChannelPage (187), DebuggerPage (199), MultimodalPage (183), DashboardPage (199), GraphListPage (168), WorkspaceSandboxPage (158), LoginPage (157), ExecutionListPage (123) | 单/双 Tab + 表格 |
| **低** (<100行) | SearchPage (96), AgentListPage (88), CapabilityListPage (88) | 简单列表/搜索 |

---

## 8. 构建与部署架构

### 8.1 Vite 构建配置

```
vite.config.ts
├── 开发服务器
│   ├── port: 3000
│   └── proxy: /api → http://localhost:8080 (后端)
│
├── 路径别名
│   └── @ → ./src
│
├── 构建优化 (Rollup manualChunks)
│   ├── vendor-react: react, react-dom, react-router-dom
│   ├── vendor-query: @tanstack/react-query
│   ├── vendor-radix: @radix-ui/* 组件
│   └── vendor-state: zustand
│
├── 产物
│   ├── outDir: dist
│   ├── sourcemap: true
│   └── chunkSizeWarningLimit: 500KB
│
└── 测试
    ├── globals: true
    ├── environment: jsdom
    └── exclude: e2e/**, node_modules/**
```

### 8.2 开发代理拓扑

```
Browser (:3000)
    │
    ├── /api/* ──proxy──→ AgentForge Server (:8080)
    │                       ├── REST API
    │                       └── SSE Streams
    │
    └── /* ──────────→ Vite Dev Server (HMR)
```

---

## 9. 测试策略

### 9.1 测试金字塔

```
         ┌─────────────┐
         │  E2E (CE-02) │  Playwright — 冒烟测试
         │  4 个维度     │  导航/渲染/可访问性/错误处理
         ├───────────────┤
         │  集成测试      │  Vitest + Testing Library
         │  Store 测试    │  auth-store / editor-store / tenant-store
         │  API 契约测试  │  contract.test.ts (550行)
         │  HTTP 测试     │  http-client.test.ts (240行)
         │  SSE 测试      │  sse-client.test.ts (57行)
         ├───────────────┤
         │  单元测试      │  各组件/模块内部测试
         └───────────────┘
```

### 9.2 E2E 测试覆盖 (smoke.spec.ts)

| 维度 | 测试用例 | 验证点 |
|------|----------|--------|
| **导航** | homepage / graphs / executions / agents / skills | 页面加载不崩溃 |
| **渲染** | graphs table / executions / agents | 数据正确渲染 |
| **可访问性** | html lang 属性 / 键盘焦点 | ARIA 合规 |
| **错误处理** | API 500 / 网络超时 | 页面保持可见不崩溃 |

### 9.3 无障碍测试工具 (a11y.ts)

- `assertAccessible`: 验证 img alt / input label / button name
- `assertKeyboardFocusable`: 验证交互元素键盘可达性

---

## 10. 国际化架构

```
i18n.ts
├── Locale 类型: "en-US" | "zh-CN"
├── translations 字典: Record<Locale, Record<string, string>>
│   ├── 120+ 翻译键 (en-US)
│   └── 120+ 翻译键 (zh-CN)
├── initLocale(): 从 localStorage / 浏览器语言初始化
├── setLocale(locale): 切换并持久化
├── getLocale(): 获取当前语言
└── t(key): 翻译函数，未找到则返回 key 本身

useLocale() Hook:
├── 响应式 locale 状态
├── setLocale 回调
└── t 翻译函数引用
```

---

## 11. 安全架构

| 层面 | 机制 |
|------|------|
| **传输层** | 所有 API 请求携带 `Authorization: Bearer <token>` |
| **登录** | Basic Auth (Base64 编码) POST `/api/auth/login` |
| **多租户隔离** | 每个请求携带 `X-Tenant-Id` 头 |
| **会话持久化** | localStorage 存储 token + user JSON |
| **身份切换检测** | HttpClient 请求前/后双重校验 token/tenantId 一致性 |
| **SSE 安全** | ExecutionEventSource 持续校验身份，切换即中止 |
| **路径白名单** | `request()` 方法仅允许 `/api/v1/` 前缀路径 |
| **缓存隔离** | AuthGuard 按身份创建独立 QueryClient，切换即销毁 |
| **凭证存储** | token 存 localStorage (TOKEN_KEY)，用户信息存 localStorage (USER_KEY) |

---

## 12. 关键设计模式

### 12.1 Command Pattern (命令模式)

用于 Graph Editor 的 Undo/Redo：

```
CommandHistory
├── undoStack: GraphCommand[]
├── redoStack: GraphCommand[]
├── execute(cmd) → 执行 + 入 undo + 清空 redo
├── undo() → 弹出 undo + 反向执行 + 入 redo
├── redo() → 弹出 redo + 执行 + 入 undo
└── onChange(listener) → 观察者模式通知

8 种命令:
├── AddNodeCommand / DeleteNodeCommand / UpdateNodeCommand
├── AddEdgeCommand / DeleteEdgeCommand / UpdateEdgeCommand
├── MoveNodeCommand
└── BatchMoveCommand
```

### 12.2 Repository Pattern (仓库模式)

TanStack Query Hooks 作为数据仓库层：
- 页面不直接调用 API Client，而是通过 Hooks
- Hooks 封装 Query Key / 缓存策略 / 失效逻辑
- 统一的 `useApiClient()` 提供认证感知的客户端实例

### 12.3 Observer Pattern (观察者模式)

- `CommandHistory.onChange()` — 图变更通知
- `ExecutionEventSource.on()` — 事件订阅
- `ExecutionEventSource.onError()` — 错误订阅

### 12.4 Strategy Pattern (策略模式)

- 节点渲染策略: `getReactFlowNodeType()` 根据 NodeType 选择专用渲染器
- 重连策略: `ExecutionEventSource.scheduleReconnect()` 指数退避

### 12.5 Serialization Pattern (序列化模式)

- `GraphDefinition` ↔ React Flow 双向转换
- Round-trip 保真性验证 (`roundTripEquals`)
- CanvasState 独立存储 (位置/视口与语义分离)

---

## 13. 性能优化策略

| 策略 | 实现 |
|------|------|
| **路由级代码分割** | 25+ 页面全部 `React.lazy()` 按需加载 |
| **供应商分块** | 4 个 manualChunks: react/query/radix/zustand |
| **查询缓存** | TanStack Query staleTime=30s，减少重复请求 |
| **条件轮询** | RUNNING/PENDING 3s，事件 2s，审批 10s，非活跃停止 |
| **SSE 替代轮询** | ExecutionEventSource 提供实时事件流 |
| **Suspense 加载** | 全局 Suspense fallback (Spinner) |
| **Bundle 分析** | chunkSizeWarningLimit=500KB |

---

## 14. 目录与文件统计

| 维度 | 数量 |
|------|------|
| Workspace 包数 | 4 (model, api-client, ui, graph-editor) |
| 应用数 | 1 (studio) |
| 页面组件 | 25+ |
| UI 组件文件 | 78 |
| Model 类型文件 | 31 |
| API Client 方法 | 60+ |
| Zustand Store | 3 |
| TanStack Query Hook | 30+ |
| 节点类型 | 16 |
| 编辑命令 | 8 |
| 翻译键 | 120+ × 2 语言 |
| E2E 测试用例 | 10 |
| 路由 | 27 |

---

## 15. 架构决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | Zustand (UI) + TanStack Query (Server) | 职责分离，避免状态同步复杂性 |
| 图形引擎 | React Flow 11 | 成熟的 DAG 可视化，社区活跃 |
| 组件库 | Radix UI + Tailwind + CVA | Headless 无障碍 + 实用优先样式 |
| API 类型 | OpenAPI 生成 + 手写视图类型 | 编译时契约校验，前后端解耦 |
| 国际化 | 自研轻量方案 | 仅 2 语言，无需 i18next 等重量级框架 |
| 构建 | Vite 5 + pnpm workspace | 快速 HMR，monorepo 原生支持 |
| SSE vs WebSocket | SSE (fetch ReadableStream) | 单向推送足够，断线重连简单 |
| 认证缓存隔离 | 按身份创建 QueryClient | 杜绝租户数据泄露 |
