import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  Box,
  Typography,
  Button,
  Flex,
  Input,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Badge,
} from "@agentforge/ui";

interface GraphDef {
  id: string;
  version: string;
  name: string;
  status: string;
  nodeCount: number;
  edgeCount: number;
  createdAt: string;
}

export function GraphListPage() {
  const api = useApiClient();
  const navigate = useNavigate();
  const [graphs, setGraphs] = useState<GraphDef[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchGraphs = useCallback(async () => {
    setLoading(true);
    try {
      setGraphs(await api.request<GraphDef[]>("GET", "/api/v1/graphs"));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { fetchGraphs(); }, [fetchGraphs]);

  const createGraph = async () => {
    if (!newName.trim()) return;
    setCreating(true);
    try {
      await api.request("POST", "/api/v1/graphs", { name: newName.trim(), version: "1.0.0", nodes: [], edges: [] });
      setShowCreate(false);
      setNewName("");
      await fetchGraphs();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "创建失败");
    } finally {
      setCreating(false);
    }
  };

  const deleteGraph = async (id: string, version: string) => {
    if (!confirm(`Delete graph "${id}"?`)) return;
    try {
      await api.deleteGraph(id, version);
      await fetchGraphs();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "删除失败");
    }
  };

  const statusColor = (s: string) => {
    if (s === "PUBLISHED") return "default";
    if (s === "DRAFT") return "secondary";
    return "outline";
  };

  return (
    <Box className="af-page">
      {error && <p role="alert">{error}</p>}
      <Flex justify="between" align="center" className="af-page__header">
        <Typography variant="h1">Graphs</Typography>
        <Flex gap="sm">
          <Button variant="secondary" onClick={fetchGraphs}>Refresh</Button>
          <Button onClick={() => setShowCreate(!showCreate)}>
            {showCreate ? "Cancel" : "New Graph"}
          </Button>
        </Flex>
      </Flex>

      {showCreate && (
        <Flex gap="sm" align="end" className="mb-4 p-3 border rounded-md">
          <Box className="flex-1">
            <Typography variant="body2" component="label">Graph Name</Typography>
            <Input
              placeholder="Enter graph name..."
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && createGraph()}
            />
          </Box>
          <Button onClick={createGraph} disabled={creating || !newName.trim()}>
            {creating ? "Creating..." : "Create"}
          </Button>
        </Flex>
      )}

      <Box className="af-page__body">
        {loading ? (
          <Typography color="muted">Loading graphs...</Typography>
        ) : graphs.length === 0 ? (
          <Typography color="muted">No graphs found. Create one to get started.</Typography>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Nodes</TableHead>
                <TableHead>Edges</TableHead>
                <TableHead>Created</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {graphs.map((g) => (
                <TableRow key={`${g.id}@${g.version}`}>
                  <TableCell>
                    <Button variant="ghost" onClick={() => navigate(`/graphs/${encodeURIComponent(g.id)}/${encodeURIComponent(g.version)}/edit`)}>
                      {g.id}
                    </Button>
                  </TableCell>
                  <TableCell>{g.name}</TableCell>
                  <TableCell>{g.version}</TableCell>
                  <TableCell>
                    <Badge variant={statusColor(g.status)}>{g.status}</Badge>
                  </TableCell>
                  <TableCell>{g.nodeCount}</TableCell>
                  <TableCell>{g.edgeCount}</TableCell>
                  <TableCell>{g.createdAt ? new Date(g.createdAt).toLocaleDateString() : "-"}</TableCell>
                  <TableCell>
                    <Flex gap="sm">
                      <Button variant="secondary" size="sm" onClick={() => navigate(`/graphs/${encodeURIComponent(g.id)}/${encodeURIComponent(g.version)}/edit`)}>
                        Edit
                      </Button>
                      <Button variant="destructive" size="sm" onClick={() => deleteGraph(g.id, g.version)}>
                        Delete
                      </Button>
                    </Flex>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Box>
    </Box>
  );
}
