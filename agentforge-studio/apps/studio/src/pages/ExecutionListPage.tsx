import { useState, useEffect, useCallback } from "react";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  Box,
  Typography,
  Button,
  Flex,
  Badge,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@agentforge/ui";

interface Execution {
  id: string;
  graphId: string;
  graphVersion: string;
  status: string;
  createdAt: string;
  startedAt: string;
  completedAt: string;
}

export function ExecutionListPage() {
  const api = useApiClient();
  const [executions, setExecutions] = useState<Execution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchExecutions = useCallback(async () => {
    setLoading(true);
    try {
      setExecutions(await api.request<Execution[]>("GET", "/api/v1/executions"));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { fetchExecutions(); }, [fetchExecutions]);

  const statusVariant = (s: string) => {
    if (s === "COMPLETED") return "default";
    if (s === "RUNNING") return "secondary";
    if (s === "FAILED") return "destructive";
    if (s === "PAUSED") return "outline";
    if (s === "CANCELLED") return "outline";
    return "outline";
  };

  const controlExecution = async (id: string, action: string) => {
    try {
      await api.request("POST", `/api/v1/executions/${encodeURIComponent(id)}/${action}`, {});
      await fetchExecutions();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败");
    }
  };

  return (
    <Box className="af-page">
      {error && <p role="alert">{error}</p>}
      <Flex justify="between" align="center" className="af-page__header">
        <Typography variant="h1">Executions</Typography>
        <Button variant="secondary" onClick={fetchExecutions}>Refresh</Button>
      </Flex>
      <Box className="af-page__body">
        {loading ? (
          <Typography color="muted">Loading executions...</Typography>
        ) : executions.length === 0 ? (
          <Typography color="muted">No executions found.</Typography>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Graph</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Created</TableHead>
                <TableHead>Completed</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {executions.map((e) => (
                <TableRow key={e.id}>
                  <TableCell>{e.id}</TableCell>
                  <TableCell>{e.graphId}</TableCell>
                  <TableCell>{e.graphVersion}</TableCell>
                  <TableCell>
                    <Badge variant={statusVariant(e.status)}>{e.status}</Badge>
                  </TableCell>
                  <TableCell>{e.createdAt ? new Date(e.createdAt).toLocaleString() : "-"}</TableCell>
                  <TableCell>{e.completedAt ? new Date(e.completedAt).toLocaleString() : "-"}</TableCell>
                  <TableCell>
                    <Flex gap="sm">
                      {e.status === "RUNNING" && (
                        <Button size="sm" variant="secondary" onClick={() => controlExecution(e.id, "pause")}>Pause</Button>
                      )}
                      {e.status === "PAUSED" && (
                        <Button size="sm" variant="secondary" onClick={() => controlExecution(e.id, "resume")}>Resume</Button>
                      )}
                      {(e.status === "RUNNING" || e.status === "PAUSED") && (
                        <Button size="sm" variant="destructive" onClick={() => controlExecution(e.id, "cancel")}>Cancel</Button>
                      )}
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
