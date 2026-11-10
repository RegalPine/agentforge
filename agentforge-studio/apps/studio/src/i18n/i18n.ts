/**
 * i18n - minimal internationalization support.
 *
 * All user-visible text must go through t() function.
 * No hardcoded strings in components.
 *
 * Supports: zh-CN, en-US (extensible).
 */

export type Locale = "en-US" | "zh-CN";

const translations: Record<Locale, Record<string, string>> = {
  "en-US": {
    "app.title": "AgentForge Studio",
    "nav.dashboard": "Dashboard",
    "nav.graphs": "Graphs",
    "nav.executions": "Executions",
    "nav.capabilities": "Capabilities",
    "nav.agents": "Agents",
    "nav.evaluation": "Evaluation",
    "nav.memory": "Memory",
    "nav.knowledge": "Knowledge",
    "nav.mcp": "MCP",
    "nav.governance": "Governance",
    "nav.observability": "Observability",
    "graph.editor.validate": "Validate",
    "graph.editor.compile": "Compile",
    "graph.editor.save": "Save",
    "graph.editor.undo": "Undo",
    "graph.editor.redo": "Redo",
    "graph.palette.title": "Node Types",
    "graph.properties.title": "Properties",
    "execution.pause": "Pause",
    "execution.resume": "Resume",
    "execution.cancel": "Cancel",
    "execution.overview": "Overview",
    "execution.timeline": "Timeline",
    "execution.state": "State",
    "execution.events": "Events",
    "common.loading": "Loading...",
    "common.error": "Error",
    "common.confirm": "Confirm",
    "common.cancel": "Cancel",
    "common.delete": "Delete",
    "common.create": "Create",
    "nav.humanInTheLoop": "Human-in-the-Loop",
    "nav.checkpoints": "Checkpoints",
    "nav.versions": "Versions & Diff",
    "nav.search": "Search",
    "execution.trace": "Trace",
    "execution.context": "Context",
    "execution.agentLoop": "Agent Loop",
    "execution.toolCalls": "Tool Calls",
    "execution.modelCalls": "Model Calls",
    "checkpoint.recovery": "Recovery",
    "checkpoint.conflict": "State Conflict",
    "version.diff": "Diff Viewer",
    "version.release": "Release Gate",
    "login.title": "Sign In",
    "login.subtitle": "Agent Development Framework",
    "login.description": "Enter your credentials to continue",
    "login.username": "Username",
    "login.usernamePlaceholder": "Enter username",
    "login.password": "Password",
    "login.passwordPlaceholder": "Enter password",
    "login.signIn": "Sign In",
    "login.signingIn": "Signing in...",
    "login.footer": "AgentForge · Built-in IAM",
    "auth.profile": "Profile",
    "auth.logout": "Sign Out",
    // FE-07: Extended page keys
    "page.dashboard.title": "Dashboard",
    "page.dashboard.recentExecutions": "Recent Executions",
    "page.dashboard.activeAgents": "Active Agents",
    "page.dashboard.systemHealth": "System Health",
    "page.graphs.title": "Graph Workspace",
    "page.graphs.create": "Create Graph",
    "page.graphs.name": "Name",
    "page.graphs.version": "Version",
    "page.graphs.status": "Status",
    "page.graphs.nodes": "Nodes",
    "page.graphs.edges": "Edges",
    "page.executions.title": "Execution Monitor",
    "page.executions.id": "Execution ID",
    "page.executions.graphId": "Graph ID",
    "page.executions.startedAt": "Started At",
    "page.executions.completedAt": "Completed At",
    "page.agents.title": "Agent Workspace",
    "page.agents.overview": "Overview",
    "page.agents.definition": "Definition",
    "page.agents.versions": "Versions",
    "page.skills.title": "Skill Workspace",
    "page.skills.contract": "Contract",
    "page.skills.dependencies": "Dependencies",
    "page.skills.security": "Security",
    "page.skills.governance": "Governance",
    "page.skills.usage": "Usage",
    "page.hitle.title": "Human-in-the-Loop",
    "page.hitle.pendingApprovals": "Pending Approvals",
    "page.hitle.approve": "Approve",
    "page.hitle.reject": "Reject",
    "page.hitle.reason": "Reason",
    "page.observability.title": "Observability",
    "page.observability.metrics": "Metrics",
    "page.observability.models": "Model Dashboard",
    "page.observability.capabilities": "Capability Health",
    "page.evaluation.title": "Evaluation",
    "page.evaluation.datasets": "Datasets",
    "page.evaluation.runs": "Runs",
    "table.noData": "No data available",
    "table.loading": "Loading...",
    "table.actions": "Actions",
    "status.running": "Running",
    "status.completed": "Completed",
    "status.failed": "Failed",
    "status.pending": "Pending",
    "status.paused": "Paused",
    "status.cancelled": "Cancelled",
  },
  "zh-CN": {
    "app.title": "AgentForge Studio",
    "nav.dashboard": "仪表盘",
    "nav.graphs": "图谱",
    "nav.executions": "执行",
    "nav.capabilities": "能力",
    "nav.agents": "智能体",
    "nav.evaluation": "评估",
    "nav.memory": "记忆",
    "nav.knowledge": "知识",
    "nav.mcp": "MCP",
    "nav.governance": "治理",
    "nav.observability": "可观测性",
    "graph.editor.validate": "验证",
    "graph.editor.compile": "编译",
    "graph.editor.save": "保存",
    "graph.editor.undo": "撤销",
    "graph.editor.redo": "重做",
    "graph.palette.title": "节点类型",
    "graph.properties.title": "属性",
    "execution.pause": "暂停",
    "execution.resume": "恢复",
    "execution.cancel": "取消",
    "execution.overview": "概览",
    "execution.timeline": "时间线",
    "execution.state": "状态",
    "execution.events": "事件",
    "common.loading": "加载中...",
    "common.error": "错误",
    "common.confirm": "确认",
    "common.cancel": "取消",
    "common.delete": "删除",
    "common.create": "创建",
    "nav.humanInTheLoop": "人工审批",
    "nav.checkpoints": "检查点",
    "nav.versions": "版本与差异",
    "nav.search": "搜索",
    "execution.trace": "追踪",
    "execution.context": "上下文",
    "execution.agentLoop": "智能体循环",
    "execution.toolCalls": "工具调用",
    "execution.modelCalls": "模型调用",
    "checkpoint.recovery": "恢复",
    "checkpoint.conflict": "状态冲突",
    "version.diff": "差异查看器",
    "version.release": "发布门禁",
    "login.title": "登录",
    "login.subtitle": "智能体开发框架",
    "login.description": "请输入凭证继续",
    "login.username": "用户名",
    "login.usernamePlaceholder": "请输入用户名",
    "login.password": "密码",
    "login.passwordPlaceholder": "请输入密码",
    "login.signIn": "登录",
    "login.signingIn": "登录中...",
    "login.footer": "AgentForge · 内置身份管理",
    "auth.profile": "个人资料",
    "auth.logout": "退出登录",
    // FE-07: 扩展页面键
    "page.dashboard.title": "仪表盘",
    "page.dashboard.recentExecutions": "最近执行",
    "page.dashboard.activeAgents": "活跃智能体",
    "page.dashboard.systemHealth": "系统健康",
    "page.graphs.title": "图谱工作区",
    "page.graphs.create": "创建图谱",
    "page.graphs.name": "名称",
    "page.graphs.version": "版本",
    "page.graphs.status": "状态",
    "page.graphs.nodes": "节点",
    "page.graphs.edges": "边",
    "page.executions.title": "执行监控",
    "page.executions.id": "执行 ID",
    "page.executions.graphId": "图谱 ID",
    "page.executions.startedAt": "开始时间",
    "page.executions.completedAt": "完成时间",
    "page.agents.title": "智能体工作区",
    "page.agents.overview": "概览",
    "page.agents.definition": "定义",
    "page.agents.versions": "版本",
    "page.skills.title": "技能工作区",
    "page.skills.contract": "契约",
    "page.skills.dependencies": "依赖",
    "page.skills.security": "安全",
    "page.skills.governance": "治理",
    "page.skills.usage": "使用情况",
    "page.hitle.title": "人工审批",
    "page.hitle.pendingApprovals": "待审批",
    "page.hitle.approve": "批准",
    "page.hitle.reject": "拒绝",
    "page.hitle.reason": "原因",
    "page.observability.title": "可观测性",
    "page.observability.metrics": "指标",
    "page.observability.models": "模型仪表盘",
    "page.observability.capabilities": "能力健康",
    "page.evaluation.title": "评估",
    "page.evaluation.datasets": "数据集",
    "page.evaluation.runs": "运行记录",
    "table.noData": "暂无数据",
    "table.loading": "加载中...",
    "table.actions": "操作",
    "status.running": "运行中",
    "status.completed": "已完成",
    "status.failed": "已失败",
    "status.pending": "等待中",
    "status.paused": "已暂停",
    "status.cancelled": "已取消",
  },
};

let currentLocale: Locale = "en-US";

const LOCALE_STORAGE_KEY = "agentforge.locale";

/**
 * Initialize locale from localStorage or browser preference.
 */
export function initLocale(): void {
  try {
    const stored = localStorage.getItem(LOCALE_STORAGE_KEY);
    if (stored === "zh-CN" || stored === "en-US") {
      currentLocale = stored;
      return;
    }
  } catch {
    // localStorage not available
  }
  // Detect browser language
  const browserLang = typeof navigator !== "undefined" ? navigator.language : "";
  currentLocale = browserLang.startsWith("zh") ? "zh-CN" : "en-US";
}

export function setLocale(locale: Locale): void {
  currentLocale = locale;
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // localStorage not available
  }
}

export function getLocale(): Locale {
  return currentLocale;
}

/**
 * Translate a key to the current locale.
 * Falls back to the key itself if not found.
 */
export function t(key: string): string {
  return translations[currentLocale]?.[key] ?? key;
}
