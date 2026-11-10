import { useState } from "react";

import {
  Box,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsList,
  TabsTrigger,
  Typography,
} from "@agentforge/ui";


/**
 * DebuggerPage - Debugger Workspace.
 * AF-ENHANCE-04: DebugSession / Breakpoint / ExecutionCursor.
 *
 * Layout:
 * Sessions → Breakpoints → Cursor → Events → Controls
 */

type DebuggerTab =
  | "sessions"
  | "breakpoints"
  | "cursor"
  | "events";

export function DebuggerPage() {
  const [activeTab, setActiveTab] = useState<DebuggerTab>("sessions");

  const tabs: { id: DebuggerTab; label: string }[] = [
    { id: "sessions", label: "Sessions" },
    { id: "breakpoints", label: "Breakpoints" },
    { id: "cursor", label: "Cursor" },
    { id: "events", label: "Events" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Debugger</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as DebuggerTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "sessions" && <SessionsPanel />}
        {activeTab === "breakpoints" && <BreakpointsPanel />}
        {activeTab === "cursor" && <CursorPanel />}
        {activeTab === "events" && <EventsPanel />}
      </Box>

      <Box className="af-page__footer">
        <Box>
          <Typography variant="h2" gutterBottom>Step Controls</Typography>
          <Box>
            <Button variant="secondary" disabled>Continue</Button>
            <Button variant="secondary" disabled>Step Over</Button>
            <Button variant="secondary" disabled>Step Into</Button>
            <Button variant="secondary" disabled>Step Out</Button>
            <Button variant="secondary" disabled>Pause</Button>
            <Button variant="destructive" disabled>Stop</Button>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

function SessionsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Debug Sessions</Typography>
      <Typography color="muted">Active debug sessions attached to executions.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Session ID</TableHead>
              <TableHead>Execution</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Breakpoints</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Attached</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No active debug sessions.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function BreakpointsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Breakpoints</Typography>
      <Typography color="muted">Node entry/exit, edge traversal, tool/model call, policy, error breakpoints.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Breakpoint ID</TableHead>
              <TableHead>Session</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Target</TableHead>
              <TableHead>Condition</TableHead>
              <TableHead>Enabled</TableHead>
              <TableHead>Hit Count</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No breakpoints set.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function CursorPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Execution Cursor</Typography>
      <Typography color="muted">Current execution position within the graph.</Typography>
      <Box className="af-metrics-grid">
        <Box className="af-metric">
          <Typography variant="caption">Event ID</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Sequence</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Cursor Type</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Node</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
      </Box>
    </Box>
  );
}

function EventsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Debug Events</Typography>
      <Typography color="muted">Events emitted during debugging: breakpoint hits, steps, state inspections.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Event ID</TableHead>
              <TableHead>Session</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Cursor Position</TableHead>
              <TableHead>Timestamp</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={5}>
                No debug events recorded.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}
