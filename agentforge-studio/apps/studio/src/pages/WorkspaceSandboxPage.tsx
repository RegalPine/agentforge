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
 * WorkspaceSandboxPage - Workspace & Sandbox Workspace.
 * AF-ENHANCE-07: Workspace / Sandbox / Artifact.
 *
 * Layout:
 * Workspaces → Sandboxes → Artifacts
 */

type WsTab = "workspaces" | "sandboxes" | "artifacts";

export function WorkspaceSandboxPage() {
  const [activeTab, setActiveTab] = useState<WsTab>("workspaces");

  const tabs: { id: WsTab; label: string }[] = [
    { id: "workspaces", label: "Workspaces" },
    { id: "sandboxes", label: "Sandboxes" },
    { id: "artifacts", label: "Artifacts" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Workspace & Sandbox</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as WsTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "workspaces" && <WorkspacesPanel />}
        {activeTab === "sandboxes" && <SandboxesPanel />}
        {activeTab === "artifacts" && <ArtifactsPanel />}
      </Box>
    </Box>
  );
}

function WorkspacesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Workspaces</Typography>
      <Typography color="muted">Isolated workspaces scoped to agent/graph/session/tenant/global.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Workspace ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Scope</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Sandboxes</TableHead>
              <TableHead>Artifacts</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Updated</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={8}>
                No workspaces created.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function SandboxesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Sandboxes</Typography>
      <Typography color="muted">Isolated execution sandboxes with resource limits.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Sandbox ID</TableHead>
              <TableHead>Workspace</TableHead>
              <TableHead>Isolation</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>CPU Limit</TableHead>
              <TableHead>Memory Limit</TableHead>
              <TableHead>Timeout</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Expires</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={9}>
                No sandboxes running.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ArtifactsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Artifacts</Typography>
      <Typography color="muted">Files, data, model outputs, logs, and checkpoints within workspaces.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Artifact ID</TableHead>
              <TableHead>Workspace</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Size</TableHead>
              <TableHead>Checksum</TableHead>
              <TableHead>Created By</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={8}>
                No artifacts stored.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}
