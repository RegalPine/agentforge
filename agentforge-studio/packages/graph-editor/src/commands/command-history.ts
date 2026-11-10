import type { GraphDefinition } from "@agentforge/model";
import type { GraphCommand } from "./graph-commands.js";

/**
 * CommandHistory - manages undo/redo stack for graph editing.
 *
 * Operates on GraphDefinition model commands, not DOM operations.
 */
export class CommandHistory {
  private readonly undoStack: GraphCommand[] = [];
  private readonly redoStack: GraphCommand[] = [];
  private currentGraph: GraphDefinition;
  private readonly listeners: Array<(graph: GraphDefinition) => void> = [];

  constructor(initialGraph: GraphDefinition) {
    this.currentGraph = initialGraph;
  }

  /**
   * Execute a command and push to undo stack.
   * Clears redo stack (new branch).
   */
  execute(command: GraphCommand): GraphDefinition {
    this.currentGraph = command.execute(this.currentGraph);
    this.undoStack.push(command);
    this.redoStack.length = 0;
    this.notify();
    return this.currentGraph;
  }

  /**
   * Undo the last command.
   */
  undo(): GraphDefinition | null {
    const command = this.undoStack.pop();
    if (!command) return null;

    this.currentGraph = command.undo(this.currentGraph);
    this.redoStack.push(command);
    this.notify();
    return this.currentGraph;
  }

  /**
   * Redo the last undone command.
   */
  redo(): GraphDefinition | null {
    const command = this.redoStack.pop();
    if (!command) return null;

    this.currentGraph = command.execute(this.currentGraph);
    this.undoStack.push(command);
    this.notify();
    return this.currentGraph;
  }

  canUndo(): boolean {
    return this.undoStack.length > 0;
  }

  canRedo(): boolean {
    return this.redoStack.length > 0;
  }

  getGraph(): GraphDefinition {
    return this.currentGraph;
  }

  /**
   * Replace the current graph without creating an undo entry.
   * Used for loading from server or import.
   */
  setGraph(graph: GraphDefinition): void {
    this.currentGraph = graph;
    this.undoStack.length = 0;
    this.redoStack.length = 0;
    this.notify();
  }

  onChange(listener: (graph: GraphDefinition) => void): () => void {
    this.listeners.push(listener);
    return () => {
      const idx = this.listeners.indexOf(listener);
      if (idx >= 0) this.listeners.splice(idx, 1);
    };
  }

  private notify(): void {
    for (const listener of this.listeners) {
      listener(this.currentGraph);
    }
  }
}
