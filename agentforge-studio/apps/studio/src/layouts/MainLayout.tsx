import { useCallback, useEffect, useMemo } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  ActivityBar,
  FileExplorer,
  TabBar,
  BreadcrumbIDE,
  BottomPanel,
  Terminal,
  StatusBar,
  TopBar,
  Box,
} from "@agentforge/ui";
import type { ActivityBarItem, FileExplorerGroup, TerminalLine } from "@agentforge/ui";
import {
  LayoutDashboard,
  Network,
  Bot,
  Wrench,
  GraduationCap,
  Play,
  Brain,
  BookOpen,
  Server,
  BarChart3,
  Eye,
  Shield,
  Users,
  Save,
  GitBranch,
  Search,
  Radio,
  Rocket,
  Dna,
  Bug,
  Box as BoxIcon,
  MonitorSpeaker,
  Settings,
  FolderOpen,
  GitBranch as GitBranchIcon,
  TerminalSquare,
} from "lucide-react";
import { useAuthStore } from "../stores/auth-store.js";
import { useTenantStore } from "../stores/tenant-store.js";
import { useExplorerStore } from "../stores/explorer-store.js";
import { useTabStore } from "../stores/tab-store.js";
import { usePanelStore } from "../stores/panel-store.js";

/**
 * Route → tab/breadcrumb mapping
 */
const ROUTE_TAB_MAP: Record<string, { title: string; icon?: string }> = {
  "/dashboard": { title: "overview.dashboard" },
  "/graphs": { title: "graphs.list" },
  "/executions": { title: "executions.list" },
  "/agents": { title: "agents.workspace" },
  "/capabilities": { title: "capabilities.workspace" },
  "/skills": { title: "skills.workspace" },
  "/memory": { title: "memory.viewer" },
  "/knowledge": { title: "knowledge.rag" },
  "/mcp": { title: "mcp.workspace" },
  "/evaluation": { title: "evaluation.workspace" },
  "/observability": { title: "observability.dashboard" },
  "/governance": { title: "governance.workspace" },
  "/human-in-the-loop": { title: "hitl.workspace" },
  "/checkpoints": { title: "checkpoints.workspace" },
  "/versions": { title: "versions.diff" },
  "/search": { title: "search.global" },
  "/services": { title: "services.channel" },
  "/deployments": { title: "deployments.control" },
  "/evolution": { title: "evolution.workspace" },
  "/debugger": { title: "debugger.workspace" },
  "/workspaces": { title: "workspaces.sandbox" },
  "/multimodal": { title: "multimodal.workspace" },
};

const ACTIVITY_ITEMS: ActivityBarItem[] = [
  { id: "explorer", icon: <FolderOpen className="h-5 w-5" />, label: "Explorer" },
  { id: "search", icon: <Search className="h-5 w-5" />, label: "Search" },
  { id: "source-control", icon: <GitBranch className="h-5 w-5" />, label: "Source Control" },
  { id: "orchestrator", icon: <Bot className="h-5 w-5" />, label: "Agent Orchestrator" },
  { id: "debugger", icon: <Bug className="h-5 w-5" />, label: "Debugger" },
  { id: "skills", icon: <GraduationCap className="h-5 w-5" />, label: "Skills" },
];

const BOTTOM_ACTIVITY_ITEMS: ActivityBarItem[] = [
  { id: "settings", icon: <Settings className="h-5 w-5" />, label: "Settings" },
];

/**
 * Build FileExplorer groups from navigation structure.
 */
function buildExplorerGroups(): FileExplorerGroup[] {
  return [
    {
      id: "core",
      title: "Core",
      icon: <BoxIcon className="h-3 w-3" />,
      defaultExpanded: true,
      children: [
        { path: "/dashboard", name: "overview.dashboard", icon: <LayoutDashboard className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/graphs", name: "main_orchestrator.flow", icon: <Network className="h-3.5 w-3.5" />, iconColor: "text-tertiary" },
        { path: "/agents", name: "supervisor_agent.sys", icon: <Bot className="h-3.5 w-3.5" />, iconColor: "text-secondary" },
        { path: "/capabilities", name: "mcp_manifest.json", icon: <Wrench className="h-3.5 w-3.5" />, iconColor: "text-accent-tool" },
        { path: "/skills", name: "skill_registry.json", icon: <GraduationCap className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/executions", name: "trace_exec.log", icon: <Play className="h-3.5 w-3.5" />, iconColor: "text-primary" },
      ],
    },
    {
      id: "intelligence",
      title: "Intelligence",
      icon: <Brain className="h-3 w-3" />,
      defaultExpanded: true,
      children: [
        { path: "/memory", name: "memory_store.mem", icon: <Brain className="h-3.5 w-3.5" />, iconColor: "text-secondary" },
        { path: "/knowledge", name: "rag_pipeline.know", icon: <BookOpen className="h-3.5 w-3.5" />, iconColor: "text-tertiary" },
        { path: "/mcp", name: "mcp_servers.json", icon: <Server className="h-3.5 w-3.5" />, iconColor: "text-primary" },
      ],
    },
    {
      id: "quality",
      title: "Quality",
      icon: <BarChart3 className="h-3 w-3" />,
      children: [
        { path: "/evaluation", name: "eval_runs.eval", icon: <BarChart3 className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/observability", name: "metrics_dashboard.obs", icon: <Eye className="h-3.5 w-3.5" />, iconColor: "text-tertiary" },
      ],
    },
    {
      id: "governance",
      title: "Governance",
      icon: <Shield className="h-3 w-3" />,
      children: [
        { path: "/governance", name: "policy_catalog.gov", icon: <Shield className="h-3.5 w-3.5" />, iconColor: "text-accent-tool" },
        { path: "/human-in-the-loop", name: "hitl_queue.hitl", icon: <Users className="h-3.5 w-3.5" />, iconColor: "text-secondary" },
      ],
    },
    {
      id: "operations",
      title: "Operations",
      icon: <Save className="h-3 w-3" />,
      children: [
        { path: "/checkpoints", name: "checkpoint_stream.ckpt", icon: <Save className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/versions", name: "version_diff.ver", icon: <GitBranch className="h-3.5 w-3.5" />, iconColor: "text-tertiary" },
        { path: "/search", name: "global_search.idx", icon: <Search className="h-3.5 w-3.5" />, iconColor: "text-primary" },
      ],
    },
    {
      id: "advanced",
      title: "Advanced",
      icon: <Rocket className="h-3 w-3" />,
      children: [
        { path: "/services", name: "service_channel.svc", icon: <Radio className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/deployments", name: "deployment_matrix.dep", icon: <Rocket className="h-3.5 w-3.5" />, iconColor: "text-tertiary" },
        { path: "/evolution", name: "evolution_lineage.evo", icon: <Dna className="h-3.5 w-3.5" />, iconColor: "text-secondary" },
        { path: "/debugger", name: "debug_session.dbg", icon: <Bug className="h-3.5 w-3.5" />, iconColor: "text-error" },
        { path: "/workspaces", name: "workspace_sandbox.ws", icon: <BoxIcon className="h-3.5 w-3.5" />, iconColor: "text-primary" },
        { path: "/multimodal", name: "multimodal_ingest.mm", icon: <MonitorSpeaker className="h-3.5 w-3.5" />, iconColor: "text-accent-tool" },
      ],
    },
  ];
}

/**
 * Build breadcrumb segments from current route path.
 */
function buildBreadcrumbs(pathname: string): { label: string; path?: string }[] {
  const segments: { label: string; path?: string }[] = [{ label: "agentforge-studio", path: "/" }];
  const parts = pathname.split("/").filter(Boolean);

  const pathMap: Record<string, string> = {
    dashboard: "overview",
    graphs: "workflows",
    executions: "traces",
    agents: "agents",
    capabilities: "capabilities",
    skills: "skills",
    memory: "memory",
    knowledge: "knowledge",
    mcp: "mcp",
    evaluation: "evaluation",
    observability: "observability",
    governance: "governance",
    "human-in-the-loop": "hitl",
    checkpoints: "checkpoints",
    versions: "versions",
    search: "search",
    services: "services",
    deployments: "deployments",
    evolution: "evolution",
    debugger: "debugger",
    workspaces: "workspaces",
    multimodal: "multimodal",
  };

  for (let i = 0; i < parts.length; i++) {
    const part = parts[i];
    const label = pathMap[part] ?? part;
    const path = "/" + parts.slice(0, i + 1).join("/");
    const isLast = i === parts.length - 1;
    segments.push({ label, path: isLast ? undefined : path });
  }

  return segments;
}

/**
 * MainLayout - IDE workstation shell.
 *
 * ┌─────┬──────────┬──────────────────────────────────────┐
 * │ Act │  File    │  TabBar (40px)                        │
 * │ ivity│ Explorer│  Breadcrumb (24px)                    │
 * │ Bar │ (240px)  │  ┌──────────────────────────────────┐ │
 * │(48px│ collaps- │  │                                  │ │
 * │     │ ible)    │  │     <Outlet /> (Editor Area)     │ │
 * │     │          │  │                                  │ │
 * │     │          │  └──────────────────────────────────┘ │
 * │     │          │  ┌──────────────────────────────────┐ │
 * │     │          │  │  BottomPanel (resizable)         │ │
 * │     │          │  └──────────────────────────────────┘ │
 * │     │          │  StatusBar (24px)                     │
 * └─────┴──────────┴──────────────────────────────────────┘
 */
export function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const tenantId = useTenantStore((s) => s.tenantId);

  // IDE stores
  const { activeActivity, explorerCollapsed, expandedGroups, setActiveActivity, toggleGroup } = useExplorerStore();
  const { openTabs, activeTabId, openTab, closeTab, setActiveTab } = useTabStore();
  const { bottomPanelOpen, bottomPanelHeight, bottomPanelActiveTab, toggleBottomPanel, setBottomPanelHeight, setBottomPanelTab } = usePanelStore();

  // Sync route → tab
  useEffect(() => {
    const path = location.pathname;
    const tabInfo = ROUTE_TAB_MAP[path];
    if (tabInfo) {
      const tabId = path;
      const title = tabInfo.title.split(".").pop() ?? path;
      openTab({ id: tabId, title, routePath: path });
    }
  }, [location.pathname, openTab]);

  // Explorer groups
  const explorerGroups = useMemo(() => buildExplorerGroups(), []);

  // Breadcrumb
  const breadcrumbs = useMemo(() => buildBreadcrumbs(location.pathname), [location.pathname]);

  // Handle navigation from explorer
  const handleFileSelect = useCallback(
    (path: string) => {
      navigate(path);
    },
    [navigate]
  );

  // Handle tab selection
  const handleTabSelect = useCallback(
    (id: string) => {
      const tab = openTabs.find((t) => t.id === id);
      if (tab?.routePath) navigate(tab.routePath);
      setActiveTab(id);
    },
    [openTabs, navigate, setActiveTab]
  );

  // Terminal lines (placeholder - will be connected to SSE later)
  const terminalLines: TerminalLine[] = useMemo(
    () => [
      { text: "[AgentForge Runtime] Engine initialized", type: "success", timestamp: "00:00:01" },
      { text: `[Tenant: ${tenantId}] Session active`, type: "info", timestamp: "00:00:02" },
      { text: "[SSE:stream:0x01] Connected to runtime events", type: "sse", timestamp: "00:00:03" },
      { text: `[User: ${user?.displayName ?? "anonymous"}] Authenticated`, type: "info", timestamp: "00:00:04" },
    ],
    [tenantId, user]
  );

  return (
    <Box className="h-screen flex flex-col bg-surface overflow-hidden">
      {/* Activity Bar — 最左侧 48px */}
      <div className="flex flex-1 overflow-hidden">
        <ActivityBar
          items={ACTIVITY_ITEMS}
          activeItem={activeActivity}
          onItemClick={setActiveActivity}
          bottomItems={BOTTOM_ACTIVITY_ITEMS}
        />

        {/* File Explorer — 可折叠 */}
        {!explorerCollapsed && (
          <FileExplorer
            groups={explorerGroups}
            activePath={location.pathname}
            onFileSelect={handleFileSelect}
            expandedGroups={expandedGroups}
            onToggleGroup={toggleGroup}
          />
        )}

        {/* Editor + Bottom Panel + StatusBar */}
        <Box className="flex-1 flex flex-col overflow-hidden">
          {/* Top: TabBar + Breadcrumb */}
          <TopBar>
            <TabBar
              tabs={openTabs}
              activeTab={activeTabId}
              onTabSelect={handleTabSelect}
              onTabClose={closeTab}
            />
            <BreadcrumbIDE segments={breadcrumbs} onSegmentClick={(p) => navigate(p)} />
          </TopBar>

          {/* Middle: Page Content */}
          <Box as="main" className="flex-1 overflow-auto" role="main">
            <div className="min-h-full p-3">
              <Outlet />
            </div>
          </Box>

          {/* Bottom: Resizable Panel */}
          {bottomPanelOpen && (
            <BottomPanel
              tabs={[
                { id: "terminal", label: "Terminal" },
                { id: "problems", label: "Problems", badge: 0 },
                { id: "output", label: "Output" },
                { id: "sse", label: "Runtime SSE", indicator: "live" },
              ]}
              activeTab={bottomPanelActiveTab}
              onTabSelect={setBottomPanelTab}
              height={bottomPanelHeight}
              onHeightChange={setBottomPanelHeight}
              onCollapse={toggleBottomPanel}
            >
              <Terminal lines={terminalLines} />
            </BottomPanel>
          )}

          {/* Status Bar */}
          <StatusBar
            leftItems={[
              { id: "git", icon: <GitBranchIcon className="h-3 w-3" />, label: "main" },
              { id: "errors", icon: <TerminalSquare className="h-3 w-3" />, label: "0", color: "text-error" },
              { id: "sse", label: "SSE: Connected", pulse: true, color: "text-success" },
            ]}
            rightItems={[
              { id: "tenant", label: tenantId },
              { id: "encoding", label: "UTF-8" },
              { id: "lang", label: "TypeScript" },
            ]}
          />
        </Box>
      </div>
    </Box>
  );
}
