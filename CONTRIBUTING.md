# Contributing to AgentForge

感谢你对 AgentForge 的关注！我们欢迎各种形式的社区贡献。

## 行为准则

本项目遵循 [Contributor Covenant](CODE_OF_CONDUCT.md) 行为准则。参与本项目即表示你同意遵守该准则。

## 如何贡献

### 报告 Bug

1. 在 [Issues](https://github.com/regalpine/agentforge/issues) 中搜索是否已有相同问题
2. 如果没有，使用 [Bug Report 模板](https://github.com/regalpine/agentforge/issues/new?template=bug_report.yml) 创建新 Issue
3. 请提供：
   - 清晰的问题描述
   - 复现步骤
   - 期望行为 vs 实际行为
   - 环境信息（Java 版本、操作系统等）

### 提出新功能

1. 先在 Issues 中搜索是否已有类似提案
2. 使用 [Feature Request 模板](https://github.com/regalpine/agentforge/issues/new?template=feature_request.yml) 提交
3. 描述使用场景和预期效果，方便社区讨论

### 提交代码

#### 开发流程

1. **Fork** 本仓库并克隆到本地
   ```bash
   git clone https://github.com/<your-username>/agentforge.git
   cd agentforge
   ```

2. **创建分支**
   ```bash
   git checkout -b feat/your-feature-name
   # 或
   git checkout -b fix/issue-number-description
   ```

3. **开发**
   - 遵循项目代码风格（见 `.editorconfig`）
   - 编写必要的单元测试
   - 确保所有测试通过：`./mvnw clean verify -B`

4. **提交**
   ```bash
   git add .
   git commit -m "feat(module): add xxx capability"
   ```
   提交信息格式：`type(scope): description`
   - `feat` — 新功能
   - `fix` — Bug 修复
   - `docs` — 文档变更
   - `refactor` — 重构
   - `test` — 测试相关
   - `chore` — 构建/工具链

5. **推送并创建 Pull Request**
   ```bash
   git push origin feat/your-feature-name
   ```

#### 代码规范

- **Java**: 遵循项目 `.editorconfig` 配置，4 空格缩进
- **TypeScript/前端**: 遵循 `agentforge-studio` 中的 ESLint 配置
- **测试**: 新功能需附带单元测试，Bug 修复需附带回归测试
- **文档**: 公共 API 需有 Javadoc 注释

#### PR 审核流程

- CI 会自动运行构建、测试和代码检查
- 至少需要 1 位维护者审核通过
- 请确保所有 CI 检查通过后再请求审核

## 开发环境搭建

```bash
# 后端
./mvnw clean install -DskipTests

# Studio 前端
cd agentforge-studio
pnpm install
pnpm dev
```

## 许可证

贡献的代码将遵循项目主许可证 [Apache License 2.0](LICENSE)。提交 PR 即表示你同意将贡献内容在此许可证下发布。
