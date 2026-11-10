import { useState } from "react";

import {
  Box,
  Button,
  Input,
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
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
 * VersionDiffPage - AF-STUDIO-01 §45-48
 *
 * Version Management: Draft → Validated → Tested → Evaluated → Approved → Released → Deprecated
 * Release Gate: Definition Validation → Unit Test → Evaluation → Security Test → Governance → Approval → Release
 * Diff Viewer: Agent / Graph / Capability / Policy / Prompt / Schema Diff
 * Graph Diff: Added Node / Removed Node / Changed Edge / Changed Route / Changed Capability / Changed Configuration
 */

type VdTab = "versions" | "diff" | "release";

export function VersionDiffPage() {
  const [activeTab, setActiveTab] = useState<VdTab>("versions");
  const [entityType, setEntityType] = useState<string>("GRAPH");

  const tabs: { key: VdTab; label: string }[] = [
    { key: "versions", label: "Versions" },
    { key: "diff", label: "Diff Viewer" },
    { key: "release", label: "Release Gate" },
  ];

  const entityTypes = ["AGENT", "GRAPH", "CAPABILITY", "POLICY", "PROMPT", "SCHEMA"];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Version & Diff</Typography>
        <Typography color="muted">
          Version lifecycle, diff comparison, and release gate status
        </Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as VdTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.key} value={tab.key}>
            {tab.label}
          </TabsTrigger>
        ))}
        </TabsList>
      </Tabs>

      <Box className="af-page__body">
        {activeTab === "versions" && <VersionListPanel />}
        {activeTab === "diff" && (
          <DiffPanel entityType={entityType} onEntityTypeChange={setEntityType} entityTypes={entityTypes} />
        )}
        {activeTab === "release" && <ReleaseGatePanel />}
      </Box>
    </Box>
  );
}

function VersionListPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Version Management</Typography>
      <Box className="af-version-lifecycle">
        <Typography>DRAFT</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography>VALIDATED</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography>TESTED</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography>EVALUATED</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography>APPROVED</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography>RELEASED</Typography>
        <Typography className="af-arrow">→</Typography>
        <Typography color="muted">DEPRECATED</Typography>
      </Box>
      <Typography color="muted" className="mt-2">Agent / Graph / Capability versions all follow this lifecycle</Typography>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Entity Type</TableHead>
            <TableHead>Entity ID</TableHead>
            <TableHead>Version</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Created</TableHead>
            <TableHead>Updated</TableHead>
            <TableHead>Changelog</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell colSpan={7}>Versions from GET /api/v1/versions/{'{entityType}'}/{'{entityId}'}</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </Box>
  );
}

function DiffPanel({
  entityType,
  onEntityTypeChange,
  entityTypes,
}: {
  entityType: string;
  onEntityTypeChange: (t: string) => void;
  entityTypes: string[];
}) {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Diff Viewer</Typography>
      <Box className="af-form af-form--inline">
        <Typography component="label" variant="body2">
          Entity Type
          <Select value={entityType} onValueChange={onEntityTypeChange}>
            <SelectTrigger><SelectValue placeholder="Select type" /></SelectTrigger>
            <SelectContent>
              {entityTypes.map((t) => (
                <SelectItem key={t} value={t}>{t}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Typography>
        <Typography component="label" variant="body2">
          Entity ID
          <Input placeholder="entity-id" />
        </Typography>
        <Typography component="label" variant="body2">
          Left Version
          <Input placeholder="v1.0" />
        </Typography>
        <Typography component="label" variant="body2">
          Right Version
          <Input placeholder="v1.1" />
        </Typography>
        <Button>Compare</Button>
      </Box>

      <Box className="af-diff-result">
        <Typography variant="h3" gutterBottom>Diff Summary</Typography>
        <Box className="af-diff-summary">
          <Typography>Added: -</Typography>
          <Typography color="destructive">Removed: -</Typography>
          <Typography color="muted">Modified: -</Typography>
          <Typography>Unchanged: -</Typography>
        </Box>

        <Typography variant="h3" gutterBottom>Changes</Typography>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Path</TableHead>
              <TableHead>Change</TableHead>
              <TableHead>Left Value</TableHead>
              <TableHead>Right Value</TableHead>
              <TableHead>Description</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={5}>Diff results from GET /api/v1/diff/{'{entityId}'}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>

      {entityType === "GRAPH" && (
        <Box className="af-graph-diff">
          <Typography variant="h3" gutterBottom>Graph Diff</Typography>
          <Box className="af-graph-diff__panels">
            <Box className="af-graph-diff__panel">
              <Typography variant="h4" gutterBottom>Left Version</Typography>
              <Typography color="muted">Graph visualization</Typography>
            </Box>
            <Box className="af-graph-diff__panel">
              <Typography variant="h4" gutterBottom>Right Version</Typography>
              <Typography color="muted">Graph visualization</Typography>
            </Box>
          </Box>
          <Typography color="muted" className="mt-2">
            Studio does NOT directly modify Execution Plan
          </Typography>
        </Box>
      )}
    </Box>
  );
}

function ReleaseGatePanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Release Gate</Typography>
      <Box className="af-pipeline">
        <Box className="af-pipeline__stage">
          <Typography>Definition Validation</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Unit Test</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Evaluation</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Security Test</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Governance</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Approval</Typography>
        </Box>
        <Box className="af-pipeline__stage">
          <Typography>Release</Typography>
        </Box>
      </Box>
      <Typography color="muted" className="mt-2">
        Studio is only the entry point for the release flow. Actual gates are executed by Evaluation / Governance systems.
      </Typography>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Gate</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Message</TableHead>
            <TableHead>Completed</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell colSpan={4}>Release gate status from GET /api/v1/versions/{'{entityType}'}/{'{entityId}'}/release-gate</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </Box>
  );
}
