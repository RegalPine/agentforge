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

interface Agent {
  id: string;
  name: string;
  version: string;
  status: string;
  description: string;
  createdAt: string;
}

export function AgentListPage() {
  const api = useApiClient();
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAgents = useCallback(async () => {
    setLoading(true);
    try {
      setAgents(await api.request<Agent[]>("GET", "/api/v1/agents"));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { fetchAgents(); }, [fetchAgents]);

  return (
    <Box className="af-page">
      {error && <p role="alert">{error}</p>}
      <Flex justify="between" align="center" className="af-page__header">
        <Typography variant="h1">Agents</Typography>
        <Button variant="secondary" onClick={fetchAgents}>Refresh</Button>
      </Flex>
      <Box className="af-page__body">
        {loading ? (
          <Typography color="muted">Loading agents...</Typography>
        ) : agents.length === 0 ? (
          <Typography color="muted">No agents registered.</Typography>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Description</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {agents.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>{a.id}</TableCell>
                  <TableCell>{a.name}</TableCell>
                  <TableCell>{a.version}</TableCell>
                  <TableCell>
                    <Badge variant={a.status === "ACTIVE" ? "default" : "secondary"}>{a.status}</Badge>
                  </TableCell>
                  <TableCell>{a.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Box>
    </Box>
  );
}
