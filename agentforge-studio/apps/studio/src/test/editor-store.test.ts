import { describe, it, expect, beforeEach } from "vitest";
import { useEditorStore } from "../stores/editor-store.js";

describe("editor-store", () => {
  beforeEach(() => {
    // Reset to initial state
    useEditorStore.setState({
      selectedNodeId: null,
      selectedEdgeId: null,
      panelOpen: true,
      editorMode: "select",
    });
  });

  describe("initial state", () => {
    it("should have no selection", () => {
      const state = useEditorStore.getState();
      expect(state.selectedNodeId).toBeNull();
      expect(state.selectedEdgeId).toBeNull();
    });

    it("should have panel open by default", () => {
      expect(useEditorStore.getState().panelOpen).toBe(true);
    });

    it("should be in select mode by default", () => {
      expect(useEditorStore.getState().editorMode).toBe("select");
    });
  });

  describe("setSelectedNodeId", () => {
    it("should set selected node", () => {
      useEditorStore.getState().setSelectedNodeId("node-1");
      expect(useEditorStore.getState().selectedNodeId).toBe("node-1");
    });

    it("should clear edge selection when node is selected", () => {
      useEditorStore.setState({ selectedEdgeId: "edge-1" });
      useEditorStore.getState().setSelectedNodeId("node-1");

      expect(useEditorStore.getState().selectedNodeId).toBe("node-1");
      expect(useEditorStore.getState().selectedEdgeId).toBeNull();
    });

    it("should allow clearing node selection with null", () => {
      useEditorStore.setState({ selectedNodeId: "node-1" });
      useEditorStore.getState().setSelectedNodeId(null);
      expect(useEditorStore.getState().selectedNodeId).toBeNull();
    });
  });

  describe("setSelectedEdgeId", () => {
    it("should set selected edge", () => {
      useEditorStore.getState().setSelectedEdgeId("edge-1");
      expect(useEditorStore.getState().selectedEdgeId).toBe("edge-1");
    });

    it("should clear node selection when edge is selected", () => {
      useEditorStore.setState({ selectedNodeId: "node-1" });
      useEditorStore.getState().setSelectedEdgeId("edge-1");

      expect(useEditorStore.getState().selectedEdgeId).toBe("edge-1");
      expect(useEditorStore.getState().selectedNodeId).toBeNull();
    });
  });

  describe("setPanelOpen", () => {
    it("should toggle panel", () => {
      useEditorStore.getState().setPanelOpen(false);
      expect(useEditorStore.getState().panelOpen).toBe(false);

      useEditorStore.getState().setPanelOpen(true);
      expect(useEditorStore.getState().panelOpen).toBe(true);
    });
  });

  describe("setEditorMode", () => {
    it("should change editor mode", () => {
      useEditorStore.getState().setEditorMode("connect");
      expect(useEditorStore.getState().editorMode).toBe("connect");

      useEditorStore.getState().setEditorMode("pan");
      expect(useEditorStore.getState().editorMode).toBe("pan");

      useEditorStore.getState().setEditorMode("select");
      expect(useEditorStore.getState().editorMode).toBe("select");
    });
  });
});
