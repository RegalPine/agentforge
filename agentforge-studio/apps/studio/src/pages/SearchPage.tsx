import { useState, useCallback } from "react";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  Box,
  Typography,
  Button,
  Flex,
  Input,
  Badge,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@agentforge/ui";

export function SearchPage() {
  const api = useApiClient();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const doSearch = useCallback(async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const data = await api.request<{ results: any[]; total: number }>("POST", "/api/v1/search", { query: query.trim() });
      setResults(data.results || []);
      setTotal(data.total || 0);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "搜索失败");
    } finally {
      setLoading(false);
    }
  }, [query, api]);

  return (
    <Box className="af-page">
      {error && <p role="alert">{error}</p>}
      <Box className="af-page__header">
        <Typography variant="h1">Global Search</Typography>
      </Box>

      <Flex gap="sm" className="af-search-bar mb-4">
        <Input
          className="flex-1"
          placeholder="Search agents, capabilities, graphs, skills..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && doSearch()}
        />
        <Button onClick={doSearch} disabled={loading}>
          {loading ? "Searching..." : "Search"}
        </Button>
      </Flex>

      <Box className="af-page__body">
        <Box className="af-search-results">
          <Typography variant="h2" gutterBottom>
            Results {total > 0 && `(${total})`}
          </Typography>
          {results.length === 0 && !loading && (
            <Typography color="muted">Enter a search query to find resources.</Typography>
          )}
          {results.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>ID</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Score</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {results.map((r: any, i: number) => (
                  <TableRow key={i}>
                    <TableCell><Badge variant="outline">{r.type}</Badge></TableCell>
                    <TableCell>{r.id}</TableCell>
                    <TableCell>{r.name}</TableCell>
                    <TableCell>{r.score}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Box>
      </Box>
    </Box>
  );
}
