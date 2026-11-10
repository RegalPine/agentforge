import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  type Node,
  type NodeTypes,
  useNodesState,
  useEdgesState,
} from "reactflow";
import "reactflow/dist/style.css";
import {
  AgentForgeNode,
  StartEndNode,
  AgentNode,
  CapabilityNode,
  SubgraphNode,
  getNodeConfig,
  getReactFlowNodeType,
  CommandHistory,
  DeleteNodeCommand,
  MoveNodeCommand,
  deserialize,
  serialize,
  extractCanvasState,
  type NodeData,
} from "@agentforge/graph-editor";
import type { GraphDefinition, CompilationResult } from "@agentforge/model";
import { ApiError } from "@agentforge/api-client";
import { useApiClient } from "../hooks/use-api-client.js";
import { useEditorStore } from "../stores/editor-store.js";
import {
  Button,
  Badge,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  StatusLight,
  Separator,
} from "@agentforge/ui";
import {
  Search,
  Play,
  Square,
  GitBranch,
  Bot,
  Wrench,
  Brain,
  Users,
  Split,
  Merge,
  RotateCcw,
  Layers,
  Trash2,
  Check,
  ChevronRight,
  ChevronDown,
  Copy,
  X,
} from "lucide-react";

// ─── Node palette categories ──────────────────────────────────────────────────

interface PaletteCategory {
  id: string;
  label: string;
  icon: React.ReactNode;
  items: { type: string; label: string; icon: React.ReactNode; color: string; badge?: string }[];
}

const PALETTE_CATEGORIES: PaletteCategory[] = [
  {
    id: "flow-boundary",
    label: "FLOW BOUNDARY",
    icon: <Play className="h-3 w-3" />,
    items: [
      { type: "start", label: "START", icon: <Play className="h-3 w-3" />, color: "text-primary" },
      { type: "end", label: "END", icon: <Square className="h-3 w-3" />, color: "text-error" },
    ],
  },
  {
    id: "integration",
    label: "INTEGRATION & AGENTS",
    icon: <Bot className="h-3 w-3" />,
    items: [
      { type: "agent", label: "AGENT", icon: <Bot className="h-3 w-3" />, color: "text-secondary", badge: "ReAct" },
      { type: "capability", label: "CAPABILITY", icon: <Wrench className="h-3 w-3" />, color: "text-accent-tool", badge: "eBPF" },
      { type: "tool", label: "TOOL", icon: <Wrench className="h-3 w-3" />, color: "text-outline", badge: "bash" },
      { type: "llm", label: "LLM INFERENCE", icon: <Brain className="h-3 w-3" />, color: "text-primary", badge: "Sonnet" },
      { type: "human", label: "HUMAN (HITL)", icon: <Users className="h-3 w-3" />, color: "text-error", badge: "Approval" },
    ],
  },
  {
    id: "control",
    label: "CONTROL LOGIC",
    icon: <GitBranch className="h-3 w-3" />,
    items: [
      { type: "condition", label: "IF BRANCH", icon: <GitBranch className="h-3 w-3" />, color: "text-primary" },
      { type: "parallel", label: "PARALLEL", icon: <Split className="h-3 w-3" />, color: "text-tertiary" },
      { type: "join", label: "JOIN", icon: <Merge className="h-3 w-3" />, color: "text-outline" },
      { type: "loop", label: "LOOP", icon: <RotateCcw className="h-3 w-3" />, color: "text-primary" },
    ],
  },
];

// ─── Sub-components ────────────────────────────────────────────────────────────

function NodeLibrary({
  searchQuery,
  onSearchChange,
  onNodeSelect,
  dagNodes,
  selectedDagNode,
  onDagNodeSelect,
}: {
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onNodeSelect: (type: string) => void;
  dagNodes: { id: string; label: string; type: string }[];
  selectedDagNode: string | null;
  onDagNodeSelect: (id: string) => void;
}) {
  const [expandedCats, setExpandedCats] = useState<Record<string, boolean>>({
    "flow-boundary": true,
    integration: true,
    control: true,
  });
  const [dagExpanded, setDagExpanded] = useState(true);

  const toggleCat = (id: string) => setExpandedCats((prev) => ({ ...prev, [id]: !prev[id] }));

  const filteredCategories = useMemo(() => {
    if (!searchQuery.trim()) return PALETTE_CATEGORIES;
    const q = searchQuery.toLowerCase();
    return PALETTE_CATEGORIES.map((cat) => ({
      ...cat,
      items: cat.items.filter(
        (item) => item.label.toLowerCase().includes(q) || item.type.toLowerCase().includes(q),
      ),
    })).filter((cat) => cat.items.length > 0);
  }, [searchQuery]);

  const totalTypes = PALETTE_CATEGORIES.reduce((sum, cat) => sum + cat.items.length, 0);

  return (
    <div className="w-60 bg-surface-container-lowest border-r border-outline-variant flex flex-col overflow-hidden shrink-0">
      {/* Search */}
      <div className="p-2 border-b border-outline-variant">
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-surface border border-outline-variant">
          <Search className="h-3 w-3 text-outline" />
          <input
            className="flex-1 bg-transparent text-[11px] font-code-sm text-on-surface placeholder:text-outline outline-none"
            placeholder={`Filter ${totalTypes} node types...`}
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      </div>

      {/* Node categories */}
      <div className="flex-1 overflow-auto p-2 space-y-1">
        {filteredCategories.map((cat) => (
          <div key={cat.id}>
            <button
              className="flex items-center gap-1.5 w-full px-1 py-1 text-[10px] font-code-sm text-outline uppercase tracking-wider hover:text-on-surface transition-colors"
              onClick={() => toggleCat(cat.id)}
            >
              {expandedCats[cat.id] ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
              {cat.icon}
              {cat.label}
            </button>
            {expandedCats[cat.id] && (
              <div className="ml-2 space-y-0.5">
                {cat.items.map((item) => (
                  <button
                    key={item.type}
                    className="flex items-center justify-between w-full px-2 py-1.5 rounded text-[11px] font-body-md text-on-surface hover:bg-surface-container-high transition-colors group"
                    onClick={() => onNodeSelect(item.type)}
                  >
                    <span className="flex items-center gap-1.5">
                      <span className={item.color}>{item.icon}</span>
                      {item.label}
                    </span>
                    {item.badge && (
                      <span className="text-[9px] font-code-sm text-outline bg-surface-container px-1 rounded">
                        {item.badge}
                      </span>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}

        {/* DAG Structure */}
        {dagNodes.length > 0 && (
          <>
            <Separator className="my-2" />
            <button
              className="flex items-center gap-1.5 w-full px-1 py-1 text-[10px] font-code-sm text-outline uppercase tracking-wider hover:text-on-surface transition-colors"
              onClick={() => setDagExpanded(!dagExpanded)}
            >
              {dagExpanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
              <Layers className="h-3 w-3" />
              DAG STRUCTURE ({dagNodes.length})
            </button>
            {dagExpanded && (
              <div className="ml-2 space-y-0.5">
                {dagNodes.map((node, i) => (
                  <button
                    key={node.id}
                    className={`flex items-center gap-1.5 w-full px-2 py-1 rounded text-[11px] font-code-sm transition-colors ${
                      selectedDagNode === node.id
                        ? "bg-surface-container-high text-primary"
                        : "text-on-surface hover:bg-surface-container-high"
                    }`}
                    onClick={() => onDagNodeSelect(node.id)}
                  >
                    <StatusLight status="running" size="sm" />
                    <span className="truncate">
                      {String(i + 1).padStart(2, "0")}. {node.label}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function NodeInspector({
  selectedNode,
  compilationDiagnostics,
  onDelete,
  onApply,
}: {
  selectedNode: Node<NodeData> | null;
  compilationDiagnostics: CompilationResult["diagnostics"] | null;
  onDelete: () => void;
  onApply: () => void;
}) {
  if (!selectedNode) {
    return (
      <div className="w-72 bg-surface-container-lowest border-l border-outline-variant flex items-center justify-center p-4 shrink-0">
        <span className="text-[12px] font-body-md text-outline">Select a node to inspect</span>
      </div>
    );
  }

  const nodeErrors = compilationDiagnostics?.filter(
    (d) => d.nodeId === selectedNode.id && d.severity === "ERROR",
  ) ?? [];

  return (
    <div className="w-72 bg-surface-container-lowest border-l border-outline-variant flex flex-col overflow-hidden shrink-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-outline-variant">
        <div className="flex items-center gap-1.5">
          <span className="text-[12px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
            Node Inspector
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <Copy className="h-3 w-3" />
          </button>
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <X className="h-3 w-3" />
          </button>
        </div>
      </div>

      {/* Node name */}
      <div className="px-3 py-2 border-b border-outline-variant">
        <span className="text-[14px] font-body-lg font-semibold text-on-surface">
          {selectedNode.data.label}
        </span>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="properties" className="flex-1 flex flex-col overflow-hidden">
        <TabsList className="mx-2 mt-2 h-7">
          <TabsTrigger value="properties" className="text-[10px]">Properties</TabsTrigger>
          <TabsTrigger value="schema" className="text-[10px]">Schema</TabsTrigger>
          <TabsTrigger value="runtime" className="text-[10px]">Runtime</TabsTrigger>
          <TabsTrigger value="test" className="text-[10px]">Test</TabsTrigger>
        </TabsList>

        <div className="flex-1 overflow-auto p-3 space-y-3">
          <TabsContent value="properties" className="mt-0 space-y-3">
            {/* Node Identifier */}
            <div>
              <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Node Identifier (UUID)</span>
              <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                {selectedNode.id}
              </div>
            </div>

            {/* Display Label */}
            <div>
              <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Display Label</span>
              <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                {selectedNode.data.label}
              </div>
            </div>

            {/* Node Type + State Category */}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Node Type</span>
                <div className="mt-1 flex items-center gap-1.5">
                  <StatusLight status="running" size="sm" />
                  <Badge variant="outline" className="text-[10px]">{selectedNode.data.definition.type}</Badge>
                </div>
              </div>
              <div>
                <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">State Category</span>
                <div className="mt-1 px-2 py-1 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                  Integration
                </div>
              </div>
            </div>

            {/* Assigned Model */}
            <div>
              <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Assigned Model</span>
              <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-primary">
                claude-3-7-sonnet-20250219
              </div>
            </div>

            {/* Execution Loop Mode */}
            <div>
              <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Execution Loop Mode</span>
              <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                ReAct Loop with Self-Reflection
              </div>
            </div>

            {/* Max Iterations + Timeout */}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Max Iterations</span>
                <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                  8
                </div>
              </div>
              <div>
                <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Timeout</span>
                <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                  15,000 ms
                </div>
              </div>
            </div>

            {/* Input Bindings */}
            <div>
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Input Bindings</span>
                <span className="text-primary text-sm cursor-pointer">+</span>
              </div>
              <div className="mt-1 space-y-1">
                <div className="flex items-center justify-between px-2 py-1 rounded bg-surface text-[11px] font-code-sm">
                  <span className="text-primary">payload.alert_data</span>
                  <span className="text-outline">→</span>
                  <span className="text-on-surface">ctx.incident</span>
                </div>
                <div className="flex items-center justify-between px-2 py-1 rounded bg-surface text-[11px] font-code-sm">
                  <span className="text-primary">sys.session_token</span>
                  <span className="text-outline">→</span>
                  <span className="text-on-surface">ctx.auth</span>
                </div>
              </div>
            </div>

            {/* Failure Strategy */}
            <div>
              <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Failure Strategy</span>
              <div className="mt-1 px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-on-surface">
                Escalate to HITL Gate Approval
              </div>
            </div>

            {/* Re-evaluate on Parent Update */}
            <div className="flex items-center justify-between px-2 py-1.5 rounded bg-surface border border-outline-variant">
              <div>
                <span className="text-[11px] font-code-sm text-on-surface">Re-evaluate on Parent Update</span>
                <p className="text-[10px] font-code-sm text-outline">Invalidate cache on state change</p>
              </div>
              <div className="h-4 w-4 rounded bg-accent-primary flex items-center justify-center">
                <Check className="h-3 w-3 text-on-primary" />
              </div>
            </div>

            {/* Compilation errors */}
            {nodeErrors.length > 0 && (
              <div className="rounded border border-error/30 bg-error/10 p-2 space-y-1">
                <span className="text-[10px] font-code-sm text-error font-semibold">Compilation Errors:</span>
                {nodeErrors.map((d, i) => (
                  <p key={i} className="text-[10px] font-code-sm text-error">
                    [{d.code}] {d.message}
                  </p>
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="schema" className="mt-0">
            <div className="px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-outline">
              Schema binding configuration (JSON Schema)
            </div>
          </TabsContent>

          <TabsContent value="runtime" className="mt-0">
            <div className="px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-outline">
              Runtime policy and execution constraints
            </div>
          </TabsContent>

          <TabsContent value="test" className="mt-0">
            <div className="px-2 py-1.5 rounded bg-surface border border-outline-variant text-[11px] font-code-sm text-outline">
              Test node with mock inputs
            </div>
          </TabsContent>
        </div>
      </Tabs>

      {/* Action buttons */}
      <div className="flex items-center gap-2 px-3 py-2 border-t border-outline-variant">
        <Button variant="secondary" size="sm" className="flex-1 text-[11px]" onClick={onDelete}>
          <Trash2 className="h-3 w-3 mr-1" />Delete (Del)
        </Button>
        <Button size="sm" className="flex-1 text-[11px]" onClick={onApply}>
          <Check className="h-3 w-3 mr-1" />Apply Changes
        </Button>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export function GraphEditorPage() {
  const { graphId, version } = useParams<{ graphId: string; version: string }>();
  const navigate = useNavigate();
  const apiClient = useApiClient();

  const [graphDef, setGraphDef] = useState<GraphDefinition | null>(null);
  const [loading, setLoading] = useState(true);
  const [nodes, setNodes, onNodesChange] = useNodesState<NodeData>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState<Node<NodeData> | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveResult, setSaveResult] = useState<string | null>(null);
  const [compilationDiagnostics, setCompilationDiagnostics] = useState<
    CompilationResult["diagnostics"] | null
  >(null);
  const [errorNodeIds, setErrorNodeIds] = useState<Set<string>>(new Set());
  const [paletteSearch, setPaletteSearch] = useState("");
  const [selectedDagNode, setSelectedDagNode] = useState<string | null>(null);

  const commandHistory = useRef<CommandHistory | null>(null);

  const nodeTypes: NodeTypes = useMemo(
    () => ({
      agentForgeNode: AgentForgeNode,
      startEndNode: StartEndNode,
      agentNode: AgentNode,
      capabilityNode: CapabilityNode,
      subgraphNode: SubgraphNode,
    }),
    [],
  );

  // DAG nodes for tree
  const dagNodes = useMemo(
    () =>
      nodes.map((n) => ({
        id: n.id,
        label: n.data.label ?? n.id,
        type: n.data.definition?.type ?? "unknown",
      })),
    [nodes],
  );

  // Fetch graph
  useEffect(() => {
    if (!graphId) return;
    setLoading(true);

    apiClient
      .getGraph(graphId, version)
      .then((def) => {
        const domainDef = def as unknown as GraphDefinition;
        setGraphDef(domainDef);
        commandHistory.current = new CommandHistory(domainDef);

        const canvas = extractCanvasState([]);
        const { nodes: rfNodes, edges: rfEdges } = deserialize(domainDef, {
          positions: (domainDef.metadata as Record<string, unknown>)?._canvas as Record<string, { x: number; y: number }> ?? {},
          viewport: canvas.viewport,
        });

        const typedNodes = rfNodes.map((n) => ({
          ...n,
          type: getReactFlowNodeType(n.data.definition.type),
        }));

        setNodes(typedNodes);
        setEdges(rfEdges);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to load graph:", err);
        setLoading(false);
      });
  }, [graphId, version, apiClient, setNodes, setEdges]);

  const onNodeClick = useCallback(
    (_: unknown, node: Node<NodeData>) => {
      setSelectedNode(node);
      useEditorStore.getState().setSelectedNodeId(node.id);
    },
    [],
  );
  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
    useEditorStore.getState().setSelectedNodeId(null);
  }, []);

  const onNodeDragStop = useCallback(
    (_: unknown, node: Node<NodeData>) => {
      if (commandHistory.current) {
        commandHistory.current.execute(new MoveNodeCommand(node.id, node.position));
      }
    },
    [],
  );

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!commandHistory.current) return;
      const isCtrlOrMeta = e.ctrlKey || e.metaKey;

      if (isCtrlOrMeta && e.key === "z" && !e.shiftKey) {
        e.preventDefault();
        const graph = commandHistory.current.undo();
        if (graph) {
          setGraphDef(graph);
          const { nodes: rfNodes, edges: rfEdges } = deserialize(graph, {
            positions: (graph.metadata as Record<string, unknown>)?._canvas as Record<string, { x: number; y: number }> ?? {},
            viewport: { x: 0, y: 0, zoom: 1 },
          });
          setNodes(rfNodes.map((n) => ({ ...n, type: getReactFlowNodeType(n.data.definition.type) })));
          setEdges(rfEdges);
        }
      }

      if (isCtrlOrMeta && (e.key === "y" || (e.key === "z" && e.shiftKey))) {
        e.preventDefault();
        const graph = commandHistory.current.redo();
        if (graph) {
          setGraphDef(graph);
          const { nodes: rfNodes, edges: rfEdges } = deserialize(graph, {
            positions: (graph.metadata as Record<string, unknown>)?._canvas as Record<string, { x: number; y: number }> ?? {},
            viewport: { x: 0, y: 0, zoom: 1 },
          });
          setNodes(rfNodes.map((n) => ({ ...n, type: getReactFlowNodeType(n.data.definition.type) })));
          setEdges(rfEdges);
        }
      }

      if (e.key === "Delete" || e.key === "Backspace") {
        const selected = useEditorStore.getState().selectedNodeId;
        if (selected && commandHistory.current) {
          e.preventDefault();
          commandHistory.current.execute(new DeleteNodeCommand(selected));
          setNodes((nds) => nds.filter((n) => n.id !== selected));
          setEdges((eds) => eds.filter((e) => e.source !== selected && e.target !== selected));
          setSelectedNode(null);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [setNodes, setEdges]);

  const handleValidate = useCallback(async () => {
    if (!graphDef) return;
    try {
      const currentGraph = serialize(nodes, edges);
      const result = await apiClient.validateGraph(currentGraph as unknown as Parameters<typeof apiClient.validateGraph>[0]);
      setValidationMessage(
        result.valid ? "✓ Graph is valid" : `✗ ${result.errors?.length ?? 0} errors found`,
      );
    } catch (err) {
      setValidationMessage(
        err instanceof ApiError ? `Validation failed: ${err.message}` : "Validation request failed",
      );
    }
  }, [graphDef, nodes, edges, apiClient]);

  const handleSave = useCallback(async () => {
    if (!graphId || !graphDef) return;
    setSaving(true);
    setSaveResult(null);
    try {
      const updatedDef = serialize(nodes, edges);
      const fullDef: GraphDefinition = {
        ...updatedDef,
        id: graphDef.id,
        version: graphDef.version,
        name: graphDef.name,
        description: graphDef.description,
        inputs: graphDef.inputs,
        outputs: graphDef.outputs,
        bindings: graphDef.bindings,
        expressions: graphDef.expressions,
        policies: graphDef.policies,
      };
      await apiClient.updateGraph(graphId, fullDef as unknown as Parameters<typeof apiClient.updateGraph>[1]);
      setSaveResult("✓ Saved successfully");
      setGraphDef(fullDef);
      commandHistory.current?.setGraph(fullDef);
    } catch (err) {
      const msg = err instanceof ApiError
        ? err.isConflict ? "Conflict: graph was modified by another user" : `Save failed: ${err.message}`
        : "Save request failed";
      setSaveResult(` ${msg}`);
    } finally {
      setSaving(false);
    }
  }, [graphId, graphDef, nodes, edges, apiClient]);

  const handleCompile = useCallback(async () => {
    if (!graphDef) return;
    try {
      const currentGraph = serialize(nodes, edges);
      const result = await apiClient.compileGraph(currentGraph as unknown as Parameters<typeof apiClient.compileGraph>[0]);
      setCompilationDiagnostics((result.diagnostics ?? null) as CompilationResult["diagnostics"] | null);

      const errIds = new Set<string>();
      for (const diag of result.diagnostics ?? []) {
        if (diag.severity === "ERROR" && diag.nodeId) errIds.add(diag.nodeId);
      }
      setErrorNodeIds(errIds);

      setValidationMessage(
        result.status === "SUCCESS"
          ? "✓ Compilation successful"
          : `✗ Compilation failed (${errIds.size} error node(s))`,
      );
    } catch (err) {
      setValidationMessage(
        err instanceof ApiError ? `Compilation failed: ${err.message}` : "Compilation request failed",
      );
    }
  }, [graphDef, nodes, edges, apiClient]);

  const nodesWithDiagnostics = useMemo(() => {
    if (errorNodeIds.size === 0) return nodes;
    return nodes.map((n) => {
      if (errorNodeIds.has(n.id)) {
        return {
          ...n,
          style: { ...n.style, borderColor: "#ef4444", boxShadow: "0 0 8px rgba(239,68,68,0.5)" },
        };
      }
      return n;
    });
  }, [nodes, errorNodeIds]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-surface">
        <span className="text-[13px] font-body-md text-outline">Loading graph...</span>
      </div>
    );
  }

  if (!graphDef) {
    return (
      <div className="flex-1 flex items-center justify-center bg-surface">
        <div className="flex flex-col items-center gap-3">
          <span className="text-[13px] font-body-md text-outline">Graph definition not found</span>
          <Button variant="secondary" size="sm" onClick={() => navigate("/graphs")}>
            Back to Graphs
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={() => navigate("/graphs")} className="h-6 text-[11px]">
            ← Graphs
          </Button>
          <span className="text-[13px] font-body-md font-semibold text-on-surface">{graphDef.name}</span>
          <Badge variant="outline" className="text-[10px]">v{graphDef.version}</Badge>
          <span className="text-[11px] font-code-sm text-outline">{graphDef.description}</span>
          {commandHistory.current?.canUndo() && (
            <Badge className="bg-accent-primary/20 text-accent-primary text-[10px]">Modified</Badge>
          )}
        </div>
        <div className="flex items-center gap-1.5">
          <Button variant="secondary" size="sm" className="h-6 text-[11px]" onClick={handleValidate}>
            Validate
          </Button>
          <Button variant="secondary" size="sm" className="h-6 text-[11px]" onClick={handleCompile}>
            Compile
          </Button>
          <Button size="sm" className="h-6 text-[11px]" onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : "Save"}
          </Button>
        </div>
      </div>

      {/* Status messages */}
      {(validationMessage || saveResult) && (
        <div className="flex items-center gap-3 px-3 py-1 border-b border-outline-variant bg-surface-container-low/50 shrink-0">
          {validationMessage && (
            <span className={`text-[11px] font-code-sm ${validationMessage.startsWith("✓") ? "text-success" : "text-error"}`}>
              {validationMessage}
            </span>
          )}
          {saveResult && (
            <span className={`text-[11px] font-code-sm ${saveResult.startsWith("✓") ? "text-success" : "text-error"}`}>
              {saveResult}
            </span>
          )}
        </div>
      )}

      {/* Main 3-column layout */}
      <div className="flex flex-1 overflow-hidden">
        {/* Node Library */}
        <NodeLibrary
          searchQuery={paletteSearch}
          onSearchChange={setPaletteSearch}
          onNodeSelect={() => {}}
          dagNodes={dagNodes}
          selectedDagNode={selectedDagNode}
          onDagNodeSelect={(id) => {
            setSelectedDagNode(id);
            const node = nodes.find((n) => n.id === id);
            if (node) {
              setSelectedNode(node);
              useEditorStore.getState().setSelectedNodeId(id);
            }
          }}
        />

        {/* Canvas */}
        <div className="flex-1 relative">
          <ReactFlow
            nodes={nodesWithDiagnostics}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            onNodeDragStop={onNodeDragStop}
            nodeTypes={nodeTypes}
            fitView
            proOptions={{ hideAttribution: true }}
          >
            <Background />
            <Controls showInteractive={false} />
            <MiniMap
              nodeColor={(n) => {
                if (errorNodeIds.has(n.id)) return "#ef4444";
                const cfg = getNodeConfig(n.data?.definition?.type);
                return cfg?.color ?? "#6b7280";
              }}
              pannable
              zoomable
            />
          </ReactFlow>
        </div>

        {/* Node Inspector */}
        <NodeInspector
          selectedNode={selectedNode}
          compilationDiagnostics={compilationDiagnostics}
          onDelete={() => {
            if (selectedNode) {
              if (commandHistory.current) {
                commandHistory.current.execute(new DeleteNodeCommand(selectedNode.id));
              }
              setNodes((nds) => nds.filter((n) => n.id !== selectedNode.id));
              setEdges((eds) => eds.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id));
              setSelectedNode(null);
            }
          }}
          onApply={handleSave}
        />
      </div>
    </div>
  );
}
