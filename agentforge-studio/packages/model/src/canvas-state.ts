/**
 * CanvasState - UI-only state that must NOT pollute GraphDefinition.
 * Positions, viewport, selection are canvas concerns, not graph semantics.
 */
export interface CanvasState {
  positions: Record<string, { x: number; y: number }>;
  viewport: {
    x: number;
    y: number;
    zoom: number;
  };
}

export function createDefaultCanvasState(): CanvasState {
  return {
    positions: {},
    viewport: { x: 0, y: 0, zoom: 1 },
  };
}
