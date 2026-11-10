import { useEffect, lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { MainLayout } from "./layouts/MainLayout.js";
import { LoginPage } from "./pages/LoginPage.js";
import { AuthGuard } from "./components/AuthGuard.js";
import { useAuthStore } from "./stores/auth-store.js";
import { Spinner } from "@agentforge/ui";

// FE-08: Route-level code splitting via React.lazy.
// Each page is loaded on demand, reducing initial bundle size.
const DashboardPage = lazy(() => import("./pages/DashboardPage.js").then(m => ({ default: m.DashboardPage })));
const GraphListPage = lazy(() => import("./pages/GraphListPage.js").then(m => ({ default: m.GraphListPage })));
const GraphEditorPage = lazy(() => import("./pages/GraphEditorPage.js").then(m => ({ default: m.GraphEditorPage })));
const ExecutionListPage = lazy(() => import("./pages/ExecutionListPage.js").then(m => ({ default: m.ExecutionListPage })));
const ExecutionDetailPage = lazy(() => import("./pages/ExecutionDetailPage.js").then(m => ({ default: m.ExecutionDetailPage })));
const AgentWorkspacePage = lazy(() => import("./pages/AgentWorkspacePage.js").then(m => ({ default: m.AgentWorkspacePage })));
const CapabilityWorkspacePage = lazy(() => import("./pages/CapabilityWorkspacePage.js").then(m => ({ default: m.CapabilityWorkspacePage })));
const EvaluationPage = lazy(() => import("./pages/EvaluationPage.js").then(m => ({ default: m.EvaluationPage })));
const MemoryPage = lazy(() => import("./pages/MemoryPage.js").then(m => ({ default: m.MemoryPage })));
const KnowledgePage = lazy(() => import("./pages/KnowledgePage.js").then(m => ({ default: m.KnowledgePage })));
const McpPage = lazy(() => import("./pages/McpPage.js").then(m => ({ default: m.McpPage })));
const GovernancePage = lazy(() => import("./pages/GovernancePage.js").then(m => ({ default: m.GovernancePage })));
const ObservabilityPage = lazy(() => import("./pages/ObservabilityPage.js").then(m => ({ default: m.ObservabilityPage })));
const HumanInTheLoopPage = lazy(() => import("./pages/HumanInTheLoopPage.js").then(m => ({ default: m.HumanInTheLoopPage })));
const CheckpointPage = lazy(() => import("./pages/CheckpointPage.js").then(m => ({ default: m.CheckpointPage })));
const VersionDiffPage = lazy(() => import("./pages/VersionDiffPage.js").then(m => ({ default: m.VersionDiffPage })));
const SearchPage = lazy(() => import("./pages/SearchPage.js").then(m => ({ default: m.SearchPage })));
const SkillWorkspacePage = lazy(() => import("./pages/SkillWorkspacePage.js").then(m => ({ default: m.SkillWorkspacePage })));
const ServiceChannelPage = lazy(() => import("./pages/ServiceChannelPage.js").then(m => ({ default: m.ServiceChannelPage })));
const DeploymentPage = lazy(() => import("./pages/DeploymentPage.js").then(m => ({ default: m.DeploymentPage })));
const EvolutionPage = lazy(() => import("./pages/EvolutionPage.js").then(m => ({ default: m.EvolutionPage })));
const DebuggerPage = lazy(() => import("./pages/DebuggerPage.js").then(m => ({ default: m.DebuggerPage })));
const WorkspaceSandboxPage = lazy(() => import("./pages/WorkspaceSandboxPage.js").then(m => ({ default: m.WorkspaceSandboxPage })));
const MultimodalPage = lazy(() => import("./pages/MultimodalPage.js").then(m => ({ default: m.MultimodalPage })));

/**
 * App - root routing configuration.
 *
 * Routes follow AF-STUDIO-01:
 * / → /dashboard
 * /graphs → graph list
 * /graphs/:graphId/:version/edit → graph editor
 * /executions → execution list
 * /executions/:executionId → execution detail (§23-30 enhanced)
 * /agents → agent workspace (§7-10)
 * /capabilities → capability workspace (§11-14)
 * /evaluation → evaluation workspace (§37-39)
 * /memory → memory viewer (§31)
 * /knowledge → knowledge/RAG viewer (§32-33)
 * /mcp → MCP workspace (§34-35)
 * /governance → governance workspace (§40-41)
 * /observability → observability dashboard (§61-63)
 * /human-in-the-loop → human approval (§42)
 * /checkpoints → checkpoint & recovery (§43-44)
 * /versions → version & diff (§45-48)
 * /skills → skill workbench (§50-58)
 * /search → global search (§49)
 * /services → service channel (Enhance3)
 * /deployments → control plane / deployments (Enhance3)
 * /evolution → evolution workspace (Enh3)
 * /debugger → debugger workspace (Enhance3)
 * /workspaces → workspace & sandbox (Enhance3)
 * /multimodal → multimodal content (Enhance3)
 */
export function App() {
  const restoreSession = useAuthStore((s) => s.restoreSession);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  return (
    <Suspense fallback={<div className="flex items-center justify-center h-screen"><Spinner size="lg" /></div>}>
    <Routes>
      {/* Public route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Protected routes */}
      <Route element={<AuthGuard><MainLayout /></AuthGuard>}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/graphs" element={<GraphListPage />} />
        <Route path="/graphs/:graphId" element={<GraphEditorPage />} />
        <Route path="/graphs/:graphId/:version/edit" element={<GraphEditorPage />} />
        <Route path="/executions" element={<ExecutionListPage />} />
        <Route path="/executions/:executionId" element={<ExecutionDetailPage />} />
        <Route path="/executions/:executionId/checkpoints" element={<CheckpointPage />} />
        <Route path="/agents" element={<AgentWorkspacePage />} />
        <Route path="/capabilities" element={<CapabilityWorkspacePage />} />
        <Route path="/evaluation" element={<EvaluationPage />} />
        <Route path="/memory" element={<MemoryPage />} />
        <Route path="/knowledge" element={<KnowledgePage />} />
        <Route path="/mcp" element={<McpPage />} />
        <Route path="/governance" element={<GovernancePage />} />
        <Route path="/observability" element={<ObservabilityPage />} />
        <Route path="/human-in-the-loop" element={<HumanInTheLoopPage />} />
        <Route path="/checkpoints" element={<CheckpointPage />} />
        <Route path="/versions" element={<VersionDiffPage />} />
        <Route path="/skills" element={<SkillWorkspacePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/services" element={<ServiceChannelPage />} />
        <Route path="/deployments" element={<DeploymentPage />} />
        <Route path="/evolution" element={<EvolutionPage />} />
        <Route path="/debugger" element={<DebuggerPage />} />
        <Route path="/workspaces" element={<WorkspaceSandboxPage />} />
        <Route path="/multimodal" element={<MultimodalPage />} />
      </Route>

      {/* Catch-all: redirect to dashboard (or login if unauthenticated) */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
    </Suspense>
  );
}
