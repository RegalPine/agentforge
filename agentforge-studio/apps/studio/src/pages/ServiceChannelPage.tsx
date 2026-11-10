import { useState } from "react";

import {
  Box,
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
 * ServiceChannelPage - Service Channel Workspace.
 * AF-ENHANCE-11 §3: AgentService / Session / Conversation.
 *
 * Layout:
 * Services → Sessions → Conversations → Health
 */

type ServiceTab =
  | "services"
  | "sessions"
  | "conversations"
  | "health";

export function ServiceChannelPage() {
  const [activeTab, setActiveTab] = useState<ServiceTab>("services");

  const tabs: { id: ServiceTab; label: string }[] = [
    { id: "services", label: "Services" },
    { id: "sessions", label: "Sessions" },
    { id: "conversations", label: "Conversations" },
    { id: "health", label: "Health" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Service Channel</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as ServiceTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "services" && <ServicesPanel />}
        {activeTab === "sessions" && <SessionsPanel />}
        {activeTab === "conversations" && <ConversationsPanel />}
        {activeTab === "health" && <HealthPanel />}
      </Box>
    </Box>
  );
}

function ServicesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Agent Services</Typography>
      <Typography color="muted">Registered agent services with namespace/name/version identity.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Service ID</TableHead>
              <TableHead>Namespace</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Version</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Sessions</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No agent services registered.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function SessionsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Sessions</Typography>
      <Typography color="muted">Active sessions: create → activate → close lifecycle.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Session ID</TableHead>
              <TableHead>Tenant</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Conversations</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Activated</TableHead>
              <TableHead>Closed</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No active sessions.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ConversationsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Conversations</Typography>
      <Typography color="muted">Conversation messages with role-based threading.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Conversation ID</TableHead>
              <TableHead>Session</TableHead>
              <TableHead>Messages</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4}>
                No conversations recorded.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function HealthPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Service Health</Typography>
      <Box className="af-metrics-grid">
        <Box className="af-metric af-metric--success">
          <Typography variant="caption">Active Services</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Active Sessions</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Total Requests</Typography>
          <Typography variant="body1">--</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Avg Latency</Typography>
          <Typography variant="body1">-- ms</Typography>
        </Box>
        <Box className="af-metric af-metric--danger">
          <Typography variant="caption">Error Rate</Typography>
          <Typography variant="body1">--%</Typography>
        </Box>
      </Box>
    </Box>
  );
}
