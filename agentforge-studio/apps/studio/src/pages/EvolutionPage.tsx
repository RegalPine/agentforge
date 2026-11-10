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
 * EvolutionPage - Evolution Workspace.
 * AF-ENHANCE-12 §4: EvolutionCandidate / Validation / Lineage.
 *
 * Layout:
 * Candidates → Changes → Evidence → Problems → Validation → Lineage → Policy
 */

type EvolutionTab =
  | "candidates"
  | "changes"
  | "evidence"
  | "problems"
  | "validation"
  | "lineage"
  | "policy";

export function EvolutionPage() {
  const [activeTab, setActiveTab] = useState<EvolutionTab>("candidates");

  const tabs: { id: EvolutionTab; label: string }[] = [
    { id: "candidates", label: "Candidates" },
    { id: "changes", label: "Changes" },
    { id: "evidence", label: "Evidence" },
    { id: "problems", label: "Problems" },
    { id: "validation", label: "Validation" },
    { id: "lineage", label: "Lineage" },
    { id: "policy", label: "Policy" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Evolution</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as EvolutionTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "candidates" && <CandidatesPanel />}
        {activeTab === "changes" && <ChangesPanel />}
        {activeTab === "evidence" && <EvidencePanel />}
        {activeTab === "problems" && <ProblemsPanel />}
        {activeTab === "validation" && <ValidationPanel />}
        {activeTab === "lineage" && <LineagePanel />}
        {activeTab === "policy" && <PolicyPanel />}
      </Box>
    </Box>
  );
}

function CandidatesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Evolution Candidates</Typography>
      <Typography color="muted">Candidates proposed for agent evolution with scoring.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Candidate ID</TableHead>
              <TableHead>Agent</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Changes</TableHead>
              <TableHead>Evidence</TableHead>
              <TableHead>Score</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={8}>
                No evolution candidates.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ChangesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Changes</Typography>
      <Typography color="muted">Scoped changes within evolution candidates.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Change ID</TableHead>
              <TableHead>Candidate</TableHead>
              <TableHead>Scope</TableHead>
              <TableHead>Target</TableHead>
              <TableHead>Description</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={5}>
                No changes recorded.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function EvidencePanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Evidence</Typography>
      <Typography color="muted">Evidence gathered to support or reject candidates.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Evidence ID</TableHead>
              <TableHead>Candidate</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Source</TableHead>
              <TableHead>Value</TableHead>
              <TableHead>Collected</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No evidence collected.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ProblemsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Problems</Typography>
      <Typography color="muted">Problems detected during evolution validation.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Problem ID</TableHead>
              <TableHead>Candidate</TableHead>
              <TableHead>Category</TableHead>
              <TableHead>Severity</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Detected</TableHead>
              <TableHead>Resolved</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No problems detected.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ValidationPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Validation Results</Typography>
      <Typography color="muted">Validation gate results for evolution candidates.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Result ID</TableHead>
              <TableHead>Candidate</TableHead>
              <TableHead>Gate</TableHead>
              <TableHead>Passed</TableHead>
              <TableHead>Score</TableHead>
              <TableHead>Details</TableHead>
              <TableHead>Validated</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No validation results.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function LineagePanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Evolution Lineage</Typography>
      <Typography color="muted">Candidate lineage tree showing evolution generations.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Generation</TableHead>
              <TableHead>Candidate ID</TableHead>
              <TableHead>Parent</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Score</TableHead>
              <TableHead>Changes</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No lineage data available.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function PolicyPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Evolution Policies</Typography>
      <Typography color="muted">Configuration for evolution behavior and constraints.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Policy ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Max Candidates</TableHead>
              <TableHead>Min Score</TableHead>
              <TableHead>Require Evidence</TableHead>
              <TableHead>Auto Approve</TableHead>
              <TableHead>Enabled</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={7}>
                No evolution policies configured.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}
