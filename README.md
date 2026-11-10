# AgentForge

**Independent Agent Development Framework and Graph Orchestration Engine**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)

AgentForge 是一个面向企业级 AI Agent 的独立开发框架与图编排引擎，提供从 Agent 定义、编译、执行到可观测的全生命周期管理能力。

## 核心特性

- **Graph 编排引擎** — 声明式 Graph 定义，支持 16 种节点类型与 4 种边类型，实现复杂 Agent 工作流编排
- **编译器与执行计划** — Graph 编译流水线，GraphIR 中间表示，路由优化与版本固定
- **Capability 系统** — 统一能力抽象与调用管线（Resolve → Validate → Authorize → Budget → Execute），支持 HTTP/JDBC/File/MCP 多种适配器
- **多 Provider 支持** — OpenAI、Anthropic、Ollama、MCP 等多种 LLM/Tool Provider 即插即用
- **Multi-Agent 协作** — Agent 间委托（Delegate）、移交（Handoff）与协作（Collaborate），内置预算传播与循环检测
- **A2A 协议** — Agent-to-Agent 互操作协议，支持 Agent Card 发现、能力协商与任务流式传输
- **多模态** — 支持文本、图像、音频、视频、JSON、二进制等多种内容类型的统一消息处理
- **分布式调度** — 内置 Lease/Fencing 机制的分布式 Worker，支持水平扩展
- **持久化与恢复** — 基于 Outbox 模式的事务性执行状态持久化与故障恢复
- **可观测性** — 全链路遥测、执行追踪与诊断
- **RAG 集成** — 支持 PostgreSQL、OpenSearch、Qdrant 等多种向量存储
- **记忆管理** — Agent 记忆存储与检索，支持 PostgreSQL 与 Redis 后端，内置写入策略控制
- **评估框架** — Agent 行为评估与质量度量
- **进化闭环** — 基于生产证据的 Agent 自进化：观察 → 评估 → 生成候选 → 实验验证 → 审批发布
- **控制平面** — 部署管理、环境隔离、发布编排与配额控制
- **多租户安全** — 内置 IAM、权限管理与凭证隔离
- **Studio 可视化** — Web 端 Graph Editor、Execution Console、Debugger、MCP 管理与 Evolution Workspace
- **SDK & CLI** — 嵌入式/远程 SDK 与命令行工具，支持多种项目模板快速启动

## 快速开始

### 环境要求

- Java 17+
- Maven 3.9+（或使用内置 Maven Wrapper）
- Node.js 22+ & pnpm 10+（仅 Studio 前端需要）

### 构建

```bash
# 编译 & 单元测试
./mvnw clean verify -B

# 集成测试（需要 Docker）
./mvnw verify -Pintegration -B

# 安全测试
./mvnw verify -Psecurity -B

# 性能基准测试
./mvnw verify -Pbenchmark -B

# 完整发布构建（含 source/javadoc）
./mvnw clean verify -Prelease -B
```

### 启动服务

```bash
# 使用 Docker Compose 启动全部服务（Server + PostgreSQL + Redis）
cd deploy/docker
docker compose up -d

# 或单独启动后端
./scripts/start-server.sh

# 启动 Studio 前端
cd agentforge-studio && pnpm install && pnpm dev
```

### 模块结构

**核心层**

| 模块 | 说明 |
|------|------|
| `agentforge-bom` | Bill of Materials 依赖管理 |
| `agentforge-kernel` | 核心模型与基础抽象 |
| `agentforge-model` | 数据模型定义 |

**Agent 与编排**

| 模块 | 说明 |
|------|------|
| `agentforge-agent` | Agent 决策循环与生命周期 |
| `agentforge-graph` | Graph 定义与编排 |
| `agentforge-graph-ir` | Graph 中间表示与编译分析 |
| `agentforge-compiler` | Graph 编译器与执行计划生成 |
| `agentforge-runtime` | 执行引擎与节点状态机 |
| `agentforge-event` | 事件模型与分发 |

**Capability 系统**

| 模块 | 说明 |
|------|------|
| `agentforge-capability` | 能力抽象、调用管线与 Skill 注册 |
| `agentforge-capability-http` | HTTP 能力适配器 |
| `agentforge-capability-jdbc` | JDBC 能力适配器 |
| `agentforge-capability-file` | 文件系统能力适配器 |

**Provider 层**

| 模块 | 说明 |
|------|------|
| `agentforge-provider-spi` | Provider SPI 契约 |
| `agentforge-provider-mock` | Mock Provider（测试用） |
| `agentforge-provider-calculator` | Calculator Provider |
| `agentforge-provider-http` | HTTP 通用 Provider |
| `agentforge-provider-agent` | Agent Provider |
| `agentforge-provider-llm` | LLM Provider 基类 |
| `agentforge-provider-mcp` | MCP 协议 Provider |
| `agentforge-provider-openai` | OpenAI Provider |
| `agentforge-provider-anthropic` | Anthropic Provider |
| `agentforge-provider-ollama` | Ollama Provider |
| `agentforge-provider-openai-compatible` | OpenAI Compatible Provider |

**智能与协作**

| 模块 | 说明 |
|------|------|
| `agentforge-multi-agent` | Multi-Agent 协作（委托/移交/协作） |
| `agentforge-a2a` | A2A 互操作协议（Agent Card/任务流） |
| `agentforge-multimodal` | 多模态内容处理（图像/音频/视频） |
| `agentforge-memory` | Agent 记忆管理 |
| `agentforge-memory-postgresql` | PostgreSQL 记忆后端 |
| `agentforge-memory-redis` | Redis 记忆后端 |
| `agentforge-rag` | RAG 检索增强生成 |
| `agentforge-rag-postgresql` | PostgreSQL 向量存储 |
| `agentforge-rag-opensearch` | OpenSearch 向量存储 |
| `agentforge-rag-qdrant` | Qdrant 向量存储 |
| `agentforge-evaluation` | Agent 评估框架 |
| `agentforge-evolution` | Agent 自进化闭环 |

**基础设施**

| 模块 | 说明 |
|------|------|
| `agentforge-persistence` | 持久化抽象层 |
| `agentforge-persistence-postgresql` | PostgreSQL 持久化实现 |
| `agentforge-distributed` | 分布式调度与一致性 |
| `agentforge-security` | 安全、IAM 与多租户 |
| `agentforge-observability` | 遥测与可观测性 |
| `agentforge-workspace` | 工作空间隔离与沙箱 |
| `agentforge-control-plane` | 控制平面（部署/环境/发布） |
| `agentforge-service` | Agent 服务通道（Session/Conversation） |

**应用层**

| 模块 | 说明 |
|------|------|
| `agentforge-sdk` | 嵌入式 / 远程 SDK |
| `agentforge-server` | REST/SSE 服务层 |
| `agentforge-cli` | 命令行工具（项目模板/Doctor/Deploy） |
| `agentforge-debugger` | Agent 调试器 |
| `agentforge-studio` | Web 可视化工作台 |

**质量保障**

| 模块 | 说明 |
|------|------|
| `agentforge-testkit` | 测试工具包 |
| `agentforge-test-fault` | 故障注入测试 |
| `agentforge-benchmark` | 性能基准测试 |
| `agentforge-hardening-tck` | 技术兼容性套件（TCK） |
| `agentforge-architecture-tests` | 架构合规测试 |
| `agentforge-examples` | 示例项目集 |

## 架构概览

```
┌──────────────────────────────────────────────────────────────┐
│                     Studio (Web UI)                           │
├──────────────────────────────────────────────────────────────┤
│                SDK (Embedded / Remote)                        │
│                CLI (Create / Doctor / Deploy)                 │
├──────────────────────────────────────────────────────────────┤
│                Server (REST / SSE API)                        │
├──────────┬──────────┬──────────┬──────────┬──────────────────┤
│ Compiler │ Runtime  │  Event   │   Observability  │ Debugger │
├──────────┴──────────┴──────────┴──────────┴──────────────────┤
│  Multi-Agent  │  A2A  │  Capability Pipeline  │  Multimodal  │
├───────────────┴───────┴───────────────────────┴──────────────┤
│  Graph IR  │  Persistence  │  Distributed  │  Control Plane  │
├────────────┴───────────────┴───────────────┴─────────────────┤
│  Provider SPI │ Memory │ RAG │ Evaluation │ Evolution │ Sec.  │
├───────────────┴────────┴─────┴────────────┴───────────┴──────┤
│                      Kernel / Model                           │
└──────────────────────────────────────────────────────────────┘
```

## 扩展开发

### 自定义 Agent 注册

用户可以通过 `AgentProvider` SPI 将自定义 Agent 注册到平台：

```java
import io.github.regalpine.agentforge.agent.AgentProvider;
import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

public class MyAgentProvider implements AgentProvider {
    @Override
    public List<AgentDefinition> provide() {
        return List.of(
            new AgentDefinition(
                new EntityId("my-agent"),
                new Version("1.0.0"),
                "My Custom Agent",
                Set.of()
            )
        );
    }

    @Override
    public RegistrationOptions options(AgentDefinition agent) {
        return RegistrationOptions.builder()
            .autoStart(true)
            .tag("team", "my-team")
            .build();
    }
}
```

然后创建 ServiceLoader 注册文件：

```
# META-INF/services/io.github.regalpine.agentforge.agent.AgentProvider
com.example.MyAgentProvider
```

打包为 jar 后放入 server classpath，启动时自动发现并注册。通过 `GET /api/v1/agents` 即可在 Studio 前端看到注册的 Agent。

完整示例见 `agentforge-examples/src/main/java/.../examples/agent/SampleAgentProvider.java`。

## 部署

### Docker Compose

```bash
cd deploy/docker
docker compose up -d
```

服务启动后访问 `http://localhost:8080`（Server）和 Studio 前端。

### 生产部署

生产环境需要配置 PostgreSQL + Redis，通过环境变量设置：

```bash
export AGENTFORGE_DEPLOYMENT_MODE=production
export AGENTFORGE_DATABASE_TYPE=postgresql
export AGENTFORGE_DATABASE_URL=jdbc:postgresql://your-postgres:5432/agentforge
export AGENTFORGE_DATABASE_USERNAME=agentforge
export AGENTFORGE_DATABASE_PASSWORD=your-password
export AGENTFORGE_REDIS_URI=redis://your-redis:6379
export AGENTFORGE_SESSION_SIGNING_KEY=$(openssl rand -hex 32)  # 64字符hex
export AGENTFORGE_ADMIN_PASSWORD=your-admin-password

java -jar agentforge-server.jar
```

或使用 Docker Compose：

```bash
cd deploy/docker
cp .env.example .env  # 编辑配置
docker compose up -d
```

**必填环境变量：**

| 变量 | 说明 |
|------|------|
| `AGENTFORGE_DEPLOYMENT_MODE` | 设为 `production` |
| `AGENTFORGE_DATABASE_TYPE` | 设为 `postgresql` |
| `AGENTFORGE_DATABASE_URL` | PostgreSQL JDBC URL |
| `AGENTFORGE_DATABASE_USERNAME` | 数据库用户名 |
| `AGENTFORGE_DATABASE_PASSWORD` | 数据库密码 |
| `AGENTFORGE_REDIS_URI` | Redis 连接 URI |
| `AGENTFORGE_SESSION_SIGNING_KEY` | JWT 签名密钥（≥64字符hex） |
| `AGENTFORGE_ADMIN_PASSWORD` | 初始管理员密码 |

**健康检查端点：**
- `/health/live` — 存活探针
- `/health/ready` — 就绪探针（检查 PostgreSQL + Redis 连接）

### Kubernetes

`deploy/kubernetes/` 提供了完整的 K8s 部署清单：

- `deployment.yaml` — 应用部署（支持滚动更新）
- `service.yaml` — Service 暴露
- `configmap.yaml` / `secret-reference.yaml` — 配置与密钥
- `hpa.yaml` — 自动水平扩缩
- `serviceaccount.yaml` — RBAC 服务账号

```bash
kubectl apply -f deploy/kubernetes/
```

## API

- [OpenAPI 规范](docs/api/openapi.yaml) — 完整的 REST API 定义

## 贡献

我们欢迎社区贡献！请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何参与。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
