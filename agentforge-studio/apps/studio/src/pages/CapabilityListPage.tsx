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

interface Capability {
  id: string;
  name: string;
  type: string;
  version: string;
  status: string;
  provider: string;
}

export function CapabilityListPage() {
  const api = useApiClient();
  const [capabilities, setCapabilities] = useState<Capability[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCapabilities = useCallback(async () => {
    setLoading(true);
    try {
      setCapabilities(await api.request<Capability[]>("GET", "/api/v1/capabilities"));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { fetchCapabilities(); }, [fetchCapabilities]);

  return (
    <Box className="af-page">
      {error && <p role="alert">{error}</p>}
      <Flex justify="between" align="center" className="af-page__header">
        <Typography variant="h1">Capabilities</Typography>
        <Button variant="secondary" onClick={fetchCapabilities}>Refresh</Button>
      </Flex>
      <Box className="af-page__body">
        {loading ? (
          <Typography color="muted">Loading capabilities...</Typography>
        ) : capabilities.length === 0 ? (
          <Typography color="muted">No capabilities registered.</Typography>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Provider</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {capabilities.map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{c.id}</TableCell>
                  <TableCell>{c.name}</TableCell>
                  <TableCell><Badge variant="outline">{c.type}</Badge></TableCell>
                  <TableCell>{c.version}</TableCell>
                  <TableCell><Badge variant={c.status === "ACTIVE" ? "default" : "secondary"}>{c.status}</Badge></TableCell>
                  <TableCell>{c.provider}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Box>
    </Box>
  );
}
