import { useState } from "react";

import {
  Box,
  Button,
  Label,
  RadioGroup,
  RadioGroupItem,
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

import { useParams } from "react-router-dom";

/**
 * CheckpointPage - AF-STUDIO-01 §43-44
 *
 * Checkpoint Viewer:
 *   Checkpoint → ID / Execution / Sequence / State Snapshot /
 *   Execution Snapshot / Graph Version / Plan Version / Checksum / Timestamp
 *   Supports: Inspect / Compare / Recover
 *
 * Recovery UI:
 *   Execution Failed → Latest Checkpoint → Inspect → Recovery Decision → Resume
 *
 * Also includes State Conflict Viewer (§29)
 */

type CpTab = "checkpoints" | "recovery" | "conflict";

export function CheckpointPage() {
  const { executionId } = useParams<{ executionId: string }>();
  const [activeTab, setActiveTab] = useState<CpTab>("checkpoints");

  const tabs: { key: CpTab; label: string }[] = [
    { key: "checkpoints", label: "Checkpoints" },
    { key: "recovery", label: "Recovery" },
    { key: "conflict", label: "State Conflict" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Checkpoint & Recovery</Typography>
        <Typography color="muted">
          {executionId ? `Execution: ${executionId}` : "Checkpoint inspection and recovery"}
        </Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as CpTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.key} value={tab.key}>
            {tab.label}
          </TabsTrigger>
        ))}
        </TabsList>
      </Tabs>

      <Box className="af-page__body">
        {activeTab === "checkpoints" && <CheckpointListPanel executionId={executionId} />}
        {activeTab === "recovery" && <RecoveryPanel executionId={executionId} />}
        {activeTab === "conflict" && <StateConflictPanel executionId={executionId} />}
      </Box>
    </Box>
  );
}

function CheckpointListPanel({ executionId }: { executionId?: string }) {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Checkpoints</Typography>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Checkpoint ID</TableHead>
            <TableHead>Sequence</TableHead>
            <TableHead>Graph Version</TableHead>
            <TableHead>Plan Version</TableHead>
            <TableHead>Checksum</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Created</TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell colSpan={8}>
              {executionId
                ? `Checkpoints from GET /api/v1/executions/${executionId}/checkpoints`
                : "Select an execution to view checkpoints"}
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>

      <Box>
        <Typography variant="h3" gutterBottom>Checkpoint Detail</Typography>
        <Table>
          <TableBody>
            <TableRow><TableCell className="font-medium">Checkpoint ID</TableCell><TableCell>-</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Execution</TableCell><TableCell>-</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Sequence</TableCell><TableCell>-</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">State Snapshot</TableCell><TableCell>(inspectable)</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Execution Snapshot</TableCell><TableCell>(inspectable)</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Graph Version</TableCell><TableCell>-</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Plan Version</TableCell><TableCell>-</TableCell></TableRow>
            <TableRow><TableCell className="font-medium">Checksum</TableCell><TableCell>-</TableCell></TableRow>
          </TableBody>
        </Table>
        <Box>
          <Button variant="secondary">Inspect</Button>
          <Button variant="secondary">Compare</Button>
          <Button>Recover</Button>
        </Box>
      </Box>
    </Box>
  );
}

function RecoveryPanel({ executionId }: { executionId?: string }) {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Recovery</Typography>
      <Typography color="muted" className="mt-2">
        Recovery Policy is still decided by Runtime / Governance, not Studio.
      </Typography>
      <Table>
        <TableBody>
          <TableRow><TableCell className="font-medium">Execution</TableCell><TableCell>{executionId || "-"}</TableCell></TableRow>
          <TableRow><TableCell className="font-medium">Failed At</TableCell><TableCell>-</TableCell></TableRow>
          <TableRow><TableCell className="font-medium">Failure Reason</TableCell><TableCell>-</TableCell></TableRow>
          <TableRow><TableCell className="font-medium">Latest Checkpoint</TableCell><TableCell>-</TableCell></TableRow>
          <TableRow><TableCell className="font-medium">Checkpoint Sequence</TableCell><TableCell>-</TableCell></TableRow>
          <TableRow><TableCell className="font-medium">Recommended Decision</TableCell><TableCell>-</TableCell></TableRow>
        </TableBody>
      </Table>

      <Typography variant="h3" gutterBottom>Available Recovery Decisions</Typography>
      <RadioGroup defaultValue="resume" className="af-recovery-decisions">
        <Box className="af-recovery-option">
          <RadioGroupItem value="resume" id="resume" />
          <Label htmlFor="resume">
            <Typography component="span" variant="body2" className="font-semibold">RESUME</Typography> - Continue from latest checkpoint
          </Label>
        </Box>
        <Box className="af-recovery-option">
          <RadioGroupItem value="retry" id="retry" />
          <Label htmlFor="retry">
            <Typography component="span" variant="body2" className="font-semibold">RETRY_FROM_CHECKPOINT</Typography> - Retry from a specific checkpoint
          </Label>
        </Box>
        <Box className="af-recovery-option">
          <RadioGroupItem value="restart" id="restart" />
          <Label htmlFor="restart">
            <Typography component="span" variant="body2" className="font-semibold">RESTART</Typography> - Start execution from beginning
          </Label>
        </Box>
        <Box className="af-recovery-option">
          <RadioGroupItem value="abort" id="abort" />
          <Label htmlFor="abort">
            <Typography component="span" variant="body2" className="font-semibold">ABORT</Typography> - Terminate execution
          </Label>
        </Box>
      </RadioGroup>

      <Button>Execute Recovery</Button>
      <Typography color="muted" className="mt-2">Data from GET /api/v1/executions/{executionId}/recovery</Typography>
    </Box>
  );
}

function StateConflictPanel({ executionId }: { executionId?: string }) {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>State Conflict Viewer</Typography>
      <Typography color="muted" className="mt-2">
        Parallel execution may cause state conflicts. Studio shows the conflict, strategy, and resolution.
      </Typography>
      <Box className="af-conflict-viewer">
        <Box className="af-conflict-node">
          <Typography variant="h4" gutterBottom>Node A</Typography>
          <Typography color="muted">State Delta A</Typography>
        </Box>
        <Box className="af-conflict-node">
          <Typography variant="h4" gutterBottom>Node B</Typography>
          <Typography color="muted">State Delta B</Typography>
        </Box>
        <Box className="af-conflict-resolver">
          <Typography variant="h4" gutterBottom>Conflict Resolver</Typography>
          <Table>
            <TableBody>
              <TableRow><TableCell className="font-medium">Strategy</TableCell><TableCell>-</TableCell></TableRow>
              <TableRow><TableCell className="font-medium">Resolution</TableCell><TableCell>-</TableCell></TableRow>
              <TableRow><TableCell className="font-medium">Committed State</TableCell><TableCell>-</TableCell></TableRow>
            </TableBody>
          </Table>
        </Box>
      </Box>
      <Typography color="muted" className="mt-2">Data from GET /api/v1/executions/{executionId}/state-conflict</Typography>
    </Box>
  );
}
