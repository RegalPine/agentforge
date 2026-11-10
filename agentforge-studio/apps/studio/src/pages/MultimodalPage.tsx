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
 * MultimodalPage - Multimodal Content Workspace.
 * AF-ENHANCE-11 §3: ContentPart / MultimodalContent / Realtime.
 *
 * Layout:
 * Content → Image Detail → Realtime Sessions → Events
 */

type MultimodalTab =
  | "content"
  | "images"
  | "realtime"
  | "events";

export function MultimodalPage() {
  const [activeTab, setActiveTab] = useState<MultimodalTab>("content");

  const tabs: { id: MultimodalTab; label: string }[] = [
    { id: "content", label: "Content" },
    { id: "images", label: "Images" },
    { id: "realtime", label: "Realtime" },
    { id: "events", label: "Events" },
  ];

  return (
    <Box className="af-page">
      <Box className="af-page__header">
        <Typography variant="h1" gutterBottom>Multimodal</Typography>
      </Box>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as MultimodalTab)}>
        <TabsList>
          {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
      </Tabs>

      <Box className="af-page__content">
        {activeTab === "content" && <ContentPanel />}
        {activeTab === "images" && <ImagesPanel />}
        {activeTab === "realtime" && <RealtimePanel />}
        {activeTab === "events" && <EventsPanel />}
      </Box>
    </Box>
  );
}

function ContentPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Multimodal Content</Typography>
      <Typography color="muted">Aggregated content parts: text, image, audio, video, JSON, binary, file.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Content ID</TableHead>
              <TableHead>Parts</TableHead>
              <TableHead>Total Size</TableHead>
              <TableHead>Empty</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4}>
                No multimodal content available.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function ImagesPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Image Details</Typography>
      <Typography color="muted">Image content with dimensions, format, and metadata.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Reference</TableHead>
              <TableHead>Width</TableHead>
              <TableHead>Height</TableHead>
              <TableHead>Format</TableHead>
              <TableHead>MIME Type</TableHead>
              <TableHead>Size</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No image content available.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function RealtimePanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Realtime Sessions</Typography>
      <Typography color="muted">SSE / WebSocket / Long Poll / gRPC streaming sessions.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Session ID</TableHead>
              <TableHead>Transport</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Events</TableHead>
              <TableHead>Connected</TableHead>
              <TableHead>Disconnected</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6}>
                No realtime sessions active.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

function EventsPanel() {
  return (
    <Box>
      <Typography variant="h2" gutterBottom>Realtime Events</Typography>
      <Typography color="muted">Events streamed through realtime sessions.</Typography>
      <Box>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Type</TableHead>
              <TableHead>Sequence</TableHead>
              <TableHead>Payload</TableHead>
              <TableHead>Timestamp</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4}>
                No realtime events received.
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}
