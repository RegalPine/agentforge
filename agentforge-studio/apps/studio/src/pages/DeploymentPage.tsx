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
 * DeploymentPage - Control Plane / Deployment Workspace.
 * AF-ENHANCE-12 §2: Deployment / Environment / Release.
 *
 * Layout:
 * Deployments → Instances → Environments → Releases → Quotas
 */

type DeploymentTab =
  | "deployments"
  | "instances"
  | "environments"
  | "releases"
  | "quotas";

export function DeploymentPage() {
  const [activeTab, setActiveTab] = useState<DeploymentTab>("deployments");

  const tabs: { id: DeploymentTab; label: string }[] = [
    { id: "deployments", label: "Deployments" },
    { id: "instances", label: "Instances" },
    { id: "environments", label: "Environments" },
    { id: "releases", label: "Releases" },
    { id: "quotas", label: "Quotas" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Control Plane</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as DeploymentTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "deployments" && <DeploymentsPanel />}
        {activeTab === "instances" && <InstancesPanel />}
        {activeTab === "environments" && <EnvironmentsPanel />}
        {activeTab === "releases" && <ReleasesPanel />}
        {activeTab === "quotas" && <QuotasPanel />}
      </Box>
    </Box>
  );
}

function DeploymentsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Deployments</Typography>
      <Typography color="muted">Namespace/name deployment with strategy-based rollout.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Deployment ID</TableHead>
              <TableHead>Namespace</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Strategy</TableHead>
              <TableHead>Instances</TableHead>
              <TableHead>Ready</TableHead>
              <TableHead>Environment</TableHead>
              <TableHead>Updated</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={9}>
                No deployments configured.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function InstancesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Runtime Instances</Typography>
      <Typography color="muted">Individual runtime instance status and resource usage.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Instance ID</TableHead>
              <TableHead>Deployment</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Endpoint</TableHead>
              <TableHead>Region</TableHead>
              <TableHead>CPU</TableHead>
              <TableHead>Memory</TableHead>
              <TableHead>Threads</TableHead>
              <TableHead>Started</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={9}>
                No runtime instances running.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function EnvironmentsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Environments</Typography>
      <Typography color="muted">Deployment environments: Development / Staging / Production / Test.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Environment ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Deployments</TableHead>
              <TableHead>Instances</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No environments configured.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ReleasesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Releases</Typography>
      <Typography color="muted">Release rollout status with canary/monitoring phases.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Release ID</TableHead>
              <TableHead>Deployment</TableHead>
              <TableHead>Version</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Rollout %</TableHead>
              <TableHead>Started</TableHead>
              <TableHead>Completed</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No releases in progress.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function QuotasPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Resource Quotas</Typography>
      <Box className="af-metrics-grid">
        <Box className="af-metric">
          <Typography variant="caption">Instances (used/max)</Typography>
          <Typography variant="body1">-- / --</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">CPU (used/max)</Typography>
          <Typography variant="body1">-- / --</Typography>
        </Box>
        <Box className="af-metric">
          <Typography variant="caption">Memory (used/max)</Typography>
          <Typography variant="body1">-- / -- MB</Typography>
        </Box>
      </Box>
    </Box>
  );
}
